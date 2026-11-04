package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RotationClockTest
{
	/**
	 * Minutes past midnight modulo 16 to rotation, written out slot by slot from the Fight Caves
	 * Spawn Predictor's table: column = minute + 1 (minutes 15 and 0 are column 1), then
	 * column 1 -> 4, 2 -> 2, 3 -> 9, 4 -> 11, 5 -> 13, 6 -> 1, 7 -> 6, 8 -> 15, 9 -> 10, 10 -> 8,
	 * 11 -> 5, 12 -> 3, 13 -> 12, 14 -> 14, 15 -> 7.
	 */
	private static final int[] SLOTS = {4, 2, 9, 11, 13, 1, 6, 15, 10, 8, 5, 3, 12, 14, 7, 4};

	@Test
	public void everyMinuteOfTheCycle()
	{
		for (int m = 0; m < 16; m++)
		{
			assertEquals("minute " + m, SLOTS[m], RotationClock.rotationAt(m));
		}
	}

	/** Spawn Predictor's own steps (SpawnPredictorPlugin.updateSchedule), re-done here to check against. */
	private static int predictor(int serverTime)
	{
		int[] resolver = {0, 4, 2, 9, 11, 13, 1, 6, 15, 10, 8, 5, 3, 12, 14, 7};
		int col = serverTime % 16;
		int minute = serverTime % 60;
		if ((col == 15 && minute % 2 != 0) || (col == 0 && minute % 2 == 0))
		{
			col = 1;
		}
		else
		{
			col++;
		}
		return resolver[col];
	}

	@Test
	public void matchesTheSpawnPredictorAllDay()
	{
		for (int t = 0; t < RotationClock.MINUTES_A_DAY; t++)
		{
			assertEquals("minute " + t, predictor(t), RotationClock.rotationAt(t));
		}
	}

	@Test
	public void theDayWrapsOnAWholeCycle()
	{
		assertEquals(0, RotationClock.MINUTES_A_DAY % RotationClock.CYCLE);
		// 23:59 is minute 15 of a cycle and 00:00 minute 0: rotation 4's two minutes span midnight.
		assertEquals(4, RotationClock.rotationAt(1439));
		assertEquals(4, RotationClock.rotationAt(0));
		assertEquals(2, RotationClock.rotationAt(1));
		assertEquals(RotationClock.rotationAt(5), RotationClock.rotationAt(5 + RotationClock.MINUTES_A_DAY));
		RotationClock.Reading late = new RotationClock.Reading(1439, 30);
		assertEquals(90, late.secondsLeft());
		assertEquals(new RotationClock.Reading(0, 0), late.plus(30));
		assertEquals(2, late.plus(90).rotation());
		assertEquals(new RotationClock.Reading(1439, 59), new RotationClock.Reading(0, 0).plus(-1));
	}

	@Test
	public void everyRotationOnceACycleAndFourForTwoMinutes()
	{
		int[] minutes = new int[16];
		for (int m = 0; m < 16; m++)
		{
			minutes[RotationClock.rotationAt(m)]++;
		}
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			assertEquals("rotation " + r, r == 4 ? 2 : 1, minutes[r]);
		}
	}

	@Test
	public void neighboursInTimeNeverShareAWaveOneSpawn()
	{
		// Why a reading on the edge of a change can keep both rotations: wave 1 tells them apart.
		for (int m = 0; m < 16; m++)
		{
			int a = RotationClock.rotationAt(m);
			int b = RotationClock.rotationAt(m + 1);
			if (a != b)
			{
				assertNotEquals("minute " + m, Rotations.spawns(a, 1), Rotations.spawns(b, 1));
			}
		}
	}

	@Test
	public void secondsLeftAndUntil()
	{
		assertEquals(41, RotationClock.secondsLeft(1, 19));
		// Minute 15 of the cycle is rotation 4's first minute: it lasts through the next.
		assertEquals(119, RotationClock.secondsLeft(15, 1));
		assertEquals(60, RotationClock.secondsLeft(16, 0));
		// At 00:01:19 rotation 2 is on; 9 is next, 4 is the last turn of the cycle.
		assertEquals(0, RotationClock.secondsUntil(2, 1, 19));
		assertEquals(41, RotationClock.secondsUntil(9, 1, 19));
		assertEquals(13 * 60 + 41, RotationClock.secondsUntil(4, 1, 19));
		assertEquals(-1, RotationClock.secondsUntil(16, 1, 19));
	}

	@Test
	public void upcomingTurns()
	{
		List<RotationClock.Slot> slots = RotationClock.upcoming(14, 20, 4);
		assertEquals(Arrays.asList(7, 4, 2, 9), Arrays.asList(slots.get(0).getRotation(), slots.get(1).getRotation(),
			slots.get(2).getRotation(), slots.get(3).getRotation()));
		assertTrue(slots.get(0).isNow());
		assertEquals(-20, slots.get(0).getStartsIn());
		assertEquals(40, slots.get(1).getStartsIn());
		// Rotation 4 is one turn of two minutes.
		assertEquals(160, slots.get(2).getStartsIn());
		assertEquals(220, slots.get(3).getStartsIn());

		// In rotation 4's second minute.
		slots = RotationClock.upcoming(16, 10, 2);
		assertEquals(4, slots.get(0).getRotation());
		assertEquals(-70, slots.get(0).getStartsIn());
		assertEquals(2, slots.get(1).getRotation());
		assertEquals(50, slots.get(1).getStartsIn());
	}

	@Test
	public void validReadings()
	{
		assertTrue(RotationClock.valid(0, 0));
		assertTrue(RotationClock.valid(1439, 59));
		assertFalse(RotationClock.valid(1440, 0));
		assertFalse(RotationClock.valid(10, 60));
		assertFalse(RotationClock.valid(-1, 3));
	}

	@Test
	public void aClearEntry()
	{
		RotationClock.Forecast f = RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 30), new RotationClock.Reading(2, 31));
		assertEquals(9, f.getSaid());
		assertFalse(f.isSplit());
		assertEquals(Arrays.asList(9), f.rotations());
		assertEquals(30, f.getSecond());
	}

	@Test
	public void anEntryAcrossAChangeKeepsBoth()
	{
		// Last read outside at :59, first inside at :00 of the next minute.
		RotationClock.Forecast f = RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 59), new RotationClock.Reading(3, 0));
		assertEquals(9, f.getSaid());
		assertEquals(11, f.getAlt());
		assertTrue(f.allows(9));
		assertTrue(f.allows(11));
		assertFalse(f.allows(13));
	}

	@Test
	public void anEntryOnTheEdgeKeepsTheNeighbour()
	{
		// No reading inside: within two seconds of a change either side may be the one.
		assertEquals(11, RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 58), null).getAlt());
		assertEquals(2, RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 1), null).getAlt());
		assertEquals(0, RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 2), null).getAlt());
		assertEquals(0, RotationClock.Forecast.atEntry(new RotationClock.Reading(2, 57), null).getAlt());
		// Rotation 4's minutes are one turn: no edge between them.
		assertEquals(0, RotationClock.Forecast.atEntry(new RotationClock.Reading(15, 59), new RotationClock.Reading(16, 0)).getAlt());
		assertEquals(0, RotationClock.Forecast.atEntry(new RotationClock.Reading(15, 59), null).getAlt());
	}
}
