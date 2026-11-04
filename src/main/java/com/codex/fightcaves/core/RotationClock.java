package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Which rotation the cave gives you by the game's own clock. The OSRS wiki (TzHaar Fight
 * Cave/Rotations): "The rotation is determined by the exact time when the player enters the
 * caves", and it "changes exactly at every minute, except for Rotation 4, which lasts for two
 * minutes before changing". Fourteen one minute rotations and rotation 4's two minutes make a
 * cycle of 16 minutes, and a day (1440 minutes) is exactly 90 cycles, so the order never slips
 * at midnight.
 *
 * <p>The order of the rotations through the cycle is the one the Fight Caves Spawn Predictor
 * plugin (github.com/damencs/spawn-predictor, BSD-2) uses: minutes since midnight modulo 16
 * picks a column of the wiki's rotation table. Its rotation numbers were cross-checked against
 * this plugin's own table ({@link Rotations}): every wave of every rotation matches. The clock is
 * read from the game: minutes since midnight and seconds past the minute, which the server sends
 * to the client.
 */
public final class RotationClock
{
	/** Minutes in one cycle of the rotations. */
	public static final int CYCLE = 16;
	public static final int MINUTES_A_DAY = 24 * 60;
	/** Seconds in a day and in a cycle. */
	static final int DAY = MINUTES_A_DAY * 60;
	static final int CYCLE_SECONDS = CYCLE * 60;
	/** Within this many seconds of a change, the next rotation may be the one you get. */
	public static final int MARGIN = 2;

	/** The rotation on at each minute of the cycle, minutes since midnight modulo 16. */
	private static final int[] BY_MINUTE = {4, 2, 9, 11, 13, 1, 6, 15, 10, 8, 5, 3, 12, 14, 7, 4};

	private RotationClock()
	{
	}

	/** The rotation you get entering at this many minutes past midnight (any whole number). */
	public static int rotationAt(int minutes)
	{
		return BY_MINUTE[Math.floorMod(minutes, CYCLE)];
	}

	/** True when a reading of the game clock makes sense: minutes in a day and seconds in a minute. */
	public static boolean valid(int minutes, int seconds)
	{
		return minutes >= 0 && minutes < MINUTES_A_DAY && seconds >= 0 && seconds < 60;
	}

	/** Seconds from the given moment until the rotation on then gives way to the next. */
	public static int secondsLeft(int minutes, int seconds)
	{
		int m = Math.floorMod(minutes, CYCLE);
		// Rotation 4 holds minute 15 of one cycle and minute 0 of the next.
		int whole = m == CYCLE - 1 ? 120 : 60;
		return whole - seconds;
	}

	/**
	 * Seconds until a rotation next comes on, or 0 when it is on now.
	 *
	 * @param rotation 1 to 15
	 */
	public static int secondsUntil(int rotation, int minutes, int seconds)
	{
		if (rotationAt(minutes) == rotation)
		{
			return 0;
		}
		for (int ahead = 1; ahead <= CYCLE; ahead++)
		{
			if (rotationAt(minutes + ahead) == rotation)
			{
				return ahead * 60 - seconds;
			}
		}
		return -1;
	}

	/** One rotation's turn on the clock. */
	public static final class Slot
	{
		private final int rotation;
		private final int startsIn;
		private final int endsIn;

		Slot(int rotation, int startsIn, int endsIn)
		{
			this.rotation = rotation;
			this.startsIn = startsIn;
			this.endsIn = endsIn;
		}

		public int getRotation()
		{
			return rotation;
		}

		/** Seconds until it comes on; 0 or less when it is on now. */
		public int getStartsIn()
		{
			return startsIn;
		}

		/** Seconds until it gives way. */
		public int getEndsIn()
		{
			return endsIn;
		}

		public boolean isNow()
		{
			return startsIn <= 0;
		}

		@Override
		public String toString()
		{
			return rotation + "@" + startsIn;
		}
	}

	/**
	 * The rotation on now and the ones after it, each once per turn: rotation 4's two minutes are
	 * one turn.
	 */
	public static List<Slot> upcoming(int minutes, int seconds, int count)
	{
		List<Slot> list = new ArrayList<>();
		int m = Math.floorMod(minutes, CYCLE);
		int left = secondsLeft(minutes, seconds);
		int in = (m == 0 ? 60 : 0) + seconds;
		list.add(new Slot(rotationAt(minutes), -in, left));
		int start = left;
		int minute = minutes + (m == CYCLE - 1 ? 2 : 1);
		while (list.size() < count)
		{
			int rotation = rotationAt(minute);
			int length = Math.floorMod(minute, CYCLE) == CYCLE - 1 ? 120 : 60;
			list.add(new Slot(rotation, start, start + length));
			start += length;
			minute += length / 60;
		}
		return Collections.unmodifiableList(list);
	}

	/** A moment on the game's clock: minutes since midnight and seconds past the minute. */
	public static final class Reading
	{
		private final int minutes;
		private final int seconds;

		public Reading(int minutes, int seconds)
		{
			this.minutes = minutes;
			this.seconds = seconds;
		}

		public int getMinutes()
		{
			return minutes;
		}

		public int getSeconds()
		{
			return seconds;
		}

		/** The same clock some seconds on, wrapping at midnight. */
		public Reading plus(int secs)
		{
			int t = Math.floorMod(minutes * 60 + seconds + secs, DAY);
			return new Reading(t / 60, t % 60);
		}

		public int rotation()
		{
			return rotationAt(minutes);
		}

		public int secondsLeft()
		{
			return RotationClock.secondsLeft(minutes, seconds);
		}

		/** Seconds since the rotation on now came on. */
		public int secondsIn()
		{
			int m = Math.floorMod(minutes, CYCLE);
			return (m == 0 ? 60 : 0) + seconds;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof Reading && ((Reading) o).minutes == minutes && ((Reading) o).seconds == seconds;
		}

		@Override
		public int hashCode()
		{
			return minutes * 60 + seconds;
		}

		@Override
		public String toString()
		{
			return String.format("%02d:%02d:%02d", minutes / 60, minutes % 60, seconds);
		}
	}

	/**
	 * What the clock said as you went in. The server decides the rotation when you enter, but the
	 * plugin only sees the clock on the ticks around it: the last one outside and the first one in
	 * the cave. When those straddle a change, or sit within {@link #MARGIN} seconds of one, both
	 * rotations are kept and the first spawns choose: two rotations next to each other in time
	 * never share a wave 1 spawn, so wave 1 always settles it.
	 */
	public static final class Forecast
	{
		private final int said;
		private final int alt;
		private final int second;

		public Forecast(int said, int alt, int second)
		{
			this.said = said;
			this.alt = alt == said ? 0 : alt;
			this.second = second;
		}

		/**
		 * @param outside the clock on the last tick outside the cave
		 * @param inside the clock on the first tick inside, or null when it could not be read
		 */
		public static Forecast atEntry(Reading outside, Reading inside)
		{
			int said = outside.rotation();
			int alt = 0;
			if (inside != null && inside.rotation() != said)
			{
				alt = inside.rotation();
			}
			else if (outside.secondsLeft() <= MARGIN)
			{
				alt = outside.plus(outside.secondsLeft()).rotation();
			}
			else if (outside.secondsIn() < MARGIN)
			{
				alt = outside.plus(-outside.secondsIn() - 1).rotation();
			}
			return new Forecast(said, alt, outside.getSeconds());
		}

		/** The rotation the clock gave. */
		public int getSaid()
		{
			return said;
		}

		/** The rotation either side of a change, or 0 when the clock was clear. */
		public int getAlt()
		{
			return alt;
		}

		/** Seconds past the minute as you went in. */
		public int getSecond()
		{
			return second;
		}

		public boolean isSplit()
		{
			return alt > 0;
		}

		public List<Integer> rotations()
		{
			List<Integer> list = new ArrayList<>();
			list.add(said);
			if (alt > 0)
			{
				list.add(alt);
			}
			return list;
		}

		public boolean allows(int rotation)
		{
			return rotation == said || alt > 0 && rotation == alt;
		}
	}
}
