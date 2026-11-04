package com.codex.fightcaves.arena;

import java.util.List;

/**
 * Between Colosseum waves: take the chest or go on? An estimate from the wiki's average reward per
 * wave and your own death rate on the next wave, said as information only, and only between waves.
 *
 * <p>The rules, from oldschool.runescape.wiki/w/Fortis_Colosseum: "Players may choose to either
 * continue and risk their rewards or end the run and claim all of their hard-won spoils." and "If
 * the player dies, teleports out during a wave, or logs out, all accumulated loot will be lost."
 *
 * <p>The averages, from oldschool.runescape.wiki/w/Rewards_Chest_(Fortis_Colosseum), as read on
 * 4 November 2026: "The average wave 1 reward is worth 28,320." through "The average wave 10 reward
 * is worth 620,861." The page gives none for waves 11 and 12 ("completing wave 12 will guarantee
 * Dizana's quiver"). They are Grand Exchange prices and drift. Whether the chest holds the sum of
 * every wave's reward so far is how the page's "accumulated loot" reads; that the averages add up
 * that way is this pilot's reading, not a quote.
 */
public final class ColosseumLoot
{
	/** The wiki's average reward per wave, waves 1 to 10, in coins. */
	static final long[] AVERAGE = {28_320, 43_449, 50_086, 61_162, 121_754, 123_496, 271_990, 306_586, 484_378, 620_861};

	private ColosseumLoot()
	{
	}

	/** The wiki's average reward for a wave, or -1 where it gives none. */
	public static long average(int wave)
	{
		return wave >= 1 && wave <= AVERAGE.length ? AVERAGE[wave - 1] : -1;
	}

	/** The wiki averages of waves 1 to {@code cleared} added up, or -1 past the last average. */
	public static long chest(int cleared)
	{
		if (cleared < 1 || cleared > AVERAGE.length)
		{
			return -1;
		}
		long sum = 0;
		for (int w = 1; w <= cleared; w++)
		{
			sum += AVERAGE[w - 1];
		}
		return sum;
	}

	/** What the estimate can say. */
	public enum Verdict
	{
		/** Fewer than {@link ArenaStats#MIN_STARTS} of your attempts reached the next wave. */
		TOO_FEW_RUNS,
		/** The wiki gives no average for this wave or the next. */
		NO_VALUE,
		/** On your own death rate, going on one more wave pays more on average. */
		GO_ON,
		/** On your own death rate, taking the chest now pays more on average. */
		TAKE,
		/** Within a few per cent either way. */
		CLOSE
	}

	/** The estimate after clearing a wave. */
	public static final class Estimate
	{
		public final int cleared;
		public final int next;
		/** Wiki averages so far, or -1. */
		public final long chest;
		/** The next wave's wiki average, or -1. */
		public final long nextAverage;
		/** Your attempts that began the next wave, and those that died on it. */
		public final int starts;
		public final int deaths;
		public final Verdict verdict;

		Estimate(int cleared, long chest, long nextAverage, int starts, int deaths, Verdict verdict)
		{
			this.cleared = cleared;
			this.next = cleared + 1;
			this.chest = chest;
			this.nextAverage = nextAverage;
			this.starts = starts;
			this.deaths = deaths;
			this.verdict = verdict;
		}

		/** Your survival of the next wave so far, 0 to 1. */
		public double survival()
		{
			return starts == 0 ? Double.NaN : 1 - deaths / (double) starts;
		}

		/** The survival above which going on pays on average: chest / (chest + next). */
		public double breakEven()
		{
			return chest > 0 && nextAverage > 0 ? chest / (double) (chest + nextAverage) : Double.NaN;
		}

		/** Going on one wave and then taking the chest, less taking it now, in coins on average. */
		public long edge()
		{
			if (chest <= 0 || nextAverage <= 0 || starts == 0)
			{
				return 0;
			}
			return Math.round(survival() * (chest + nextAverage) - chest);
		}
	}

	/** Within this share of the chest either way, the call is too close to make. */
	static final double CLOSE = 0.03;

	/** The estimate after clearing wave {@code cleared}, or null when there is no next wave. */
	public static Estimate estimate(int cleared, List<ArenaLog.Run> runs)
	{
		if (cleared < 1 || cleared >= Arena.COLOSSEUM.getWaves())
		{
			return null;
		}
		ArenaStats.WaveRisk risk = ArenaStats.risks(Arena.COLOSSEUM, runs)[cleared + 1];
		long chest = chest(cleared);
		long next = average(cleared + 1);
		Verdict v;
		if (chest < 0 || next < 0)
		{
			v = Verdict.NO_VALUE;
		}
		else if (!risk.known())
		{
			v = Verdict.TOO_FEW_RUNS;
		}
		else
		{
			Estimate e = new Estimate(cleared, chest, next, risk.starts, risk.deaths, Verdict.CLOSE);
			long edge = e.edge();
			v = Math.abs(edge) <= chest * CLOSE ? Verdict.CLOSE : edge > 0 ? Verdict.GO_ON : Verdict.TAKE;
		}
		return new Estimate(cleared, chest, next, risk.starts, risk.deaths, v);
	}
}
