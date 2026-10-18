package com.codex.fightcaves.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds where to stand. Every tile you could reach in a few seconds is tried: each creature's
 * walk toward that tile is played out, and the tile is scored by who could hit you, how soon,
 * and how many at once. Rangers and magers that end up stuck behind a pillar cost nearly
 * nothing; anything that can hit you from where it spawned costs the most. Ties go to the
 * tile closest to where you already are.
 */
public final class StandPlanner
{
	/** How far to look, in steps you would walk. */
	static final int REACH = 14;
	/** How far you can hit a stuck creature from, for a typical crossbow or staff. */
	static final int YOUR_RANGE = 7;

	private StandPlanner()
	{
	}

	/** A creature on the floor, or about to be. */
	public static final class Foe
	{
		final Monster monster;
		final int x;
		final int y;

		/** @param x the south-west tile of its body */
		public Foe(Monster monster, int x, int y)
		{
			this.monster = monster;
			this.x = x;
			this.y = y;
		}

		public Monster getMonster()
		{
			return monster;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}
	}

	/** One creature's walk toward the chosen tile. */
	public static final class Outcome
	{
		private final Foe foe;
		private final Chase chase;
		private final boolean inReach;

		Outcome(Foe foe, Chase chase, boolean inReach)
		{
			this.foe = foe;
			this.chase = chase;
			this.inReach = inReach;
		}

		/**
		 * True when it is stuck where you can still hit it with range or magic: the classic
		 * safespot, where you fight and it cannot fight back.
		 */
		public boolean isInReach()
		{
			return inReach;
		}

		public Foe getFoe()
		{
			return foe;
		}

		public Chase getChase()
		{
			return chase;
		}
	}

	/** Where to stand and what each creature will do if you stand there. */
	public static final class Spot
	{
		private final int x;
		private final int y;
		private final double cost;
		private final int steps;
		private final List<Outcome> outcomes;

		Spot(int x, int y, double cost, int steps, List<Outcome> outcomes)
		{
			this.x = x;
			this.y = y;
			this.cost = cost;
			this.steps = steps;
			this.outcomes = outcomes;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		/** Lower is safer. */
		public double getCost()
		{
			return cost;
		}

		/** Steps to walk there. */
		public int getSteps()
		{
			return steps;
		}

		public List<Outcome> getOutcomes()
		{
			return outcomes;
		}

		/** Creatures that end up stuck and cannot hit you here. */
		public int stuck()
		{
			int n = 0;
			for (Outcome o : outcomes)
			{
				if (!o.chase.attacks())
				{
					n++;
				}
			}
			return n;
		}

		/** True when every ranger and mager is stuck here. */
		public boolean safeFromRange()
		{
			for (Outcome o : outcomes)
			{
				if (o.foe.monster.isRanged() && o.chase.attacks())
				{
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * @param px the tile you stand on now
	 * @return the best tile, or null when there is nothing to plan for
	 */
	public static Spot plan(Grid grid, List<Foe> foes, int px, int py)
	{
		if (foes.isEmpty() || !grid.walkable(px, py))
		{
			return null;
		}
		int w = grid.getWidth();
		int h = grid.getHeight();
		int[][] steps = new int[w][h];
		for (int[] row : steps)
		{
			java.util.Arrays.fill(row, -1);
		}
		ArrayDeque<int[]> queue = new ArrayDeque<>();
		steps[px][py] = 0;
		queue.add(new int[]{px, py});
		Spot best = null;
		while (!queue.isEmpty())
		{
			int[] t = queue.poll();
			int s = steps[t[0]][t[1]];
			if (!underAny(foes, t[0], t[1]))
			{
				Spot spot = score(grid, foes, t[0], t[1], s);
				if (best == null || spot.cost < best.cost - 1e-9)
				{
					best = spot;
				}
			}
			if (s >= REACH)
			{
				continue;
			}
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int nx = t[0] + dx;
					int ny = t[1] + dy;
					if ((dx != 0 || dy != 0) && nx >= 0 && ny >= 0 && nx < w && ny < h
						&& steps[nx][ny] < 0 && grid.canStep(t[0], t[1], 1, dx, dy))
					{
						steps[nx][ny] = s + 1;
						queue.add(new int[]{nx, ny});
					}
				}
			}
		}
		return best;
	}

	/** Scores one tile; also used to say how the tile you are on now fares. */
	public static Spot score(Grid grid, List<Foe> foes, int x, int y, int steps)
	{
		List<Outcome> outcomes = new ArrayList<>();
		double cost = 0;
		List<Integer> arrivals = new ArrayList<>();
		int hidden = 0;
		for (Foe f : foes)
		{
			Chase c = Chase.run(grid, f.monster, f.x, f.y, x, y);
			int threat = f.monster.getThreat();
			boolean inReach = false;
			switch (c.getEnd())
			{
				case AT_ONCE:
					cost += threat * (f.monster.isRanged() ? 1.7 : 1.3);
					arrivals.add(0);
					break;
				case REACHES:
					cost += threat * (1 + Math.max(0, 8 - c.getTicks()) * 0.06);
					arrivals.add(c.getTicks());
					break;
				default:
					// A stuck melee creature you can still shoot is the best case of all. Anything
					// else stuck still has to be dealt with later, so it is not free.
					inReach = !f.monster.isRanged()
						&& Grid.distance(c.getX(), c.getY(), f.monster.getSize(), x, y) <= YOUR_RANGE
						&& grid.areaSees(c.getX(), c.getY(), f.monster.getSize(), x, y);
					cost += threat * (inReach ? 0.05 : f.monster.isRanged() ? 0.12 : 0.3);
					if (!inReach)
					{
						hidden++;
					}
					break;
			}
			outcomes.add(new Outcome(f, c, inReach));
		}
		// Hiding from everything stalls the wave rather than winning it.
		if (hidden == foes.size())
		{
			cost += 2;
		}
		// Creatures that arrive together hit together.
		Collections.sort(arrivals);
		for (int i = 1; i < arrivals.size(); i++)
		{
			if (arrivals.get(i) - arrivals.get(i - 1) < 3)
			{
				cost += 0.6;
			}
		}
		// Running covers two tiles a tick.
		cost += ((steps + 1) / 2) * 0.25;
		outcomes.sort((a, b) -> Integer.compare(b.foe.monster.getThreat(), a.foe.monster.getThreat()));
		return new Spot(x, y, cost, steps, Collections.unmodifiableList(outcomes));
	}

	private static boolean underAny(List<Foe> foes, int x, int y)
	{
		for (Foe f : foes)
		{
			int s = f.monster.getSize();
			if (x >= f.x && x < f.x + s && y >= f.y && y < f.y + s)
			{
				return true;
			}
		}
		return false;
	}
}
