package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class RotationTrackerTest
{
	@Test
	public void everyRotationIsKnownByWaveFour()
	{
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			RotationTracker t = new RotationTracker();
			for (int w = 1; w <= 4; w++)
			{
				List<Spawn> spawns = Rotations.spawns(r, w);
				Collections.shuffle(spawns);
				t.observe(w, spawns);
			}
			assertEquals(r, t.rotation());
			assertTrue(t.clean());
		}
	}

	@Test
	public void waveOneLeavesThree()
	{
		RotationTracker t = new RotationTracker();
		t.observe(1, Collections.singletonList(Spawn.C));
		assertEquals(Arrays.asList(1, 2, 3), t.candidates());
		assertEquals(0, t.rotation());
		assertEquals(2, t.decidingWave(2));
	}

	@Test
	public void sevenAndEightWaitForWaveFour()
	{
		RotationTracker t = new RotationTracker();
		for (int w = 1; w <= 3; w++)
		{
			t.observe(w, Rotations.spawns(7, w));
		}
		assertEquals(Arrays.asList(7, 8), t.candidates());
		assertEquals(4, t.decidingWave(4));
	}

	@Test
	public void oneMisreadDoesNotLoseTheRotation()
	{
		RotationTracker t = new RotationTracker();
		t.observe(1, Rotations.spawns(12, 1));
		t.observe(2, Arrays.asList(Spawn.NW, Spawn.NW));
		for (int w = 3; w <= 8; w++)
		{
			t.observe(w, Rotations.spawns(12, w));
		}
		assertEquals(12, t.rotation());
	}

	@Test
	public void tableIsComplete()
	{
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			for (int w = 1; w <= Waves.LAST; w++)
			{
				assertEquals(Waves.of(w).size(), Rotations.placements(r, w).size());
			}
		}
	}
}
