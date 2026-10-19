package com.codex.fightcaves.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the pilot keeps for one account, saved as a single blob: the run in progress so a
 * run survives logging out, past runs, how fast prayer goes, and what it has learned about the
 * cave itself (where each spawn point really is, and any wave that spawned differently from the
 * table).
 */
public final class Logbook
{
	public static final int KEEP_RUNS = 60;
	/**
	 * A save at least this long is far bigger than any the pilot writes (60 runs come to about
	 * 30,000 characters) and takes long enough to read that it is read off the client thread.
	 */
	public static final int LARGE = 200_000;

	/** How a run ended. */
	public enum Result
	{
		CAPE,
		DIED,
		LEFT
	}

	/** One finished run. */
	public static final class Run
	{
		public int rotation;
		public Result result;
		public int wave;
		public int ticks;
		public long endedAt;
		public int dosesUsed;
		/** Ticks each wave took, wave 1 first; 0 for a wave that was not timed. */
		public List<Integer> splits = new ArrayList<>();
		/** Damage you took on each wave, wave 1 first. */
		public List<Integer> hurt = new ArrayList<>();
		/** True when the run was not watched from wave 1 or a wave went unseen, so its time is not a record. */
		public boolean partial;
		/** The first wave the pilot saw, 0 or 1 for a run watched from the start. */
		public int firstWave;
		/** For a fall before Jad: why it happened, in a sentence or two (see {@link Fall}), or null. */
		public String fall;
		/** Your hitpoints over the fall's last ten seconds or so, oldest first, or null. */
		public List<Integer> fallHp;

		public long millis()
		{
			return ticks * 600L;
		}
	}

	/** The run in progress. */
	public static final class Active
	{
		public int rotation;
		public int wave;
		public int ticks;
		public int waveStartTick;
		public long startedAt;
		public int dosesUsed;
		/** Ticks each wave took, wave 1 first; 0 for a wave that was not timed. */
		public List<Integer> splits = new ArrayList<>();
		/** Damage you took on each wave so far, wave 1 first, the wave on now included. */
		public List<Integer> hurt = new ArrayList<>();
		/** True once the run has a wave the pilot did not see from start to end. */
		public boolean partial;
		/** The first wave the pilot saw, 0 or 1 for a run watched from the start. */
		public int firstWave;
		/** Spawn codes seen per wave, so a resumed run can rebuild its rotation. */
		public Map<Integer, List<String>> seen = new HashMap<>();
	}

	public Active active;
	public List<Run> runs = new ArrayList<>();
	public int attempts;
	public int capes;
	public int bestTicks;
	public int furthest;
	public int[] deaths = new int[Waves.LAST + 1];
	public double prayerSpent = PrayerPacer.PRIOR_RATE * PrayerPacer.PRIOR_WEIGHT;
	public double prayerHitpoints = PrayerPacer.PRIOR_WEIGHT;
	/** Spawn code to {sum x, sum y, count} of the centres seen there, in the cave's own coordinates. */
	public Map<String, double[]> anchors = new HashMap<>();
	/** "rotation:wave" to the spawn codes actually seen, biggest creature first. */
	public Map<String, List<String>> corrections = new HashMap<>();
	/** Runs per rotation, rotations 1 to 15. */
	public int[] rotations = new int[Rotations.COUNT + 1];
	/** "rotation:reach" to the road to Jad worked out for it, one letter per wave (see {@link Outlook}). */
	public Map<String, String> outlooks = new HashMap<>();
	/** Prayer doses in the bank as the pilot last counted them, or -1 before it has seen the bank. */
	public long bankDoses = -1;
	/** When that was, in epoch milliseconds. */
	public long bankSeenAt;

	/**
	 * The logbook saved as {@code json}, or a fresh one when there is none or nothing in it can be
	 * read. A save damaged in one place (a figure of the wrong type, a run from a newer version)
	 * keeps everything else: what cannot be read is left out, a field or a run at a time.
	 */
	public static Logbook read(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return new Logbook();
		}
		Logbook whole = parse(gson, json);
		if (whole != null)
		{
			return whole;
		}
		JsonObject root;
		try
		{
			JsonElement e = gson.fromJson(json, JsonElement.class);
			root = e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (RuntimeException e)
		{
			root = null;
		}
		if (root == null)
		{
			return new Logbook();
		}
		salvage(gson, root, null);
		Logbook kept = parse(gson, root);
		return kept == null ? new Logbook() : kept;
	}

	/**
	 * True when saving {@code book}, read from {@code json}, in place of {@code json} would lose
	 * something it holds: a field or a value this version does not know (one a newer version wrote,
	 * such as a way of ending a run it has never heard of), or one it had to mend or leave out. A
	 * field {@code json} lacks, as an older save does, loses nothing.
	 */
	public static boolean wouldLose(Gson gson, String json, Logbook book)
	{
		if (json == null || json.trim().isEmpty())
		{
			return false;
		}
		JsonElement was;
		try
		{
			was = gson.fromJson(json, JsonElement.class);
		}
		catch (RuntimeException e)
		{
			return true;
		}
		return was != null && !covers(was, gson.toJsonTree(book));
	}

	/** True when {@code now} holds everything {@code was} does: every field, list entry and value. */
	private static boolean covers(JsonElement was, JsonElement now)
	{
		if (was.isJsonNull())
		{
			// Gson leaves an empty field out, and a gap in a list reads as 0.
			return true;
		}
		if (was.isJsonObject())
		{
			if (!now.isJsonObject())
			{
				return false;
			}
			JsonObject have = now.getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : was.getAsJsonObject().entrySet())
			{
				JsonElement kept = have.get(e.getKey());
				if (!e.getValue().isJsonNull() && (kept == null || !covers(e.getValue(), kept)))
				{
					return false;
				}
			}
			return true;
		}
		if (was.isJsonArray())
		{
			JsonArray a = was.getAsJsonArray();
			if (!now.isJsonArray() || now.getAsJsonArray().size() != a.size())
			{
				return false;
			}
			JsonArray b = now.getAsJsonArray();
			for (int i = 0; i < a.size(); i++)
			{
				if (!covers(a.get(i), b.get(i)))
				{
					return false;
				}
			}
			return true;
		}
		return was.equals(now);
	}

	/** The logbook in {@code json} (a string or a tree), repaired, or null when it does not read. */
	private static Logbook parse(Gson gson, Object json)
	{
		try
		{
			Logbook read = json instanceof JsonElement ? gson.fromJson((JsonElement) json, Logbook.class)
				: gson.fromJson((String) json, Logbook.class);
			return read == null ? null : read.repair();
		}
		catch (RuntimeException e)
		{
			// Gson lets some through unwrapped: a NumberFormatException for "garbage" in a double field.
			return null;
		}
	}

	/**
	 * Drops what does not read from {@code obj}: a whole field, one run of the list, or one field
	 * of a run or of the run on now. {@code within} is the logbook field {@code obj} sits in ("runs"
	 * or "active"), or null for the logbook itself; each piece is tried alone, placed where it belongs.
	 */
	private static void salvage(Gson gson, JsonObject obj, String within)
	{
		for (String key : new ArrayList<>(obj.keySet()))
		{
			JsonElement value = obj.get(key);
			if (within == null && "runs".equals(key) && value.isJsonArray())
			{
				JsonArray good = new JsonArray();
				for (JsonElement run : value.getAsJsonArray())
				{
					if (run.isJsonObject())
					{
						salvage(gson, run.getAsJsonObject(), "runs");
						good.add(run);
					}
				}
				obj.add(key, good);
				continue;
			}
			if (within == null && "active".equals(key) && value.isJsonObject())
			{
				salvage(gson, value.getAsJsonObject(), "active");
				continue;
			}
			JsonObject alone = new JsonObject();
			alone.add(key, value);
			JsonObject probe = alone;
			if ("runs".equals(within))
			{
				JsonArray list = new JsonArray();
				list.add(alone);
				probe = new JsonObject();
				probe.add("runs", list);
			}
			else if ("active".equals(within))
			{
				probe = new JsonObject();
				probe.add("active", alone);
			}
			if (parse(gson, probe) == null)
			{
				obj.remove(key);
			}
		}
	}

	/** True for a figure that saves as JSON and is not below zero. */
	private static boolean usable(double v)
	{
		return !Double.isNaN(v) && !Double.isInfinite(v) && v >= 0;
	}

	/** {@code v} held between lo and hi. */
	private static int clamp(int v, int lo, int hi)
	{
		return Math.max(lo, Math.min(hi, v));
	}

	/** A per-wave list with every gap or figure below zero read as 0, cut at the last wave, or an empty one. */
	private static List<Integer> filled(List<Integer> list)
	{
		List<Integer> out = new ArrayList<>();
		if (list != null)
		{
			for (Integer v : list)
			{
				if (out.size() == Waves.LAST)
				{
					break;
				}
				out.add(v == null || v < 0 ? 0 : v);
			}
		}
		return out;
	}

	/** Every figure below zero read as 0. */
	private static void noneBelowZero(int[] counts)
	{
		for (int i = 0; i < counts.length; i++)
		{
			counts[i] = Math.max(0, counts[i]);
		}
	}

	/**
	 * Fills in anything an older save left out, and puts right what a damaged or hand-edited one
	 * holds: gaps, figures below zero or out of range, more runs than are kept, and figures that
	 * could not be saved again (not a number, or infinite).
	 */
	public Logbook repair()
	{
		if (runs == null)
		{
			runs = new ArrayList<>();
		}
		if (deaths == null || deaths.length != Waves.LAST + 1)
		{
			deaths = new int[Waves.LAST + 1];
		}
		if (anchors == null)
		{
			anchors = new HashMap<>();
		}
		if (corrections == null)
		{
			corrections = new HashMap<>();
		}
		if (rotations == null || rotations.length != Rotations.COUNT + 1)
		{
			rotations = new int[Rotations.COUNT + 1];
		}
		if (outlooks == null)
		{
			outlooks = new HashMap<>();
		}
		noneBelowZero(deaths);
		noneBelowZero(rotations);
		attempts = Math.max(0, attempts);
		capes = Math.max(0, capes);
		bestTicks = Math.max(0, bestTicks);
		furthest = clamp(furthest, 0, Waves.LAST);
		if (bankDoses < 0)
		{
			bankDoses = -1;
		}
		// A hand-edited or damaged save can hold gaps; a gap reads as untimed or unhurt.
		if (runs.contains(null))
		{
			runs = new ArrayList<>(runs);
			runs.removeIf(java.util.Objects::isNull);
		}
		for (Run r : runs)
		{
			r.splits = filled(r.splits);
			r.hurt = filled(r.hurt);
			if (r.fallHp != null && r.fallHp.contains(null))
			{
				r.fallHp = null;
			}
			// A way of ending a run this version does not know reads as walking out.
			if (r.result == null)
			{
				r.result = Result.LEFT;
			}
			r.wave = clamp(r.wave, 0, Waves.LAST);
			r.firstWave = clamp(r.firstWave, 0, Waves.LAST);
			r.ticks = Math.max(0, r.ticks);
			r.dosesUsed = Math.max(0, r.dosesUsed);
			if (r.rotation < 0 || r.rotation > Rotations.COUNT)
			{
				r.rotation = 0;
			}
		}
		trim();
		if (!usable(prayerSpent) || !usable(prayerHitpoints) || prayerHitpoints <= 0)
		{
			prayerSpent = PrayerPacer.PRIOR_RATE * PrayerPacer.PRIOR_WEIGHT;
			prayerHitpoints = PrayerPacer.PRIOR_WEIGHT;
		}
		// An anchor that is not three plain figures would not save again.
		anchors.values().removeIf(a -> a == null || a.length != 3 || !usable(a[0]) || !usable(a[1]) || !usable(a[2]));
		outlooks.values().removeIf(java.util.Objects::isNull);
		// A spawn code that does not decode would throw each time it is read back.
		corrections.values().removeIf(codes -> !Spawn.areCodes(codes));
		if (active != null)
		{
			active.splits = filled(active.splits);
			active.hurt = filled(active.hurt);
			if (active.seen == null)
			{
				active.seen = new HashMap<>();
			}
			active.seen.values().removeIf(codes -> !Spawn.areCodes(codes));
			active.seen.keySet().removeIf(w -> w == null || w < 1 || w > Waves.LAST);
			active.wave = clamp(active.wave, 1, Waves.LAST);
			active.firstWave = clamp(active.firstWave, 0, Waves.LAST);
			active.ticks = Math.max(0, active.ticks);
			active.waveStartTick = clamp(active.waveStartTick, 0, active.ticks);
			active.dosesUsed = Math.max(0, active.dosesUsed);
			if (active.rotation < 0 || active.rotation > Rotations.COUNT)
			{
				active.rotation = 0;
			}
		}
		return this;
	}

	/** Drops the oldest runs past {@link #KEEP_RUNS}, but never your best cape: the splits race it. */
	private void trim()
	{
		if (runs.size() <= KEEP_RUNS)
		{
			return;
		}
		Run pb = Splits.personalBest(runs);
		List<Run> kept = new ArrayList<>();
		int drop = runs.size() - KEEP_RUNS;
		for (Run r : runs)
		{
			if (drop > 0 && r != pb)
			{
				drop--;
			}
			else
			{
				kept.add(r);
			}
		}
		runs = kept;
	}

	/** Closes the run in progress and files it. */
	public Run finish(Result result)
	{
		if (active == null)
		{
			return null;
		}
		Run run = new Run();
		run.rotation = active.rotation;
		run.result = result;
		run.wave = active.wave;
		run.ticks = active.ticks;
		run.endedAt = System.currentTimeMillis();
		run.dosesUsed = active.dosesUsed;
		run.splits = new ArrayList<>(active.splits);
		run.hurt = new ArrayList<>(active.hurt);
		run.partial = active.partial;
		run.firstWave = active.firstWave;
		runs.add(run);
		trim();
		attempts++;
		furthest = Math.max(furthest, active.wave);
		if (result == Result.CAPE)
		{
			capes++;
			// A run picked up part way through would claim a time it never took.
			if (!active.partial && (bestTicks == 0 || active.ticks < bestTicks))
			{
				bestTicks = active.ticks;
			}
		}
		else if (result == Result.DIED && active.wave >= 1 && active.wave <= Waves.LAST)
		{
			deaths[active.wave]++;
		}
		if (active.rotation >= 1 && active.rotation <= Rotations.COUNT)
		{
			rotations[active.rotation]++;
		}
		active = null;
		return run;
	}

	/**
	 * Starts a run at the wave just called. A run first seen past wave 1 (the plugin turned on
	 * part way, or a run carried over from another computer) is marked partial, with untimed
	 * waves before it, so its splits still line up with their waves.
	 */
	public Active begin(int wave, long now)
	{
		Active a = new Active();
		a.startedAt = now;
		a.wave = wave;
		a.firstWave = Math.max(1, wave);
		for (int w = 1; w < wave; w++)
		{
			a.splits.add(0);
			a.hurt.add(0);
		}
		a.hurt.add(0);
		a.partial = wave > 1;
		active = a;
		return a;
	}

	/**
	 * TzTok-Jad or a healer is on the floor, so it is Jad's wave whatever the wave calls said. A
	 * run on an earlier wave is moved on to it (the call was missed), and with no run open one is
	 * picked up there (the plugin turned on, or you logged in, part way through his wave), so
	 * that nothing is offered while he is out.
	 *
	 * @return true when the run on now changed
	 */
	public boolean jadSeen(long now)
	{
		if (active == null)
		{
			begin(Waves.JAD, now);
			return true;
		}
		if (active.wave < Waves.JAD)
		{
			advance(active, Waves.JAD);
			return true;
		}
		return false;
	}

	/**
	 * Moves the run on to the wave just called: the wave before is timed, and waves skipped
	 * (a wave message missed) are left untimed and the run marked partial. Calling the same
	 * wave again, as the game does when you log back in, restarts its clock.
	 *
	 * @return true when a wave was cleared
	 */
	public static boolean advance(Active a, int wave)
	{
		boolean cleared = false;
		if (wave == a.wave + 1)
		{
			setAt(a.splits, a.wave, a.ticks - a.waveStartTick);
			cleared = true;
		}
		else if (wave > a.wave + 1)
		{
			// The time since the last call covers more than one wave, so none of them is timed.
			for (int w = a.wave; w < wave; w++)
			{
				setAt(a.splits, w, 0);
			}
			a.partial = true;
		}
		a.wave = wave;
		a.waveStartTick = a.ticks;
		// Every wave reached has a damage entry, so a wave you took no hits on still counts as played.
		while (a.hurt.size() < Math.min(wave, Waves.LAST))
		{
			a.hurt.add(0);
		}
		return cleared;
	}

	/** Adds damage taken to the wave on now. */
	public static void hurt(Active a, int damage)
	{
		if (a.wave < 1 || a.wave > Waves.LAST || damage <= 0)
		{
			return;
		}
		while (a.hurt.size() < a.wave)
		{
			a.hurt.add(0);
		}
		a.hurt.set(a.wave - 1, a.hurt.get(a.wave - 1) + damage);
	}

	/** Sets wave w's entry in a per-wave list, filling any gap before it with zeros. */
	public static void setAt(List<Integer> list, int w, int value)
	{
		while (list.size() < w)
		{
			list.add(0);
		}
		list.set(w - 1, value);
	}

	/** The spawns a wave uses in a rotation: what was seen there before, otherwise the table. */
	public List<Placement> placements(int rotation, int wave)
	{
		return placements(corrections, rotation, wave);
	}

	/** {@link #placements(int, int)} from a copy of the corrections. */
	public static List<Placement> placements(Map<String, List<String>> corrections, int rotation, int wave)
	{
		List<Placement> table = Rotations.placements(rotation, wave);
		List<String> seen = corrections == null ? null : corrections.get(rotation + ":" + wave);
		if (seen == null || seen.size() != table.size())
		{
			return table;
		}
		List<Placement> list = new ArrayList<>();
		for (int i = 0; i < table.size(); i++)
		{
			list.add(new Placement(table.get(i).getMonster(), Spawn.ofCode(seen.get(i))));
		}
		return list;
	}
}
