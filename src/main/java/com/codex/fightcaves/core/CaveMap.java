package com.codex.fightcaves.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where the five spawn points are on the floor. Coordinates are the cave's own (the template
 * the game copies for each run), which stay the same from run to run. Each spawn point starts
 * on its known tile; every creature seen appearing there nudges it to the tile really used, so
 * a spawn the game has moved by a tile or two still lines up.
 */
public final class CaveMap
{
	/** The south-west corner of the cave's map square. */
	public static final int BASE_X = 2368;
	public static final int BASE_Y = 5056;
	/** The walkable floor's bounds within the map square, inclusive. */
	static final int FLOOR_MIN_X = 3;
	static final int FLOOR_MAX_X = 56;
	static final int FLOOR_MIN_Y = 6;
	static final int FLOOR_MAX_Y = 61;
	/** How far a learned spawn may sit from its known tile before it is treated as a misreading. */
	private static final double DRIFT = 6;

	/** Saved anchor keys for one size of creature are the spawn code, this, and the size. */
	static final String SIZE = "#";

	private final Map<Spawn, double[]> learned = new EnumMap<>(Spawn.class);
	/** "code#size" to the average south-west tile creatures of that size appeared on. */
	private final Map<String, double[]> bySize = new java.util.HashMap<>();

	/** @param anchors saved sightings: spawn code to {sum of x, sum of y, count} of south-west tiles */
	public CaveMap(Map<String, double[]> anchors)
	{
		if (anchors == null)
		{
			return;
		}
		for (Map.Entry<String, double[]> e : anchors.entrySet())
		{
			double[] a = e.getValue();
			if (a == null || a.length != 3 || a[2] <= 0)
			{
				continue;
			}
			try
			{
				String key = e.getKey();
				int hash = key.indexOf(SIZE);
				Spawn s = Spawn.ofCode(hash < 0 ? key : key.substring(0, hash));
				double x = a[0] / a[2];
				double y = a[1] / a[2];
				double[] k = known(s);
				if (Math.abs(x - k[0]) <= DRIFT && Math.abs(y - k[1]) <= DRIFT)
				{
					if (hash < 0)
					{
						learned.put(s, new double[]{x, y});
					}
					else
					{
						bySize.put(key, new double[]{x, y});
					}
				}
			}
			catch (IllegalArgumentException ignored)
			{
				// An unknown code from a newer save; skip it.
			}
		}
	}

	/** A spawn's known tile in the cave's own coordinates. */
	static double[] known(Spawn s)
	{
		return new double[]{BASE_X + s.getTileX(), BASE_Y + s.getTileY()};
	}

	/** True once a creature has been seen appearing at this spawn point. */
	public boolean seen(Spawn spawn)
	{
		return learned.containsKey(spawn);
	}

	/** The south-west tile creatures appear on at a spawn point, in cave coordinates. */
	public double[] anchor(Spawn spawn)
	{
		double[] a = learned.get(spawn);
		return a != null ? a.clone() : known(spawn);
	}

	/**
	 * The spawn point a creature appeared at, from the south-west tile of its body: the nearest
	 * one. The points are at least fourteen tiles apart, so a tile or two of error cannot
	 * confuse them.
	 */
	public Spawn classify(double x, double y)
	{
		Spawn best = null;
		double bestD = Double.MAX_VALUE;
		for (Spawn s : Spawn.values())
		{
			double[] c = anchor(s);
			double d = (c[0] - x) * (c[0] - x) + (c[1] - y) * (c[1] - y);
			if (d < bestD)
			{
				bestD = d;
				best = s;
			}
		}
		return best;
	}

	/** The south-west tile a creature appears on at a spawn point. */
	public int[] tile(Spawn spawn)
	{
		double[] c = anchor(spawn);
		return new int[]{(int) Math.round(c[0]), (int) Math.round(c[1])};
	}

	/**
	 * The south-west tile a creature of this size appears on at a spawn point: where that size
	 * was seen there before, otherwise where any creature was. A mager's body is five tiles
	 * across and a bat's one, so the game may not put both corners on the same tile.
	 */
	public int[] tile(Spawn spawn, int size)
	{
		double[] c = bySize.get(spawn.getCode() + SIZE + size);
		if (c == null)
		{
			return tile(spawn);
		}
		return new int[]{(int) Math.round(c[0]), (int) Math.round(c[1])};
	}

	/** Adds one sighting, by the creature's south-west tile, to the saved anchors. */
	public static void learn(Map<String, double[]> anchors, Spawn spawn, double x, double y)
	{
		learn(anchors, spawn.getCode(), spawn, x, y);
	}

	/** Adds one sighting to the spawn's anchor and to the anchor for creatures of its size. */
	public static void learn(Map<String, double[]> anchors, Spawn spawn, int size, double x, double y)
	{
		learn(anchors, spawn.getCode(), spawn, x, y);
		learn(anchors, spawn.getCode() + SIZE + size, spawn, x, y);
	}

	private static void learn(Map<String, double[]> anchors, String key, Spawn spawn, double x, double y)
	{
		double[] k = known(spawn);
		if (Math.abs(x - k[0]) > DRIFT || Math.abs(y - k[1]) > DRIFT)
		{
			return;
		}
		double[] a = anchors.computeIfAbsent(key, c -> new double[3]);
		// Keep the average from drifting forever: after 20 sightings, older ones count less.
		if (a[2] >= 20)
		{
			double keep = 19 / a[2];
			a[0] *= keep;
			a[1] *= keep;
			a[2] = 19;
		}
		a[0] += x;
		a[1] += y;
		a[2] += 1;
	}

	/** A tile's place across the floor, 0 at the west edge and 1 at the east, from its x in the map square. */
	public static double fx(double tileX)
	{
		return clamp((tileX - FLOOR_MIN_X) / (FLOOR_MAX_X - FLOOR_MIN_X));
	}

	/** A tile's place up the floor, 0 at the south edge and 1 at the north, from its y in the map square. */
	public static double fy(double tileY)
	{
		return clamp((tileY - FLOOR_MIN_Y) / (FLOOR_MAX_Y - FLOOR_MIN_Y));
	}

	private static double clamp(double v)
	{
		return Math.max(0, Math.min(1, v));
	}
}
