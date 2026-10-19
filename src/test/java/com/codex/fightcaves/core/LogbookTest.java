package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Runs picked up part way, missed wave calls, logging back in, and the damage each wave took. */
public class LogbookTest
{
	/** Plays waves from..to at a fixed number of ticks each, calling each wave as the game does. */
	private static void play(Logbook.Active a, int from, int to, int each)
	{
		for (int w = from; w <= to; w++)
		{
			a.ticks += each;
			Logbook.advance(a, w + 1);
		}
	}

	@Test
	public void runPickedUpPartWayKeepsItsSplitsOnTheirWaves()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(30, 0);
		assertTrue(a.partial);
		assertEquals(30, a.firstWave);
		assertEquals(29, a.splits.size());
		play(a, 30, 62, 50);
		a.ticks += 400;
		Logbook.advance(a, Waves.LAST + 1);
		a.wave = Waves.LAST;
		Logbook.Run run = book.finish(Logbook.Result.CAPE);
		assertEquals(Waves.LAST, run.splits.size());
		assertEquals(Integer.valueOf(0), run.splits.get(28));
		assertEquals(Integer.valueOf(50), run.splits.get(29));
		assertEquals(Integer.valueOf(400), run.splits.get(62));
		// A cape picked up at wave 30 never took its time, so it is no record and races nothing.
		assertEquals(0, book.bestTicks);
		assertEquals(1, book.capes);
		assertNull(Splits.personalBest(book.runs));
		assertEquals(0, Splits.span(run, 1, 7));
		assertEquals(7 * 50, Splits.span(run, 36, 42));
	}

	@Test
	public void wholeRunIsARecord()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		assertFalse(a.partial);
		play(a, 1, 62, 40);
		a.ticks += 300;
		Logbook.advance(a, Waves.LAST + 1);
		a.wave = Waves.LAST;
		book.finish(Logbook.Result.CAPE);
		assertEquals(62 * 40 + 300, book.bestTicks);
		assertEquals(book.runs.get(0), Splits.personalBest(book.runs));
	}

	@Test
	public void missedWaveCallLeavesAGapNotAShift()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		play(a, 1, 4, 30);
		// Wave 5 is on; the call for wave 6 is missed and the next heard is wave 7's.
		a.ticks += 70;
		assertFalse(Logbook.advance(a, 7));
		assertTrue(a.partial);
		assertEquals(Arrays.asList(30, 30, 30, 30, 0, 0), a.splits);
		a.ticks += 30;
		assertTrue(Logbook.advance(a, 8));
		assertEquals(Integer.valueOf(30), a.splits.get(6));
		assertEquals(7, a.splits.size());
	}

	@Test
	public void loggingBackInRestartsTheWaveClock()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		play(a, 1, 9, 30);
		a.ticks += 25;
		// Logged out mid-wave 10; the game calls wave 10 again on the way back in.
		assertFalse(Logbook.advance(a, 10));
		assertFalse(a.partial);
		assertEquals(9, a.splits.size());
		a.ticks += 40;
		Logbook.advance(a, 11);
		assertEquals(Integer.valueOf(40), a.splits.get(9));
	}

	@Test
	public void damageGoesToTheWaveOnNow()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		Logbook.hurt(a, 5);
		play(a, 1, 2, 30);
		Logbook.hurt(a, 12);
		Logbook.hurt(a, 0);
		Logbook.hurt(a, 3);
		assertEquals(Arrays.asList(5, 0, 15), a.hurt);
		Logbook.Run run = book.finish(Logbook.Result.DIED);
		assertEquals(Arrays.asList(5, 0, 15), run.hurt);
		assertEquals(1, book.deaths[3]);
	}

	@Test
	public void olderSavesAreRepaired()
	{
		Logbook book = new Logbook();
		Logbook.Run old = new Logbook.Run();
		old.splits = null;
		old.hurt = null;
		book.runs = Collections.singletonList(old);
		book.outlooks = null;
		book.repair();
		assertEquals(0, old.splits.size());
		assertEquals(0, old.hurt.size());
		assertEquals(0, book.outlooks.size());
		assertEquals(0, Damage.total(old));
	}

	@Test
	public void wavesWithoutAHitStillCountAsPlayed()
	{
		Logbook book = new Logbook();
		Logbook.Active hurtRun = book.begin(1, 0);
		Logbook.hurt(hurtRun, 14);
		play(hurtRun, 1, 1, 30);
		book.finish(Logbook.Result.DIED);
		// A clean run over the same two waves halves the average rather than being left out.
		Logbook.Active clean = book.begin(1, 0);
		play(clean, 1, 1, 30);
		book.finish(Logbook.Result.LEFT);
		assertEquals(14 / 4.0, Damage.perWaveByStretch(book.runs)[0], 1e-9);
	}

	@Test
	public void aDamagedSaveWithGapsInItsListsStillReads()
	{
		Logbook book = new Logbook();
		book.runs = new java.util.ArrayList<>();
		book.runs.add(null);
		for (int i = 0; i < 2; i++)
		{
			Logbook.Run r = new Logbook.Run();
			r.splits = new java.util.ArrayList<>(Arrays.asList(null, 40));
			r.hurt = new java.util.ArrayList<>(Arrays.asList(3, null));
			r.fallHp = new java.util.ArrayList<>(Arrays.asList(30, null, 0));
			book.runs.add(r);
		}
		book.repair();
		assertEquals(2, book.runs.size());
		assertEquals(Arrays.asList(0, 40), book.runs.get(0).splits);
		assertEquals(Arrays.asList(3, 0), book.runs.get(0).hurt);
		assertNull(book.runs.get(0).fallHp);
		// Wave 1 was never timed in these runs, so there is no usual time to forecast from.
		assertNull(Splits.forecast(book.runs, 1, 0, 0));
	}

	@Test
	public void aDamagedRunInProgressAndCorrectionsStillRead()
	{
		Logbook book = new Logbook();
		book.active = new Logbook.Active();
		book.active.splits = new java.util.ArrayList<>(Arrays.asList(null, 40));
		book.active.hurt = new java.util.ArrayList<>(Arrays.asList(null, 2));
		book.active.seen = new java.util.HashMap<>();
		book.active.seen.put(3, null);
		book.active.seen.put(4, new java.util.ArrayList<>(Arrays.asList("C", null)));
		book.active.seen.put(5, new java.util.ArrayList<>(Arrays.asList("??")));
		List<Placement> table = Rotations.placements(1, 1);
		List<String> good = new java.util.ArrayList<>();
		for (Placement p : table)
		{
			good.add(p.getSpawn().getCode());
		}
		book.corrections = new java.util.HashMap<>();
		book.corrections.put("1:1", good);
		book.corrections.put("1:2", new java.util.ArrayList<>(Arrays.asList("nowhere")));
		book.corrections.put("1:3", null);
		book.repair();
		assertEquals(Arrays.asList(0, 40), book.active.splits);
		assertEquals(Arrays.asList(0, 2), book.active.hurt);
		// Replaying what a run had seen on entering the cave decodes every code kept.
		assertTrue(book.active.seen.isEmpty());
		assertEquals(1, book.corrections.size());
		assertEquals(table.size(), book.placements(1, 1).size());
		assertEquals(Rotations.placements(1, 2).size(), book.placements(1, 2).size());
	}

	@Test
	public void jadOnTheFloorWithNoRunOpenPicksOneUpOnHisWave()
	{
		// Turned on, or logged in, part way through Jad's wave: no call comes, but he is out.
		Logbook book = new Logbook();
		assertTrue(book.jadSeen(1000));
		assertEquals(Waves.JAD, book.active.wave);
		assertTrue(book.active.partial);
		assertEquals(Waves.JAD - 1, book.active.splits.size());
		// Seen again on the next tick, nothing changes.
		assertFalse(book.jadSeen(2000));
		assertEquals(1000, book.active.startedAt);
	}

	@Test
	public void jadOnTheFloorAfterAMissedCallMovesTheRunOnToHisWave()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		play(a, 1, 60, 40);
		assertEquals(61, a.wave);
		assertTrue(book.jadSeen(0));
		assertEquals(Waves.JAD, a.wave);
		assertTrue(a.partial);
		assertFalse(book.jadSeen(0));
	}
}
