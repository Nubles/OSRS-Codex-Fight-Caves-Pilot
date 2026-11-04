package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.ClockTrust;
import com.codex.fightcaves.core.RotationClock;
import com.codex.fightcaves.core.RotationPicks;

/** The rotation clock's words, shared by the panel and the plaque at the cave mouth. */
public final class ClockText
{
	/** Seconds left in a turn at which the plaque warns that it is about to change. */
	public static final int CHANGING = 5;

	private ClockText()
	{
	}

	/** "0:41", "1:05", "12:00". */
	public static String clock(int seconds)
	{
		seconds = Math.max(0, seconds);
		return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
	}

	/** "13 in 0:41": the next rotation and when it comes on. */
	public static String next(RotationClock.Reading r)
	{
		return r.plus(r.secondsLeft()).rotation() + " in " + clock(r.secondsLeft());
	}

	/** True in the last few seconds of a turn, when going in may give either rotation. */
	public static boolean changing(RotationClock.Reading r)
	{
		return r.secondsLeft() <= CHANGING || r.secondsIn() < RotationClock.MARGIN;
	}

	/**
	 * "Your fastest, 9, is on now" or "Your fastest, 9, in 3:12", or null with no pick. A pick on
	 * now comes first; else the fastest, else the steadiest.
	 */
	public static String pick(RotationClock.Reading r, RotationPicks picks)
	{
		if (picks == null || picks.isEmpty())
		{
			return null;
		}
		int now = r.rotation();
		if (now == picks.getFastest() || now == picks.getSteadiest())
		{
			return (now == picks.getFastest() ? "Your fastest, " : "Your steadiest, ") + now + ", is on now";
		}
		boolean fast = picks.getFastest() > 0;
		int rotation = fast ? picks.getFastest() : picks.getSteadiest();
		String who = fast ? "Your fastest, " : "Your steadiest, ";
		return who + rotation + ", in " + clock(RotationClock.secondsUntil(rotation, r.getMinutes(), r.getSeconds()));
	}

	/** The plaque's word on a clock this account cannot trust, or null when it can. */
	public static String doubt(ClockTrust trust)
	{
		if (trust == null || trust.getLevel() != ClockTrust.Level.DOUBTFUL)
		{
			return null;
		}
		return "Missed " + trust.getRecentMisses() + " of your last " + trust.getRecent() + ": spawns decide";
	}
}
