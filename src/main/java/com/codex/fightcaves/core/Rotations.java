package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where each wave spawns in each of the cave's 15 rotations, numbered as players number them.
 * A row is one wave; it lists the 15 rotations in order, and each rotation lists the spawn
 * point of every creature in the wave, smallest creature first. "SE+C" on wave 4 means the
 * Tz-Kih appears in the south-east and the Tz-Kek in the centre.
 */
public final class Rotations
{
	public static final int COUNT = 15;

	private static final String[] TABLE = {
		"C C C NW NW NW S S S SE SE SE SW SW SW",
		"NW+SW S+SE SE+SW C+S C+SE SW+SE NW+C NW+C SE+SW S+NW SW+C SW+S C+NW S+NW SE+S",
		"SW SE SW S SE SE C C SW NW C S NW NW S",
		"S+SE C+SW NW+S SW+SE S+SW NW+S SE+S SW+SE NW+C SE+C SW+NW C+NW SE+SW S+C C+NW",
		"NW+C+S NW+SW+C C+S+NW C+NW+SW NW+C+S C+SE+NW SW+C+SE S+NW+SW SW+SE+NW SW+S+SE SE+S+SW S+SE+C S+NW+SE SE+SW+S SE+SW+C",
		"NW+C NW+SW C+S C+NW NW+C C+SE SW+C S+NW SW+SE SW+S SE+S S+SE S+NW SE+SW SE+SW",
		"C SW S NW C SE C NW SE S S SE NW SW SW",
		"SW+SE S+SE SW+SE SE+SW SE+S S+SW SW+NW S+C NW+S C+NW C+NW C+SW SE+C NW+C NW+S",
		"S+NW+SW NW+C+S C+NW+SW S+NW+SE SW+C+SE NW+C+S SE+S+SW SE+SW+S C+SE+NW S+SE+C SE+SW+C NW+SW+C SW+S+SE SW+SE+NW C+S+NW",
		"NW+S C+NW NW+C NW+S C+SW C+NW S+SE SW+SE SE+C SE+S SW+SE SW+NW S+SW SE+SW S+C",
		"S+C+NW SW+SE+C SE+SW+NW SE+C+NW SW+NW+C SE+S+C C+NW+S NW+C+SW S+SW+SE C+SW+SE NW+S+SW S+SE+SW C+NW+S NW+S+SE SW+SE+S",
		"SE+SW+S+C S+NW+SW+SE S+NW+SE+SW SW+S+SE+C SE+S+SW+NW SW+C+SE+S SE+SW+C+NW SW+SE+NW+C NW+C+S+SW NW+SW+C+SW C+S+NW+S NW+C+S+SE S+SE+C+NW C+SE+NW+S C+NW+SW+SE",
		"SE+SW+S S+NW+SW S+NW+SE SW+S+SE SE+S+SW SW+C+SE SE+SW+C SW+SE+NW NW+C+S NW+SW+C C+S+NW NW+C+S S+SE+C C+SE+NW C+NW+SW",
		"SE+SW S+NW S+NW SW+S SE+S SW+C SE+SW SW+SE NW+C NW+SW C+S NW+C S+SE C+SE C+NW",
		"SW NW NW S S C SW SE C SW S C SE SE NW",
		"NW+C S+C SE+C C+NW C+NW SW+NW NW+S NW+S SE+S S+SE SW+SE SW+SE C+SW S+SW SE+SW",
		"SW+SE+NW SE+SW+S SW+S+SE S+SE+C SE+SW+C SE+S+SW C+S+NW C+SE+NW SW+C+SE NW+C+S C+NW+SW S+NW+SW NW+SW+C NW+C+S S+NW+SE",
		"SE+SW SW+SE S+SW SE+S SW+SE S+SE S+C SE+C C+SW C+NW NW+C NW+S SW+NW C+NW NW+S",
		"NW+S+SE NW+C+SW C+NW+S C+SW+SE NW+S+SW C+NW+S SW+SE+S S+SW+SE SW+NW+C SW+SE+C SE+SW+NW S+C+NW S+SE+SW SE+S+C SE+C+NW",
		"C+SE+NW+S SW+SE+NW+C S+SE+C+NW NW+SW+C+SW C+S+NW+S SE+SW+C+NW C+NW+SW+SE NW+C+S+SW SE+S+SW+NW S+NW+SW+SE S+NW+SE+SW SE+SW+S+C NW+C+S+SE SW+C+SE+S SW+S+SE+C",
		"C+SE+NW SW+SE+NW S+SE+C NW+SW+C C+S+NW SE+SW+C C+NW+SW NW+C+S SE+S+SW S+NW+SW S+NW+SE SE+SW+S NW+C+S SW+C+SE SW+S+SE",
		"SE+C SE+SW SE+S SW+NW S+C SW+SE NW+C C+NW S+SE NW+S NW+S SW+SE C+NW C+SW S+SW",
		"S+SW+SE NW+S+SE C+SW+SE S+SE+SW SW+SE+S NW+S+SW SE+SW+NW SE+S+C C+NW+S S+C+NW SE+C+NW NW+C+SW SW+SE+C SW+NW+C C+NW+S",
		"NW+C+S+SW C+SE+NW+S NW+SW+C+SW NW+C+S+SE C+NW+SW+SE C+S+NW+S S+NW+SE+SW SW+C+SE+S SE+SW+C+NW SE+SW+S+C SW+S+SE+C SW+SE+NW+C S+NW+SW+SE SE+S+SW+NW S+SE+C+NW",
		"C+NW+S SE+C+NW SW+NW+C C+NW+S NW+C+SW S+C+NW NW+S+SE C+SW+SE SW+SE+C SW+SE+S S+SW+SE SE+SW+NW NW+S+SW S+SE+SW SE+S+C",
		"SE+S+C+NW S+SW+SE+C S+SE+SW+NW SW+SE+C+NW SE+SW+NW+C SW+SE+S+C SE+C+NW+S SW+NW+C+SW NW+S+SW+SE NW+C+SW+SE C+NW+S+SW NW+S+SE+SW S+C+NW+S C+NW+S+SE C+SW+SE+S",
		"SW+C+SE+S+C NW+C+S+SW+SE NW+C+S+SE+SW S+NW+SW+SE+C S+NW+SE+SW+NW C+NW+SW+SE+S SW+S+SE+C+NW SE+S+SW+NW+C C+S+NW+S+SW SW+SE+NW+C+SW S+SE+C+NW+S C+SE+NW+S+SE SE+SW+S+C+NW SE+SW+C+NW+S NW+SW+C+SW+SE",
		"SW+C+SE+S NW+C+S+SW NW+C+S+SE S+NW+SW+SE S+NW+SE+SW C+NW+SW+SE SW+S+SE+C SE+S+SW+NW C+S+NW+S SW+SE+NW+C S+SE+C+NW C+SE+NW+S SE+SW+S+C SE+SW+C+NW NW+SW+C+SW",
		"SW+C+SE NW+C+S NW+C+S S+NW+SW S+NW+SE C+NW+SW SW+S+SE SE+S+SW C+S+NW SW+SE+NW S+SE+C C+SE+NW SE+SW+S SE+SW+C NW+SW+C",
		"SW+C NW+C NW+C S+NW S+NW C+NW SW+S SE+S C+S SW+SE S+SE C+SE SE+SW SE+SW NW+SW",
		"C C C NW NW NW S S S SE SE SE SW SW SW",
		"SW+NW SE+S SW+SE S+C SE+C SE+SW C+NW C+NW SW+SE NW+S C+SW S+SW NW+C NW+S S+SE",
		"SE+S+SW SW+C+SE S+NW+SW SE+SW+S SW+S+SE S+NW+SE S+SE+C SE+SW+C C+NW+SW C+SE+NW NW+SW+C NW+C+S SW+SE+NW C+S+NW NW+C+S",
		"S+SE C+SW NW+S SW+SE S+SW NW+S SE+S SW+SE NW+C SE+C SW+NW C+NW SE+SW S+C C+NW",
		"C+NW+S SW+NW+C S+C+NW NW+C+SW C+NW+S SE+C+NW C+SW+SE NW+S+SW SE+SW+NW S+SW+SE S+SE+SW SE+S+C NW+S+SE SW+SE+S SW+SE+C",
		"SE+SW+C+NW SE+S+SW+NW SE+SW+S+C SW+SE+NW+C S+SE+C+NW SW+S+SE+C NW+SW+C+SW C+S+NW+S S+NW+SE+SW NW+C+S+SW NW+C+S+SE SW+C+SE+S C+SE+NW+S C+NW+SW+SE S+NW+SW+SE",
		"SE+SW+C SE+S+SW SE+SW+S SW+SE+NW S+SE+C SW+S+SE NW+SW+C C+S+NW S+NW+SE NW+C+S NW+C+S SW+C+SE C+SE+NW C+NW+SW S+NW+SW",
		"SW+SE S+SE SW+SE SE+SW SE+S S+SW SW+NW S+C NW+S C+NW C+NW C+SW SE+C NW+C NW+S",
		"NW+S+SW C+NW+S NW+C+SW NW+S+SE C+SW+SE C+NW+S S+SE+SW SW+SE+S SE+C+NW SE+S+C SW+SE+C SW+NW+C S+SW+SE SE+SW+NW S+C+NW",
		"C+S+NW+S SE+SW+C+NW SW+SE+NW+C C+SE+NW+S NW+SW+C+SW S+SE+C+NW NW+C+S+SE C+NW+SW+SE SW+S+SE+C SW+C+SE+S S+NW+SW+SE SE+S+SW+NW NW+C+S+SW S+NW+SE+SW SE+SW+S+C",
		"S+C+NW SW+SE+C SE+SW+NW SE+C+NW SW+NW+C SE+S+C C+NW+S NW+C+SW S+SW+SE C+SW+SE NW+S+SW S+SE+SW C+NW+S NW+S+SE SW+SE+S",
		"SW+SE+S+C NW+S+SW+SE NW+S+SE+SW S+SW+SE+C S+SE+SW+NW C+SW+SE+S SW+SE+C+NW SE+SW+NW+C C+NW+S+SW SW+NW+C+SW S+C+NW+S C+NW+S+SE SE+S+C+NW SE+C+NW+S NW+C+SW+SE",
		"C+NW+SW+SE+S C+S+NW+S+SW C+SE+NW+S+SE NW+C+S+SW+SE NW+C+S+SE+SW NW+SW+C+SW+SE S+NW+SW+SE+C S+NW+SE+SW+NW S+SE+C+NW+S SE+S+SW+NW+C SE+SW+S+C+NW SE+SW+C+NW+S SW+C+SE+S+C SW+S+SE+C+NW SW+SE+NW+C+SW",
		"C+NW+SW+SE C+S+NW+S C+SE+NW+S NW+C+S+SW NW+C+S+SE NW+SW+C+SW S+NW+SW+SE S+NW+SE+SW S+SE+C+NW SE+S+SW+NW SE+SW+S+C SE+SW+C+NW SW+C+SE+S SW+S+SE+C SW+SE+NW+C",
		"C+NW+SW C+S+NW C+SE+NW NW+C+S NW+C+S NW+SW+C S+NW+SW S+NW+SE S+SE+C SE+S+SW SE+SW+S SE+SW+C SW+C+SE SW+S+SE SW+SE+NW",
		"NW+C S+C SE+C C+NW C+NW SW+NW NW+S NW+S SE+S S+SE SW+SE SW+SE C+SW S+SW SE+SW",
		"SE+SW+NW SW+SE+S S+SW+SE SE+S+C SW+SE+C S+SE+SW S+C+NW SE+C+NW C+SW+SE C+NW+S NW+C+SW NW+S+SW SW+NW+C C+NW+S NW+S+SE",
		"S+NW+SE+SW C+NW+SW+SE NW+C+S+SW SW+C+SE+S S+NW+SW+SE NW+C+S+SE SE+SW+S+C SW+S+SE+C NW+SW+C+SW SE+SW+C+NW SW+SE+NW+C C+S+NW+S SE+S+SW+NW S+SE+C+NW C+SE+NW+S",
		"NW+S+SE NW+C+SW C+NW+S C+SW+SE NW+S+SW C+NW+S SW+SE+S S+SW+SE SW+NW+C SW+SE+C SE+SW+NW S+C+NW S+SE+SW SE+S+C SE+C+NW",
		"SE+C+NW+S SE+SW+NW+C SE+S+C+NW SW+NW+C+SW S+C+NW+S SW+SE+C+NW NW+C+SW+SE C+NW+S+SW S+SE+SW+NW NW+S+SW+SE NW+S+SE+SW SW+SE+S+C C+NW+S+SE C+SW+SE+S S+SW+SE+C",
		"SW+S+SE+C+NW S+NW+SE+SW+NW SW+C+SE+S+C SE+S+SW+NW+C SE+SW+S+C+NW S+NW+SW+SE+C SW+SE+NW+C+SW S+SE+C+NW+S NW+C+S+SE+SW C+S+NW+S+SW C+SE+NW+S+SE C+NW+SW+SE+S SE+SW+C+NW+S NW+SW+C+SW+SE NW+C+S+SW+SE",
		"SW+S+SE+C S+NW+SE+SW SW+C+SE+S SE+S+SW+NW SE+SW+S+C S+NW+SW+SE SW+SE+NW+C S+SE+C+NW NW+C+S+SE C+S+NW+S C+SE+NW+S C+NW+SW+SE SE+SW+C+NW NW+SW+C+SW NW+C+S+SW",
		"S+SW+SE NW+S+SE C+SW+SE S+SE+SW SW+SE+S NW+S+SW SE+SW+NW SE+S+C C+NW+S S+C+NW SE+C+NW NW+C+SW SW+SE+C SW+NW+C C+NW+S",
		"C+NW+S+SW SE+C+NW+S SW+NW+C+SW C+NW+S+SE NW+C+SW+SE S+C+NW+S NW+S+SE+SW C+SW+SE+S SW+SE+C+NW SW+SE+S+C S+SW+SE+C SE+SW+NW+C NW+S+SW+SE S+SE+SW+NW SE+S+C+NW",
		"S+SE+C+NW+S SW+S+SE+C+NW SE+S+SW+NW+C SE+SW+C+NW+S SW+SE+NW+C+SW SE+SW+S+C+NW C+SE+NW+S+SE NW+SW+C+SW+SE S+NW+SW+SE+C C+NW+SW+SE+S NW+C+S+SW+SE S+NW+SE+SW+NW C+S+NW+S+SW NW+C+S+SE+SW SW+C+SE+S+C",
		"SE+S+C+NW S+SW+SE+C S+SE+SW+NW SW+SE+C+NW SE+SW+NW+C SW+SE+S+C SE+C+NW+S SW+NW+C+SW NW+S+SW+SE NW+C+SW+SE C+NW+S+SW NW+S+SE+SW S+C+NW+S C+NW+S+SE C+SW+SE+S",
		"C+SW+SE+S+C C+NW+S+SW+SE C+NW+S+SE+SW NW+S+SW+SE+C NW+S+SE+SW+NW NW+C+SW+SE+S S+SW+SE+C+NW S+SE+SW+NW+C S+C+NW+S+SW SE+SW+NW+C+SW SE+S+C+NW+S SE+C+NW+S+SE SW+SE+S+C+NW SW+SE+C+NW+S SW+NW+C+SW+SE",
		"NW+SW+C+SW+SE+S S+SE+C+NW+S+SW SE+SW+C+NW+S+SE C+S+NW+S+SW+SE C+SE+NW+S+SE+SW SW+SE+NW+C+SW+SE NW+C+S+SW+SE+C NW+C+S+SE+SW+NW SE+SW+S+C+NW+S S+NW+SE+SW+NW+C SW+C+SE+S+C+NW SW+S+SE+C+NW+S C+NW+SW+SE+S+C S+NW+SW+SE+C+NW SE+S+SW+NW+C+SW",
		"NW+SW+C+SW+SE S+SE+C+NW+S SE+SW+C+NW+S C+S+NW+S+SW C+SE+NW+S+SE SW+SE+NW+C+SW NW+C+S+SW+SE NW+C+S+SE+SW SE+SW+S+C+NW S+NW+SE+SW+NW SW+C+SE+S+C SW+S+SE+C+NW C+NW+SW+SE+S S+NW+SW+SE+C SE+S+SW+NW+C",
		"NW+SW+C+SW S+SE+C+NW SE+SW+C+NW C+S+NW+S C+SE+NW+S SW+SE+NW+C NW+C+S+SW NW+C+S+SE SE+SW+S+C S+NW+SE+SW SW+C+SE+S SW+S+SE+C C+NW+SW+SE S+NW+SW+SE SE+S+SW+NW",
		"NW+SW+C S+SE+C SE+SW+C C+S+NW C+SE+NW SW+SE+NW NW+C+S NW+C+S SE+SW+S S+NW+SE SW+C+SE SW+S+SE C+NW+SW S+NW+SW SE+S+SW",
		"NW+SW S+SE SE+SW C+S C+SE SW+SE NW+C NW+C SE+SW S+NW SW+C SW+S C+NW S+NW SE+S",
		"SW SE SW S SE SE C C SW NW C S NW NW S"
	};

	private static final Spawn[][][] SPAWNS = parse();

	private Rotations()
	{
	}

	private static Spawn[][][] parse()
	{
		Spawn[][][] out = new Spawn[Waves.LAST + 1][][];
		for (int w = 1; w <= Waves.LAST; w++)
		{
			String[] rotations = TABLE[w - 1].split(" ");
			if (rotations.length != COUNT)
			{
				throw new IllegalStateException("Wave " + w + " lists " + rotations.length + " rotations");
			}
			out[w] = new Spawn[COUNT][];
			int creatures = Waves.of(w).size();
			for (int r = 0; r < COUNT; r++)
			{
				String[] codes = rotations[r].split("\\+");
				if (codes.length != creatures)
				{
					throw new IllegalStateException("Wave " + w + " rotation " + (r + 1) + " places " + codes.length + " of " + creatures);
				}
				out[w][r] = new Spawn[codes.length];
				for (int i = 0; i < codes.length; i++)
				{
					out[w][r][i] = Spawn.ofCode(codes[i]);
				}
			}
		}
		return out;
	}

	/**
	 * Where a wave's creatures appear in a rotation, biggest creature first.
	 *
	 * @param rotation 1 to 15
	 * @param wave 1 to 63
	 */
	public static List<Placement> placements(int rotation, int wave)
	{
		if (rotation < 1 || rotation > COUNT || wave < 1 || wave > Waves.LAST)
		{
			return Collections.emptyList();
		}
		List<Monster> monsters = Waves.of(wave);
		Spawn[] spawns = SPAWNS[wave][rotation - 1];
		List<Placement> list = new ArrayList<>();
		for (int i = monsters.size() - 1; i >= 0; i--)
		{
			list.add(new Placement(monsters.get(i), spawns[i]));
		}
		return list;
	}

	/** The spawn points a wave uses in a rotation, in the order {@link #placements} gives them. */
	public static List<Spawn> spawns(int rotation, int wave)
	{
		List<Spawn> list = new ArrayList<>();
		for (Placement p : placements(rotation, wave))
		{
			list.add(p.getSpawn());
		}
		return list;
	}
}
