package com.codex.fightcaves.core;

/**
 * Plays out a creature walking at you the way the game moves it: no path finding, just a step
 * toward you each tick, diagonally if it can, otherwise along whichever single axis is open.
 * That is why pillars work. A ranger or mager stops as soon as it is in range and can see
 * you; a melee creature stops once it is beside you. A creature that can neither attack nor
 * step any closer is stuck behind something, and stays there while you stand still.
 */
public final class Chase
{
	/** How a creature's walk toward you ends. */
	public enum End
	{
		/** It can attack you from where it spawned. */
		AT_ONCE,
		/** It walks into a position to attack you. */
		REACHES,
		/** It gets stuck behind something and cannot attack. */
		STUCK
	}

	/** Long enough to cross the whole cave. */
	static final int MAX_TICKS = 60;

	private final End end;
	private final int ticks;
	private final int x;
	private final int y;

	private Chase(End end, int ticks, int x, int y)
	{
		this.end = end;
		this.ticks = ticks;
		this.x = x;
		this.y = y;
	}

	public End getEnd()
	{
		return end;
	}

	/** Ticks until it attacks, or until it stopped moving when stuck. */
	public int getTicks()
	{
		return ticks;
	}

	/** Where it ends up: the south-west corner of its body. */
	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public boolean attacks()
	{
		return end != End.STUCK;
	}

	/** Whether a creature here could attack a target tile right now. */
	public static boolean canAttack(Grid grid, Monster m, int x, int y, int tx, int ty)
	{
		int size = m.getSize();
		if (Grid.meleeReach(x, y, size, tx, ty))
		{
			return true;
		}
		return m.isRanged() && Grid.distance(x, y, size, tx, ty) <= m.getRange() && grid.areaSees(x, y, size, tx, ty);
	}

	/**
	 * @param x the creature's south-west tile
	 * @param tx the tile you stand on
	 */
	public static Chase run(Grid grid, Monster m, int x, int y, int tx, int ty)
	{
		return walk(grid, m, x, y, tx, ty, null);
	}

	/**
	 * Plays the walk out like {@link #run} and also records it: the south-west tile the creature
	 * stands on at each tick, the tile it spawned on first. Used to rehearse a wave before it
	 * spawns.
	 */
	public static Chase trace(Grid grid, Monster m, int x, int y, int tx, int ty, java.util.List<int[]> path)
	{
		return walk(grid, m, x, y, tx, ty, path);
	}

	private static Chase walk(Grid grid, Monster m, int x, int y, int tx, int ty, java.util.List<int[]> path)
	{
		int size = m.getSize();
		if (path != null)
		{
			path.add(new int[]{x, y});
		}
		if (canAttack(grid, m, x, y, tx, ty))
		{
			return new Chase(End.AT_ONCE, 0, x, y);
		}
		for (int tick = 1; tick <= MAX_TICKS; tick++)
		{
			int dx = tx < x ? -1 : tx > x + size - 1 ? 1 : 0;
			int dy = ty < y ? -1 : ty > y + size - 1 ? 1 : 0;
			int sx = 0;
			int sy = 0;
			if (dx != 0 && dy != 0 && grid.canStep(x, y, size, dx, dy) && !covers(x + dx, y + dy, size, tx, ty))
			{
				sx = dx;
				sy = dy;
			}
			else if (dx != 0 && grid.canStep(x, y, size, dx, 0) && !covers(x + dx, y, size, tx, ty))
			{
				sx = dx;
			}
			else if (dy != 0 && grid.canStep(x, y, size, 0, dy) && !covers(x, y + dy, size, tx, ty))
			{
				sy = dy;
			}
			if (sx == 0 && sy == 0)
			{
				return new Chase(End.STUCK, tick - 1, x, y);
			}
			x += sx;
			y += sy;
			if (path != null)
			{
				path.add(new int[]{x, y});
			}
			if (canAttack(grid, m, x, y, tx, ty))
			{
				return new Chase(End.REACHES, tick, x, y);
			}
		}
		return new Chase(End.STUCK, MAX_TICKS, x, y);
	}

	private static boolean covers(int x, int y, int size, int tx, int ty)
	{
		return tx >= x && tx < x + size && ty >= y && ty < y + size;
	}
}
