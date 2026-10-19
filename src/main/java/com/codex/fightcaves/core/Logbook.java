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
		/** Ticks each wave took, wave 1 first. */
		public List<Integer> splits = new ArrayList<>();

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
		public List<Integer> splits = new ArrayList<>();
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
		runs.add(run);
		while (runs.size() > KEEP_RUNS)
		{
			runs.remove(0);
		}
		attempts++;
		furthest = Math.max(furthest, active.wave);
		if (result == Result.CAPE)
		{
			capes++;
			if (bestTicks == 0 || active.ticks < bestTicks)
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
