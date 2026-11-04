package com.codex.fightcaves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.TreeSet;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * The pilot gives no in-fight cues for any creature: no tile to stand on, no prayer to use, no kill
 * order or attack timing, no eat warning. Settings keep their saved keys; the ones that drove those
 * cues are gone, and so is the code behind them.
 */
public class NoFightCuesTest
{
	@Test
	public void savedSettingsKeepTheirKeys()
	{
		TreeSet<String> keys = new TreeSet<>();
		for (Method m : CodexFightCavesPilotConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				keys.add(item.keyName());
			}
		}
		assertEquals(new TreeSet<>(Arrays.asList("plaque", "jadDoses", "chat",
			// The wave ghost.
			"ghost",
			// The opt-in local record.
			"fieldNotes",
			// The rotation clock.
			"rotationClock",
			// Inferno and Colosseum run logs.
			"arenaLogs", "colosseumLootNote")), keys);
		assertEquals("codexfightcavespilot", CodexFightCavesPilotConfig.GROUP);
	}

	@Test
	public void theCueCodeIsGone()
	{
		for (String name : new String[]{"core.StandPlanner", "core.PrayerCall", "core.KillOrder", "core.Rehearsal",
			"core.Compare", "core.Outlook", "overlay.CaveOverlay", "overlay.Marks"})
		{
			try
			{
				Class.forName("com.codex.fightcaves." + name);
				fail(name + " gave in-fight cues and must not come back");
			}
			catch (ClassNotFoundException expected)
			{
				// Gone, as it should be.
			}
		}
	}
}
