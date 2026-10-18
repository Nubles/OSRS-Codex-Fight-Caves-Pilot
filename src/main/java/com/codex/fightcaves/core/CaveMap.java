package com.codex.fightcaves.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where the five spawn points are on the floor. Until a creature has been seen appearing at a
 * spawn point, its place is guessed from the cave's outline; every creature seen appearing then
 * pins its spawn point to the tile it really used. Coordinates are the cave's own, which stay
 * the same from run to run even though each run gets a fresh copy of the cave.
 */
public final class CaveMap
{
	private final int minX;
	private final int minY;
	private final int maxX;
	private final int maxY;
	private final Map<Spawn, double[]> learned = new EnumMap<>(Spawn.class);

	/** @param minX the cave floor's bounds, inclusive */
	public CaveMap(int minX, int minY, int maxX, int maxY, Map<String, double[]> anchors)
	{
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		if (anchors != null)
		{
			for (Map.Entry<String, double[]> e : anchors.entrySet())
			{
				double[] a = e.getValue();
				if (a != null && a.length == 3 && a[2] > 0)
				{
					try
					{
						Spawn s = Spawn.ofCode(e.getKey());
						if (inside(a[0] / a[2], a[1] / a[2]))
						{
							learned.put(s, new double[]{a[0] / a[2], a[1] / a[2]});
						}
					}
					catch (IllegalArgumentException ignored)
					{
						// An unknown code from a newer save; skip it.
					}
				}
			}
		}
	}

	private boolean inside(double x, double y)
	{
		return x >= minX - 2 && x <= maxX + 2 && y >= minY - 2 && y <= maxY + 2;
	}

	public int getMinX()
	{
		return minX;
	}

	public int getMinY()
	{
		return minY;
	}

	public int getMaxX()
	{
		return maxX;
	}

	public int getMaxY()
	{
		return maxY;
	}

	/** True once a creature has been seen appearing at this spawn point. */
	public boolean known(Spawn spawn)
	{
		return learned.containsKey(spawn);
	}

	/** The middle of a spawn point, in cave coordinates. */
	public double[] centre(Spawn spawn)
	{
		double[] a = learned.get(spawn);
		if (a != null)
		{
			return a.clone();
		}
		return new double[]{
			minX + spawn.getFx() * (maxX - minX),
			minY + spawn.getFy() * (maxY - minY)
		};
	}

	/**
	 * The spawn point a creature appeared at, from the middle of its body: the nearest one.
	 */
	public Spawn classify(double cx, double cy)
	{
		Spawn best = null;
		double bestD = Double.MAX_VALUE;
		for (Spawn s : Spawn.values())
		{
			double[] c = centre(s);
			double d = (c[0] - cx) * (c[0] - cx) + (c[1] - cy) * (c[1] - cy);
			if (d < bestD)
			{
				bestD = d;
				best = s;
			}
		}
		return best;
	}

	/** The south-west tile a creature of this size appears on at a spawn point. */
	public int[] tile(Spawn spawn, Monster m)
	{
		double[] c = centre(spawn);
		double half = (m.getSize() - 1) / 2.0;
		return new int[]{(int) Math.round(c[0] - half), (int) Math.round(c[1] - half)};
	}

	/** Adds one sighting to the saved anchors. */
	public static void learn(Map<String, double[]> anchors, Spawn spawn, double cx, double cy)
	{
		double[] a = anchors.computeIfAbsent(spawn.getCode(), k -> new double[3]);
		// Keep the average from drifting forever: after 20 sightings, older ones count less.
		if (a[2] >= 20)
		{
			double keep = 19 / a[2];
			a[0] *= keep;
			a[1] *= keep;
			a[2] = 19;
		}
		a[0] += cx;
		a[1] += cy;
		a[2] += 1;
	}
}
