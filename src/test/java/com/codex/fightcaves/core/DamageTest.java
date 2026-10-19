package com.codex.fightcaves.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.HitsplatID;
import org.junit.Test;

public class DamageTest
{
	private static Logbook.Run run(Logbook.Result result, int wave, int firstWave, Integer... hurt)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = wave;
		r.firstWave = firstWave;
		r.hurt = new ArrayList<>(Arrays.asList(hurt));
		return r;
	}

	@Test
	public void onlyHitsThatTakeHitpointsCount()
	{
		assertTrue(Damage.counts(HitsplatID.DAMAGE_ME, 12));
		assertTrue(Damage.counts(HitsplatID.DAMAGE_MAX_ME, 97));
		assertTrue(Damage.counts(HitsplatID.DAMAGE_OTHER, 3));
		assertFalse(Damage.counts(HitsplatID.BLOCK_ME, 0));
		assertFalse(Damage.counts(HitsplatID.HEAL, 10));
		assertFalse(Damage.counts(HitsplatID.PRAYER_DRAIN, 4));
		assertFalse(Damage.counts(HitsplatID.DAMAGE_ME, 0));
	}

	@Test
	public void averagesArePerWavePlayed()
	{
		// Fell on wave 3 after 10 + 20 + 30; a second run picked up at wave 2 took 40 there.
		Logbook.Run a = run(Logbook.Result.DIED, 3, 1, 10, 20, 30);
		Logbook.Run b = run(Logbook.Result.LEFT, 2, 2, 0, 40);
		double[] avg = Damage.perWaveByStretch(Arrays.asList(a, b));
		// Waves played in stretch 1-7: a's 1, 2, 3 and b's 2 (b's wave 1 was never seen).
		assertEquals((10 + 20 + 30 + 40) / 4.0, avg[0], 1e-9);
		assertEquals(0, avg[1], 1e-9);
		assertEquals(0, Damage.worstStretch(avg));
		assertEquals(-1, Damage.worstStretch(new double[Splits.STRETCHES]));
	}

	@Test
	public void runsWithoutDamageAreLeftOut()
	{
		Logbook.Run old = run(Logbook.Result.DIED, 40, 1);
		assertArrayEquals(new double[Splits.STRETCHES], Damage.perWaveByStretch(Collections.singletonList(old)), 1e-9);
	}

	@Test
	public void hardestWavesWorstFirst()
	{
		Logbook.Run r = run(Logbook.Result.DIED, 5, 1, 4, 30, 0, 30, 61);
		List<int[]> worst = Damage.hardest(r, 3);
		assertEquals(3, worst.size());
		assertArrayEquals(new int[]{5, 61}, worst.get(0));
		// A tie goes to the earlier wave.
		assertArrayEquals(new int[]{2, 30}, worst.get(1));
		assertArrayEquals(new int[]{4, 30}, worst.get(2));
		assertEquals(125, Damage.total(r));
		assertEquals("Hurt most on waves 5 (61), 2 (30) and 4 (30). Wave 5 took 61 before the fall.", Damage.summary(r));
		assertEquals("", Damage.summary(run(Logbook.Result.LEFT, 2, 1, 0, 0)));
	}
}
