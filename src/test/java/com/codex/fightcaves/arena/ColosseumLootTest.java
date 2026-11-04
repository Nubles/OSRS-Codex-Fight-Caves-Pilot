package com.codex.fightcaves.arena;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 * The take-or-go-on estimate. Averages from oldschool.runescape.wiki/w/Rewards_Chest_(Fortis_Colosseum):
 * "The average wave 1 reward is worth 28,320." ... "The average wave 10 reward is worth 620,861."
 */
public class ColosseumLootTest
{
	@Test
	public void theWikiAveragesAsQuoted()
	{
		assertEquals(28_320, ColosseumLoot.average(1));
		assertEquals(43_449, ColosseumLoot.average(2));
		assertEquals(50_086, ColosseumLoot.average(3));
		assertEquals(61_162, ColosseumLoot.average(4));
		assertEquals(121_754, ColosseumLoot.average(5));
		assertEquals(123_496, ColosseumLoot.average(6));
		assertEquals(271_990, ColosseumLoot.average(7));
		assertEquals(306_586, ColosseumLoot.average(8));
		assertEquals(484_378, ColosseumLoot.average(9));
		assertEquals(620_861, ColosseumLoot.average(10));
		// The page gives none for 11 and 12.
		assertEquals(-1, ColosseumLoot.average(11));
		assertEquals(-1, ColosseumLoot.average(12));
		assertEquals(28_320 + 43_449 + 50_086, ColosseumLoot.chest(3));
	}

	private static List<ArenaLog.Run> runs(int reachedNext, int diedOnNext, int cleared)
	{
		List<ArenaLog.Run> runs = new ArrayList<>();
		for (int i = 0; i < reachedNext; i++)
		{
			boolean died = i < diedOnNext;
			ArenaLog.Run r = new ArenaLog.Run();
			r.firstWave = 1;
			r.reached = cleared + 1;
			r.cleared = died ? cleared : cleared + 1;
			r.result = died ? ArenaLog.Result.DIED : ArenaLog.Result.CLAIMED;
			runs.add(r);
		}
		return runs;
	}

	@Test
	public void saysNothingUntilEnoughRunsReachedTheNextWave()
	{
		ColosseumLoot.Estimate e = ColosseumLoot.estimate(3, runs(4, 1, 3));
		assertEquals(ColosseumLoot.Verdict.TOO_FEW_RUNS, e.verdict);
		assertEquals(4, e.starts);
	}

	@Test
	public void goingOnPaysWhenYouRarelyDieThere()
	{
		// Chest after 3: 121,855; wave 4 adds 61,162. Break-even survival 121,855 / 183,017 = 66.6%.
		ColosseumLoot.Estimate e = ColosseumLoot.estimate(3, runs(10, 1, 3));
		assertEquals(ColosseumLoot.Verdict.GO_ON, e.verdict);
		assertEquals(0.6658, e.breakEven(), 1e-3);
		assertEquals(Math.round(0.9 * (121_855 + 61_162) - 121_855), e.edge());
	}

	@Test
	public void takingPaysWhenYouOftenDieThere()
	{
		ColosseumLoot.Estimate e = ColosseumLoot.estimate(3, runs(10, 5, 3));
		assertEquals(ColosseumLoot.Verdict.TAKE, e.verdict);
	}

	@Test
	public void tooCloseToCallNearBreakEven()
	{
		// 2 of 6 died: survival 66.7% against a break-even of 66.6%.
		ColosseumLoot.Estimate e = ColosseumLoot.estimate(3, runs(6, 2, 3));
		assertEquals(ColosseumLoot.Verdict.CLOSE, e.verdict);
	}

	@Test
	public void noEstimateWhereTheWikiGivesNoValue()
	{
		assertEquals(ColosseumLoot.Verdict.NO_VALUE, ColosseumLoot.estimate(10, runs(10, 1, 10)).verdict);
		assertEquals(ColosseumLoot.Verdict.NO_VALUE, ColosseumLoot.estimate(11, runs(10, 1, 11)).verdict);
		assertNull(ColosseumLoot.estimate(12, runs(10, 1, 12)));
		assertNull(ColosseumLoot.estimate(0, runs(10, 1, 0)));
	}
}
