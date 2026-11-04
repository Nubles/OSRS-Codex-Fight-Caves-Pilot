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

/** What could reach you (for the fall review), weapon ranges and the spawn tiles. */
public class FightTest
{
	private static final Grid FLOOR = ChaseTest.pillarFloor();

	@Test
	public void magerAndRangerInSightAreBothThreats()
	{
		// What the fall review reads after a run: who could reach you, and with what.
		List<Threat> t = Threat.of(FLOOR, Arrays.asList(new Foe(Monster.KET_ZEK, 4, 20), new Foe(Monster.TOK_XIL, 20, 20)), 10, 22);
		assertEquals(2, t.size());
		assertTrue(t.stream().anyMatch(x -> x.getStyle() == Protect.MAGIC));
		assertTrue(t.stream().anyMatch(x -> x.getStyle() == Protect.MISSILES));
	}

	@Test
	public void magerBesideYouHitsWithMelee()
	{
		// The mager's body covers 4..8 on both axes; you stand just east of it.
		List<Threat> t = Threat.of(FLOOR, Collections.singletonList(new Foe(Monster.KET_ZEK, 4, 4)), 9, 6);
		assertEquals(Protect.MELEE, t.get(0).getStyle());
	}

	@Test
	public void nothingNearAndJadAreNoThreats()
	{
		assertTrue(Threat.of(FLOOR, Collections.singletonList(new Foe(Monster.TZ_KIH, 2, 2)), 25, 25).isEmpty());
		assertTrue(Threat.of(FLOOR, Collections.singletonList(new Foe(Monster.TZTOK_JAD, 4, 20)), 10, 22).isEmpty());
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
