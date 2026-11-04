package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The self-check: the clock against the spawns, run by run, and the prior it may seed. */
public class ClockTrustTest
{
	/** Checks from a string, oldest first: H a hit, M a miss, E a miss at the change of a minute. */
	private static List<Logbook.ClockCheck> checks(String s)
	{
		Logbook b = new Logbook();
		for (char c : s.toCharArray())
		{
			b.checkClock(9, 0, c == 'H' ? 9 : 13, c == 'E' ? 59 : 30, 0);
		}
		return b.clock;
	}

	private static ClockTrust.Level level(String s)
	{
		return ClockTrust.of(checks(s)).getLevel();
	}

	@Test
	public void levels()
	{
		assertEquals(ClockTrust.Level.UNPROVEN, level(""));
		assertEquals(ClockTrust.Level.UNPROVEN, level("HH"));
		assertEquals(ClockTrust.Level.TRUSTED, level("HHH"));
		assertEquals(ClockTrust.Level.SHAKY, level("HHHM"));
		assertEquals(ClockTrust.Level.SHAKY, level("HHMH"));
		assertEquals(ClockTrust.Level.TRUSTED, level("HHMHHH"));
		assertEquals(ClockTrust.Level.DOUBTFUL, level("HHHMM"));
		assertEquals(ClockTrust.Level.DOUBTFUL, level("MHMHH"));
		// Two misses still within the last five: not yet trusted again.
		assertEquals(ClockTrust.Level.DOUBTFUL, level("MMHHH"));
		assertEquals(ClockTrust.Level.TRUSTED, level("MMHHHH"));
		assertEquals(ClockTrust.Level.UNPROVEN, level("M"));
	}

	@Test
	public void onlyTrustedSeedsTheReader()
	{
		assertTrue(ClockTrust.of(checks("HHH")).trusted());
		assertFalse(ClockTrust.of(checks("HHHM")).trusted());
		assertFalse(ClockTrust.of(checks("HH")).trusted());
	}

	@Test
	public void saysSoHonestly()
	{
		ClockTrust t = ClockTrust.of(checks("HHHMM"));
		assertEquals(3, t.getHits());
		assertEquals(5, t.getChecks());
		assertTrue(t.describe(), t.describe().contains("missed 2 of your last 5"));
		assertTrue(t.describe().contains("spawns only"));
		ClockTrust edge = ClockTrust.of(checks("HHHEE"));
		assertEquals(2, edge.getEdgeMisses());
		assertTrue(edge.describe(), edge.describe().contains("go in mid-minute"));
		assertTrue(ClockTrust.of(checks("HHHH")).describe().contains("4 of your 4 runs right"));
	}

	@Test
	public void aSplitCallHitsEitherSide()
	{
		Logbook b = new Logbook();
		b.checkClock(9, 11, 11, 59, 0);
		assertTrue(b.clock.get(0).hit());
		b.checkClock(9, 11, 13, 59, 0);
		assertFalse(b.clock.get(1).hit());
	}

	@Test
	public void keepsTheLastThirtyAndRepairs()
	{
		Logbook b = new Logbook();
		for (int i = 0; i < 40; i++)
		{
			b.checkClock(1 + i % 15, 0, 1 + i % 15, i % 60, i);
		}
		assertEquals(Logbook.KEEP_CLOCK, b.clock.size());
		assertEquals(10, b.clock.get(0).at);

		Gson gson = new Gson();
		String json = "{\"clock\":[{\"said\":9,\"read\":9,\"second\":30},{\"said\":0,\"read\":4},null,{\"said\":3,\"read\":99},{\"said\":3,\"read\":3,\"second\":75}]}";
		Logbook read = Logbook.read(gson, json);
		assertEquals(2, read.clock.size());
		assertEquals(59, read.clock.get(1).second);
		// An older save has no checks at all.
		assertEquals(0, Logbook.read(gson, "{\"attempts\":3}").clock.size());
	}

	@Test
	public void theClockNamesTheRotationFromWaveOne()
	{
		RotationTracker t = new RotationTracker();
		t.prior(Collections.singletonList(9));
		assertEquals(0, t.rotation());
		assertEquals(9, t.likely());
		assertTrue(t.fromPrior());
		// Run start resets the reader but not the clock's word.
		t.reset();
		assertEquals(9, t.likely());
		t.observe(1, Rotations.spawns(9, 1));
		assertEquals(9, t.likely());
		assertTrue(t.fromPrior());
		t.observe(2, Rotations.spawns(9, 2));
		assertEquals(9, t.rotation());
		assertFalse(t.fromPrior());
	}

	@Test
	public void spawnsThatDisagreeAlwaysWin()
	{
		RotationTracker t = new RotationTracker();
		t.prior(Collections.singletonList(9));
		// Wave 1 in the centre: rotations 1, 2 or 3, never 9 (south).
		t.observe(1, Rotations.spawns(2, 1));
		assertTrue(t.priorContradicted());
		assertEquals(0, t.likely());
		assertEquals(Arrays.asList(1, 2, 3), t.candidates());
		t.observe(2, Rotations.spawns(2, 2));
		assertEquals(2, t.rotation());
		assertEquals(2, t.likely());
	}

	@Test
	public void aSplitCallIsSettledByWaveOne()
	{
		for (int m = 0; m < 16; m++)
		{
			int a = RotationClock.rotationAt(m);
			int b = RotationClock.rotationAt(m + 1);
			if (a == b)
			{
				continue;
			}
			for (int truth : new int[]{a, b})
			{
				RotationTracker t = new RotationTracker();
				t.prior(new ArrayList<>(Arrays.asList(a, b)));
				assertEquals(0, t.likely());
				t.observe(1, Rotations.spawns(truth, 1));
				assertEquals("minute " + m, truth, t.likely());
			}
		}
	}

	@Test
	public void picks()
	{
		List<Logbook.Run> runs = new ArrayList<>();
		runs.add(run(9, Logbook.Result.CAPE, 63, 4000));
		runs.add(run(5, Logbook.Result.CAPE, 63, 4500));
		runs.add(run(5, Logbook.Result.CAPE, 63, 4600));
		runs.add(run(5, Logbook.Result.CAPE, 63, 4700));
		runs.add(run(9, Logbook.Result.DIED, 40, 2000));
		runs.add(run(9, Logbook.Result.DIED, 41, 2000));
		RotationPicks p = RotationPicks.of(runs);
		assertEquals(9, p.getFastest());
		assertEquals(5, p.getSteadiest());
		assertTrue(p.favours(9));
		assertFalse(p.favours(1));
		assertTrue(RotationPicks.of(new ArrayList<>()).isEmpty());
	}

	private static Logbook.Run run(int rotation, Logbook.Result result, int wave, int ticks)
	{
		Logbook.Run r = new Logbook.Run();
		r.rotation = rotation;
		r.result = result;
		r.wave = wave;
		r.ticks = ticks;
		return r;
	}
}
