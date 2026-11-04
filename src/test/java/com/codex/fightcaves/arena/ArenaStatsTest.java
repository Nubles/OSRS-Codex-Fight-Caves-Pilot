package com.codex.fightcaves.arena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ArenaStatsTest
{
	static ArenaLog.Run run(ArenaLog.Result result, int reached, Integer... splits)
	{
		ArenaLog.Run r = new ArenaLog.Run();
		r.firstWave = 1;
		r.result = result;
		r.reached = reached;
		r.cleared = result == ArenaLog.Result.DIED || result == ArenaLog.Result.LEFT ? reached - 1 : reached;
		r.splits = new ArrayList<>(Arrays.asList(splits));
		int sum = 0;
		for (int s : splits)
		{
			sum += s;
		}
		r.ticks = sum;
		r.endedAt = 1_767_225_600_000L;
		return r;
	}

	@Test
	public void deathRatesAreDeathsOverAttemptsThatBeganTheWave()
	{
		List<ArenaLog.Run> runs = new ArrayList<>();
		for (int i = 0; i < 4; i++)
		{
			runs.add(run(ArenaLog.Result.DIED, 3, 50, 50));
		}
		runs.add(run(ArenaLog.Result.CLAIMED, 2, 50, 50));
		runs.add(run(ArenaLog.Result.DIED, 2, 50));
		ArenaStats.WaveRisk[] r = ArenaStats.risks(Arena.COLOSSEUM, runs);
		assertEquals(6, r[1].starts);
		assertEquals(6, r[2].starts);
		assertEquals(1, r[2].deaths);
		assertEquals(4, r[3].starts);
		assertEquals(4, r[3].deaths);
		// Four attempts reached wave 3: too few to say its rate.
		assertFalse(r[3].known());
		assertTrue(r[2].known());
		assertEquals(1 / 6.0, r[2].rate(), 1e-9);
		assertEquals(Arrays.asList(2), waves(ArenaStats.deadliest(r, 3)));
	}

	@Test
	public void aRunPickedUpPartWayOnlyCountsTheWavesSeen()
	{
		ArenaLog.Run r = run(ArenaLog.Result.DIED, 6);
		r.firstWave = 5;
		r.partial = true;
		ArenaStats.WaveRisk[] risks = ArenaStats.risks(Arena.COLOSSEUM, Arrays.asList(r));
		assertEquals(0, risks[4].starts);
		assertEquals(1, risks[5].starts);
		assertEquals(1, risks[6].deaths);
	}

	private static List<Integer> waves(List<ArenaStats.WaveRisk> list)
	{
		List<Integer> out = new ArrayList<>();
		for (ArenaStats.WaveRisk r : list)
		{
			out.add(r.wave);
		}
		return out;
	}

	@Test
	public void theGhostIsTheFastestWholeRunElseTheFurthest()
	{
		Integer[] slow = new Integer[12];
		Integer[] fast = new Integer[12];
		Arrays.fill(slow, 100);
		Arrays.fill(fast, 90);
		ArenaLog.Run far = run(ArenaLog.Result.DIED, 8, 80, 80, 80, 80, 80, 80, 80);
		List<ArenaLog.Run> runs = new ArrayList<>(Arrays.asList(far));
		ArenaStats.Ghost g = ArenaStats.ghost(Arena.COLOSSEUM, runs, null);
		assertFalse(g.complete);
		assertEquals(7, g.cleared());
		ArenaLog.Run a = run(ArenaLog.Result.COMPLETE, 12, slow);
		ArenaLog.Run b = run(ArenaLog.Result.COMPLETE, 12, fast);
		runs.add(a);
		runs.add(b);
		g = ArenaStats.ghost(Arena.COLOSSEUM, runs, null);
		assertTrue(g.complete);
		assertEquals(b, g.run);
		assertEquals(a, ArenaStats.ghost(Arena.COLOSSEUM, runs, b).run);
		assertEquals(b, ArenaStats.best(Arena.COLOSSEUM, runs));
	}

	@Test
	public void aPartialRunIsNeverTheGhost()
	{
		ArenaLog.Run p = run(ArenaLog.Result.DIED, 4, 10, 10, 10);
		p.partial = true;
		assertNull(ArenaStats.ghost(Arena.COLOSSEUM, Arrays.asList(p), null));
	}

	@Test
	public void theReportSaysWhereTheRunLostTime()
	{
		ArenaLog.Run ghost = run(ArenaLog.Result.CLAIMED, 4, 50, 50, 50, 50);
		ArenaLog.Run r = run(ArenaLog.Result.DIED, 4, 60, 45, 70);
		ArenaStats.Ghost g = ArenaStats.ghost(Arena.COLOSSEUM, Arrays.asList(ghost, r), r);
		ArenaStats.Report rep = ArenaStats.report(g, r);
		assertEquals(3, rep.splits.size());
		assertEquals(Integer.valueOf(25), rep.behind);
		assertEquals(3, rep.through);
		assertEquals(3, rep.slowest.get(0).wave);
		assertEquals(1, rep.slowest.get(1).wave);
		assertEquals(2, rep.slowest.size());
	}

	@Test
	public void theClockIsTheInfernosTimeInsideOrTheColosseumsWaves()
	{
		ArenaLog.Run r = run(ArenaLog.Result.DIED, 3, 50, 50);
		r.ticks = 130;
		r.waveTicks = 7;
		assertEquals(130, ArenaStats.clock(Arena.INFERNO, r));
		assertEquals(107, ArenaStats.clock(Arena.COLOSSEUM, r));
	}
}
