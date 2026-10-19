package com.codex.fightcaves.core;

import java.util.List;

/**
 * One finished run told as a keepsake: which cape it was, how each wave went against every run
 * before it, and the facts worth saying about it. It feeds the run scroll on the Records page
 * and at the cave mouth. Each wave is weighed only against runs that ended earlier, so a run
 * keeps the story it had on the day, however many runs follow it.
 */
public final class RunStory
{
	/** How one wave of the run went. */
	public enum Tier
	{
		/** As fast as or faster than any earlier run cleared it. */
		BEST,
		/** Within a tenth of the earlier best. */
		NEAR,
		/** Slower than that. */
		PLAIN,
		/** Cleared, and no earlier run had cleared it to compare with. */
		FIRST,
		/** Cleared but not timed, such as a wave before the pilot was turned on. */
		UNTIMED,
		/** The wave the run ended on, short of a cape. */
		ENDED,
		/** Never reached. */
		UNPLAYED
	}

	/** Within this share of the earlier best, a wave counts as near it. */
	static final double NEAR = 0.10;

	private final Logbook.Run run;
	private final Tier[] tiers = new Tier[Waves.LAST + 1];
	private final int[] ticks = new int[Waves.LAST + 1];
	private final int[] best = new int[Waves.LAST + 1];
	private final int[] hurt = new int[Waves.LAST + 1];
	private int bestWaves;
	private int capeNumber;
	private Integer versusOldBest;
	private Integer versusLastCape;
	private boolean furthestYet;

	private RunStory(Logbook.Run run)
	{
		this.run = run;
	}

	/**
	 * @param runs every run the logbook keeps, oldest first
	 * @param run the run to tell, normally one of them
	 * @param capes how many capes the logbook has counted in all, since old runs are let go
	 */
	public static RunStory of(List<Logbook.Run> runs, Logbook.Run run, int capes)
	{
		RunStory s = new RunStory(run);
		int at = runs.indexOf(run);
		int earlierFurthest = 0;
		int earlierBestCape = 0;
		Logbook.Run lastCape = null;
		int capesAfter = 0;
		int capesUpTo = 0;
		for (int i = 0; i < runs.size(); i++)
		{
			Logbook.Run r = runs.get(i);
			boolean earlier = at >= 0 ? i < at : r != run && r.endedAt < run.endedAt;
			boolean cape = r.result == Logbook.Result.CAPE;
			if (at >= 0 && i > at && cape)
			{
				capesAfter++;
			}
			if (!earlier)
			{
				continue;
			}
			capesUpTo += cape ? 1 : 0;
			earlierFurthest = Math.max(earlierFurthest, r.wave);
			if (cape && !r.partial && r.ticks > 0)
			{
				earlierBestCape = earlierBestCape == 0 ? r.ticks : Math.min(earlierBestCape, r.ticks);
				lastCape = r;
			}
			if (r.splits != null)
			{
				for (int w = 1; w <= r.splits.size() && w <= Waves.LAST; w++)
				{
					Integer t = r.splits.get(w - 1);
					if (t != null && t > 0 && (s.best[w] == 0 || t < s.best[w]))
					{
						s.best[w] = t;
					}
				}
			}
		}

		boolean caped = run.result == Logbook.Result.CAPE;
		int cleared = caped ? Waves.LAST : Math.max(0, run.wave - 1);
		for (int w = 1; w <= Waves.LAST; w++)
		{
			Integer h = run.hurt != null && w <= run.hurt.size() ? run.hurt.get(w - 1) : null;
			s.hurt[w] = h == null ? 0 : Math.max(0, h);
			if (w > cleared)
			{
				s.tiers[w] = !caped && w == run.wave ? Tier.ENDED : Tier.UNPLAYED;
				continue;
			}
			Integer t = run.splits != null && w <= run.splits.size() ? run.splits.get(w - 1) : null;
			if (t == null || t <= 0)
			{
				s.tiers[w] = Tier.UNTIMED;
				continue;
			}
			s.ticks[w] = t;
			if (s.best[w] == 0)
			{
				s.tiers[w] = Tier.FIRST;
			}
			else if (t <= s.best[w])
			{
				s.tiers[w] = Tier.BEST;
				s.bestWaves++;
			}
			else
			{
				s.tiers[w] = t <= s.best[w] * (1 + NEAR) ? Tier.NEAR : Tier.PLAIN;
			}
		}

		if (caped)
		{
			// Old runs are let go, so count back from the logbook's total when the run is in it.
			s.capeNumber = at >= 0 && capes > capesAfter ? capes - capesAfter : capesUpTo + 1;
			if (!run.partial && run.ticks > 0)
			{
				s.versusOldBest = earlierBestCape > 0 ? run.ticks - earlierBestCape : null;
				s.versusLastCape = lastCape != null ? run.ticks - lastCape.ticks : null;
			}
		}
		s.furthestYet = !caped && earlierFurthest > 0 && run.wave > earlierFurthest;
		return s;
	}

	public Logbook.Run getRun()
	{
		return run;
	}

	/** How wave w went, waves 1 to 63. */
	public Tier tier(int w)
	{
		return tiers[w];
	}

	/** Ticks wave w took, or 0 when it was not timed. */
	public int ticks(int w)
	{
		return ticks[w];
	}

	/** The fastest any earlier run cleared wave w, or 0. */
	public int best(int w)
	{
		return best[w];
	}

	/** Damage taken on wave w. */
	public int hurt(int w)
	{
		return hurt[w];
	}

	/** Waves this run cleared at least as fast as any run before it. */
	public int getBestWaves()
	{
		return bestWaves;
	}

	/** Which cape this was, counting from your first; 0 for a run that did not cape. */
	public int getCapeNumber()
	{
		return capeNumber;
	}

	/** Ticks against your best cape before it, negative when faster; null when there was none or it does not count. */
	public Integer getVersusOldBest()
	{
		return versusOldBest;
	}

	/** Ticks against the cape just before it, negative when faster; null when there was none or it does not count. */
	public Integer getVersusLastCape()
	{
		return versusLastCape;
	}

	/** True for a run that ended further into the cave than any before it. */
	public boolean isFurthestYet()
	{
		return furthestYet;
	}

	/** The longest wave the run timed, in ticks, for scaling a chart; at least 1. */
	public int longestWave()
	{
		int max = 1;
		for (int w = 1; w <= Waves.LAST; w++)
		{
			max = Math.max(max, ticks[w]);
		}
		return max;
	}

	/** The most damage the run took on one wave; 0 when it took none. */
	public int worstHurt()
	{
		int max = 0;
		for (int w = 1; w <= Waves.LAST; w++)
		{
			max = Math.max(max, hurt[w]);
		}
		return max;
	}
}
