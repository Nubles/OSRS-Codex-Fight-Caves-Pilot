package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Why the gilt tile beats the one you are on. Both tiles are played out against the same
 * creatures and set side by side, creature by creature, with one sentence on what changes:
 * fewer creatures able to hit you, one prayer covering what is left, or the hits arriving later
 * and one at a time.
 */
public final class Compare
{
	/** How much safer the gilt tile has to score before a comparison is worth showing. */
	static final double WORTH = 0.5;

	/** One creature on both tiles. */
	public static final class Row
	{
		private final Monster monster;
		private final String here;
		private final boolean hereSafe;
		private final String there;
		private final boolean thereSafe;

		Row(Monster monster, String here, boolean hereSafe, String there, boolean thereSafe)
		{
			this.monster = monster;
			this.here = here;
			this.hereSafe = hereSafe;
			this.there = there;
			this.thereSafe = thereSafe;
		}

		public Monster getMonster()
		{
			return monster;
		}

		/** What it does if you stay where you are, in a word or two. */
		public String getHere()
		{
			return here;
		}

		public boolean isHereSafe()
		{
			return hereSafe;
		}

		/** What it does if you stand on the gilt tile. */
		public String getThere()
		{
			return there;
		}

		public boolean isThereSafe()
		{
			return thereSafe;
		}
	}

	private final List<Row> rows;
	private final String headline;

	private Compare(List<Row> rows, String headline)
	{
		this.rows = Collections.unmodifiableList(rows);
		this.headline = headline;
	}

	public List<Row> getRows()
	{
		return rows;
	}

	public String getHeadline()
	{
		return headline;
	}

	/**
	 * @param here the tile you are on, scored against the same creatures as the gilt tile
	 * @param there the gilt tile
	 * @return the comparison, or null when the gilt tile is not clearly safer or is where you are
	 */
	public static Compare of(StandPlanner.Spot here, StandPlanner.Spot there)
	{
		if (here == null || there == null || here.getX() == there.getX() && here.getY() == there.getY()
			|| here.getCost() <= there.getCost() + WORTH)
		{
			return null;
		}
		List<Row> rows = new ArrayList<>();
		for (StandPlanner.Outcome h : here.getOutcomes())
		{
			StandPlanner.Outcome t = match(there.getOutcomes(), h.getFoe());
			if (t == null)
			{
				return null;
			}
			rows.add(new Row(h.getFoe().getMonster(), brief(h), !h.getChase().attacks(), brief(t), !t.getChase().attacks()));
		}
		return new Compare(rows, headline(here, there));
	}

	private static StandPlanner.Outcome match(List<StandPlanner.Outcome> outcomes, StandPlanner.Foe foe)
	{
		for (StandPlanner.Outcome o : outcomes)
		{
			if (o.getFoe() == foe)
			{
				return o;
			}
		}
		return null;
	}

	/** What a creature does from a tile in a word or two: "hits now", "6 ticks", "stuck". */
	public static String brief(StandPlanner.Outcome o)
	{
		Chase c = o.getChase();
		boolean melee = o.getStyle() == Monster.Style.MELEE && o.getFoe().getMonster().isRanged();
		switch (c.getEnd())
		{
			case AT_ONCE:
				return melee ? "melee now" : "hits now";
			case REACHES:
				return c.getTicks() + (c.getTicks() == 1 ? " tick" : " ticks");
			default:
				return o.isInReach() ? "safespot" : "stuck";
		}
	}

	static int hitters(StandPlanner.Spot s)
	{
		int n = 0;
		for (StandPlanner.Outcome o : s.getOutcomes())
		{
			if (o.getChase().attacks())
			{
				n++;
			}
		}
		return n;
	}

	static Set<Monster.Style> styles(StandPlanner.Spot s)
	{
		Set<Monster.Style> set = EnumSet.noneOf(Monster.Style.class);
		for (StandPlanner.Outcome o : s.getOutcomes())
		{
			if (o.getChase().attacks() && o.getStyle() != null)
			{
				set.add(o.getStyle());
			}
		}
		return set;
	}

	/** The first tick anything hits, or -1. */
	static int firstHit(StandPlanner.Spot s)
	{
		int first = -1;
		for (StandPlanner.Outcome o : s.getOutcomes())
		{
			if (o.getChase().attacks() && (first < 0 || o.getChase().getTicks() < first))
			{
				first = o.getChase().getTicks();
			}
		}
		return first;
	}

	static String headline(StandPlanner.Spot here, StandPlanner.Spot there)
	{
		int h = hitters(here);
		int t = hitters(there);
		Set<Monster.Style> hs = styles(here);
		Set<Monster.Style> ts = styles(there);
		String mixed = hs.size() > 1 ? ", " + join(hs) + " at once" : "";
		if (t < h)
		{
			return "Here " + count(h) + " can hit you" + mixed + ". On the gilt tile, " + (t == 0 ? "none can." : (t == 1 ? "only 1." : t + "."));
		}
		if (hs.size() > 1 && ts.size() <= 1)
		{
			return "Here " + join(hs) + " hit you at once and one prayer cannot block both. On the gilt tile one prayer covers it.";
		}
		int hf = firstHit(here);
		int tf = firstHit(there);
		if (hf >= 0 && tf > hf)
		{
			return "The gilt tile buys time: the first hit lands on tick " + tf + " instead of " + (hf == 0 ? "at once." : "tick " + hf + ".");
		}
		return "The gilt tile spaces the hits out, so they come one at a time.";
	}

	private static String count(int n)
	{
		return n == 1 ? "1 creature" : n + " creatures";
	}

	private static String join(Set<Monster.Style> styles)
	{
		List<String> names = new ArrayList<>();
		for (Monster.Style s : styles)
		{
			names.add(s == Monster.Style.MELEE ? "melee" : s == Monster.Style.RANGED ? "range" : "magic");
		}
		if (names.size() <= 1)
		{
			return String.join("", names);
		}
		return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
	}
}
