package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
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
}
