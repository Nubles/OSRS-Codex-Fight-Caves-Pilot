package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your personal best run as a ghost to race, wave by wave, and the report on a run set against
 * it. The ghost is your fastest cape timed from wave 1; before your first cape it is your
 * furthest run timed from wave 1 (the quicker of two that got as far), so there is something to
 * race from the second run on. It is a clock and a tally, nothing more: it never says how to
 * fight a wave.
 */
public final class Ghost
{
	/** Waves the report names for time and for supplies. */
	public static final int WORST = 3;

	/** One wave of a run against the same wave of the ghost. */
	public static final class Split
	{
		public final int wave;
		/** Ticks the run took on the wave, or 0 untimed. */
		public final int ticks;
		/** Ticks the ghost took, or 0 when it has none. */
		public final int ghostTicks;
		/** Run clock against the ghost's at the end of this wave (positive is behind), or null when either clock is not known. */
		public final Integer behind;
		public final int doses;
		public final int food;
		public final int ghostDoses;
		public final int ghostFood;

		Split(int wave, int ticks, int ghostTicks, Integer behind, int doses, int food, int ghostDoses, int ghostFood)
		{
			this.wave = wave;
			this.ticks = ticks;
			this.ghostTicks = ghostTicks;
			this.behind = behind;
			this.doses = doses;
			this.food = food;
			this.ghostDoses = ghostDoses;
			this.ghostFood = ghostFood;
		}

		/** Ticks lost on this wave against the ghost (negative is gained), or null when either is untimed. */
		public Integer lost()
		{
			return ticks > 0 && ghostTicks > 0 ? ticks - ghostTicks : null;
		}

		/** Supplies used past the ghost's on this wave: doses plus food. */
		public int overSupplies()
		{
			return doses + food - ghostDoses - ghostFood;
		}
	}

	/** What the report says of a run against the ghost. */
	public static final class Report
	{
		private final List<Split> splits;
		private final List<Split> slowest;
		private final List<Split> hungriest;
		private final Integer behind;
		private final int doses;
		private final int food;
		private final int ghostDoses;
		private final int ghostFood;
		private final int through;

		Report(List<Split> splits, List<Split> slowest, List<Split> hungriest, Integer behind, int doses, int food, int ghostDoses,
			int ghostFood, int through)
		{
			this.splits = splits;
			this.slowest = slowest;
			this.hungriest = hungriest;
			this.behind = behind;
			this.doses = doses;
			this.food = food;
			this.ghostDoses = ghostDoses;
			this.ghostFood = ghostFood;
			this.through = through;
		}

		/** Every wave the run reached, against the ghost. */
		public List<Split> getSplits()
		{
			return splits;
		}

		/** Up to {@link #WORST} waves that lost the most time, most first. */
		public List<Split> getSlowest()
		{
			return slowest;
		}

		/** Up to {@link #WORST} waves that used the most supplies past the ghost, most first. */
		public List<Split> getHungriest()
		{
			return hungriest;
		}

		/** The run's clock against the ghost's at the last wave both cleared, or null. */
		public Integer getBehind()
		{
			return behind;
		}

		/** Doses and food over the waves both runs went through. */
		public int getDoses()
		{
			return doses;
		}

		public int getFood()
		{
			return food;
		}

		public int getGhostDoses()
		{
			return ghostDoses;
		}

		public int getGhostFood()
		{
			return ghostFood;
		}

		/** The last wave both the run and the ghost cleared, which the totals run to. */
		public int getThrough()
		{
			return through;
		}
	}

	private final Logbook.Run run;
	private final boolean cape;

	private Ghost(Logbook.Run run, boolean cape)
	{
		this.run = run;
		this.cape = cape;
	}

	/** The ghost to race among {@code runs}, leaving {@code not} out (the run being judged), or null when there is none. */
	public static Ghost of(List<Logbook.Run> runs, Logbook.Run not)
	{
		Logbook.Run best = null;
		for (Logbook.Run r : runs)
		{
			if (r != not && r.result == Logbook.Result.CAPE && !r.partial && Splits.span(r, 1, Waves.LAST) > 0
				&& (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		if (best != null)
		{
			return new Ghost(best, true);
		}
		int bestReach = 0;
		int bestTicks = 0;
		for (Logbook.Run r : runs)
		{
			if (r == not || r.partial)
			{
				continue;
			}
			int reach = cleared(r);
			int ticks = reach == 0 ? 0 : Splits.span(r, 1, reach);
			if (reach > 0 && ticks > 0 && (reach > bestReach || reach == bestReach && ticks < bestTicks))
			{
				best = r;
				bestReach = reach;
				bestTicks = ticks;
			}
		}
		return best == null ? null : new Ghost(best, false);
	}

	/** Waves a run cleared, each timed, from wave 1 on. */
	static int cleared(Logbook.Run r)
	{
		int n = 0;
		while (r.splits != null && n < r.splits.size() && r.splits.get(n) > 0)
		{
			n++;
		}
		return n;
	}

	/** True when the ghost is a cape; otherwise your furthest run. */
	public boolean isCape()
	{
		return cape;
	}

	/** The ghost's run. */
	public Logbook.Run getRun()
	{
		return run;
	}

	/** Waves the ghost cleared. */
	public int getCleared()
	{
		return cleared(run);
	}

	/** The ghost's clock at the end of a wave, or 0 when it did not clear it. */
	public int clockAt(int wave)
	{
		return wave < 1 || wave > getCleared() ? 0 : Splits.span(run, 1, wave);
	}

	/**
	 * A run in progress against the ghost: its clock against the ghost's at the end of the last
	 * wave cleared (positive is behind), or null when the run was picked up part way or the ghost
	 * never got that far.
	 */
	public Integer behind(int cleared, int runClockThen, boolean partial)
	{
		int g = clockAt(cleared);
		return partial || g == 0 ? null : runClockThen - g;
	}

	/** The run's waves against the ghost's, wave 1 to the last wave the run reached. */
	public List<Split> splits(List<Integer> times, List<Integer> doses, List<Integer> food, int reached, boolean partial)
	{
		List<Split> list = new ArrayList<>();
		int clock = 0;
		boolean clockKnown = !partial;
		for (int w = 1; w <= Math.min(reached, Waves.LAST); w++)
		{
			int t = at(times, w);
			if (t <= 0)
			{
				clockKnown = false;
			}
			clock += t;
			int g = clockAt(w);
			Integer behind = clockKnown && t > 0 && g > 0 ? clock - g : null;
			list.add(new Split(w, t, at(run.splits, w), behind, at(doses, w), at(food, w), at(run.doses, w), at(run.food, w)));
		}
		return list;
	}

	/** The report on a finished run against this ghost. */
	public Report report(Logbook.Run r)
	{
		List<Split> splits = splits(r.splits, r.doses, r.food, r.wave, r.partial);
		List<Split> slow = new ArrayList<>();
		List<Split> hungry = new ArrayList<>();
		Integer behind = null;
		int doses = 0;
		int food = 0;
		int gDoses = 0;
		int gFood = 0;
		int through = 0;
		int ghostCleared = getCleared();
		for (Split s : splits)
		{
			// Only waves both runs cleared are weighed: the wave a run fell on was never finished.
			boolean both = s.ticks > 0 && s.wave <= ghostCleared;
			if (!both)
			{
				continue;
			}
			through = s.wave;
			if (s.behind != null)
			{
				behind = s.behind;
			}
			doses += s.doses;
			food += s.food;
			gDoses += s.ghostDoses;
			gFood += s.ghostFood;
			if (s.lost() != null && s.lost() > 0)
			{
				slow.add(s);
			}
			if (s.overSupplies() > 0)
			{
				hungry.add(s);
			}
		}
		slow.sort((a, b) -> b.lost() - a.lost() != 0 ? b.lost() - a.lost() : a.wave - b.wave);
		hungry.sort((a, b) -> b.overSupplies() - a.overSupplies() != 0 ? b.overSupplies() - a.overSupplies() : a.wave - b.wave);
		return new Report(Collections.unmodifiableList(splits), top(slow), top(hungry), behind, doses, food, gDoses, gFood, through);
	}

	private static List<Split> top(List<Split> list)
	{
		return Collections.unmodifiableList(new ArrayList<>(list.subList(0, Math.min(WORST, list.size()))));
	}

	private static int at(List<Integer> list, int wave)
	{
		return list == null || wave < 1 || wave > list.size() || list.get(wave - 1) == null ? 0 : Math.max(0, list.get(wave - 1));
	}
}
