package com.codex.fightcaves.arena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;

/**
 * Inferno and Colosseum attempts read from the game's own lines. The lines are written as the
 * game sends them, colour tags and all: "Wave: N" (RuneLite TimersAndBuffsPlugin), the TzKal-Zuk
 * kill count and "Duration:" (RuneLite ChatCommandsPluginTest), the Inferno's pause line
 * (TimersAndBuffsPlugin) and the Colosseum's chest line (ScreenshotPlugin). The Colosseum's wave
 * lines are unconfirmed (Fortis Colosseum plugin, Blert).
 */
public class ArenaTrackerTest
{
	private final ArenaTracker t = new ArenaTracker();
	private static final Arena INF = Arena.INFERNO;
	private static final Arena COL = Arena.COLOSSEUM;

	private void ticks(Arena here, int n)
	{
		for (int i = 0; i < n; i++)
		{
			t.tick(here, 0);
		}
	}

	@Test
	public void infernoWavesAreTimedBetweenCalls()
	{
		assertTrue(t.chat("<col=ef1020>Wave: 1</col>", INF, 1));
		ticks(INF, 10);
		t.chat("<col=ef1020>Wave: 2</col>", INF, 2);
		ticks(INF, 7);
		t.chat("<col=ef1020>Wave: 3</col>", INF, 3);
		ArenaLog.Run r = t.getLog().inferno.active;
		assertEquals(Arrays.asList(10, 7), r.splits);
		assertEquals(3, r.reached);
		assertEquals(2, r.cleared);
		assertFalse(r.partial);
		assertTrue(t.inWave(INF));
	}

	@Test
	public void theFightCaveKeepsItsOwnWaveCalls()
	{
		// Outside both arenas a wave call is not theirs.
		assertFalse(t.chat("Wave: 1", null, 0));
		assertNull(t.getLog().inferno.active);
	}

	@Test
	public void aDeathEndsTheRunOnItsWave()
	{
		t.chat("Wave: 1", INF, 0);
		ticks(INF, 5);
		t.chat("Wave: 2", INF, 0);
		t.death(INF, 9);
		ArenaLog.Run r = t.getLog().inferno.runs.get(0);
		assertEquals(ArenaLog.Result.DIED, r.result);
		assertEquals(2, r.reached);
		assertEquals(1, r.cleared);
		assertNull(t.getLog().inferno.active);
		assertTrue(t.takeChanged());
	}

	@Test
	public void aPausedInfernoWaitsAndTimesOnlyTimeInside()
	{
		t.chat("Wave: 1", INF, 0);
		ticks(INF, 4);
		t.chat("<col=ef1020>The Inferno has been paused. You may now log out.", INF, 0);
		ticks(null, 50);
		assertEquals("still open while paused", 1, t.getLog().inferno.active.reached);
		ticks(INF, 2);
		t.chat("Wave: 2", INF, 0);
		assertEquals(Arrays.asList(6), t.getLog().inferno.active.splits);
		assertFalse(t.getLog().inferno.paused);
	}

	@Test
	public void leavingWithoutPauseEndsTheRun()
	{
		t.chat("Wave: 1", INF, 0);
		ticks(INF, 3);
		ticks(null, ArenaTracker.SETTLE_TICKS);
		assertEquals(ArenaLog.Result.LEFT, t.getLog().inferno.runs.get(0).result);
	}

	@Test
	public void zuksKillCountFinishesAndTheDurationIsKept()
	{
		t.chat("Wave: 1", INF, 0);
		ticks(INF, 3);
		t.chat("Wave: 69", INF, 0);
		ticks(INF, 20);
		t.chat("Your TzKal-Zuk kill count is: <col=ff0000>2</col>.", INF, 5);
		t.chat("Duration: <col=ff0000>104:31</col> (new personal best)", INF, 5);
		ArenaLog.Run r = t.getLog().inferno.runs.get(0);
		assertEquals(ArenaLog.Result.COMPLETE, r.result);
		assertEquals(69, r.cleared);
		assertEquals(20, (int) r.splits.get(68));
		assertEquals(10452, r.gameTicks);
		assertEquals(10452, r.time());
	}

	@Test
	public void aNewFirstWaveClosesAnOldRun()
	{
		t.chat("Wave: 1", INF, 0);
		t.chat("Wave: 2", INF, 0);
		t.chat("Wave: 1", INF, 0);
		assertEquals(1, t.getLog().inferno.runs.size());
		assertEquals(ArenaLog.Result.LEFT, t.getLog().inferno.runs.get(0).result);
		assertEquals(1, t.getLog().inferno.active.reached);
	}

	@Test
	public void colosseumWavesTakeTheGamesOwnTimes()
	{
		assertTrue(t.chat("Wave 1 completed! Wave duration: <col=ff0000>0:46.20</col>", COL, 0));
		ArenaLog.Run r = t.getLog().colosseum.active;
		assertEquals(Arrays.asList(77), r.splits);
		assertEquals(1, t.betweenWaves(COL));
		assertFalse(t.inWave(COL));
		// The next wave begins: the first hit says so.
		t.damage(COL, 0);
		assertEquals(0, t.betweenWaves(COL));
		assertTrue(t.inWave(COL));
		assertEquals(2, r.reached);
		t.chat("Wave 2 completed! Wave duration: 1:00.00", COL, 0);
		assertEquals(Arrays.asList(77, 100), r.splits);
		assertEquals(177, r.ticks);
	}

	@Test
	public void takingTheChestBetweenWavesIsAClaim()
	{
		t.chat("Wave 1 completed! Wave duration: 0:30.00", COL, 0);
		t.chat("Wave 2 completed! Wave duration: 0:30.00", COL, 0);
		t.chat("Search the chest nearby to retrieve your earned rewards!", COL, 0);
		ArenaLog.Run r = t.getLog().colosseum.runs.get(0);
		assertEquals(ArenaLog.Result.CLAIMED, r.result);
		assertEquals(2, r.reached);
		assertEquals(2, r.cleared);
	}

	@Test
	public void dyingInAColosseumWaveCountsAgainstTheNextWave()
	{
		t.chat("Wave 1 completed! Wave duration: 0:30.00", COL, 0);
		t.death(COL, 0);
		ArenaLog.Run r = t.getLog().colosseum.runs.get(0);
		assertEquals(ArenaLog.Result.DIED, r.result);
		assertEquals(2, r.reached);
		assertEquals(1, r.cleared);
	}

	@Test
	public void wave12IsSolAndFinishesTheRun()
	{
		for (int w = 1; w <= 12; w++)
		{
			t.chat("Wave " + w + " completed! Wave duration: 1:00.00", COL, 0);
		}
		t.chat("Colosseum duration: <col=ff0000>12:30.00</col>", COL, 0);
		ArenaLog.Run r = t.getLog().colosseum.runs.get(0);
		assertEquals(ArenaLog.Result.COMPLETE, r.result);
		assertEquals(1200, r.ticks);
		assertEquals(1250, r.gameTicks);
		// The chest after Sol does not file a second run.
		t.chat("Search the chest nearby to retrieve your earned rewards!", COL, 0);
		assertEquals(1, t.getLog().colosseum.runs.size());
	}

	@Test
	public void aRunPickedUpPartWayIsNotARecord()
	{
		t.chat("Wave 5 completed! Wave duration: 1:00.00", COL, 0);
		ArenaLog.Run r = t.getLog().colosseum.active;
		assertTrue(r.partial);
		assertEquals(5, r.firstWave);
		assertEquals(0, r.clockAt(5));
	}

	@Test
	public void aHitBeforeTheFirstLineStartsWaveOne()
	{
		t.damage(COL, 0);
		assertEquals(1, t.getLog().colosseum.active.reached);
		t.chat("Wave 1 completed! Wave duration: 1:00.00", COL, 0);
		assertFalse(t.getLog().colosseum.active.partial);
	}

	@Test
	public void ticksOfReadsTheGamesTimes()
	{
		assertEquals(77, ArenaTracker.ticksOf("0:46.20"));
		assertEquals(10452, ArenaTracker.ticksOf("104:31"));
		assertEquals(6205, ArenaTracker.ticksOf("1:02:03"));
		assertEquals(0, ArenaTracker.ticksOf("x"));
	}

	@Test
	public void theLogSurvivesASaveAndAnOpenRunToo()
	{
		t.chat("Wave: 1", INF, 0);
		ticks(INF, 3);
		Gson gson = new Gson();
		ArenaLog back = ArenaLog.read(gson, gson.toJson(t.getLog()));
		assertEquals(3, back.inferno.active.waveTicks);
		ArenaLog junk = ArenaLog.read(gson, "{not json");
		assertTrue(junk.inferno.runs.isEmpty());
		ArenaLog none = ArenaLog.read(gson, null);
		assertTrue(none.colosseum.runs.isEmpty());
	}
}
