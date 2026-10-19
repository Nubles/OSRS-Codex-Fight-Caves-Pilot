package com.codex.fightcaves.core;

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

	/** Fills in anything an older save left out. */
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
		for (Run r : runs)
		{
			if (r.splits == null)
			{
				r.splits = new ArrayList<>();
			}
			if (r.hurt == null)
			{
				r.hurt = new ArrayList<>();
			}
		}
		if (prayerHitpoints <= 0)
		{
			prayerSpent = PrayerPacer.PRIOR_RATE * PrayerPacer.PRIOR_WEIGHT;
			prayerHitpoints = PrayerPacer.PRIOR_WEIGHT;
		}
		if (active != null)
		{
			if (active.splits == null)
			{
				active.splits = new ArrayList<>();
			}
			if (active.seen == null)
			{
				active.seen = new HashMap<>();
			}
			if (active.hurt == null)
			{
				active.hurt = new ArrayList<>();
			}
		}
		return this;
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
		// Drop the oldest runs, but never your best cape: the splits race it.
		Run pb = Splits.personalBest(runs);
		for (int i = 0; runs.size() > KEEP_RUNS && i < runs.size(); )
		{
			if (runs.get(i) == pb)
			{
				i++;
			}
			else
			{
				runs.remove(i);
			}
		}
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
