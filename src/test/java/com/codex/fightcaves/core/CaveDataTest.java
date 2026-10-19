package com.codex.fightcaves.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The cave's data checked against what players use: the rotation table against the community's
 * rotation keys (first spawn, then the two spawns of wave 2, and wave 4 for the one pair those
 * cannot split), and spawn tiles learned per size of creature.
 */
public class CaveDataTest
{
	/** Spawn codes' letters, sorted, the way the community's key writes a wave: "NW+SW" is "NSWW". */
	private static String key(List<Spawn> spawns)
	{
		StringBuilder b = new StringBuilder();
		for (Spawn s : spawns)
		{
			b.append(s.getCode());
		}
		char[] c = b.toString().toCharArray();
		Arrays.sort(c);
		return new String(c);
	}

	@Test
	public void rotationTableMatchesTheCommunityKeys()
	{
		Map<String, Integer> keys = new HashMap<>();
		keys.put("C-NSWW", 1);
		keys.put("C-ESS", 2);
		keys.put("C-ESSW", 3);
		keys.put("NW-CS", 4);
		keys.put("NW-CES", 5);
		keys.put("NW-ESSW", 6);
		keys.put("S-ESSW", 9);
		keys.put("SE-NSW", 10);
		keys.put("SE-CSW", 11);
		keys.put("SE-SSW", 12);
		keys.put("SW-CNW", 13);
		keys.put("SW-NSW", 14);
		keys.put("SW-ESS", 15);
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			String k = Rotations.spawns(r, 1).get(0).getCode() + "-" + key(Rotations.spawns(r, 2));
			if (r == 7 || r == 8)
			{
				assertEquals("S-CNW", k);
				assertEquals(r == 7 ? "ESS" : "ESSW", key(Rotations.spawns(r, 4)));
			}
			else
			{
				assertEquals("rotation " + r, Integer.valueOf(r), keys.get(k));
			}
		}
	}

	@Test
	public void everyRotationIsTheSameLadder()
	{
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			for (int w = 1; w <= Waves.LAST; w++)
			{
				List<Placement> p = Rotations.placements(r, w);
				List<Monster> monsters = new ArrayList<>();
				for (Placement x : p)
				{
					monsters.add(x.getMonster());
				}
				assertEquals(Waves.biggestFirst(w), monsters);
			}
		}
		assertEquals(Arrays.asList(Monster.TZ_KIH, Monster.TZ_KIH, Monster.TZ_KEK, Monster.TOK_XIL, Monster.YT_MEJKOT, Monster.KET_ZEK), Waves.of(58));
		assertEquals(Arrays.asList(Monster.KET_ZEK, Monster.KET_ZEK), Waves.of(62));
	}

	@Test
	public void spawnTilesAreLearnedForEachSize()
	{
		Map<String, double[]> anchors = new HashMap<>();
		double sx = CaveMap.BASE_X + Spawn.S.getTileX();
		double sy = CaveMap.BASE_Y + Spawn.S.getTileY();
		// A mager's corner lands two tiles south-west of a bat's on the same spawn point.
		CaveMap.learn(anchors, Spawn.S, 5, sx - 2, sy - 2);
		CaveMap.learn(anchors, Spawn.S, 1, sx, sy);
		CaveMap map = new CaveMap(anchors);
		assertArrayEquals(new int[]{(int) sx - 2, (int) sy - 2}, map.tile(Spawn.S, 5));
		assertArrayEquals(new int[]{(int) sx, (int) sy}, map.tile(Spawn.S, 1));
		// A size never seen there falls back to the spawn's average.
		assertArrayEquals(new int[]{(int) sx - 1, (int) sy - 1}, map.tile(Spawn.S, 3));
		assertEquals(Spawn.S, map.classify(sx - 2, sy - 2));
	}

	@Test
	public void farOffSightingsAreNotLearned()
	{
		Map<String, double[]> anchors = new HashMap<>();
		CaveMap.learn(anchors, Spawn.NW, 5, CaveMap.BASE_X, CaveMap.BASE_Y);
		assertEquals(0, anchors.size());
	}
}
