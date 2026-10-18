package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import org.junit.Test;

public class WavesTest
{
	@Test
	public void ladderMatchesTheCave()
	{
		assertEquals(Arrays.asList(Monster.TZ_KIH), Waves.of(1));
		assertEquals(Arrays.asList(Monster.TZ_KEK), Waves.of(3));
		assertEquals(Arrays.asList(Monster.TZ_KEK, Monster.TZ_KEK), Waves.of(6));
		assertEquals(Arrays.asList(Monster.TOK_XIL), Waves.of(7));
		assertEquals(Arrays.asList(Monster.YT_MEJKOT), Waves.of(15));
		assertEquals(Arrays.asList(Monster.YT_MEJKOT, Monster.YT_MEJKOT), Waves.of(30));
		assertEquals(Arrays.asList(Monster.KET_ZEK), Waves.of(31));
		assertEquals(6, Waves.of(58).size());
		assertEquals(Arrays.asList(Monster.KET_ZEK, Monster.KET_ZEK), Waves.of(62));
		assertEquals(Arrays.asList(Monster.TZTOK_JAD), Waves.of(63));
		assertEquals(0, Waves.of(64).size());
	}

	@Test
	public void firstAppearances()
	{
		assertEquals(1, Waves.firstWave(Monster.TZ_KIH));
		assertEquals(7, Waves.firstWave(Monster.TOK_XIL));
		assertEquals(31, Waves.firstWave(Monster.KET_ZEK));
		assertEquals(63, Waves.firstWave(Monster.TZTOK_JAD));
	}

	@Test
	public void hitpointsCountSplitBlobs()
	{
		assertEquals(10, Waves.hitpoints(1));
		assertEquals(40, Waves.hitpoints(3));
		assertEquals("Mager, Mager", Waves.describe(62));
	}
}
