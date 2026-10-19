package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SplitsTest
{
	/** A run whose every cleared wave took the same number of ticks. */
	private static Logbook.Run run(Logbook.Result result, int cleared, int each)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = Math.min(Waves.LAST, cleared + (result == Logbook.Result.CAPE ? 0 : 1));
		for (int i = 0; i < cleared; i++)
		{
			r.splits.add(each);
		}
		r.ticks = cleared * each;
		return r;
	}

	@Test
	public void nineStretchesOfSeven()
	{
		Splits s = Splits.of(Collections.emptyList(), null);
		assertEquals(9, s.getStretches().size());
		assertEquals("1-7", s.getStretches().get(0).label());
		assertEquals("57-Jad", s.getStretches().get(8).label());
		assertEquals(0, s.getSumOfBest());
		assertNull(s.mostToGain());
	}

	@Test
	public void personalBestIsTheFastestCape()
	{
		Logbook.Run slow = run(Logbook.Result.CAPE, 63, 60);
		Logbook.Run fast = run(Logbook.Result.CAPE, 63, 50);
		Logbook.Run died = run(Logbook.Result.DIED, 40, 10);
		assertEquals(fast, Splits.personalBest(Arrays.asList(slow, fast, died)));
		assertNull(Splits.personalBest(Collections.singletonList(died)));
	}

	@Test
	public void bestOfEachWaveAddsUpAndShowsWhatIsOnTheTable()
	{
		Logbook.Run cape = run(Logbook.Result.CAPE, 63, 50);
		// A faster but failed run proves waves 1 to 14 can go at 40 ticks each.
		Logbook.Run died = run(Logbook.Result.DIED, 14, 40);
		Splits s = Splits.of(Arrays.asList(cape, died), null);
		assertEquals(63 * 50, s.getPbTicks());
		assertEquals(14 * 40 + 49 * 50, s.getSumOfBest());
		assertEquals(14 * 10, s.onTheTable());
		Splits.Stretch first = s.getStretches().get(0);
		assertEquals(350, first.getPb());
		assertEquals(280, first.getBest());
		assertEquals(70, first.toGain());
		// The latest run is compared: 70 ticks ahead of the cape in the first stretch.
		assertEquals(Integer.valueOf(-70), first.versusPb());
		// It never cleared waves 15 to 21, so there is nothing to compare there.
		assertNull(s.getStretches().get(2).versusPb());
		assertEquals(0, s.getStretches().get(2).getRun());
		assertEquals(first, s.mostToGain());
	}

	@Test
	public void liveRunRacesTheCape()
	{
		List<Logbook.Run> runs = Collections.singletonList(run(Logbook.Result.CAPE, 63, 50));
		assertEquals(Integer.valueOf(-20), Splits.versusPb(runs, 3, 130));
		// A faster run that died is not the cape being raced.
		List<Logbook.Run> withFall = Arrays.asList(run(Logbook.Result.CAPE, 63, 50), run(Logbook.Result.DIED, 20, 30));
		assertEquals(Integer.valueOf(-20), Splits.versusPb(withFall, 3, 130));
		assertNull(Splits.versusPb(Collections.emptyList(), 3, 130));
		assertNull(Splits.versusPb(runs, 0, 0));
	}

	@Test
	public void recentSplitsNewestFirstAgainstTheCape()
	{
		List<Logbook.Run> runs = Collections.singletonList(run(Logbook.Result.CAPE, 63, 50));
		List<int[]> recent = Splits.recent(runs, new ArrayList<>(Arrays.asList(45, 52, 61)), 2);
		assertEquals(2, recent.size());
		assertEquals(3, recent.get(0)[0]);
		assertEquals(11, recent.get(0)[2]);
		assertEquals(2, recent.get(1)[2]);
		assertEquals(Integer.MIN_VALUE, Splits.recent(Collections.emptyList(), Arrays.asList(45), 3).get(0)[2]);
	}

	@Test
	public void theBestCapeOutlivesTheRunsKept()
	{
		Logbook b = new Logbook();
		b.active = new Logbook.Active();
		for (int i = 0; i < Waves.LAST; i++)
		{
			b.active.splits.add(50);
		}
		b.active.wave = Waves.LAST;
		b.active.ticks = Waves.LAST * 50;
		Logbook.Run cape = b.finish(Logbook.Result.CAPE);
		for (int i = 0; i < Logbook.KEEP_RUNS + 5; i++)
		{
			b.active = new Logbook.Active();
			b.active.wave = 3;
			b.finish(Logbook.Result.DIED);
		}
		assertEquals(Logbook.KEEP_RUNS, b.runs.size());
		assertEquals(cape, Splits.personalBest(b.runs));
	}
}
