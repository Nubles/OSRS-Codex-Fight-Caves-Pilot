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
		assertEquals(Integer.valueOf(-20), Splits.versusPb(runs, 3, 130, false));
		// A faster run that died is not the cape being raced.
		List<Logbook.Run> withFall = Arrays.asList(run(Logbook.Result.CAPE, 63, 50), run(Logbook.Result.DIED, 20, 30));
		assertEquals(Integer.valueOf(-20), Splits.versusPb(withFall, 3, 130, false));
		assertNull(Splits.versusPb(Collections.emptyList(), 3, 130, false));
		assertNull(Splits.versusPb(runs, 0, 0, false));
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

	@Test
	public void untimedWavesNeverCountAsFast()
	{
		Logbook.Run cape = run(Logbook.Result.CAPE, 63, 50);
		// Picked up at wave 20: waves 1 to 19 untimed, the rest at 30 ticks.
		Logbook.Run partial = run(Logbook.Result.DIED, 40, 30);
		partial.partial = true;
		for (int i = 0; i < 19; i++)
		{
			partial.splits.set(i, 0);
		}
		Splits s = Splits.of(Arrays.asList(cape, partial), partial);
		assertEquals(350, s.getStretches().get(0).getBest());
		assertEquals(0, s.getStretches().get(0).getRun());
		assertEquals(0, s.getStretches().get(2).getRun());
		assertEquals(7 * 30, s.getStretches().get(3).getRun());
		assertEquals(19 * 50 + 21 * 30 + 23 * 50, s.getSumOfBest());
		List<int[]> recent = Splits.recent(Collections.singletonList(cape), Arrays.asList(0, 0, 41), 3);
		assertEquals(1, recent.size());
		assertEquals(3, recent.get(0)[0]);
		assertNull(Splits.versusPb(Collections.singletonList(cape), 3, 130, true));
	}

	private static Logbook.Run timed(int perWave, int waves)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = Logbook.Result.DIED;
		r.wave = waves;
		for (int w = 1; w <= waves; w++)
		{
			r.splits.add(perWave);
		}
		return r;
	}

	@Test
	public void forecastsJadFromYourUsualWaves()
	{
		java.util.List<Logbook.Run> runs = new java.util.ArrayList<>();
		runs.add(timed(40, 62));
		runs.add(timed(50, 62));
		runs.add(timed(45, 62));
		// Wave 60 of 62 begun 10 ticks ago, the clock at 2,600: 35 left of it, then 61 and 62 at 45.
		assertEquals(Integer.valueOf(2600 + 35 + 45 + 45), Splits.forecast(runs, 60, 2600, 10));
		// Running over the usual time leaves nothing more of the wave on now.
		assertEquals(Integer.valueOf(3000 + 90), Splits.forecast(runs, 60, 3000, 80));
		assertNull("no forecast on Jad's own wave", Splits.forecast(runs, Waves.JAD, 3000, 0));
	}

	@Test
	public void noForecastWithoutTwoTimesForEveryWaveLeft()
	{
		java.util.List<Logbook.Run> runs = new java.util.ArrayList<>();
		runs.add(timed(40, 62));
		runs.add(timed(50, 30));
		assertNull(Splits.forecast(runs, 20, 800, 0));
		assertNull("waves 31 on were cleared only once", Splits.forecast(runs, 30, 0, 0));
		runs.add(timed(46, 62));
		// Waves 20 to 30: 40, 50 and 46, usually 46. Waves 31 to 62: 40 and 46, usually 43.
		assertEquals(Integer.valueOf(800 + 11 * 46 + 32 * 43), Splits.forecast(runs, 20, 800, 0));
	}
}
