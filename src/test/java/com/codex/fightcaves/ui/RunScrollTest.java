package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.RotationRecord;
import com.codex.fightcaves.core.RunStory;
import com.codex.fightcaves.core.Waves;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RunScrollTest
{
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
		r.rotation = 9;
		return r;
	}

	@Test
	public void titlesNameTheCapeOrTheWave()
	{
		assertEquals("1st", RunScroll.ordinal(1));
		assertEquals("2nd", RunScroll.ordinal(2));
		assertEquals("3rd", RunScroll.ordinal(3));
		assertEquals("11th", RunScroll.ordinal(11));
		assertEquals("12th", RunScroll.ordinal(12));
		assertEquals("13th", RunScroll.ordinal(13));
		assertEquals("21st", RunScroll.ordinal(21));
		assertEquals("112th", RunScroll.ordinal(112));
		assertEquals("1,234th", RunScroll.ordinal(1234));
		Logbook.Run cape = run(Logbook.Result.CAPE, 63, 50, 1);
		assertEquals("First fire cape", RunScroll.title(RunStory.of(Collections.singletonList(cape), cape, 1)));
		assertEquals("8th fire cape", RunScroll.title(RunStory.of(Collections.singletonList(cape), cape, 8)));
		Logbook book = new Logbook();
		book.runs.add(cape);
		book.attempts = 70;
		book.capes = 5;
		RunStory unknown = RunStory.of(book, cape);
		assertEquals("Fire cape", RunScroll.title(unknown));
		assertFalse(RunScroll.lines(unknown).contains("Your first fire cape. Every cape after races this one."));
		Logbook.Run quick = run(Logbook.Result.CAPE, 63, 45, 2);
		book.runs.add(quick);
		assertTrue(RunScroll.lines(RunStory.of(book, quick)).contains("63 waves at your fastest on record, in bright gold."));
		Logbook.Run fell = run(Logbook.Result.DIED, 54, 50, 2);
		assertEquals("Fell on wave 55", RunScroll.title(RunStory.of(Collections.singletonList(fell), fell, 0)));
		Logbook.Run left = run(Logbook.Result.LEFT, 30, 50, 2);
		assertEquals("Left on wave 31", RunScroll.title(RunStory.of(Collections.singletonList(left), left, 0)));
	}

	@Test
	public void linesSayWhatMadeTheRun()
	{
		Logbook.Run first = run(Logbook.Result.CAPE, 63, 50, 1);
		assertEquals(Collections.singletonList("Your first fire cape. Every cape after races this one."),
			RunScroll.lines(RunStory.of(Collections.singletonList(first), first, 1)));

		Logbook.Run best = run(Logbook.Result.CAPE, 63, 48, 2);
		best.hurt = new ArrayList<>(Collections.nCopies(63, 2));
		best.hurt.set(45, 44);
		assertEquals(Arrays.asList("A new best cape, 1:16 under the old one.", "63 waves at your fastest yet, in bright gold.",
			"Hurt most on wave 46, which took 44."), RunScroll.lines(RunStory.of(Arrays.asList(first, best), best, 2)));

		Logbook.Run slower = run(Logbook.Result.CAPE, 63, 52, 3);
		assertEquals(Collections.singletonList("2:31 off your best cape, the one just before it."),
			RunScroll.lines(RunStory.of(Arrays.asList(first, best, slower), slower, 3)));
		Logbook.Run faster = run(Logbook.Result.CAPE, 63, 50, 4);
		assertEquals(Collections.singletonList("1:16 faster than the cape before it, 1:16 off your best."),
			RunScroll.lines(RunStory.of(Arrays.asList(first, best, slower, faster), faster, 4)));

		Logbook.Run fell = run(Logbook.Result.DIED, 54, 52, 5);
		assertEquals(Collections.emptyList(), RunScroll.lines(RunStory.of(Arrays.asList(first, best, slower, faster, fell), fell, 4)));
		Logbook.Run early = run(Logbook.Result.DIED, 10, 52, 0);
		Logbook.Run further = run(Logbook.Result.DIED, 20, 60, 1);
		assertEquals(Collections.singletonList("Your furthest yet."), RunScroll.lines(RunStory.of(Arrays.asList(early, further), further, 0)));
	}

	@Test
	public void theRecordLineReadsForOneRunOrMany()
	{
		Logbook.Run cape = run(Logbook.Result.CAPE, 63, 50, 1);
		Logbook.Run fell = run(Logbook.Result.DIED, 45, 50, 2);
		Logbook.Run fell2 = run(Logbook.Result.DIED, 45, 50, 3);
		assertEquals("No run of yours on this rotation yet.", PilotPanel.recordLine(RotationRecord.of(Collections.emptyList(), 9)));
		assertEquals("You caped it once before, in 31:30.", PilotPanel.recordLine(RotationRecord.of(Collections.singletonList(cape), 9)));
		assertEquals("Your one run on it fell on wave 46.", PilotPanel.recordLine(RotationRecord.of(Collections.singletonList(fell), 9)));
		assertEquals("Your runs on it: 1 cape from 3, best 31:30. Wave 46 ended 2 of them.",
			PilotPanel.recordLine(RotationRecord.of(Arrays.asList(cape, fell, fell2), 9)));
		assertEquals("Your runs on it: 2, furthest wave 46. Wave 46 ended 2 of them.",
			PilotPanel.recordLine(RotationRecord.of(Arrays.asList(fell, fell2), 9)));
	}

	@Test
	public void jadsWaveIsQuiet()
	{
		assertTrue(view(View.State.IN_CAVE, Waves.JAD).quiet());
		assertFalse(view(View.State.IN_CAVE, Waves.JAD - 1).quiet());
		assertFalse(view(View.State.OUTSIDE, 0).quiet());
		assertFalse(View.loggedOut().quiet());
	}

	@Test
	public void bigCountsAreGrouped()
	{
		assertEquals("1,234", Format.count(1234));
		assertEquals("2,000,000,000 doses", Format.doses(2_000_000_000L));
		assertEquals("1 dose", Format.doses(1));
	}

	private static View view(View.State state, int wave)
	{
		return new View(state, wave, 9, Collections.singletonList(9), 0, true, 1, null, null, null, 50, 70, true, 0, 0, null, new Logbook());
	}
}
