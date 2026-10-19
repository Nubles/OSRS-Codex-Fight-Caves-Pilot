package com.codex.fightcaves.core;

/**
 * Whether you are in the cave, steadied against a single odd tick. A run is only closed once you
 * have been seen outside for a couple of ticks in a row, and a tick on which your own tile cannot
 * be read (just after logging in, before the game has placed you) says nothing either way. A run
 * left open is only called over once you have stood outside a little longer, so logging back in
 * mid-run never ends it.
 */
public final class Presence
{
	/** Ticks seen outside in a row before you count as having left. */
	public static final int LEAVE_TICKS = 2;
	/** Ticks seen outside in a row before a run left open is called over. */
	public static final int SETTLE_TICKS = 3;

	/** What changed this tick. */
	public enum Change
	{
		NONE,
		ENTERED,
		LEFT
	}

	private boolean inside;
	private int outside;

	/**
	 * One tick.
	 *
	 * @param known whether your tile could be read at all this tick
	 * @param inCave whether that tile is in the cave
	 */
	public Change tick(boolean known, boolean inCave)
	{
		if (!known)
		{
			return Change.NONE;
		}
		if (inCave)
		{
			outside = 0;
			if (!inside)
			{
				inside = true;
				return Change.ENTERED;
			}
			return Change.NONE;
		}
		outside++;
		if (inside && outside >= LEAVE_TICKS)
		{
			inside = false;
			return Change.LEFT;
		}
		return Change.NONE;
	}

	/**
	 * You were seen in the cave between ticks, by something that only happens there: the wave
	 * call, or a creature of the cave appearing. The game can send those on the very tick it
	 * places you, before the tick itself is read, and they must not be lost to the order the
	 * events arrive in.
	 *
	 * @return true when this is the moment you entered, false when you were already in
	 */
	public boolean enterNow()
	{
		outside = 0;
		if (inside)
		{
			return false;
		}
		inside = true;
		return true;
	}

	/** In the cave, counting a lone tick outside as still in. */
	public boolean isInside()
	{
		return inside;
	}

	/** Ticks in a row seen outside the cave. */
	public int outsideFor()
	{
		return outside;
	}

	/** Logged out or hopping: nothing is known until you are seen again. */
	public void reset()
	{
		inside = false;
		outside = 0;
	}
}
