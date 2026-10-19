package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.HitsplatID;

/**
 * Where your runs hurt. Every hit you take in the cave is added to the wave it landed on, so a
 * run can be read for the waves that cost it the most, and all your runs together show which
 * stretch of seven waves keeps hurting you. Averages are per wave played, so a run that ended
 * part way through a stretch, or was picked up part way, counts only the waves it saw.
 */
public final class Damage
{
	private Damage()
	{
	}

	/** True for a hitsplat that takes hitpoints off you, not a heal or a drain of something else. */
	public static boolean counts(int type, int amount)
	{
		if (amount <= 0)
		{
			return false;
		}
		switch (type)
		{
			case HitsplatID.HEAL:
			case HitsplatID.CYAN_UP:
			case HitsplatID.CYAN_DOWN:
			case HitsplatID.PRAYER_DRAIN:
			case HitsplatID.SANITY_DRAIN:
			case HitsplatID.SANITY_RESTORE:
			case HitsplatID.BLOCK_ME:
			case HitsplatID.BLOCK_OTHER:
				return false;
			default:
				return true;
		}
	}

	/**
	 * Average damage per wave played in each stretch of seven waves, across every run that
	 * played some of it; Jad's wave is the last entry. 0 where no run has been.
	 */
	public static double[] perWaveByStretch(List<Logbook.Run> runs)
	{
		double[] sum = new double[Splits.STRETCHES];
		int[] played = new int[Splits.STRETCHES];
		for (Logbook.Run r : runs)
		{
			if (r.hurt == null || r.hurt.isEmpty())
			{
				continue;
			}
			int from = Math.max(1, r.firstWave);
			for (int w = from; w <= Math.min(r.wave, Waves.LAST); w++)
			{
				int s = (w - 1) / Splits.STRETCH;
				sum[s] += w - 1 < r.hurt.size() ? r.hurt.get(w - 1) : 0;
				played[s]++;
			}
		}
		double[] avg = new double[Splits.STRETCHES];
		for (int s = 0; s < avg.length; s++)
		{
			avg[s] = played[s] == 0 ? 0 : sum[s] / played[s];
		}
		return avg;
	}

	/** Damage a run took, all waves together. */
	public static int total(Logbook.Run run)
	{
		int t = 0;
		if (run != null && run.hurt != null)
		{
			for (int d : run.hurt)
			{
				t += d;
			}
		}
		return t;
	}

	/** The waves that hurt a run the most: {wave, damage}, worst first, at most count of them. */
	public static List<int[]> hardest(Logbook.Run run, int count)
	{
		List<int[]> list = new ArrayList<>();
		if (run == null || run.hurt == null)
		{
			return list;
		}
		for (int i = 0; i < run.hurt.size(); i++)
		{
			if (run.hurt.get(i) > 0)
			{
				list.add(new int[]{i + 1, run.hurt.get(i)});
			}
		}
		// Worst first; on a tie the earlier wave, which came first in the run.
		list.sort((a, b) -> a[1] != b[1] ? Integer.compare(b[1], a[1]) : Integer.compare(a[0], b[0]));
		return list.size() > count ? new ArrayList<>(list.subList(0, count)) : list;
	}

	/** The stretch that hurts most per wave played, as an index into the stretches, or -1. */
	public static int worstStretch(double[] perWave)
	{
		int worst = -1;
		for (int s = 0; s < perWave.length; s++)
		{
			if (perWave[s] > 0 && (worst < 0 || perWave[s] > perWave[worst]))
			{
				worst = s;
			}
		}
		return worst;
	}

	/**
	 * One sentence on a run's damage: its worst waves, and on the wave it fell on how much
	 * that wave took. Empty when nothing was recorded.
	 */
	public static String summary(Logbook.Run run)
	{
		List<int[]> worst = hardest(run, 3);
		if (worst.isEmpty())
		{
			return "";
		}
		StringBuilder b = new StringBuilder("Hurt most on wave");
		b.append(worst.size() > 1 ? "s " : " ");
		for (int i = 0; i < worst.size(); i++)
		{
			if (i > 0)
			{
				b.append(i == worst.size() - 1 ? " and " : ", ");
			}
			b.append(worst.get(i)[0]).append(" (").append(worst.get(i)[1]).append(')');
		}
		b.append('.');
		if (run.result == Logbook.Result.DIED && run.wave >= 1 && run.wave <= run.hurt.size())
		{
			b.append(" Wave ").append(run.wave).append(" took ").append(run.hurt.get(run.wave - 1)).append(" before the fall.");
		}
		return b.toString();
	}
}
