package com.codex.fightcaves.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Rehearsing a wave before it spawns, and setting the tile you are on against the gilt one. */
public class RehearsalTest
{
	private static final Grid FLOOR = ChaseTest.pillarFloor();

	@Test
	public void traceMatchesRunAndStartsOnTheSpawn()
	{
		List<int[]> path = new ArrayList<>();
		Chase traced = Chase.trace(FLOOR, Monster.TZ_KIH, 3, 3, 8, 8, path);
		Chase run = Chase.run(FLOOR, Monster.TZ_KIH, 3, 3, 8, 8);
		assertEquals(run.getEnd(), traced.getEnd());
		assertEquals(run.getTicks(), traced.getTicks());
		assertArrayEquals(new int[]{3, 3}, path.get(0));
		// One tile per tick it walked, and the last tile is where it ended.
		assertEquals(run.getTicks() + 1, path.size());
		assertArrayEquals(new int[]{run.getX(), run.getY()}, path.get(path.size() - 1));
	}

	@Test
	public void eachStepIsOneTile()
	{
		List<int[]> path = new ArrayList<>();
		Chase.trace(FLOOR, Monster.YT_MEJKOT, 2, 22, 25, 3, path);
		for (int i = 1; i < path.size(); i++)
		{
			assertTrue(Math.abs(path.get(i)[0] - path.get(i - 1)[0]) <= 1);
			assertTrue(Math.abs(path.get(i)[1] - path.get(i - 1)[1]) <= 1);
		}
	}

	@Test
	public void magerBehindThePillarSticksAndTheBatComes()
	{
		Rehearsal r = Rehearsal.of(FLOOR, Arrays.asList(
			new StandPlanner.Foe(Monster.TZ_KIH, 20, 3),
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 13)), 17, 14, 7);
		// Biggest first.
		Rehearsal.Track mager = r.getTracks().get(0);
		Rehearsal.Track bat = r.getTracks().get(1);
		assertEquals(Monster.KET_ZEK, mager.getMonster());
		assertEquals(Chase.End.STUCK, mager.getEnd());
		assertTrue(mager.describe(), mager.describe().contains("stuck"));
		assertEquals(Chase.End.REACHES, bat.getEnd());
		assertEquals(bat.getTicks(), r.firstHit());
		assertEquals(Monster.TZ_KIH, r.firstHitter());
		assertEquals(1, r.stuck());
		assertTrue(r.headline(), r.headline().startsWith("First hit on tick " + bat.getTicks()));
		assertTrue(r.headline(), r.headline().contains("1 of 2 stick"));
		assertEquals(Math.max(mager.getPath().size(), bat.getPath().size()) - 1, r.length());
		// Past its end a track holds its last tile.
		assertArrayEquals(bat.getPath().get(bat.getPath().size() - 1), bat.at(99));
	}

	@Test
	public void nothingReachingYouSaysSo()
	{
		Rehearsal r = Rehearsal.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 13)), 17, 14, 7);
		assertEquals(-1, r.firstHit());
		assertNull(r.firstHitter());
		assertEquals("Nothing reaches you: every creature sticks.", r.headline());
	}

	@Test
	public void spawningInSightHitsAtOnce()
	{
		Rehearsal r = Rehearsal.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 20)), 10, 22, 7);
		assertEquals(0, r.firstHit());
		assertEquals("can hit you as it spawns", r.getTracks().get(0).describe());
		assertEquals("The mager can hit you the moment it spawns.", r.headline());
	}

	@Test
	public void twoArrivingTogetherAreFlagged()
	{
		Rehearsal r = Rehearsal.of(FLOOR, Arrays.asList(
			new StandPlanner.Foe(Monster.TZ_KIH, 3, 3),
			new StandPlanner.Foe(Monster.TZ_KIH, 3, 5)), 8, 8, 1);
		assertTrue(r.together());
		assertTrue(r.headline(), r.headline().endsWith("Two reach you together."));
	}

	@Test
	public void mapMovesEveryTileAndFailsWhole()
	{
		Rehearsal r = Rehearsal.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.TZ_KIH, 3, 3)), 8, 8, 1);
		Rehearsal moved = r.map(t -> new int[]{t[0] + 100, t[1] + 200});
		assertNotNull(moved);
		assertArrayEquals(new int[]{108, 208}, moved.getStand());
		assertArrayEquals(new int[]{103, 203}, moved.getTracks().get(0).getPath().get(0));
		assertNull(r.map(t -> t[0] > 5 ? null : t));
	}

	@Test
	public void compareExplainsFewerHitters()
	{
		List<StandPlanner.Foe> foes = Arrays.asList(
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 13),
			new StandPlanner.Foe(Monster.TOK_XIL, 4, 20));
		// Out in the open west of the pillar both see you; tucked east of it neither does.
		StandPlanner.Spot here = StandPlanner.score(FLOOR, foes, 10, 18, 0, 7);
		StandPlanner.Spot there = StandPlanner.score(FLOOR, foes, 18, 14, 8, 7);
		Compare c = Compare.of(here, there);
		assertNotNull(c);
		assertEquals(2, c.getRows().size());
		Compare.Row mager = c.getRows().get(0);
		assertEquals(Monster.KET_ZEK, mager.getMonster());
		assertEquals("hits now", mager.getHere());
		assertTrue(mager.isThereSafe());
		assertTrue(c.getHeadline(), c.getHeadline().startsWith("Here 2 creatures can hit you, range and magic at once."));
	}

	@Test
	public void compareStaysQuietWhenNotClearlySafer()
	{
		List<StandPlanner.Foe> foes = Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 13));
		StandPlanner.Spot a = StandPlanner.score(FLOOR, foes, 18, 14, 0, 7);
		StandPlanner.Spot b = StandPlanner.score(FLOOR, foes, 18, 15, 1, 7);
		assertNull(Compare.of(a, b));
		assertNull(Compare.of(a, a));
		assertNull(Compare.of(null, b));
	}
}
