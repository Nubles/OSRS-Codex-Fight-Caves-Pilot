package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * The road to Jad: every wave of your rotation rated for how it will go when it spawns, so the
 * hard ones are known before they arrive. Each wave is planned the way the pilot plans the next
 * wave as one ends: from the tile the wave before left you on, the safest tile in reach is found
 * and what can still hit you there decides the rating.
 *
 * <ul>
 * <li>Clean: every ranger and mager ends up stuck; only creatures that walk up to hit you
 * come, and one prayer covers them.</li>
 * <li>Pray: something hits from range wherever you stand in reach, but in one style, so the
 * right prayer blocks it all.</li>
 * <li>Split: two styles reach you at once from every tile in reach, so one prayer cannot block
 * both. These are the waves to plan around.</li>
 * </ul>
 *
 * Ratings are worked out one wave at a time (a wave's plan takes a few milliseconds), and saved
 * as one letter per wave so the Waves page can show them outside the cave. Jad's wave is never
 * rated.
 */
public final class Outlook
{
	/** How a wave will go when it spawns. */
	public enum Rating
	{
		CLEAN('c', "Clean"),
		PRAY('p', "Pray"),
		SPLIT('s', "Split");

		private final char code;
		private final String label;

		Rating(char code, String label)
		{
			this.code = code;
			this.label = label;
		}

		public char getCode()
		{
			return code;
		}

		public String getLabel()
		{
			return label;
		}

		/** The rating saved as this letter, or null for a wave not rated. */
		public static Rating of(char code)
		{
			for (Rating r : values())
			{
				if (r.code == code)
				{
					return r;
				}
			}
			return null;
		}
	}

	/** The letter for a wave not rated yet. */
	public static final char UNKNOWN = '-';

	private final char[] codes = new char[Waves.LAST + 1];
	private final String[] reasons = new String[Waves.LAST + 1];
	private int nextWave;
	private int x;
	private int y;

	/**
	 * Starts rating from a wave, from the tile you stand on now.
	 *
	 * @param saved letters already worked out for this rotation, or null
	 */
	public Outlook(String saved, int fromWave, int x, int y)
	{
		Arrays.fill(codes, UNKNOWN);
		if (saved != null)
		{
			for (int w = 1; w <= Waves.LAST && w - 1 < saved.length(); w++)
			{
				codes[w] = Rating.of(saved.charAt(w - 1)) != null ? saved.charAt(w - 1) : UNKNOWN;
			}
		}
		// A wave with nothing that hits from range is clean wherever it spawns: only melee comes.
		for (int w = 1; w < Waves.JAD; w++)
		{
			if (!hasRanged(w))
			{
				codes[w] = Rating.CLEAN.getCode();
			}
		}
		codes[Waves.JAD] = UNKNOWN;
		this.nextWave = Math.max(1, fromWave);
		this.x = x;
		this.y = y;
	}

	/** True once every wave from the starting one to the last before Jad is rated. */
	public boolean done()
	{
		return nextWave >= Waves.JAD;
	}

	/**
	 * Rates one more wave.
	 *
	 * @param foes a wave's creatures where they spawn, in the grid's tiles; empty when unknown
	 * @return the wave rated, or 0 when there was nothing left to rate
	 */
	public int step(Grid grid, IntFunction<List<StandPlanner.Foe>> foes, int range)
	{
		if (done())
		{
			return 0;
		}
		int wave = nextWave++;
		if (!hasRanged(wave))
		{
			return wave;
		}
		List<StandPlanner.Foe> list = foes.apply(wave);
		StandPlanner.Spot spot = list.isEmpty() ? null : StandPlanner.plan(grid, list, x, y, range);
		if (spot == null)
		{
			return wave;
		}
		codes[wave] = rate(spot).getCode();
		reasons[wave] = reason(spot);
		// You end the wave about where the plan put you, and the next wave is planned from there.
		x = spot.getX();
		y = spot.getY();
		return wave;
	}

	/** Waves rated so far, counting those rated before this visit. */
	public int rated()
	{
		int n = 0;
		for (int w = 1; w < Waves.JAD; w++)
		{
			n += codes[w] == UNKNOWN ? 0 : 1;
		}
		return n;
	}

	/** A wave's rating, or null when not rated yet. */
	public Rating rating(int wave)
	{
		return wave < 1 || wave > Waves.LAST ? null : Rating.of(codes[wave]);
	}

	/** Why a wave got its rating, in a sentence, or null when it was rated on another visit. */
	public String reason(int wave)
	{
		return wave < 1 || wave > Waves.LAST ? null : reasons[wave];
	}

	/** One letter per wave, wave 1 first, for saving. */
	public String encode()
	{
		return new String(codes, 1, Waves.LAST);
	}

	/**
	 * The saved road to Jad for a rotation: the one worked out for this reach, otherwise any,
	 * since most waves rate the same whatever you wield. Null when none is saved.
	 */
	public static String saved(java.util.Map<String, String> outlooks, int rotation, int reach)
	{
		if (outlooks == null || rotation <= 0)
		{
			return null;
		}
		String exact = outlooks.get(rotation + ":" + reach);
		if (exact != null)
		{
			return exact;
		}
		String prefix = rotation + ":";
		String any = null;
		int anyReach = Integer.MAX_VALUE;
		for (java.util.Map.Entry<String, String> e : outlooks.entrySet())
		{
			if (!e.getKey().startsWith(prefix))
			{
				continue;
			}
			int r;
			try
			{
				r = Integer.parseInt(e.getKey().substring(prefix.length()));
			}
			catch (NumberFormatException ignored)
			{
				continue;
			}
			// The lowest reach first, by number, so the answer does not depend on the map's order.
			if (r < anyReach)
			{
				anyReach = r;
				any = e.getKey();
			}
		}
		return any == null ? null : outlooks.get(any);
	}

	/** The first wave after this one rated at least as hard as the given rating, or 0. */
	public static int nextAtLeast(String codes, int after, Rating rating)
	{
		if (codes == null)
		{
			return 0;
		}
		for (int w = Math.max(1, after + 1); w < Waves.JAD && w - 1 < codes.length(); w++)
		{
			Rating r = Rating.of(codes.charAt(w - 1));
			if (r != null && r.ordinal() >= rating.ordinal())
			{
				return w;
			}
		}
		return 0;
	}

	/** How many waves after this one, up to the last before Jad, carry a rating. */
	public static int counted(String codes, int after, Rating rating)
	{
		int n = 0;
		for (int w = Math.max(1, after + 1); codes != null && w < Waves.JAD && w - 1 < codes.length(); w++)
		{
			if (Rating.of(codes.charAt(w - 1)) == rating)
			{
				n++;
			}
		}
		return n;
	}

	private static boolean hasRanged(int wave)
	{
		for (Monster m : Waves.of(wave))
		{
			if (m.isRanged())
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * How much of a second style it takes to call a wave split: as much as a ranger. A bat
	 * nibbling at you while you pray against a ranger is no reason to plan the wave around it.
	 */
	static final int SPLIT_THREAT = Monster.TOK_XIL.getThreat();

	/** What the safest tile in reach leaves able to hit you. */
	public static Rating rate(StandPlanner.Spot spot)
	{
		Map<Monster.Style, Integer> styles = new EnumMap<>(Monster.Style.class);
		boolean ranged = false;
		for (StandPlanner.Outcome o : spot.getOutcomes())
		{
			if (o.getChase().attacks() && o.getStyle() != null)
			{
				styles.merge(o.getStyle(), o.getFoe().getMonster().getThreat(), Integer::sum);
				ranged |= o.getStyle() != Monster.Style.MELEE;
			}
		}
		// The prayer goes on the heaviest style; what is left over decides whether it is a split.
		int total = 0;
		int top = 0;
		for (int t : styles.values())
		{
			total += t;
			top = Math.max(top, t);
		}
		if (styles.size() > 1 && total - top >= SPLIT_THREAT)
		{
			return Rating.SPLIT;
		}
		return ranged ? Rating.PRAY : Rating.CLEAN;
	}

	/** One sentence on the rating, naming the creatures behind it. */
	public static String reason(StandPlanner.Spot spot)
	{
		Rating rating = rate(spot);
		List<String> ranged = new ArrayList<>();
		List<String> close = new ArrayList<>();
		for (StandPlanner.Outcome o : spot.getOutcomes())
		{
			if (!o.getChase().attacks() || o.getStyle() == null)
			{
				continue;
			}
			String name = o.getFoe().getMonster().getNickname().toLowerCase();
			List<String> into = o.getStyle() == Monster.Style.MELEE ? close : ranged;
			if (!into.contains(name))
			{
				into.add(name);
			}
		}
		switch (rating)
		{
			case SPLIT:
				if (close.isEmpty())
				{
					return "The " + String.join(" and the ", ranged) + " see you from every tile in reach, in two styles one prayer cannot both block.";
				}
				return "The " + String.join(" and the ", ranged) + (ranged.size() == 1 ? " sees" : " see") + " you from every tile in reach while the "
					+ String.join(" and the ", close) + " come" + (close.size() == 1 ? "s" : "") + " to you, and one prayer cannot block both.";
			case PRAY:
				return "The " + String.join(" and the ", ranged) + (ranged.size() == 1 ? " sees" : " see") + " you from every tile in reach; one prayer covers "
					+ (ranged.size() == 1 ? "it" : "them") + (close.isEmpty() ? "." : ", and the " + String.join(" and the ", close) + " that come" + (close.size() == 1 ? "s" : "") + " hit lightly.");
			default:
				return close.isEmpty() ? "Nothing can hit you from the gilt tile." : "Every ranger and mager sticks behind a pillar; only melee comes.";
		}
	}
}
