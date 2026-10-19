package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class FormTest
{
	private static Logbook.Run run(Logbook.Result result, int wave, int ticks)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = wave;
		r.ticks = ticks;
		return r;
	}

	/** A logbook of runs, counted in its totals as the logbook would count them. */
	private static Logbook book(Logbook.Run... runs)
	{
		Logbook b = new Logbook();
		for (Logbook.Run r : runs)
		{
			b.runs.add(r);
			b.attempts++;
			b.capes += r.result == Logbook.Result.CAPE ? 1 : 0;
		}
		return b;
	}

	@Test
	public void noRunsNoForm()
	{
		assertNull(Form.of(new Logbook()));
		assertNull(Form.of(null));
	}

	@Test
	public void aFirstRunIsTheWholeStory()
	{
		Form f = Form.of(book(run(Logbook.Result.DIED, 31, 900)));
		assertEquals(1, f.getPips().size());
		assertEquals("Your one run: no capes.", f.line());
		assertEquals("Last 2: 1 cape (50%).", Form.of(book(run(Logbook.Result.DIED, 31, 900), run(Logbook.Result.CAPE, 63, 4000))).line());
	}

	@Test
	public void theLastTenAgainstEveryRunWithCapesInARow()
	{
		Logbook b = book();
		for (int i = 0; i < 10; i++)
		{
			b = add(b, run(Logbook.Result.DIED, 40 + i, 2000));
		}
		Logbook.Result[] last = {Logbook.Result.CAPE, Logbook.Result.DIED, Logbook.Result.LEFT, Logbook.Result.CAPE, Logbook.Result.CAPE,
			Logbook.Result.DIED, Logbook.Result.CAPE, Logbook.Result.CAPE, Logbook.Result.CAPE, Logbook.Result.CAPE};
		for (Logbook.Result r : last)
		{
			b = add(b, run(r, r == Logbook.Result.CAPE ? 63 : 50, 4000));
		}
		Form f = Form.of(b);
		assertEquals(10, f.getPips().size());
		assertEquals(Logbook.Result.CAPE, f.getPips().get(0).getResult());
		assertEquals(Logbook.Result.LEFT, f.getPips().get(2).getResult());
		assertEquals(4, f.getStreak());
		assertEquals("Last 10: 7 capes (70%), against 35% over all 20 runs. 4 in a row.", f.line());
	}

	@Test
	public void runsTheLogbookLetGoStillCountInTheOverallRate()
	{
		// The logbook keeps 60 runs; its totals keep every one.
		Logbook b = book(run(Logbook.Result.CAPE, 63, 4000), run(Logbook.Result.DIED, 12, 400));
		b.attempts = 1_250;
		b.capes = 3;
		assertEquals("Last 2: 1 cape (50%), against 1% over all 1,250 runs.", Form.of(b).line());
	}

	@Test
	public void percentNeverRoundsToAllOrNone()
	{
		assertEquals(1, Form.percent(1, 300));
		assertEquals(99, Form.percent(299, 300));
		assertEquals(100, Form.percent(3, 3));
		assertEquals(0, Form.percent(0, 3));
		assertEquals(40, Form.percent(8, 20));
		assertEquals(0, Form.percent(1, 0));
	}

	@Test
	public void theUsualCapeIsTheMiddleTimedOne()
	{
		Logbook.Run partial = run(Logbook.Result.CAPE, 63, 1000);
		partial.partial = true;
		Logbook b = book(run(Logbook.Result.CAPE, 63, 4500), run(Logbook.Result.DIED, 40, 2000), run(Logbook.Result.CAPE, 63, 4100),
			partial, run(Logbook.Result.CAPE, 63, 4300), run(Logbook.Result.CAPE, 63, 4200));
		// 4100, 4200, 4300, 4500: the faster middle one; the part-way cape is not a time.
		assertEquals(4200, Form.usualCape(b));
		assertEquals(4, Form.timedCapes(b));
		assertEquals(-1, Form.usualCape(book(run(Logbook.Result.DIED, 40, 2000))));
	}

	private static Logbook add(Logbook b, Logbook.Run r)
	{
		b.runs.add(r);
		b.attempts++;
		b.capes += r.result == Logbook.Result.CAPE ? 1 : 0;
		return b;
	}
}
