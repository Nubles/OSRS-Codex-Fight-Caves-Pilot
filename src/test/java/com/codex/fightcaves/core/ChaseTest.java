package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class ChaseTest
{
	/** An open 30 by 30 floor with a solid 4 by 4 pillar in the middle. */
	static Grid pillarFloor()
	{
		int[][] flags = new int[30][30];
		for (int x = 0; x < 30; x++)
		{
			for (int y = 0; y < 30; y++)
			{
				boolean edge = x == 0 || y == 0 || x == 29 || y == 29;
				boolean pillar = x >= 13 && x <= 16 && y >= 13 && y <= 16;
				if (edge || pillar)
				{
					flags[x][y] = Grid.SOLID;
				}
			}
		}
		return new Grid(flags);
	}

	@Test
	public void openFloorSeesYou()
	{
		Grid g = pillarFloor();
		assertTrue(g.sees(3, 3, 10, 5));
		assertFalse(g.sees(10, 14, 20, 14));
	}

	@Test
	public void magerBehindAPillarGetsStuck()
	{
		Grid g = pillarFloor();
		// You stand just east of the pillar; the mager spawns straight west of it.
		Chase c = Chase.run(g, Monster.KET_ZEK, 4, 13, 17, 14);
		assertEquals(Chase.End.STUCK, c.getEnd());
		assertEquals(Chase.End.AT_ONCE, Chase.run(g, Monster.KET_ZEK, 4, 20, 10, 22).getEnd());
	}

	@Test
	public void meleeWalksAroundToYou()
	{
		Grid g = pillarFloor();
		Chase c = Chase.run(g, Monster.TZ_KIH, 3, 3, 8, 8);
		assertEquals(Chase.End.REACHES, c.getEnd());
		assertTrue(c.getTicks() > 0);
	}

	@Test
	public void plannerPicksThePillar()
	{
		Grid g = pillarFloor();
		StandPlanner.Spot spot = StandPlanner.plan(g,
			Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 13)), 20, 6);
		assertNotNull(spot);
		assertTrue(spot.safeFromRange());
	}

	@Test
	public void scoreNamesEveryFoe()
	{
		Grid g = pillarFloor();
		StandPlanner.Spot spot = StandPlanner.score(g, Arrays.asList(
			new StandPlanner.Foe(Monster.TZ_KIH, 3, 3),
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 20)), 10, 22, 0);
		assertEquals(Monster.KET_ZEK, spot.getOutcomes().get(0).getFoe().getMonster());
		assertEquals(2, spot.getOutcomes().size());
	}
}
