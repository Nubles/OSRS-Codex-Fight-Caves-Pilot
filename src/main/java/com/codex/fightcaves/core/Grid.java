package com.codex.fightcaves.core;

/**
 * The cave floor as the game's collision flags, with the game's own rules for where a creature
 * of a given size can step and what it can see. The two rules are ported from RuneLite's
 * WorldArea (BSD 2-Clause, Copyright (c) 2017, Adam and RuneLite contributors) so they can run
 * on a plain grid, away from the client, and be tested.
 */
public final class Grid
{
	public static final int BLOCK_MOVEMENT_NORTH_WEST = 0x1;
	public static final int BLOCK_MOVEMENT_NORTH = 0x2;
	public static final int BLOCK_MOVEMENT_NORTH_EAST = 0x4;
	public static final int BLOCK_MOVEMENT_EAST = 0x8;
	public static final int BLOCK_MOVEMENT_SOUTH_EAST = 0x10;
	public static final int BLOCK_MOVEMENT_SOUTH = 0x20;
	public static final int BLOCK_MOVEMENT_SOUTH_WEST = 0x40;
	public static final int BLOCK_MOVEMENT_WEST = 0x80;
	public static final int BLOCK_MOVEMENT_FULL = 0x100 | 0x40000 | 0x200000;
	public static final int BLOCK_LINE_OF_SIGHT_NORTH = BLOCK_MOVEMENT_NORTH << 9;
	public static final int BLOCK_LINE_OF_SIGHT_EAST = BLOCK_MOVEMENT_EAST << 9;
	public static final int BLOCK_LINE_OF_SIGHT_SOUTH = BLOCK_MOVEMENT_SOUTH << 9;
	public static final int BLOCK_LINE_OF_SIGHT_WEST = BLOCK_MOVEMENT_WEST << 9;
	public static final int BLOCK_LINE_OF_SIGHT_FULL = 0x20000;
	/** The flag an unloaded or out-of-bounds tile reads as. */
	public static final int SOLID = BLOCK_MOVEMENT_FULL | BLOCK_LINE_OF_SIGHT_FULL;

	private final int[][] flags;
	private final int width;
	private final int height;

	/** @param flags collision flags indexed [x][y], as the client keeps them */
	public Grid(int[][] flags)
	{
		this.width = flags.length;
		this.height = width == 0 ? 0 : flags[0].length;
		this.flags = new int[width][];
		for (int x = 0; x < width; x++)
		{
			this.flags[x] = flags[x].clone();
		}
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public int flags(int x, int y)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
		{
			return SOLID;
		}
		return flags[x][y];
	}

	/** True when a creature or player can stand on the tile. */
	public boolean walkable(int x, int y)
	{
		return (flags(x, y) & BLOCK_MOVEMENT_FULL) == 0;
	}

	/** True when a size by size creature with its south-west corner here fits on open floor. */
	public boolean fits(int x, int y, int size)
	{
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				if (!walkable(x + i, y + j))
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Whether a size by size creature with its south-west corner at x,y can take one step in a
	 * direction, by the game's collision rules.
	 */
	public boolean canStep(int x, int y, int size, int dx, int dy)
	{
		dx = Integer.signum(dx);
		dy = Integer.signum(dy);
		if (dx == 0 && dy == 0)
		{
			return true;
		}
		int startX = x + dx;
		int startY = y + dy;
		int checkX = startX + (dx > 0 ? size - 1 : 0);
		int checkY = startY + (dy > 0 ? size - 1 : 0);
		int endX = startX + size - 1;
		int endY = startY + size - 1;

		int xFlags = BLOCK_MOVEMENT_FULL;
		int yFlags = BLOCK_MOVEMENT_FULL;
		int xyFlags = BLOCK_MOVEMENT_FULL;
		int xWallSouth = BLOCK_MOVEMENT_FULL;
		int xWallNorth = BLOCK_MOVEMENT_FULL;
		int yWallWest = BLOCK_MOVEMENT_FULL;
		int yWallEast = BLOCK_MOVEMENT_FULL;
		if (dx < 0)
		{
			xFlags |= BLOCK_MOVEMENT_EAST;
			xWallSouth |= BLOCK_MOVEMENT_SOUTH | BLOCK_MOVEMENT_SOUTH_EAST;
			xWallNorth |= BLOCK_MOVEMENT_NORTH | BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dx > 0)
		{
			xFlags |= BLOCK_MOVEMENT_WEST;
			xWallSouth |= BLOCK_MOVEMENT_SOUTH | BLOCK_MOVEMENT_SOUTH_WEST;
			xWallNorth |= BLOCK_MOVEMENT_NORTH | BLOCK_MOVEMENT_NORTH_WEST;
		}
		if (dy < 0)
		{
			yFlags |= BLOCK_MOVEMENT_NORTH;
			yWallWest |= BLOCK_MOVEMENT_WEST | BLOCK_MOVEMENT_NORTH_WEST;
			yWallEast |= BLOCK_MOVEMENT_EAST | BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dy > 0)
		{
			yFlags |= BLOCK_MOVEMENT_SOUTH;
			yWallWest |= BLOCK_MOVEMENT_WEST | BLOCK_MOVEMENT_SOUTH_WEST;
			yWallEast |= BLOCK_MOVEMENT_EAST | BLOCK_MOVEMENT_SOUTH_EAST;
		}
		if (dx < 0 && dy < 0)
		{
			xyFlags |= BLOCK_MOVEMENT_NORTH_EAST;
		}
		if (dx < 0 && dy > 0)
		{
			xyFlags |= BLOCK_MOVEMENT_SOUTH_EAST;
		}
		if (dx > 0 && dy < 0)
		{
			xyFlags |= BLOCK_MOVEMENT_NORTH_WEST;
		}
		if (dx > 0 && dy > 0)
		{
			xyFlags |= BLOCK_MOVEMENT_SOUTH_WEST;
		}

		if (dx != 0)
		{
			for (int ty = startY; ty <= endY; ty++)
			{
				if ((flags(checkX, ty) & xFlags) != 0)
				{
					return false;
				}
			}
			for (int ty = startY + 1; ty <= endY; ty++)
			{
				if ((flags(checkX, ty) & xWallSouth) != 0)
				{
					return false;
				}
			}
			for (int ty = endY - 1; ty >= startY; ty--)
			{
				if ((flags(checkX, ty) & xWallNorth) != 0)
				{
					return false;
				}
			}
		}
		if (dy != 0)
		{
			for (int tx = startX; tx <= endX; tx++)
			{
				if ((flags(tx, checkY) & yFlags) != 0)
				{
					return false;
				}
			}
			for (int tx = startX + 1; tx <= endX; tx++)
			{
				if ((flags(tx, checkY) & yWallWest) != 0)
				{
					return false;
				}
			}
			for (int tx = endX - 1; tx >= startX; tx--)
			{
				if ((flags(tx, checkY) & yWallEast) != 0)
				{
					return false;
				}
			}
		}
		if (dx != 0 && dy != 0)
		{
			if ((flags(checkX, checkY) & xyFlags) != 0)
			{
				return false;
			}
			if (size == 1)
			{
				if ((flags(checkX, checkY - dy) & xFlags) != 0)
				{
					return false;
				}
				if ((flags(checkX - dx, checkY) & yFlags) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	/** Whether a projectile from one tile reaches another, by the game's line of sight rule. */
	public boolean sees(int x1, int y1, int x2, int y2)
	{
		if (x1 == x2 && y1 == y2)
		{
			return true;
		}
		int dx = x2 - x1;
		int dy = y2 - y1;
		int dxAbs = Math.abs(dx);
		int dyAbs = Math.abs(dy);
		int xFlags = BLOCK_LINE_OF_SIGHT_FULL | (dx < 0 ? BLOCK_LINE_OF_SIGHT_EAST : BLOCK_LINE_OF_SIGHT_WEST);
		int yFlags = BLOCK_LINE_OF_SIGHT_FULL | (dy < 0 ? BLOCK_LINE_OF_SIGHT_NORTH : BLOCK_LINE_OF_SIGHT_SOUTH);
		if (dxAbs > dyAbs)
		{
			int x = x1;
			int yBig = (y1 << 16) + 0x8000;
			int slope = (dy << 16) / dxAbs;
			if (dy < 0)
			{
				yBig--;
			}
			int direction = dx < 0 ? -1 : 1;
			while (x != x2)
			{
				x += direction;
				int y = yBig >>> 16;
				if ((flags(x, y) & xFlags) != 0)
				{
					return false;
				}
				yBig += slope;
				int nextY = yBig >>> 16;
				if (nextY != y && (flags(x, nextY) & yFlags) != 0)
				{
					return false;
				}
			}
		}
		else
		{
			int y = y1;
			int xBig = (x1 << 16) + 0x8000;
			int slope = (dx << 16) / dyAbs;
			if (dx < 0)
			{
				xBig--;
			}
			int direction = dy < 0 ? -1 : 1;
			while (y != y2)
			{
				y += direction;
				int x = xBig >>> 16;
				if ((flags(x, y) & yFlags) != 0)
				{
					return false;
				}
				xBig += slope;
				int nextX = xBig >>> 16;
				if (nextX != x && (flags(nextX, y) & xFlags) != 0)
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Whether a size by size creature with its south-west corner at x,y can see a tile: true
	 * when any tile on the creature's edge facing the target has line of sight to it.
	 */
	public boolean areaSees(int x, int y, int size, int tx, int ty)
	{
		if (tx >= x && tx < x + size && ty >= y && ty < y + size)
		{
			return false;
		}
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				boolean edge = i == 0 || j == 0 || i == size - 1 || j == size - 1;
				if (edge && sees(x + i, y + j, tx, ty))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Chebyshev distance from the nearest tile of a size by size area to a tile. */
	public static int distance(int x, int y, int size, int tx, int ty)
	{
		int ddx = tx < x ? x - tx : tx > x + size - 1 ? tx - (x + size - 1) : 0;
		int ddy = ty < y ? y - ty : ty > y + size - 1 ? ty - (y + size - 1) : 0;
		return Math.max(ddx, ddy);
	}

	/** True when a tile touches a size by size area on one of its sides, not a corner. */
	public static boolean meleeReach(int x, int y, int size, int tx, int ty)
	{
		int ddx = tx < x ? x - tx : tx > x + size - 1 ? tx - (x + size - 1) : 0;
		int ddy = ty < y ? y - ty : ty > y + size - 1 ? ty - (y + size - 1) : 0;
		return ddx + ddy == 1;
	}
}
