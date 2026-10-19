package com.codex.fightcaves.core;

import java.util.List;

/**
 * How your runs have gone on one rotation: how many, how many caped, the best cape and the wave
 * that ends the most of them, so the panel can say "2 capes from 3 runs here, best 45:12" the
 * moment the rotation is read, and on the Waves page while you study it.
 */
public final class RotationRecord
{
	private int runs;
	private int capes;
	private int bestTicks;
	private int furthest;
	private int wall;
	private int wallFalls;

	private RotationRecord()
	{
	}

	/** The record from the runs the logbook keeps, oldest first. */
	public static RotationRecord of(List<Logbook.Run> runs, int rotation)
	{
		RotationRecord r = new RotationRecord();
		int[] falls = new int[Waves.LAST + 1];
		for (Logbook.Run run : runs)
		{
			if (run == null || run.rotation != rotation || rotation <= 0)
			{
				continue;
			}
			r.runs++;
			r.furthest = Math.max(r.furthest, Math.min(Waves.LAST, run.wave));
			if (run.result == Logbook.Result.CAPE)
			{
				r.capes++;
				if (!run.partial && run.ticks > 0 && (r.bestTicks == 0 || run.ticks < r.bestTicks))
				{
					r.bestTicks = run.ticks;
				}
			}
			else if (run.result == Logbook.Result.DIED && run.wave >= 1 && run.wave <= Waves.LAST)
			{
				falls[run.wave]++;
			}
		}
		// The wave that ended the most runs; on a tie the earlier one, which you meet first.
		for (int w = 1; w <= Waves.LAST; w++)
		{
			if (falls[w] > r.wallFalls)
			{
				r.wall = w;
				r.wallFalls = falls[w];
			}
		}
		return r;
	}

	public int getRuns()
	{
		return runs;
	}

	public int getCapes()
	{
		return capes;
	}

	/** The fastest fully timed cape on this rotation, in ticks, or 0. */
	public int getBestTicks()
	{
		return bestTicks;
	}

	/** The furthest wave reached on this rotation, or 0. */
	public int getFurthest()
	{
		return furthest;
	}

	/** The wave that ended the most runs here, or 0 when none fell. */
	public int getWall()
	{
		return wall;
	}

	/** How many runs fell on {@link #getWall()}. */
	public int getWallFalls()
	{
		return wallFalls;
	}
}
