package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The wave ghost: which run it is, the race against it, and the report on a run. */
public class GhostTest
{
	/** A run that cleared {@code waves} waves at {@code each} ticks, ending as {@code result}. */
	private static Logbook.Run run(Logbook.Result result, int waves, int each)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = result == Logbook.Result.CAPE ? Waves.LAST : waves + 1;
		for (int w = 1; w <= waves; w++)
		{
			r.splits.add(each);
			r.doses.add(0);
			r.food.add(0);
		}
		r.ticks = waves * each;
		return r;
	}

	@Test
	public void theFastestCapeIsTheGhost()
	{
		Logbook.Run slow = run(Logbook.Result.CAPE, 63, 12);
		Logbook.Run fast = run(Logbook.Result.CAPE, 63, 10);
		Logbook.Run far = run(Logbook.Result.DIED, 62, 5);
		Ghost g = Ghost.of(Arrays.asList(slow, far, fast), null);
		assertTrue(g.isCape());
		assertEquals(fast, g.getRun());
		assertEquals(630, g.clockAt(63));
		assertEquals(100, g.clockAt(10));
	}

	@Test
	public void beforeACapeTheFurthestRunIsTheGhost()
	{
		Logbook.Run near = run(Logbook.Result.DIED, 20, 10);
		Logbook.Run far = run(Logbook.Result.DIED, 40, 12);
		Logbook.Run farButSlow = run(Logbook.Result.DIED, 40, 14);
		Ghost g = Ghost.of(Arrays.asList(near, farButSlow, far), null);
		assertFalse(g.isCape());
		assertEquals(far, g.getRun());
		assertEquals(40, g.getCleared());
		// Past the ghost's last wave there is no clock to race.
		assertEquals(0, g.clockAt(41));
		assertNull(g.behind(41, 999, false));
	}

	@Test
	public void aPartRunIsNeverTheGhostNorRaced()
	{
		Logbook.Run part = run(Logbook.Result.CAPE, 63, 5);
		part.partial = true;
		assertNull(Ghost.of(Collections.singletonList(part), null));
		Ghost g = Ghost.of(Collections.singletonList(run(Logbook.Result.CAPE, 63, 10)), null);
		assertNull(g.behind(5, 40, true));
		assertEquals(Integer.valueOf(-10), g.behind(5, 40, false));
	}

	@Test
	public void theRunJudgedIsLeftOutOfItsOwnGhost()
	{
		Logbook.Run only = run(Logbook.Result.CAPE, 63, 10);
		assertNull(Ghost.of(Collections.singletonList(only), only));
		assertNotNull(Ghost.of(Collections.singletonList(only), null));
	}

	@Test
	public void splitsRunTheClockAgainstTheGhostWaveByWave()
	{
		Ghost g = Ghost.of(Collections.singletonList(run(Logbook.Result.CAPE, 63, 10)), null);
		List<Integer> times = Arrays.asList(12, 9, 0, 10);
		List<Ghost.Split> s = g.splits(times, Arrays.asList(1, 0, 0, 0), Arrays.asList(0, 2, 0, 0), 4, false);
		assertEquals(4, s.size());
		assertEquals(Integer.valueOf(2), s.get(0).behind);
		assertEquals(Integer.valueOf(2), s.get(0).lost());
		assertEquals(Integer.valueOf(1), s.get(1).behind);
		assertEquals(Integer.valueOf(-1), s.get(1).lost());
		// An untimed wave breaks the clock from there on, though each later wave is still set against the ghost's.
		assertNull(s.get(2).behind);
		assertNull(s.get(3).behind);
		assertEquals(Integer.valueOf(0), s.get(3).lost());
		assertEquals(1, s.get(0).doses);
		assertEquals(2, s.get(1).food);
	}

	@Test
	public void theReportNamesTheWavesThatLostTheMostTimeAndSupplies()
	{
		Logbook.Run ghostRun = run(Logbook.Result.CAPE, 63, 10);
		ghostRun.doses.set(45, 1);
		Logbook.Run r = run(Logbook.Result.DIED, 50, 10);
		r.splits.set(45, 48); // wave 46: +38
		r.splits.set(37, 22); // wave 38: +12
		r.splits.set(10, 8);  // wave 11: -2, a gain, never named
		r.splits.set(49, 31); // wave 50: +21
		r.splits.set(20, 11); // wave 21: +1
		r.doses.set(45, 3);   // wave 46: 3 doses against 1
		r.food.set(45, 1);
		r.food.set(48, 2);    // wave 49: 2 food against none
		Ghost g = Ghost.of(Arrays.asList(ghostRun, r), r);
		Ghost.Report rep = g.report(r);
		assertEquals(3, rep.getSlowest().size());
		assertEquals(46, rep.getSlowest().get(0).wave);
		assertEquals(50, rep.getSlowest().get(1).wave);
		assertEquals(38, rep.getSlowest().get(2).wave);
		assertEquals(2, rep.getHungriest().size());
		assertEquals(46, rep.getHungriest().get(0).wave);
		assertEquals(3, rep.getHungriest().get(0).overSupplies());
		assertEquals(49, rep.getHungriest().get(1).wave);
		assertEquals(50, rep.getThrough());
		assertEquals(3, rep.getDoses());
		assertEquals(3, rep.getFood());
		assertEquals(1, rep.getGhostDoses());
		assertEquals(Integer.valueOf(38 + 12 - 2 + 21 + 1), rep.getBehind());
	}

	@Test
	public void theWaveARunFellOnIsNotWeighed()
	{
		Logbook.Run ghostRun = run(Logbook.Result.CAPE, 63, 10);
		Logbook.Run r = run(Logbook.Result.DIED, 5, 10);
		// The wave it fell on has no split; supplies drunk there do not count against the ghost.
		r.doses.add(4);
		Ghost.Report rep = Ghost.of(Arrays.asList(ghostRun, r), r).report(r);
		assertEquals(5, rep.getThrough());
		assertTrue(rep.getHungriest().isEmpty());
		assertEquals(0, rep.getDoses());
	}

	@Test
	public void suppliesAreFiledOnTheWaveOnNow()
	{
		Logbook book = new Logbook();
		Logbook.Active a = book.begin(1, 0);
		Logbook.drank(a, 1);
		Logbook.advance(a, 2);
		Logbook.advance(a, 3);
		Logbook.drank(a, 2);
		Logbook.ate(a, 1);
		assertEquals(Arrays.asList(1, 0, 2), a.doses);
		assertEquals(Arrays.asList(0, 0, 1), a.food);
		Logbook.Run done = book.finish(Logbook.Result.DIED);
		assertEquals(Arrays.asList(1, 0, 2), done.doses);
		assertEquals(Arrays.asList(0, 0, 1), done.food);
	}

	@Test
	public void anOlderSaveWithoutSuppliesReadsAndLosesNothing()
	{
		com.google.gson.Gson gson = new com.google.gson.Gson();
		String old = "{\"runs\":[{\"result\":\"CAPE\",\"wave\":63,\"ticks\":600,\"splits\":[10,10]}]}";
		Logbook b = Logbook.read(gson, old);
		assertEquals(new ArrayList<Integer>(), b.runs.get(0).doses);
		assertFalse(Logbook.wouldLose(gson, old, b));
		// Damaged figures are mended: a gap reads as nothing used.
		Logbook d = Logbook.read(gson, "{\"runs\":[{\"result\":\"DIED\",\"wave\":3,\"doses\":[1,null,-4]}]}");
		assertEquals(Arrays.asList(1, 0, 0), d.runs.get(0).doses);
	}
}
