package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A creature that can hit you now or within a couple of ticks from where you stand, and the
 * style it hits with from there (a ranger or mager beside you hits with melee). The prayer call,
 * the danger line and the fall review all start from these. Jad is never weighed: when he is on
 * the floor the list is empty, as his attacks are yours to read.
 */
public final class Threat
{
	/** Creatures that will hit within this many ticks count as hitting now. */
	public static final int SOON = 2;

	private final Monster monster;
	private final Protect style;

	Threat(Monster monster, Protect style)
	{
		this.monster = monster;
		this.style = style;
	}

	public Monster getMonster()
	{
		return monster;
	}

	/** Its biggest hit with the style it will hit you with from where it stands. */
	public int getMaxHit()
	{
		return monster.maxHitWith(style == Protect.MELEE ? Monster.Style.MELEE : monster.getStyle());
	}

	/** The prayer that blocks it from where it will hit you. */
	public Protect getStyle()
	{
		return style;
	}

	/** What can hit you soon at a tile; empty when nothing can or Jad is on the floor. */
	public static List<Threat> of(Grid grid, List<StandPlanner.Foe> foes, int x, int y)
	{
		List<Threat> list = new ArrayList<>();
		for (StandPlanner.Foe f : foes)
		{
			if (f.monster == Monster.TZTOK_JAD)
			{
				return Collections.emptyList();
			}
			Chase c = Chase.run(grid, f.monster, f.x, f.y, x, y);
			if (!c.attacks() || c.getTicks() > SOON)
			{
				continue;
			}
			Protect p = Protect.against(f.monster.styleAt(c.getX(), c.getY(), x, y));
			if (p != null)
			{
				list.add(new Threat(f.monster, p));
			}
		}
		return list;
	}

	/** The most the threats your prayer does not block can hit for between them. */
	public static int unblocked(List<Threat> threats, Protect up)
	{
		int sum = 0;
		for (Threat t : threats)
		{
			if (t.style != up)
			{
				sum += t.getMaxHit();
			}
		}
		return sum;
	}

	/**
	 * The damage that could land on you at once when it is enough to kill you, otherwise 0: the
	 * cue to eat. Only what your prayer lets through counts.
	 *
	 * @param hp your hitpoints now
	 */
	public static int danger(List<Threat> threats, Protect up, int hp)
	{
		int could = unblocked(threats, up);
		return hp > 0 && could >= hp ? could : 0;
	}
}
