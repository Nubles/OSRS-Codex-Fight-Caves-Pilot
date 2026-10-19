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

	@Test
	public void panelOpenedOnJadsWaveStepsBackBeforeItsFreshView() throws Exception
	{
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			// Last drawn outside the cave, Waves page and all; closed; then opened on Jad's wave.
			PilotPanel panel = new PilotPanel();
			Logbook book = new Logbook();
			panel.update(new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, null, null, 50, 70, false, 0, 0, null, book));
			panel.onDeactivate();
			assertFalse(panel.steppedBack(1));
			panel.jadWave(true);
			panel.onActivate();
			for (int tab = 0; tab < 3; tab++)
			{
				assertTrue(panel.steppedBack(tab));
			}
		});
	}

	@Test
	public void panelOpenedOffJadsWaveKeepsItsPages() throws Exception
	{
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			panel.update(new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, null, null, 50, 70, false, 0, 0, null, new Logbook()));
			panel.jadWave(false);
			panel.onActivate();
			assertFalse(panel.steppedBack(1));
		});
	}

	@Test
	public void fullRecordsWithTheFormStripStepBackOnJadsWave() throws Exception
	{
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			// A logbook with enough runs for every Records card, the form strip included.
			Logbook book = new Logbook();
			for (int i = 0; i < 12; i++)
			{
				Logbook.Run r = new Logbook.Run();
				r.result = i % 3 == 0 ? Logbook.Result.CAPE : Logbook.Result.DIED;
				r.wave = r.result == Logbook.Result.CAPE ? Waves.LAST : 40 + i;
				for (int w = 0; w < r.wave; w++)
				{
					r.splits.add(50);
				}
				r.ticks = r.wave * 50;
				r.endedAt = 1_760_000_000_000L + i * 86_400_000L;
				r.rotation = 5;
				book.runs.add(r);
			}
			book.attempts = 12;
			book.capes = 4;
			book.bestTicks = 63 * 50;
			book.furthest = Waves.LAST;
			PilotPanel panel = new PilotPanel();
			panel.update(new View(View.State.OUTSIDE, 0, 0, null, 0, false, 5, null, null, null, 50, 70, false, 0, 0, null, book));
			assertTrue(contains(panel, FormStrip.class));
			panel.update(new View(View.State.IN_CAVE, Waves.JAD, 0, null, 0, false, 5, null, null, null, 50, 70, false, 0, 0, null, book));
			for (int tab = 0; tab < 3; tab++)
			{
				assertTrue(panel.steppedBack(tab));
			}
			assertFalse(contains(panel, FormStrip.class));
			assertFalse(contains(panel, StatTile.class));
			assertFalse(contains(panel, RunScroll.class));
		});
	}

	@Test
	public void aDamagedSaveCannotOpenJadsWave() throws Exception
	{
		// A run saved past the last wave (a hand-edited or damaged save) is read as Jad's wave,
		// so it steps back rather than drawing a wave 64 or 9999 in full.
		Logbook book = Logbook.read(net.runelite.http.api.RuneLiteAPI.GSON,
			"{\"active\":{\"wave\":9999,\"rotation\":99,\"ticks\":-5,\"splits\":[1,2,3]},\"runs\":[]}");
		assertTrue(book.active.wave == Waves.JAD);
		assertTrue(inCave(book).quiet());
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			panel.update(inCave(book));
			for (int tab = 0; tab < 3; tab++)
			{
				assertTrue(panel.steppedBack(tab));
			}
			assertFalse(contains(panel, StatTile.class));
			assertFalse(contains(panel, CaveChart.class));
		});
	}

	private static boolean contains(java.awt.Component c, Class<?> type)
	{
		if (type.isInstance(c))
		{
			return true;
		}
		if (c instanceof java.awt.Container)
		{
			for (java.awt.Component k : ((java.awt.Container) c).getComponents())
			{
				if (contains(k, type))
				{
					return true;
				}
			}
		}
		return false;
	}
}
