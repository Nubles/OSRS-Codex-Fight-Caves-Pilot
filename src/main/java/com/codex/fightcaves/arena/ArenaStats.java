package com.codex.fightcaves.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What an arena's log says, worked out from your own attempts only: how many and how many
 * finished, how far each got, the wave each death came on as a share of the attempts that reached
 * it, and the ghost of your best run with any run set against it.
 */
public final class ArenaStats
{
	/** Attempts that must have reached a wave before its death rate is stated. */
	public static final int MIN_STARTS = 5;
	/** Waves a report names as the most time lost. */
	public static final int WORST = 3;

	private ArenaStats()
	{
	}

	/** How often you die on one wave: of the attempts that began it, how many died there. */
	public static final class WaveRisk
	{
		public final int wave;
		public final int starts;
		public final int deaths;

		WaveRisk(int wave, int starts, int deaths)
		{
			this.wave = wave;
			this.starts = starts;
			this.deaths = deaths;
		}

		/** True when enough attempts reached the wave for its rate to be said. */
		public boolean known()
		{
			return starts >= MIN_STARTS;
		}

		/** Deaths over starts, 0 to 1; NaN with no starts. */
		public double rate()
		{
			return starts == 0 ? Double.NaN : deaths / (double) starts;
		}
	}

	/** Every wave's death rate, index 1 to the arena's last wave (index 0 unused). */
	public static WaveRisk[] risks(Arena arena, List<ArenaLog.Run> runs)
	{
		int n = arena.getWaves();
		int[] starts = new int[n + 1];
		int[] deaths = new int[n + 1];
		for (ArenaLog.Run r : runs)
		{
			int from = Math.max(1, r.firstWave);
			for (int w = from; w <= Math.min(n, r.reached); w++)
			{
				starts[w]++;
			}
			if (r.result == ArenaLog.Result.DIED && r.reached >= 1 && r.reached <= n)
			{
				deaths[r.reached]++;
			}
		}
		WaveRisk[] out = new WaveRisk[n + 1];
		for (int w = 1; w <= n; w++)
		{
			out[w] = new WaveRisk(w, starts[w], deaths[w]);
		}
		out[0] = new WaveRisk(0, 0, 0);
		return out;
	}

	/** Up to {@code count} waves with a stated rate, deadliest first. */
	public static List<WaveRisk> deadliest(WaveRisk[] risks, int count)
	{
		List<WaveRisk> list = new ArrayList<>();
		for (int w = 1; w < risks.length; w++)
		{
			if (risks[w].known() && risks[w].deaths > 0)
			{
				list.add(risks[w]);
			}
		}
		list.sort((a, b) -> Double.compare(b.rate(), a.rate()) != 0 ? Double.compare(b.rate(), a.rate()) : a.wave - b.wave);
		return new ArrayList<>(list.subList(0, Math.min(count, list.size())));
	}

	/** Deaths by wave, index 1 to the last wave. */
	public static int[] deaths(Arena arena, List<ArenaLog.Run> runs)
	{
		int[] d = new int[arena.getWaves() + 1];
		for (ArenaLog.Run r : runs)
		{
			if (r.result == ArenaLog.Result.DIED && r.reached >= 1 && r.reached < d.length)
			{
				d[r.reached]++;
			}
		}
		return d;
	}

	/** Your fastest whole run, timed from wave 1, or null. */
	public static ArenaLog.Run best(Arena arena, List<ArenaLog.Run> runs)
	{
		ArenaLog.Run best = null;
		for (ArenaLog.Run r : runs)
		{
			if (r.result == ArenaLog.Result.COMPLETE && r.timedThrough() >= arena.getWaves()
				&& (best == null || r.clockAt(arena.getWaves()) < best.clockAt(arena.getWaves())))
			{
				best = r;
			}
		}
		return best;
	}

	/** The ghost to race. */
	public static final class Ghost
	{
		public final ArenaLog.Run run;
		/** True when it is a whole run; otherwise your furthest. */
		public final boolean complete;

		Ghost(ArenaLog.Run run, boolean complete)
		{
			this.run = run;
			this.complete = complete;
		}

		public int cleared()
		{
			return run.timedThrough();
		}

		public int clockAt(int wave)
		{
			return wave <= cleared() ? run.clockAt(wave) : 0;
		}
	}

	/**
	 * The ghost among {@code runs}, leaving {@code not} out: your fastest whole run, or before
	 * your first, the attempt that cleared the most waves timed from wave 1 (the quicker of two
	 * that went as far). Null when no attempt cleared a timed wave.
	 */
	public static Ghost ghost(Arena arena, List<ArenaLog.Run> runs, ArenaLog.Run not)
	{
		List<ArenaLog.Run> others = new ArrayList<>(runs);
		others.remove(not);
		ArenaLog.Run best = best(arena, others);
		if (best != null)
		{
			return new Ghost(best, true);
		}
		ArenaLog.Run far = null;
		for (ArenaLog.Run r : others)
		{
			int c = r.timedThrough();
			if (c == 0)
			{
				continue;
			}
			if (far == null || c > far.timedThrough() || c == far.timedThrough() && r.clockAt(c) < far.clockAt(c))
			{
				far = r;
			}
		}
		return far == null ? null : new Ghost(far, false);
	}

	/** One wave of a run beside the same wave of the ghost. */
	public static final class Split
	{
		public final int wave;
		public final int ticks;
		public final int ghostTicks;
		/** The run's clock against the ghost's at the end of this wave (positive is behind), or null. */
		public final Integer behind;

		Split(int wave, int ticks, int ghostTicks, Integer behind)
		{
			this.wave = wave;
			this.ticks = ticks;
			this.ghostTicks = ghostTicks;
			this.behind = behind;
		}

		/** Ticks lost on this wave against the ghost, or null when either is untimed. */
		public Integer lost()
		{
			return ticks > 0 && ghostTicks > 0 ? ticks - ghostTicks : null;
		}
	}

	/** A finished run set against its ghost. */
	public static final class Report
	{
		public final List<Split> splits;
		public final List<Split> slowest;
		/** At the last wave both cleared, or null. */
		public final Integer behind;
		public final int through;

		Report(List<Split> splits, List<Split> slowest, Integer behind, int through)
		{
			this.splits = splits;
			this.slowest = slowest;
			this.behind = behind;
			this.through = through;
		}
	}

	/** {@code run} against {@code ghost}, over the waves the run cleared. */
	public static Report report(Ghost ghost, ArenaLog.Run run)
	{
		List<Split> list = new ArrayList<>();
		List<Split> slow = new ArrayList<>();
		Integer behind = null;
		int through = 0;
		int clock = 0;
		boolean clockKnown = !run.partial;
		for (int w = 1; w <= run.cleared && w <= run.splits.size(); w++)
		{
			int t = run.splits.get(w - 1);
			if (t <= 0)
			{
				clockKnown = false;
			}
			clock += t;
			int g = ghost.clockAt(w);
			int gt = w <= ghost.run.splits.size() && w <= ghost.cleared() ? ghost.run.splits.get(w - 1) : 0;
			Integer b = clockKnown && g > 0 ? clock - g : null;
			Split s = new Split(w, t, gt, b);
			list.add(s);
			if (b != null)
			{
				behind = b;
				through = w;
			}
			if (s.lost() != null && s.lost() > 0)
			{
				slow.add(s);
			}
		}
		slow.sort((a, b) -> b.lost() - a.lost() != 0 ? b.lost() - a.lost() : a.wave - b.wave);
		return new Report(Collections.unmodifiableList(list), Collections.unmodifiableList(new ArrayList<>(slow.subList(0, Math.min(WORST, slow.size())))),
			behind, through);
	}

	/** The run's clock now: the Inferno's time inside, or the Colosseum's waves so far and the wave on now. */
	public static int clock(Arena arena, ArenaLog.Run r)
	{
		if (r == null)
		{
			return 0;
		}
		if (arena == Arena.INFERNO)
		{
			return r.ticks;
		}
		int sum = r.waveTicks;
		for (int t : r.splits)
		{
			sum += t;
		}
		return sum;
	}
}
