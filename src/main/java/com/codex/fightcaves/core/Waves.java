package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What each of the 63 waves holds. The cave counts like a ladder: each creature appears on
 * its own, then with every smaller wave in turn, then twice, and the next creature up takes
 * over. That rule gives all 62 waves before Jad, so nothing is typed out by hand.
 */
public final class Waves
{
	public static final int LAST = 63;
	public static final int JAD = 63;

	private static final Monster[] LADDER = {
		Monster.TZ_KIH, Monster.TZ_KEK, Monster.TOK_XIL, Monster.YT_MEJKOT, Monster.KET_ZEK
	};
	private static final List<List<Monster>> WAVES = build();

	private Waves()
	{
	}

	private static List<List<Monster>> build()
	{
		List<List<Monster>> seq = new ArrayList<>();
		seq.add(Collections.emptyList());
		for (Monster top : LADDER)
		{
			List<List<Monster>> below = new ArrayList<>(seq);
			for (List<Monster> wave : below)
			{
				List<Monster> next = new ArrayList<>(wave);
				next.add(top);
				seq.add(next);
			}
			List<Monster> twice = new ArrayList<>();
			twice.add(top);
			twice.add(top);
			seq.add(twice);
		}
		List<List<Monster>> waves = new ArrayList<>();
		waves.add(Collections.emptyList());
		// seq[0] is the empty wave; seq[1..62] are waves 1..62.
		for (int w = 1; w < JAD; w++)
		{
			waves.add(Collections.unmodifiableList(seq.get(w)));
		}
		waves.add(Collections.singletonList(Monster.TZTOK_JAD));
		return Collections.unmodifiableList(waves);
	}

	/** The creatures of a wave, smallest first, or an empty list outside 1 to 63. */
	public static List<Monster> of(int wave)
	{
		if (wave < 1 || wave > LAST)
		{
			return Collections.emptyList();
		}
		return WAVES.get(wave);
	}

	/** The wave's creatures biggest first, the order they are worth reading in. */
	public static List<Monster> biggestFirst(int wave)
	{
		List<Monster> list = new ArrayList<>(of(wave));
		Collections.reverse(list);
		return list;
	}

	/** Hitpoints to chew through in a wave, counting a Tz-Kek's two halves. */
	public static int hitpoints(int wave)
	{
		int hp = 0;
		for (Monster m : of(wave))
		{
			hp += m.getHitpoints();
			if (m == Monster.TZ_KEK)
			{
				hp += 2 * Monster.TZ_KIH.getHitpoints();
			}
		}
		return hp;
	}

	/** Hitpoints from this wave through the last one before Jad. */
	public static int hitpointsFrom(int wave)
	{
		int hp = 0;
		for (int w = Math.max(1, wave); w < JAD; w++)
		{
			hp += hitpoints(w);
		}
		return hp;
	}

	/** The first wave a creature appears in. */
	public static int firstWave(Monster monster)
	{
		for (int w = 1; w <= LAST; w++)
		{
			if (of(w).contains(monster))
			{
				return w;
			}
		}
		return -1;
	}

	/** "Mager, Melee, Bat" */
	public static String describe(int wave)
	{
		StringBuilder b = new StringBuilder();
		for (Monster m : biggestFirst(wave))
		{
			if (b.length() > 0)
			{
				b.append(", ");
			}
			b.append(m.getNickname());
		}
		return b.toString();
	}
}
