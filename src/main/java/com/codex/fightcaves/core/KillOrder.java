package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What to kill first, from where you stand. Creatures hitting you come first, ordered by the
 * damage each deals for every hitpoint it takes to kill (the order that leaves the least damage
 * taken overall), so a weak creature hitting hard dies before a strong one hitting softly. A
 * prayer that is up counts: what it blocks barely matters, what it does not matters most. Tz-Kih
 * drain prayer even through it, Yt-MejKot undo your work on anything below half health, and a
 * Tz-Kek takes its two halves' hitpoints with it. Creatures stuck where you can hit them come
 * next, the deadliest first, and creatures you cannot reach yet come last.
 */
public final class KillOrder
{
	/** How soon a creature must hit you to count as on you. */
	static final int ON_YOU = 6;

	private KillOrder()
	{
	}

	/** Why a creature sits where it does in the order. */
	public enum Reason
	{
		DRAINS("drains prayer"),
		HEALS("heals the others"),
		UNPRAYED("hits you unprayed"),
		SPLITS("splits in two on death"),
		PRAYED("on you, prayed"),
		STUCK_IN_REACH("stuck in your reach"),
		OUT_OF_REACH("out of reach");

		private final String text;

		Reason(String text)
		{
			this.text = text;
		}

		public String getText()
		{
			return text;
		}
	}

	/** One creature's place in the order. */
	public static final class Target
	{
		private final StandPlanner.Foe foe;
		private final Reason reason;
		private final boolean reachable;
		private final double priority;

		Target(StandPlanner.Foe foe, Reason reason, boolean reachable, double priority)
		{
			this.foe = foe;
			this.reason = reason;
			this.reachable = reachable;
			this.priority = priority;
		}

		public StandPlanner.Foe getFoe()
		{
			return foe;
		}

		public Reason getReason()
		{
			return reason;
		}

		/** False when you cannot hit it from here and it is not coming to you. */
		public boolean isReachable()
		{
			return reachable;
		}

		double getPriority()
		{
			return priority;
		}
	}

	/**
	 * @param prayer the protection prayer up, or null
	 * @param range how far you attack from
	 * @return every creature, first to kill first; empty with Jad on the floor
	 */
	public static List<Target> rank(Grid grid, List<StandPlanner.Foe> foes, int x, int y, int range, Protect prayer)
	{
		List<Target> on = new ArrayList<>();
		List<Target> stuck = new ArrayList<>();
		List<Target> away = new ArrayList<>();
		boolean hurt = false;
		for (StandPlanner.Foe f : foes)
		{
			if (f.monster == Monster.TZTOK_JAD)
			{
				return Collections.emptyList();
			}
			if (f.monster != Monster.YT_MEJKOT && f.health < 0.5)
			{
				hurt = true;
			}
		}
		for (StandPlanner.Foe f : foes)
		{
			Monster m = f.monster;
			double hp = m.getHitpoints() * f.health + (m == Monster.TZ_KEK ? 2 * Monster.TZ_KIH.getHitpoints() : 0);
			hp = Math.max(1, hp);
			Chase c = Chase.run(grid, m, f.x, f.y, x, y);
			boolean heals = m == Monster.YT_MEJKOT && hurt;
			if (c.attacks() && c.getTicks() <= ON_YOU)
			{
				Protect p = Protect.against(m.styleAt(c.getX(), c.getY(), x, y));
				boolean prayed = p != null && p == prayer;
				double rate = m.getMaxHit() / 2.0 * (prayed ? 0.05 : 1);
				Reason reason = prayed ? Reason.PRAYED : Reason.UNPRAYED;
				if (m == Monster.TZ_KIH)
				{
					rate += 1.5;
					reason = Reason.DRAINS;
				}
				if (heals)
				{
					rate += 5;
					reason = Reason.HEALS;
				}
				if (m == Monster.TZ_KEK && reason != Reason.UNPRAYED)
				{
					reason = Reason.SPLITS;
				}
				on.add(new Target(f, reason, true, rate / hp));
			}
			else if (StandPlanner.canReach(grid, f.x, f.y, m.getSize(), x, y, range))
			{
				stuck.add(new Target(f, heals ? Reason.HEALS : Reason.STUCK_IN_REACH, true, m.getMaxHit() + (heals ? 50 : 0)));
			}
			else
			{
				away.add(new Target(f, heals ? Reason.HEALS : Reason.OUT_OF_REACH, c.attacks(), m.getMaxHit()));
			}
		}
		List<Target> all = new ArrayList<>();
		for (List<Target> group : java.util.Arrays.asList(on, stuck, away))
		{
			group.sort((a, b) -> Double.compare(b.priority, a.priority));
			all.addAll(group);
		}
		return Collections.unmodifiableList(all);
	}
}
