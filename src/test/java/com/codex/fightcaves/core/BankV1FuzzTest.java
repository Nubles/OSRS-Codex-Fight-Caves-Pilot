package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.gameval.ItemID;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

/**
 * Codex Bank Steward's {@code codexbanksteward/bankV1}, read here and never written, damaged every
 * way a save can be. None may throw, and none may count doses below zero or wrap a huge stack round.
 */
public class BankV1FuzzTest
{
	private static final Gson GSON = RuneLiteAPI.GSON;
	private static final long NOW = 1_760_000_000_000L;

	private static final String[] BAD = {
		"\"\"", "\"garbage\"", "[]", "[1,\"x\",null]", "{}", "{\"a\":1}", "null", "true", "-1", "-2147483648",
		"2147483648", "99999999999999999999", "1e400", "-1e400", "NaN", "Infinity", "0.5", "\"12\"",
	};

	private static String snapshot(String version, String savedAt, String items, String truncated)
	{
		return "{\"version\":" + version + ",\"savedAt\":" + savedAt + ",\"items\":" + items + ",\"truncated\":" + truncated + "}";
	}

	@Test
	public void everyDamagedFieldReadsWithoutThrowing()
	{
		String items = "{\"995\":1234567,\"" + ItemID._4DOSEPRAYERRESTORE + "\":30}";
		List<String> saves = new ArrayList<>();
		for (String bad : BAD)
		{
			saves.add(snapshot(bad, "1760000000000", items, "false"));
			saves.add(snapshot("1", bad, items, "false"));
			saves.add(snapshot("1", "1760000000000", bad, "false"));
			saves.add(snapshot("1", "1760000000000", items, bad));
			saves.add(snapshot("1", "1760000000000", "{\"" + ItemID._4DOSEPRAYERRESTORE + "\":" + bad + "}", "false"));
			saves.add(snapshot("1", "1760000000000", "{" + bad + ":3}", "false"));
		}
		String good = snapshot("1", "1760000000000", items, "false");
		for (int cut = 0; cut < good.length(); cut++)
		{
			saves.add(good.substring(0, cut));
		}
		saves.add("garbage");
		saves.add("\u0000");
		saves.add(snapshot("2", "1760000000000", items, "false") + "{\"next\":true}");
		for (String json : saves)
		{
			Banked b = Banked.steward(GSON, json);
			if (b != null)
			{
				assertTrue(json, b.getDoses() >= 0);
				assertTrue(json, b.getSavedAt() >= 0);
				PilotPanelText.of(b, NOW);
			}
		}
	}

	@Test
	public void aFutureVersionWithTheSameItemsStillCounts()
	{
		// A breaking change gets a new key, so a newer version under bankV1 keeps its items map.
		Banked b = Banked.steward(GSON, snapshot("7", "1760000000000", "{\"" + ItemID._4DOSEPRAYERRESTORE + "\":30}", "false"));
		assertEquals(120, b.getDoses());
	}

	@Test
	public void aHugeStackIsCappedNotWrappedRound()
	{
		// No stack in the bank passes 2,147,483,647. A bigger figure is damage: it was wrapping
		// round past the top of a long and reading as no doses at all.
		Banked b = Banked.steward(GSON, "{\"items\":{\"" + ItemID._4DOSEPRAYERRESTORE + "\":9223372036854775807}}");
		assertEquals(4L * Integer.MAX_VALUE, b.getDoses());
		Banked e = Banked.steward(GSON, "{\"items\":{\"" + ItemID._4DOSEPRAYERRESTORE + "\":1e30}}");
		assertEquals(4L * Integer.MAX_VALUE, e.getDoses());
		Banked two = Banked.steward(GSON, "{\"items\":{\"" + ItemID._4DOSEPRAYERRESTORE + "\":2147483647,\""
			+ ItemID._3DOSEPRAYERRESTORE + "\":2147483647,\"" + ItemID._4DOSE2RESTORE + "\":2147483647}}");
		assertEquals(11L * Integer.MAX_VALUE, two.getDoses());
	}

	@Test
	public void aSnapshotFromTheFutureDoesNotOutrankYourOwnLook()
	{
		// A clock set years ahead when Bank Steward saved: that snapshot must not win over the
		// pilot's own count from a minute ago for ever more, nor read as "just now".
		Banked own = new Banked(40, NOW - 60_000L, Banked.Source.PILOT);
		Banked ahead = Banked.steward(GSON, snapshot("1", String.valueOf(NOW + 10 * 365 * 86_400_000L),
			"{\"" + ItemID._4DOSEPRAYERRESTORE + "\":30}", "false"));
		assertSame(own, Banked.newest(own, ahead, NOW));
		assertEquals(0, Banked.newest(null, ahead, NOW).getSavedAt());
		// And a negative time reads as unknown.
		assertEquals(0, Banked.steward(GSON, snapshot("1", "-5", "{}", "false")).getSavedAt());
	}

	@Test
	public void aThousandItemBankReadsInAFewMilliseconds()
	{
		StringBuilder items = new StringBuilder("{");
		// 1,500 kinds of item, none of them a potion (ids from 30,000 up), plus thirty prayer potions.
		for (int id = 30_000; id < 31_500; id++)
		{
			items.append(id == 30_000 ? "" : ",").append('"').append(id).append("\":").append(id * 7);
		}
		items.append(",\"").append(ItemID._4DOSEPRAYERRESTORE).append("\":30}");
		String json = snapshot("1", "1760000000000", items.toString(), "false");
		for (int i = 0; i < 50; i++)
		{
			Banked.steward(GSON, json);
		}
		long t = System.nanoTime();
		int goes = 100;
		for (int i = 0; i < goes; i++)
		{
			Banked.steward(GSON, json);
		}
		double ms = (System.nanoTime() - t) / 1e6 / goes;
		assertTrue(ms + " ms", ms < 3);
		assertEquals(120, Banked.steward(GSON, json).getDoses());
	}

	/** The panel's bank card text, so a damaged figure is also drawn. */
	private static final class PilotPanelText
	{
		static String of(Banked b, long now)
		{
			return b.getDoses() + " " + b.runs(24) + " " + com.codex.fightcaves.ui.Format.ago(b.getSavedAt(), now);
		}
	}
}
