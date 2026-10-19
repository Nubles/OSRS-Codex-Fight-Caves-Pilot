package com.codex.fightcaves.core;

/**
 * A wave call heard before you were seen in the cave. On the tick the game moves you in, the
 * "Wave: 1" message and the first spawns can arrive before your new tile can be read, so the
 * call is held for a few ticks and played once you are seen inside. A call that is not followed
 * into the cave in time (the Inferno and other TzHaar fights also call waves) is dropped.
 */
public final class EarlyCall
{
	/** Ticks a call is held for, waiting to see you in the cave. */
	public static final int HOLD_TICKS = 3;

	private int wave;
	private int tick;

	/** Holds a wave call heard while not yet seen in the cave. */
	public void hear(int wave, int tick)
	{
		this.wave = wave;
		this.tick = tick;
	}

	/**
	 * The held call, once you are seen in the cave, or 0 when there is none or it is too old.
	 * Taking it clears it.
	 */
	public int take(int now)
	{
		int w = wave;
		boolean fresh = w > 0 && now - tick <= HOLD_TICKS && now >= tick;
		clear();
		return fresh ? w : 0;
	}

	public void clear()
	{
		wave = 0;
		tick = 0;
	}
}
