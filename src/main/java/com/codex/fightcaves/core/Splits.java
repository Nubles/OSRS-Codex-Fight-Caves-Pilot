package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your runs cut into splits, the way speedrunners read a run. The 63 waves fall into nine
 * stretches of seven. For each stretch it knows your personal best run's time (the fastest
 * cape), your best ever time for that stretch from any run that got through it, and how the
 * latest run compares. Adding the best of every wave gives the time you have already proven you
 * can do, and the gap to your best cape is time on the table.
 */
public final class Splits
{
	public static final int STRETCH = 7;
	public static final int STRETCHES = Waves.LAST / STRETCH;

	/** One stretch of seven waves. */
	public static final class Stretch
	{
		private final int from;
		private final int to;
		/** Ticks in your best cape, or 0 with no cape. */
		private final int pb;
		/** Fewest ticks any run took for these waves, or 0 when none got through them. */
		private final int best;
		/** Ticks in the run being compared, or 0 when it did not get through them. */
		private final int run;

		Stretch(int from, int to, int pb, int best, int run)
		{
			this.from = from;
			this.to = to;
			this.pb = pb;
			this.best = best;
			this.run = run;
		}

		public int getFrom()
		{
			return from;
		}

		public int getTo()
		{
			return to;
		}

		public int getPb()
		{
			return pb;
		}

		public int getBest()
		{
			return best;
		}

		public int getRun()
		{
			return run;
		}

		/** "1-7", with the last one "57-Jad". */
		public String label()
		{
			return from + "-" + (to == Waves.JAD ? "Jad" : String.valueOf(to));
		}

		/** Ticks the run was behind (positive) or ahead of your best cape here, or null. */
		public Integer versusPb()
		{
			return pb > 0 && run > 0 ? run - pb : null;
		}

		/** Ticks your best cape lost here against your best ever stretch. */
		public int toGain()
		{
			return pb > 0 && best > 0 ? Math.max(0, pb - best) : 0;
		}
	}

	private final List<Stretch> stretches;
	private final int sumOfBest;
	private final int pbTicks;
	private final int compared;

	private Splits(List<Stretch> stretches, int sumOfBest, int pbTicks, int compared)
	{
		this.stretches = Collections.unmodifiableList(stretches);
		this.sumOfBest = sumOfBest;
		this.pbTicks = pbTicks;
		this.compared = compared;
	}

	/**
	 * @param runs every filed run, oldest first
	 * @param compare the run to set against your best, or null for the latest run
	 */
	public static Splits of(List<Logbook.Run> runs, Logbook.Run compare)
	{
		Logbook.Run pb = personalBest(runs);
		Logbook.Run against = compare;
		if (against == null && !runs.isEmpty())
		{
			against = runs.get(runs.size() - 1);
		}
		int[] gold = gold(runs);
		List<Stretch> list = new ArrayList<>();
		for (int s = 0; s < STRETCHES; s++)
		{
			int from = s * STRETCH + 1;
			int to = from + STRETCH - 1;
			list.add(new Stretch(from, to, span(pb, from, to), bestSpan(runs, from, to), span(against, from, to)));
		}
		int sum = 0;
		for (int w = 1; w <= Waves.LAST; w++)
		{
			if (gold[w] == 0)
			{
				sum = 0;
				break;
			}
			sum += gold[w];
		}
		return new Splits(list, sum, pb == null ? 0 : pb.ticks, against == null ? 0 : against.wave);
	}

	public List<Stretch> getStretches()
	{
		return stretches;
	}

	/** Every wave at its best ever, added up, or 0 until every wave has been cleared at least once. */
	public int getSumOfBest()
	{
		return sumOfBest;
	}

	/** Your best cape's time, or 0. */
	public int getPbTicks()
	{
		return pbTicks;
	}

	/** Time your best cape could lose if every wave went as well as it ever has, or 0. */
	public int onTheTable()
	{
		return pbTicks > 0 && sumOfBest > 0 ? Math.max(0, pbTicks - sumOfBest) : 0;
	}

	/** The stretch where your best cape is furthest off your best, or null. */
	public Stretch mostToGain()
	{
		Stretch most = null;
		for (Stretch s : stretches)
		{
			if (s.toGain() > 0 && (most == null || s.toGain() > most.toGain()))
			{
				most = s;
			}
		}
		return most;
	}

	/** The wave the compared run reached. */
	public int getCompared()
	{
		return compared;
	}

	/** The fastest caped run with every wave timed, or null. */
	public static Logbook.Run personalBest(List<Logbook.Run> runs)
	{
		Logbook.Run best = null;
		for (Logbook.Run r : runs)
		{
			if (r.result == Logbook.Result.CAPE && r.splits != null && r.splits.size() >= Waves.LAST
				&& (best == null || r.ticks < best.ticks))
			{
				best = r;
			}
		}
		return best;
	}

	/** The fewest ticks each wave ever took, index 1 to 63, 0 where no run cleared it. */
	static int[] gold(List<Logbook.Run> runs)
	{
		int[] gold = new int[Waves.LAST + 1];
		for (Logbook.Run r : runs)
		{
			if (r.splits == null)
			{
				continue;
			}
			for (int i = 0; i < r.splits.size() && i < Waves.LAST; i++)
			{
				int t = r.splits.get(i);
				if (t > 0 && (gold[i + 1] == 0 || t < gold[i + 1]))
				{
					gold[i + 1] = t;
				}
			}
		}
		return gold;
	}

	/** Ticks a run spent on waves from..to, or 0 when it did not clear them all. */
	static int span(Logbook.Run run, int from, int to)
	{
		if (run == null || run.splits == null || run.splits.size() < to)
		{
			return 0;
		}
		int sum = 0;
		for (int w = from; w <= to; w++)
		{
			sum += run.splits.get(w - 1);
		}
		return sum;
	}

	private static int bestSpan(List<Logbook.Run> runs, int from, int to)
	{
		int best = 0;
		for (Logbook.Run r : runs)
		{
			int t = span(r, from, to);
			if (t > 0 && (best == 0 || t < best))
			{
				best = t;
			}
		}
		return best;
	}

	/**
	 * Where a run in progress stands against your best cape: ticks behind (positive) or ahead
	 * at the end of the last wave it cleared, or null with no cape to race.
	 *
	 * @param cleared the waves the run has cleared
	 * @param ticks the run's time when it cleared them
	 */
	public static Integer versusPb(List<Logbook.Run> runs, int cleared, int ticks)
	{
		Logbook.Run pb = personalBest(runs);
		if (pb == null || cleared < 1 || cleared > pb.splits.size())
		{
			return null;
		}
		return ticks - span(pb, 1, cleared);
	}

	/**
	 * The latest waves a run in progress has cleared, each against the same wave in your best
	 * cape: {wave, ticks, ticks against the cape or Integer.MIN_VALUE when there is none}.
	 */
	public static List<int[]> recent(List<Logbook.Run> runs, List<Integer> splits, int count)
	{
		Logbook.Run pb = personalBest(runs);
		List<int[]> list = new ArrayList<>();
		for (int i = Math.max(0, splits.size() - count); i < splits.size(); i++)
		{
			int t = splits.get(i);
			int delta = pb != null && i < pb.splits.size() ? t - pb.splits.get(i) : Integer.MIN_VALUE;
			list.add(new int[]{i + 1, t, delta});
		}
		Collections.reverse(list);
		return list;
	}
}
