package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PrayerPacerTest
{
	@Test
	public void restoresFollowTheGame()
	{
		assertEquals(24, PrayerPacer.restore(70, false));
		assertEquals(25, PrayerPacer.restore(70, true));
		assertEquals(17, PrayerPacer.restore(43, false));
	}

	@Test
	public void onlyRangedWavesTeach()
	{
		assertFalse(PrayerPacer.teaches(3));
		assertTrue(PrayerPacer.teaches(7));
		assertFalse(PrayerPacer.teaches(15));
		assertTrue(PrayerPacer.teaches(31));
		assertFalse(PrayerPacer.teaches(63));
	}

	@Test
	public void plentyIsAmpleAndNothingIsShort()
	{
		PrayerPacer p = new PrayerPacer();
		assertEquals(PrayerPacer.State.AMPLE, p.pace(60, 70, 70, 80, 0, 4).getState());
		assertEquals(PrayerPacer.State.SHORT, p.pace(10, 70, 70, 0, 0, 4).getState());
		assertEquals(47, p.pace(10, 70, 70, 4, 0, 4).getDrinkBelow());
	}

	@Test
	public void learningMovesTheRate()
	{
		PrayerPacer p = new PrayerPacer();
		double before = p.rate();
		for (int i = 0; i < 40; i++)
		{
			p.learn(31, 40);
		}
		assertTrue(p.rate() > before);
		p.learn(3, 1000);
		assertTrue(p.rate() < 0.3);
	}
}
