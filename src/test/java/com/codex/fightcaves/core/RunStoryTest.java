package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class RunStoryTest
{
	/** A run whose every cleared wave took the same number of ticks. */
	private static Logbook.Run run(Logbook.Result result, int cleared, int each, long endedAt)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = Math.min(Waves.LAST, cleared + (result == Logbook.Result.CAPE ? 0 : 1));
		for (int i = 0; i < cleared; i++)
		{
			r.splits.add(each);
		}
		r.ticks = cleared * each;
		r.endedAt = endedAt;
		return r;
	}

	@Test
	public void aFirstRunHasNothingToBeatAndIsTheFirstCape()
	{
		Logbook.Run cape = run(Logbook.Result.CAPE, 63, 50, 1);
		RunStory s = RunStory.of(Collections.singletonList(cape), cape, 1);
		assertEquals(1, s.getCapeNumber());
		assertEquals(0, s.getBestWaves());
		assertEquals(RunStory.Tier.FIRST, s.tier(1));
		assertEquals(RunStory.Tier.FIRST, s.tier(Waves.JAD));
		assertNull(s.getVersusOldBest());
		assertNull(s.getVersusLastCape());
	}

	@Test
	public void wavesAreWeighedOnlyAgainstEarlierRuns()
	{
		Logbook.Run first = run(Logbook.Result.CAPE, 63, 50, 1);
		Logbook.Run second = run(Logbook.Result.CAPE, 63, 54, 2);
		// Wave 3 went faster than ever on the second run, wave 4 far slower.
		second.splits.set(2, 40);
		second.splits.set(3, 80);
		Logbook.Run third = run(Logbook.Result.CAPE, 63, 30, 3);
		List<Logbook.Run> runs = Arrays.asList(first, second, third);
		RunStory s = RunStory.of(runs, second, 3);
		assertEquals(2, s.getCapeNumber());
		assertEquals(RunStory.Tier.NEAR, s.tier(1));
		assertEquals(RunStory.Tier.BEST, s.tier(3));
		assertEquals(RunStory.Tier.PLAIN, s.tier(4));
		assertEquals(1, s.getBestWaves());
		assertEquals(50, s.best(3));
		// The faster third run, which came later, changes nothing about the second's story.
		assertEquals(Integer.valueOf(second.ticks - first.ticks), s.getVersusLastCape());
		assertEquals(Integer.valueOf(second.ticks - first.ticks), s.getVersusOldBest());
	}

	@Test
	public void aNewBestCapeIsMeasuredAgainstTheOldBestNotTheLastCape()
	{
		Logbook.Run fast = run(Logbook.Result.CAPE, 63, 40, 1);
		Logbook.Run slow = run(Logbook.Result.CAPE, 63, 60, 2);
		Logbook.Run best = run(Logbook.Result.CAPE, 63, 38, 3);
		RunStory s = RunStory.of(Arrays.asList(fast, slow, best), best, 3);
		assertEquals(Integer.valueOf(63 * 38 - 63 * 40), s.getVersusOldBest());
		assertEquals(Integer.valueOf(63 * 38 - 63 * 60), s.getVersusLastCape());
		assertEquals(Waves.LAST, s.getBestWaves());
	}

	@Test
	public void capesLetGoFromTheLogbookStillCount()
	{
		// The logbook keeps 60 runs but has counted 12 capes; the latest of the two kept is the 12th.
		Logbook.Run a = run(Logbook.Result.CAPE, 63, 50, 1);
		Logbook.Run b = run(Logbook.Result.CAPE, 63, 50, 2);
		assertEquals(12, RunStory.of(Arrays.asList(a, b), b, 12).getCapeNumber());
		assertEquals(11, RunStory.of(Arrays.asList(a, b), a, 12).getCapeNumber());
	}

	@Test
	public void aFallMarksItsWaveAndLeavesTheRestUnplayed()
	{
		Logbook.Run earlier = run(Logbook.Result.DIED, 30, 50, 1);
		Logbook.Run fell = run(Logbook.Result.DIED, 44, 50, 2);
		fell.hurt = new ArrayList<>(Collections.nCopies(45, 3));
		fell.hurt.set(44, 61);
		RunStory s = RunStory.of(Arrays.asList(earlier, fell), fell, 0);
		assertEquals(0, s.getCapeNumber());
		assertEquals(RunStory.Tier.ENDED, s.tier(45));
		assertEquals(RunStory.Tier.UNPLAYED, s.tier(46));
		assertEquals(RunStory.Tier.FIRST, s.tier(31));
		assertEquals(61, s.hurt(45));
		assertEquals(61, s.worstHurt());
		assertTrue(s.isFurthestYet());
		assertFalse(RunStory.of(Arrays.asList(earlier, fell), earlier, 0).isFurthestYet());
	}

	@Test
	public void aPartialCapeClaimsNoTimeAndUntimedWavesStayDark()
	{
		Logbook.Run before = run(Logbook.Result.CAPE, 63, 50, 1);
		Logbook.Run partial = run(Logbook.Result.CAPE, 63, 45, 2);
		partial.partial = true;
		partial.splits.set(0, 0);
		RunStory s = RunStory.of(Arrays.asList(before, partial), partial, 2);
		assertNull(s.getVersusOldBest());
		assertNull(s.getVersusLastCape());
		assertEquals(RunStory.Tier.UNTIMED, s.tier(1));
		assertEquals(RunStory.Tier.BEST, s.tier(2));
	}

	@Test
	public void aDamagedRunWithGapsAndShortListsIsReadSafely()
	{
		Logbook.Run odd = new Logbook.Run();
		odd.result = Logbook.Result.CAPE;
		odd.wave = Waves.LAST;
		odd.splits = new ArrayList<>(Arrays.asList(20, null, -4));
		odd.hurt = null;
		RunStory s = RunStory.of(Collections.singletonList(odd), odd, 0);
		assertEquals(RunStory.Tier.FIRST, s.tier(1));
		assertEquals(RunStory.Tier.UNTIMED, s.tier(2));
		assertEquals(RunStory.Tier.UNTIMED, s.tier(3));
		assertEquals(RunStory.Tier.UNTIMED, s.tier(Waves.JAD));
		assertEquals(0, s.worstHurt());
		assertEquals(20, s.longestWave());
		assertEquals(1, s.getCapeNumber());
	}

	@Test
	public void aRunNotInTheListIsToldAgainstRunsThatEndedBeforeIt()
	{
		Logbook.Run before = run(Logbook.Result.CAPE, 63, 50, 1);
		Logbook.Run after = run(Logbook.Result.CAPE, 63, 10, 9);
		Logbook.Run outside = run(Logbook.Result.CAPE, 63, 49, 5);
		RunStory s = RunStory.of(Arrays.asList(before, after), outside, 2);
		assertEquals(Waves.LAST, s.getBestWaves());
		assertEquals(2, s.getCapeNumber());
	}

	@Test
	public void theRotationRecordCountsOnlyThatRotation()
	{
		Logbook.Run a = run(Logbook.Result.CAPE, 63, 50, 1);
		a.rotation = 9;
		Logbook.Run b = run(Logbook.Result.CAPE, 63, 40, 2);
		b.rotation = 9;
		b.partial = true;
		Logbook.Run c = run(Logbook.Result.DIED, 45, 50, 3);
		c.rotation = 9;
		Logbook.Run d = run(Logbook.Result.DIED, 45, 50, 4);
		d.rotation = 9;
		Logbook.Run e = run(Logbook.Result.DIED, 20, 50, 5);
		e.rotation = 9;
		Logbook.Run other = run(Logbook.Result.CAPE, 63, 10, 6);
		other.rotation = 3;
		RotationRecord r = RotationRecord.of(Arrays.asList(a, b, c, d, e, other, null), 9);
		assertEquals(5, r.getRuns());
		assertEquals(2, r.getCapes());
		// The partial cape claims no time.
		assertEquals(63 * 50, r.getBestTicks());
		assertEquals(46, r.getWall());
		assertEquals(2, r.getWallFalls());
		assertEquals(Waves.LAST, r.getFurthest());
		assertEquals(0, RotationRecord.of(Arrays.asList(a, other), 0).getRuns());
		assertEquals(0, RotationRecord.of(Collections.emptyList(), 4).getRuns());
	}
}
