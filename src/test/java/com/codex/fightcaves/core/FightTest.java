package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The prayer call, the kill order, weapon ranges and the spawn tiles. */
public class FightTest
{
	private static final Grid FLOOR = ChaseTest.pillarFloor();

	@Test
	public void magerInSightCallsForMagic()
	{
		PrayerCall call = PrayerCall.of(FLOOR, Arrays.asList(
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 20),
			new StandPlanner.Foe(Monster.TOK_XIL, 20, 20)), 10, 22);
		assertEquals(Protect.MAGIC, call.getPrayer());
		assertEquals(Protect.MISSILES, call.getAlso());
	}

	@Test
	public void magerBesideYouHitsWithMelee()
	{
		// The mager's body covers 4..8 on both axes; you stand just east of it.
		PrayerCall call = PrayerCall.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.KET_ZEK, 4, 4)), 9, 6);
		assertEquals(Protect.MELEE, call.getPrayer());
	}

	@Test
	public void nothingNearMeansNoCall()
	{
		assertNull(PrayerCall.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.TZ_KIH, 2, 2)), 25, 25));
		assertNull(PrayerCall.of(FLOOR, Collections.singletonList(new StandPlanner.Foe(Monster.TZTOK_JAD, 4, 20)), 10, 22));
	}

	@Test
	public void unprayedRangerDiesBeforePrayedMager()
	{
		List<StandPlanner.Foe> foes = Arrays.asList(
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 20),
			new StandPlanner.Foe(Monster.TOK_XIL, 20, 20));
		List<KillOrder.Target> order = KillOrder.rank(FLOOR, foes, 10, 22, 7, Protect.MAGIC);
		assertEquals(Monster.TOK_XIL, order.get(0).getFoe().getMonster());
		assertEquals(KillOrder.Reason.UNPRAYED, order.get(0).getReason());
		assertEquals(KillOrder.Reason.PRAYED, order.get(1).getReason());
	}

	@Test
	public void healerGoesFirstOnceOthersAreHurt()
	{
		List<StandPlanner.Foe> foes = Arrays.asList(
			new StandPlanner.Foe(Monster.KET_ZEK, 3, 20, 0.4, null),
			new StandPlanner.Foe(Monster.YT_MEJKOT, 8, 20, 1, null));
		List<KillOrder.Target> order = KillOrder.rank(FLOOR, foes, 12, 22, 7, Protect.MAGIC);
		assertEquals(Monster.YT_MEJKOT, order.get(0).getFoe().getMonster());
		assertEquals(KillOrder.Reason.HEALS, order.get(0).getReason());
	}

	@Test
	public void stuckInReachComesAfterThoseOnYou()
	{
		// A bat on you, and a mager stuck west of the pillar while you stand east of it.
		List<StandPlanner.Foe> foes = Arrays.asList(
			new StandPlanner.Foe(Monster.KET_ZEK, 4, 13),
			new StandPlanner.Foe(Monster.TZ_KIH, 18, 14));
		List<KillOrder.Target> order = KillOrder.rank(FLOOR, foes, 17, 14, 7, null);
		assertEquals(Monster.TZ_KIH, order.get(0).getFoe().getMonster());
		assertEquals(KillOrder.Reason.DRAINS, order.get(0).getReason());
		assertEquals(KillOrder.Reason.OUT_OF_REACH, order.get(1).getReason());
	}

	@Test
	public void standingWhereTwoStylesHitCostsMore()
	{
		List<StandPlanner.Foe> foes = Arrays.asList(
			new StandPlanner.Foe(Monster.YT_MEJKOT, 4, 4),
			new StandPlanner.Foe(Monster.KET_ZEK, 20, 4));
		double one = StandPlanner.score(FLOOR, foes.subList(0, 1), 9, 6, 0, 7).getCost()
			+ StandPlanner.score(FLOOR, foes.subList(1, 2), 9, 6, 0, 7).getCost();
		assertTrue(StandPlanner.score(FLOOR, foes, 9, 6, 0, 7).getCost() > one);
	}

	@Test
	public void weaponRanges()
	{
		assertEquals(5, WeaponRange.of("Toxic blowpipe"));
		assertEquals(7, WeaponRange.of("Rune crossbow"));
		assertEquals(8, WeaponRange.of("Armadyl crossbow"));
		assertEquals(10, WeaponRange.of("Magic longbow"));
		assertEquals(7, WeaponRange.of("Magic shortbow (i)"));
		assertEquals(10, WeaponRange.of("Twisted bow"));
		assertEquals(7, WeaponRange.of("Trident of the swamp"));
		assertEquals(10, WeaponRange.of("Ancient staff"));
		assertEquals(3, WeaponRange.of("Rune dart"));
		assertEquals(2, WeaponRange.of("Dragon halberd"));
		assertEquals(1, WeaponRange.of("Abyssal whip"));
		assertEquals(1, WeaponRange.of(null));
	}

	@Test
	public void spawnsStartOnTheirKnownTiles()
	{
		CaveMap map = new CaveMap(null);
		assertEquals(CaveMap.BASE_X + 37, map.tile(Spawn.S)[0]);
		assertEquals(CaveMap.BASE_Y + 17, map.tile(Spawn.S)[1]);
		assertEquals(Spawn.NW, map.classify(CaveMap.BASE_X + 12, CaveMap.BASE_Y + 47));
		// A Ket-Zek centred on the south spawn instead still reads as south.
		assertEquals(Spawn.S, map.classify(CaveMap.BASE_X + 35, CaveMap.BASE_Y + 15));
	}

	@Test
	public void sightingsNudgeASpawnButMisreadsDoNot()
	{
		Map<String, double[]> anchors = new HashMap<>();
		CaveMap.learn(anchors, Spawn.C, CaveMap.BASE_X + 33, CaveMap.BASE_Y + 31);
		CaveMap.learn(anchors, Spawn.C, CaveMap.BASE_X + 60, CaveMap.BASE_Y + 31);
		CaveMap map = new CaveMap(anchors);
		assertEquals(CaveMap.BASE_X + 33, map.tile(Spawn.C)[0]);
		assertEquals(CaveMap.BASE_Y + 31, map.tile(Spawn.C)[1]);
		assertTrue(map.seen(Spawn.C));
	}
}
