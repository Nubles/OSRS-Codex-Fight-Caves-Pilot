package com.codex.fightcaves.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Waves;
import org.junit.Test;

/** Jad's wave leaves nothing on the panel or the plaque, however the pilot learned it was his wave. */
public class QuietTest
{
	private static View inCave(Logbook book)
	{
		int wave = book.active == null ? 0 : book.active.wave;
		return new View(View.State.IN_CAVE, wave, 0, null, 0, false, 1, null, null, null, 50, 70, false, 0, 0, null, book);
	}

	@Test
	public void jadSeenWithNoRunOpenQuietsTheView()
	{
		// In the cave with no run open (no wave call heard) the view was on wave 0 and every page
		// was drawn in full, the Waves page's chart included, though Jad was out.
		Logbook book = new Logbook();
		assertFalse(inCave(book).quiet());
		book.jadSeen(0);
		assertTrue(inCave(book).quiet());
	}

	@Test
	public void outsideTheCaveIsNeverQuiet()
	{
		assertFalse(new View(View.State.OUTSIDE, Waves.JAD, 0, null, 0, false, 0, null, null, null, 50, 70, false, 0, 0, null, new Logbook()).quiet());
	}
}
