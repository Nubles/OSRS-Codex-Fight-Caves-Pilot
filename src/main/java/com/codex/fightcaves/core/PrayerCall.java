package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Which protection prayer the fight on the floor calls for, from where you stand: of the
 * creatures that can hit you now or within a couple of ticks, the prayer that blocks the most
 * damage. A ranger or mager beside you counts as melee, since that is how it hits up close.
 * Jad is never called: his attacks are yours to read.
 */
public final class PrayerCall
{
	/** Creatures that will hit within this many ticks count as hitting now. */
	static final int SOON = Threat.SOON;

	private final Protect prayer;
	private final Protect also;
	private final List<Monster> answered;

	private PrayerCall(Protect prayer, Protect also, List<Monster> answered)
	{
		this.prayer = prayer;
		this.also = also;
		this.answered = answered;
	}

	/** The prayer to have up. */
	public Protect getPrayer()
	{
		return prayer;
	}

	/** A second style also hitting you, which no prayer can block alongside the first, or null. */
	public Protect getAlso()
	{
		return also;
	}

	/** The creatures the prayer answers, biggest first. */
	public List<Monster> getAnswered()
	{
		return answered;
	}

	/** @return the call, or null when nothing can hit you soon or Jad is on the floor */
	public static PrayerCall of(Grid grid, List<StandPlanner.Foe> foes, int x, int y)
	{
		for (StandPlanner.Foe f : foes)
		{
			if (f.monster == Monster.TZTOK_JAD)
			{
				return null;
			}
		}
		return of(Threat.of(grid, foes, x, y));
	}

	/** The call for threats already worked out (see {@link Threat#of}); null when there are none. */
	public static PrayerCall of(List<Threat> threats)
	{
		Map<Protect, Integer> damage = new EnumMap<>(Protect.class);
		Map<Protect, List<Monster>> who = new EnumMap<>(Protect.class);
		for (Threat t : threats)
		{
			damage.merge(t.getStyle(), t.getMonster().getMaxHit(), Integer::sum);
			who.computeIfAbsent(t.getStyle(), k -> new ArrayList<>()).add(t.getMonster());
		}
		Protect best = null;
		Protect second = null;
		for (Protect p : Protect.values())
		{
			int d = damage.getOrDefault(p, 0);
			if (d == 0)
			{
				continue;
			}
			if (best == null || d > damage.get(best))
			{
				second = best;
				best = p;
			}
			else if (second == null || d > damage.get(second))
			{
				second = p;
			}
		}
		if (best == null)
		{
			return null;
		}
		List<Monster> answered = who.get(best);
		answered.sort((a, b) -> Integer.compare(b.getMaxHit(), a.getMaxHit()));
		return new PrayerCall(best, second, Collections.unmodifiableList(answered));
	}
}
