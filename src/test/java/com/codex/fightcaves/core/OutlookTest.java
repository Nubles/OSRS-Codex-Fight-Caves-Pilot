package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The road to Jad: rating each wave by what the safest tile in reach leaves able to hit you. */
public class OutlookTest
{
	private static final Grid FLOOR = ChaseTest.pillarFloor();

	private static StandPlanner.Spot at(int x, int y, StandPlanner.Foe... foes)
	{
		return StandPlanner.score(FLOOR, Arrays.asList(foes), x, y, 0, 7);
	}

	@Test
	public void magerStuckBehindThePillarIsClean()
	{
		StandPlanner.Spot spot = at(17, 14, new StandPlanner.Foe(Monster.KET_ZEK, 4, 13), new StandPlanner.Foe(Monster.TZ_KIH, 3, 3));
		assertEquals(Outlook.Rating.CLEAN, Outlook.rate(spot));
		assertEquals("Every ranger and mager sticks behind a pillar; only melee comes.", Outlook.reason(spot));
	}

	@Test
	public void magerInSightAloneIsPray()
	{
		StandPlanner.Spot spot = at(10, 22, new StandPlanner.Foe(Monster.KET_ZEK, 4, 20));
		assertEquals(Outlook.Rating.PRAY, Outlook.rate(spot));
		assertEquals("The mager sees you from every tile in reach; one prayer covers it.", Outlook.reason(spot));
	}

	@Test
	public void magerInSightWithMeleeComingIsSplit()
	{
		StandPlanner.Spot spot = at(10, 22, new StandPlanner.Foe(Monster.KET_ZEK, 4, 20), new StandPlanner.Foe(Monster.YT_MEJKOT, 20, 20));
		assertEquals(Outlook.Rating.SPLIT, Outlook.rate(spot));
		assertTrue(Outlook.reason(spot).startsWith("The mager sees you from every tile in reach while the melee comes to you"));
	}

	@Test
	public void wavesWithNothingRangedAreCleanBeforeAnyPlanning()
	{
		Outlook o = new Outlook(null, 50, 10, 10);
		for (int w = 1; w <= 6; w++)
		{
			assertEquals(Outlook.Rating.CLEAN, o.rating(w));
		}
		assertEquals(Outlook.Rating.CLEAN, o.rating(15));
		assertEquals(Outlook.Rating.CLEAN, o.rating(30));
		assertNull(o.rating(7));
		assertNull(o.rating(Waves.JAD));
		assertEquals(Waves.LAST, o.encode().length());
		assertEquals(Outlook.UNKNOWN, o.encode().charAt(Waves.JAD - 1));
	}

	@Test
	public void stepsRateOneWaveAtATimeAndChainTheTile()
	{
		// Every wave puts one mager west of the pillar; standing east of it keeps him stuck.
		Outlook o = new Outlook(null, 31, 18, 14);
		List<StandPlanner.Foe> mager = Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 13));
		int rated = o.step(FLOOR, w -> mager, 7);
		assertEquals(31, rated);
		assertEquals(Outlook.Rating.CLEAN, o.rating(31));
		assertTrue(o.reason(31) != null);
		int steps = 1;
		while (!o.done())
		{
			o.step(FLOOR, w -> mager, 7);
			steps++;
		}
		assertEquals(Waves.JAD - 31, steps);
		assertEquals(0, o.step(FLOOR, w -> mager, 7));
	}

	@Test
	public void unknownSpawnsLeaveAWaveUnrated()
	{
		Outlook o = new Outlook(null, 7, 18, 14);
		o.step(FLOOR, w -> Collections.emptyList(), 7);
		assertNull(o.rating(7));
	}

	@Test
	public void savedLettersSurviveAndNextHardWaveIsFound()
	{
		StringBuilder b = new StringBuilder();
		for (int w = 1; w <= Waves.LAST; w++)
		{
			b.append(w == 20 ? 'p' : w == 46 ? 's' : w == 53 ? 's' : w == 63 ? '-' : 'c');
		}
		String codes = b.toString();
		Outlook o = new Outlook(codes, 40, 10, 10);
		assertEquals(Outlook.Rating.SPLIT, o.rating(46));
		assertEquals(46, Outlook.nextAtLeast(codes, 40, Outlook.Rating.SPLIT));
		assertEquals(53, Outlook.nextAtLeast(codes, 46, Outlook.Rating.SPLIT));
		assertEquals(0, Outlook.nextAtLeast(codes, 53, Outlook.Rating.SPLIT));
		assertEquals(20, Outlook.nextAtLeast(codes, 10, Outlook.Rating.PRAY));
		assertEquals(2, Outlook.counted(codes, 40, Outlook.Rating.SPLIT));
		assertEquals(0, Outlook.counted(codes, 40, Outlook.Rating.PRAY));
	}

	@Test
	public void savedOutlookPrefersYourReach()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("3:7", "seven");
		saved.put("3:1", "one");
		saved.put("13:7", "other rotation");
		assertEquals("seven", Outlook.saved(saved, 3, 7));
		assertEquals("one", Outlook.saved(saved, 3, 4));
		assertNull(Outlook.saved(saved, 4, 7));
		assertNull(Outlook.saved(null, 3, 7));
		// "Lowest reach" is by number: a staff's 10 is not below a blowpipe's 5.
		Map<String, String> reaches = new HashMap<>();
		reaches.put("3:10", "staff");
		reaches.put("3:5", "blowpipe");
		assertEquals("blowpipe", Outlook.saved(reaches, 3, 0));
		assertFalse("13:7".startsWith("3:"));
	}
}
