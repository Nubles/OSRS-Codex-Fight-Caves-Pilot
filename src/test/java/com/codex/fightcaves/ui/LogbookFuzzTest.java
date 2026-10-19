package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.PrayerPacer;
import com.codex.fightcaves.core.Splits;
import com.codex.fightcaves.core.Waves;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.runelite.http.api.RuneLiteAPI;
import org.junit.Test;

/**
 * The saved logbook ({@code codexfightcavespilot/logbook}) damaged every way a save can be: empty,
 * cut short, garbage, a field of the wrong type, a huge, negative or not-a-number figure, a field
 * from a newer version. Each must load without an exception, save again, and draw every page of
 * the panel in and out of the cave; a damaged field must not cost the rest of the logbook.
 */
public class LogbookFuzzTest
{
	/** The Gson the client injects. */
	private static final Gson GSON = RuneLiteAPI.GSON;

	/** Values dropped in place of a field, as raw JSON text. */
	private static final String[] BAD = {
		"\"\"", "\"garbage\"", "[]", "[1,\"x\",null]", "{}", "{\"a\":1}", "null", "true", "-1", "-2147483648",
		"2147483648", "99999999999999999999", "1e400", "-1e400", "NaN", "Infinity", "0.5", "\"12\"",
	};

	/** A full logbook: twelve runs (capes, falls, a walk-out), a run on wave 40, learned spawns and roads. */
	static Logbook full()
	{
		Logbook b = new Logbook();
		long t = 1_760_000_000_000L;
		for (int i = 0; i < 12; i++)
		{
			Logbook.Active a = b.begin(1, t);
			a.rotation = 1 + i % 15;
			int end = i % 3 == 0 ? Waves.LAST : 30 + i;
			for (int w = 1; w < end; w++)
			{
				a.ticks += 40 + w;
				Logbook.hurt(a, w % 7);
				Logbook.advance(a, w + 1);
			}
			a.ticks += 200;
			a.dosesUsed = end / 2;
			Logbook.Run r = b.finish(i % 3 == 0 ? Logbook.Result.CAPE : i == 4 ? Logbook.Result.LEFT : Logbook.Result.DIED);
			r.endedAt = t + i * 86_400_000L;
			if (r.result == Logbook.Result.DIED)
			{
				r.fall = "Prayer ran out on wave " + r.wave + ".";
				r.fallHp = new ArrayList<>(Arrays.asList(70, 50, 20, 0));
			}
		}
		Logbook.Active a = b.begin(1, t);
		a.rotation = 7;
		for (int w = 1; w < 40; w++)
		{
			a.ticks += 45;
			Logbook.advance(a, w + 1);
		}
		a.seen.put(1, Arrays.asList("S"));
		b.anchors.put("S", new double[]{37.5 * 4, 17.5 * 4, 4});
		b.corrections.put("7:2", Arrays.asList("S", "SW"));
		b.outlooks.put("7:0", "CCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCCPPSCC?");
		b.bankDoses = 120;
		b.bankSeenAt = t;
		return b;
	}

	/** Every place a damaged value can sit: each top field, each field of the first run and of the run on now. */
	private static List<String[]> paths(JsonObject root)
	{
		List<String[]> list = new ArrayList<>();
		for (Map.Entry<String, JsonElement> e : root.entrySet())
		{
			list.add(new String[]{e.getKey()});
		}
		for (String k : root.getAsJsonArray("runs").get(0).getAsJsonObject().keySet())
		{
			list.add(new String[]{"runs", k});
		}
		for (String k : root.getAsJsonObject("active").keySet())
		{
			list.add(new String[]{"active", k});
		}
		list.add(new String[]{"anchors", "S"});
		list.add(new String[]{"corrections", "7:2"});
		list.add(new String[]{"outlooks", "7:0"});
		list.add(new String[]{"active", "seen", "1"});
		return list;
	}

	/** The full logbook's JSON with the value at {@code path} replaced by raw text. */
	private static String damaged(String[] path, String raw)
	{
		JsonObject root = GSON.toJsonTree(full()).getAsJsonObject();
		JsonObject at = root;
		for (int i = 0; i < path.length - 1; i++)
		{
			JsonElement next = at.get(path[i]);
			at = next.isJsonArray() ? next.getAsJsonArray().get(0).getAsJsonObject() : next.getAsJsonObject();
		}
		at.addProperty(path[path.length - 1], "@@FUZZ@@");
		return GSON.toJson(root).replace("\"@@FUZZ@@\"", raw);
	}

	/** Loads, saves again and draws every page from it, in and out of the cave. */
	private static Logbook survive(String json, PilotPanel panel)
	{
		Logbook b = Logbook.read(GSON, json);
		assertNotNull(json, b);
		// Saving, and the panel's own copy, both write the logbook back out.
		String again = GSON.toJson(b);
		Logbook copy = Logbook.read(GSON, again);
		PrayerPacer pacer = new PrayerPacer(copy.prayerSpent, copy.prayerHitpoints);
		PrayerPacer.Pace full = pacer.pace(1, 70, 70, 0, 0, 4);
		int wave = copy.active == null ? 0 : copy.active.wave;
		Splits.forecast(copy.runs, 1, 0, 0);
		if (copy.active != null)
		{
			Splits.recent(copy.runs, copy.active.splits, 3);
			Splits.versusPb(copy.runs, Math.max(0, wave - 1), copy.active.waveStartTick, copy.active.partial);
			Splits.forecast(copy.runs, wave, copy.active.ticks, copy.active.ticks - copy.active.waveStartTick);
		}
		panel.update(new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, null, null, 70, 70, pacer.learned(),
			full.getDosesNeeded(), 0, null, copy));
		for (int tab = 0; tab < 3; tab++)
		{
			panel.showTab(tab);
		}
		int rotation = copy.active == null ? 0 : copy.active.rotation;
		PrayerPacer.Pace pace = wave > 0 ? pacer.pace(wave, 50, 70, 10, 4, 4) : null;
		panel.update(new View(View.State.IN_CAVE, wave, rotation, null, 0, false, 1, null, null, pace, 50, 70, pacer.learned(),
			0, copy.active == null ? 0 : copy.active.ticks, null, copy).withOutlook(copy.outlooks.get(rotation + ":0"), null));
		for (int tab = 0; tab < 3; tab++)
		{
			panel.showTab(tab);
			// However damaged the save, a run on Jad's wave leaves every page stepped back.
			assertEquals(json, wave >= Waves.JAD, panel.steppedBack(tab));
		}
		return b;
	}

	@Test
	public void everyDamagedFieldLoadsSavesAndDraws() throws Exception
	{
		JsonObject root = GSON.toJsonTree(full()).getAsJsonObject();
		List<String> failures = new ArrayList<>();
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			for (String[] path : paths(root))
			{
				for (String raw : BAD)
				{
					String json = damaged(path, raw);
					try
					{
						survive(json, panel);
					}
					catch (RuntimeException | AssertionError e)
					{
						failures.add(String.join(".", path) + " = " + raw + ": " + e);
					}
				}
			}
		});
		assertTrue(failures.size() + " failures, first: " + failures, failures.isEmpty());
	}

	@Test
	public void wholeSaveDamagedStartsAfresh() throws Exception
	{
		String good = GSON.toJson(full());
		List<String> saves = new ArrayList<>(Arrays.asList(
			null, "", " ", "{", "}", "[", "[]", "null", "0", "-1", "\"logbook\"", "garbage", "{\"runs\":", "\u0000\u0001",
			"{\"version\":99,\"format\":\"next\"}"));
		for (int cut = 1; cut < good.length(); cut += Math.max(1, good.length() / 40))
		{
			saves.add(good.substring(0, cut));
		}
		List<String> failures = new ArrayList<>();
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			for (String json : saves)
			{
				try
				{
					survive(json, panel);
				}
				catch (RuntimeException | AssertionError e)
				{
					failures.add((json == null ? "null" : json.substring(0, Math.min(30, json.length()))) + ": " + e);
				}
			}
		});
		assertTrue(failures.size() + " failures, first: " + failures, failures.isEmpty());
	}

	@Test
	public void oneDamagedFieldKeepsTheRest()
	{
		// A wrong-typed figure, or a run from a newer version, must not wipe twelve runs and the run on now.
		for (String[] path : new String[][]{{"bankDoses"}, {"deaths"}, {"anchors"}, {"prayerSpent"}, {"runs", "splits"}, {"runs", "fall"}})
		{
			Logbook b = Logbook.read(GSON, damaged(path, "\"garbage\""));
			assertTrue(String.join(".", path), b.runs.size() >= 11);
			assertNotNull(String.join(".", path), b.active);
			assertEquals(String.join(".", path), 40, b.active.wave);
		}
	}

	@Test
	public void tenThousandRunsLoadOnceAndThenCostLittle() throws Exception
	{
		// A hand-edited save, or one from a build that kept everything: 10,000 runs, a maxed record.
		Logbook big = full();
		Logbook.Run model = big.runs.get(1);
		for (int i = 0; i < 10_000; i++)
		{
			Logbook.Run r = new Logbook.Run();
			r.rotation = 1 + i % 15;
			r.result = i % 2 == 0 ? Logbook.Result.CAPE : Logbook.Result.DIED;
			r.wave = r.result == Logbook.Result.CAPE ? Waves.LAST : 1 + i % 62;
			r.splits = new ArrayList<>(model.splits);
			r.hurt = new ArrayList<>(model.hurt);
			r.ticks = 3000 + i % 900;
			r.endedAt = 1_700_000_000_000L + i * 60_000L;
			big.runs.add(r);
		}
		big.attempts = 99_999;
		big.capes = 50_000;
		big.furthest = Waves.LAST;
		String json = GSON.toJson(big);

		long t0 = System.nanoTime();
		Logbook b = Logbook.read(GSON, json);
		long loadMs = (System.nanoTime() - t0) / 1_000_000;
		// Read once, at login (about 130 ms here, the one slow step, and only for a save the pilot
		// never writes); then only the runs kept are carried, and the best cape among them.
		assertTrue(b.runs.size() <= Logbook.KEEP_RUNS + 1);
		assertEquals(99_999, b.attempts);
		assertTrue("load took " + loadMs + " ms", loadMs < 1500);

		// What the plugin does on the client thread each time the panel's view changes: copy the
		// logbook and work out the forecast and splits. Warmed up, then timed over many goes.
		for (int i = 0; i < 50; i++)
		{
			Logbook.read(GSON, GSON.toJson(b));
		}
		int goes = 200;
		long t1 = System.nanoTime();
		for (int i = 0; i < goes; i++)
		{
			Logbook copy = Logbook.read(GSON, GSON.toJson(b));
			Splits.forecast(copy.runs, 1, 0, 0);
			Splits.recent(copy.runs, copy.active.splits, 3);
			Splits.versusPb(copy.runs, copy.active.wave - 1, copy.active.waveStartTick, false);
		}
		double eachMs = (System.nanoTime() - t1) / 1e6 / goes;
		assertTrue("each view took " + eachMs + " ms", eachMs < 5);

		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			survive(GSON.toJson(b), panel);
			assertFalse(panel.steppedBack(0));
		});
	}
}
