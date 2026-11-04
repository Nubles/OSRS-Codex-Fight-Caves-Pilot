package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.fightcaves.arena.Arena;
import com.codex.fightcaves.arena.ArenaLog;
import com.codex.fightcaves.arena.ColosseumLoot;
import com.codex.fightcaves.core.Logbook;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import org.junit.Test;

/**
 * The Arenas page, and the rule that during an Inferno or Colosseum wave the whole panel holds a
 * plain clock and nothing else: no ghost, no rate, no estimate.
 */
public class ArenaPageTest
{
	static ArenaLog log()
	{
		ArenaLog log = new ArenaLog();
		for (int i = 0; i < 6; i++)
		{
			ArenaLog.Run r = new ArenaLog.Run();
			r.firstWave = 1;
			r.reached = i < 2 ? 4 : 5;
			r.cleared = i < 2 ? 3 : 5;
			r.result = i < 2 ? ArenaLog.Result.DIED : ArenaLog.Result.CLAIMED;
			r.splits = new ArrayList<>(Arrays.asList(60, 70, 80, 90, 100).subList(0, r.cleared));
			r.ticks = 300;
			r.endedAt = 1_767_225_600_000L + i * 86_400_000L;
			log.colosseum.runs.add(r);
		}
		ArenaLog.Run open = new ArenaLog.Run();
		open.firstWave = 1;
		open.reached = 3;
		open.cleared = 3;
		open.splits = new ArrayList<>(Arrays.asList(55, 66, 77));
		log.colosseum.active = open;
		return log;
	}

	static View outside()
	{
		return new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, 50, 70, false, 0, 0, null, new Logbook());
	}

	static List<String> texts(Component c)
	{
		List<String> out = new ArrayList<>();
		if (c instanceof JLabel)
		{
			out.add(((JLabel) c).getText());
		}
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				out.addAll(texts(k));
			}
		}
		return out;
	}

	private static boolean has(List<String> texts, String s)
	{
		for (String t : texts)
		{
			if (t != null && t.contains(s))
			{
				return true;
			}
		}
		return false;
	}

	@Test
	public void duringAWaveEveryPageHoldsOnlyTheClock()
	{
		PilotPanel panel = new PilotPanel();
		ArenaLog log = log();
		panel.update(outside().withArenas(log, Arena.COLOSSEUM, true, 0, true));
		for (int tab = 0; tab < 4; tab++)
		{
			assertTrue("tab " + tab, panel.arenaQuietShown(tab));
		}
		List<String> t = texts(panel.arenasPage().getViewport().getView());
		assertEquals(3, t.size());
		assertTrue(has(t, "Wave 3 of 12"));
		assertFalse(has(t, "loot"));
		assertFalse(has(t, "ghost"));
	}

	@Test
	public void betweenWavesTheLootNoteShows()
	{
		PilotPanel panel = new PilotPanel();
		panel.update(outside().withArenas(log(), Arena.COLOSSEUM, false, 3, true));
		assertFalse(panel.arenaQuietShown(3));
		List<String> t = texts(panel.arenasPage().getViewport().getView());
		assertTrue(has(t, "Take the loot or go on?"));
		assertTrue(has(t, "You survived wave 4 in 4 of 6 runs"));
	}

	@Test
	public void theLootNoteCanBeTurnedOff()
	{
		PilotPanel panel = new PilotPanel();
		panel.update(outside().withArenas(log(), Arena.COLOSSEUM, false, 3, false));
		assertFalse(has(texts(panel.arenasPage().getViewport().getView()), "Take the loot or go on?"));
	}

	@Test
	public void awayFromTheArenaThereIsNoLootNote()
	{
		PilotPanel panel = new PilotPanel();
		panel.update(outside().withArenas(log(), null, false, 0, true));
		panel.showArena(Arena.COLOSSEUM);
		List<String> t = texts(panel.arenasPage().getViewport().getView());
		assertFalse(has(t, "Take the loot or go on?"));
		assertTrue(has(t, "Your death rate by wave".toUpperCase()) || has(t, "Deadliest") || has(t, "rate"));
	}

	@Test
	public void lootLinesReadPlainly()
	{
		List<ArenaLog.Run> runs = log().colosseum.runs;
		List<String> lines = ArenaPage.lootLines(ColosseumLoot.estimate(3, runs));
		assertEquals("Chest so far: about 121,855 coins (the wiki's averages, waves 1-3).", lines.get(0));
		assertEquals("Wave 4 adds about 61,162 on average.", lines.get(1));
		assertEquals("You survived wave 4 in 4 of 6 runs (67%).", lines.get(2));
		assertEquals("Going on pays on average if you survive wave 4 more than 67% of the time.", lines.get(3));
		assertEquals("By your own runs it is too close to call.", lines.get(4));
		List<String> few = ArenaPage.lootLines(ColosseumLoot.estimate(5, runs));
		assertTrue(few.get(2), few.get(2).startsWith("Your death rate on wave 6 is not known yet: 0 of your runs began it"));
	}
}
