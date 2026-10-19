package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A wave played out before it spawns. Every creature of the next wave is walked from its spawn
 * point toward the tile you mean to stand on, tick by tick, the way the game moves it. The
 * result is a set of tracks to replay on the panel and draw on the floor, and a plain account
 * of how the wave will go: who sticks behind what, who reaches you and when the first hit lands.
 */
public final class Rehearsal
{
	/** Seconds in a game tick. */
	private static final double TICK_SECONDS = 0.6;
	/** Two creatures starting on you within this many ticks of each other hit together. */
	private static final int TOGETHER = 2;

	/** One creature's walk. */
	public static final class Track
	{
		private final Monster monster;
		private final List<int[]> path;
		private final Chase.End end;
		private final int ticks;
		private final Monster.Style style;
		private final boolean inReach;

		Track(Monster monster, List<int[]> path, Chase.End end, int ticks, Monster.Style style, boolean inReach)
		{
			this.monster = monster;
			this.path = Collections.unmodifiableList(path);
			this.end = end;
			this.ticks = ticks;
			this.style = style;
			this.inReach = inReach;
		}

		public Monster getMonster()
		{
			return monster;
		}

		/** The south-west tile of its body at each tick, where it spawned first. */
		public List<int[]> getPath()
		{
			return path;
		}

		/** Where it is on a tick: it stays on its last tile once it stops. */
		public int[] at(int tick)
		{
			return path.get(Math.max(0, Math.min(path.size() - 1, tick)));
		}

		public Chase.End getEnd()
		{
			return end;
		}

		/** Ticks until it attacks, or until it stops when stuck. */
		public int getTicks()
		{
			return ticks;
		}

		public boolean attacks()
		{
			return end != Chase.End.STUCK;
		}

		/** How it hits you once there, or null when it never does. */
		public Monster.Style getStyle()
		{
			return style;
		}

		/** True when it sticks somewhere you can still hit it from the tile. */
		public boolean isInReach()
		{
			return inReach;
		}

		/** "walks 6 ticks, then stuck out of sight" */
		public String describe()
		{
			String walk = ticks == 0 ? "" : "walks " + ticks + (ticks == 1 ? " tick, then " : " ticks, then ");
			switch (end)
			{
				case AT_ONCE:
					return style == Monster.Style.MELEE && monster.isRanged() ? "spawns beside you, hits with melee" : "can hit you as it spawns";
				case REACHES:
					if (style == Monster.Style.MELEE && monster.isRanged())
					{
						return walk + "beside you with melee";
					}
					return walk + (monster.isRanged() ? "sees you and fires" : "reaches you");
				default:
					if (inReach)
					{
						return (ticks == 0 ? "stuck where it spawns" : walk + "stuck") + ", in your reach";
					}
					return (ticks == 0 ? "stuck where it spawns" : walk + "stuck") + (monster.isRanged() ? ", cannot see you" : "");
			}
		}

		/** The same walk with every tile moved, such as from scene tiles to the cave's own. */
		Track map(UnaryOperator<int[]> move)
		{
			List<int[]> moved = new ArrayList<>();
			for (int[] t : path)
			{
				int[] m = move.apply(t);
				if (m == null)
				{
					return null;
				}
				moved.add(m);
			}
			return new Track(monster, moved, end, ticks, style, inReach);
		}
	}

	private final List<Track> tracks;
	private final int[] stand;

	private Rehearsal(List<Track> tracks, int[] stand)
	{
		this.tracks = Collections.unmodifiableList(tracks);
		this.stand = stand;
	}

	/**
	 * @param foes the wave as it will spawn, by each creature's south-west tile
	 * @param x the tile you will stand on
	 * @param range how far you attack from
	 */
	public static Rehearsal of(Grid grid, List<StandPlanner.Foe> foes, int x, int y, int range)
	{
		List<Track> tracks = new ArrayList<>();
		for (StandPlanner.Foe f : foes)
		{
			List<int[]> path = new ArrayList<>();
			Chase c = Chase.trace(grid, f.monster, f.x, f.y, x, y, path);
			Monster.Style style = c.attacks() ? f.monster.styleAt(c.getX(), c.getY(), x, y) : null;
			boolean inReach = !c.attacks() && StandPlanner.canReach(grid, c.getX(), c.getY(), f.monster.getSize(), x, y, range);
			tracks.add(new Track(f.monster, path, c.getEnd(), c.getTicks(), style, inReach));
		}
		// Biggest first, as the panel lists a wave.
		tracks.sort((a, b) -> Integer.compare(b.monster.getLevel(), a.monster.getLevel()));
		return new Rehearsal(tracks, new int[]{x, y});
	}

	public List<Track> getTracks()
	{
		return tracks;
	}

	/** The tile you stand on. */
	public int[] getStand()
	{
		return stand.clone();
	}

	/** Ticks until the last creature stops moving or starts hitting you. */
	public int length()
	{
		int n = 0;
		for (Track t : tracks)
		{
			n = Math.max(n, t.path.size() - 1);
		}
		return n;
	}

	/** The tick the first hit can land on, or -1 when nothing ever reaches you. */
	public int firstHit()
	{
		int first = -1;
		for (Track t : tracks)
		{
			if (t.attacks() && (first < 0 || t.ticks < first))
			{
				first = t.ticks;
			}
		}
		return first;
	}

	/** The creature that hits first, or null. */
	public Monster firstHitter()
	{
		Track first = null;
		for (Track t : tracks)
		{
			if (t.attacks() && (first == null || t.ticks < first.ticks
				|| t.ticks == first.ticks && t.monster.getThreat() > first.monster.getThreat()))
			{
				first = t;
			}
		}
		return first == null ? null : first.monster;
	}

	/** How many end up stuck. */
	public int stuck()
	{
		int n = 0;
		for (Track t : tracks)
		{
			if (!t.attacks())
			{
				n++;
			}
		}
		return n;
	}

	/** True when two creatures start on you within a couple of ticks of each other. */
	public boolean together()
	{
		List<Integer> arrivals = new ArrayList<>();
		for (Track t : tracks)
		{
			if (t.attacks())
			{
				arrivals.add(t.ticks);
			}
		}
		Collections.sort(arrivals);
		for (int i = 1; i < arrivals.size(); i++)
		{
			if (arrivals.get(i) - arrivals.get(i - 1) <= TOGETHER)
			{
				return true;
			}
		}
		return false;
	}

	/** One line on how the wave goes from the tile: when the first hit lands and from what. */
	public String headline()
	{
		if (tracks.isEmpty())
		{
			return "";
		}
		int first = firstHit();
		if (first < 0)
		{
			return "Nothing reaches you: every creature sticks.";
		}
		Monster m = firstHitter();
		String who = m.getNickname().toLowerCase();
		String when = first == 0 ? "The " + who + " can hit you the moment it spawns."
			: "First hit on tick " + first + " (" + seconds(first) + "s), from the " + who + ".";
		if (together())
		{
			when += " Two reach you together.";
		}
		else if (stuck() > 0)
		{
			when += " " + stuck() + " of " + tracks.size() + " stick.";
		}
		return when;
	}

	/** The same rehearsal with every tile moved; null if any tile cannot be. */
	public Rehearsal map(UnaryOperator<int[]> move)
	{
		List<Track> moved = new ArrayList<>();
		for (Track t : tracks)
		{
			Track m = t.map(move);
			if (m == null)
			{
				return null;
			}
			moved.add(m);
		}
		int[] s = move.apply(stand);
		return s == null ? null : new Rehearsal(moved, s);
	}

	private static String seconds(int ticks)
	{
		double s = ticks * TICK_SECONDS;
		return s == Math.floor(s) ? String.valueOf((int) s) : String.format(java.util.Locale.ROOT, "%.1f", s);
	}
}
