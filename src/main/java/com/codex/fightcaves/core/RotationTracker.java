package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Works out the rotation from where waves spawn. Every rotation starts as a candidate; each
 * wave seen is compared with what every rotation would have spawned, and rotations that
 * disagree drop out. Wave 1 leaves three, wave 2 usually leaves one, and the one pair that
 * waves 1 to 3 cannot tell apart is settled on wave 4.
 *
 * Only which spawn points were used is compared, not which creature stood on each, and a
 * rotation is never ruled out for good: the one with the fewest disagreements wins, so one
 * misread spawn cannot leave the pilot with no answer.
 */
public final class RotationTracker
{
	private final int[] misses = new int[Rotations.COUNT];
	private int seen;

	public void reset()
	{
		java.util.Arrays.fill(misses, 0);
		seen = 0;
	}

	/** Restores a tracker from a saved rotation, for a run resumed after logging out. */
	public void assume(int rotation)
	{
		reset();
		if (rotation >= 1 && rotation <= Rotations.COUNT)
		{
			for (int r = 0; r < Rotations.COUNT; r++)
			{
				misses[r] = r == rotation - 1 ? 0 : 1;
			}
			seen = 1;
		}
	}

	/**
	 * Compares one wave's spawns with every rotation.
	 *
	 * @param spawns the spawn point of every creature that appeared when the wave began
	 */
	public void observe(int wave, List<Spawn> spawns)
	{
		if (wave < 1 || wave > Waves.LAST || spawns.isEmpty())
		{
			return;
		}
		Map<Spawn, Integer> seenCounts = count(spawns);
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			if (!seenCounts.equals(count(Rotations.spawns(r, wave))))
			{
				misses[r - 1]++;
			}
		}
		seen++;
	}

	private static Map<Spawn, Integer> count(List<Spawn> spawns)
	{
		Map<Spawn, Integer> map = new EnumMap<>(Spawn.class);
		for (Spawn s : spawns)
		{
			map.merge(s, 1, Integer::sum);
		}
		return map;
	}

	/** Rotations still in the running, lowest number first; all 15 before any wave is seen. */
	public List<Integer> candidates()
	{
		int best = Integer.MAX_VALUE;
		for (int m : misses)
		{
			best = Math.min(best, m);
		}
		List<Integer> list = new ArrayList<>();
		for (int r = 0; r < Rotations.COUNT; r++)
		{
			if (misses[r] == best)
			{
				list.add(r + 1);
			}
		}
		return list;
	}

	/** The rotation once only one is left, or 0. */
	public int rotation()
	{
		List<Integer> c = candidates();
		return seen > 0 && c.size() == 1 ? c.get(0) : 0;
	}

	/** True when the winner agreed with every wave seen. */
	public boolean clean()
	{
		int r = rotation();
		return r > 0 && misses[r - 1] == 0;
	}

	public int wavesSeen()
	{
		return seen;
	}

	/**
	 * The first wave from here on whose spawns would split the remaining candidates, or 0 when
	 * nothing left to see would split them.
	 */
	public int decidingWave(int from)
	{
		List<Integer> c = candidates();
		if (c.size() < 2)
		{
			return 0;
		}
		for (int w = Math.max(1, from); w <= Waves.LAST; w++)
		{
			Map<Spawn, Integer> first = count(Rotations.spawns(c.get(0), w));
			for (int i = 1; i < c.size(); i++)
			{
				if (!first.equals(count(Rotations.spawns(c.get(i), w))))
				{
					return w;
				}
			}
		}
		return 0;
	}
}
