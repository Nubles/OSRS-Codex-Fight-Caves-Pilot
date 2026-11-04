package com.codex.fightcaves.core;

import java.util.List;

/**
 * How far this account's rotation clock can be trusted, from its own checks: each run compares
 * what the clock said as you went in with what the spawns then read. The spawns are always the
 * answer; the clock only earns the right to name the rotation before them once it has been right
 * three runs running, and loses it again on a second miss in five.
 */
public final class ClockTrust
{
	/** Checks in a row the clock must get right before it names the rotation from wave 1. */
	public static final int EARN = 3;
	/** The recent checks looked at, and the misses among them that mean it is not to be trusted. */
	public static final int RECENT = 5;
	public static final int DOUBT = 2;
	/** Seconds either side of a minute that count as "at the change". */
	public static final int EDGE = 5;

	public enum Level
	{
		/** Too few checks yet: shown, but the spawns name the rotation. */
		UNPROVEN,
		/** Right the last three runs and at most one miss in five: it names the rotation from wave 1. */
		TRUSTED,
		/** One recent miss: shown, but the spawns name the rotation until it is right three in a row again. */
		SHAKY,
		/** Two misses or more in the last five: the pilot says so and reads the spawns only. */
		DOUBTFUL
	}

	private final Level level;
	private final int checks;
	private final int hits;
	private final int recent;
	private final int recentMisses;
	private final int edgeMisses;
	private final int misses;

	private ClockTrust(Level level, int checks, int hits, int recent, int recentMisses, int misses, int edgeMisses)
	{
		this.level = level;
		this.checks = checks;
		this.hits = hits;
		this.recent = recent;
		this.recentMisses = recentMisses;
		this.misses = misses;
		this.edgeMisses = edgeMisses;
	}

	/** @param checks the account's checks, oldest first */
	public static ClockTrust of(List<Logbook.ClockCheck> checks)
	{
		int n = 0;
		int hits = 0;
		int edge = 0;
		if (checks != null)
		{
			for (Logbook.ClockCheck c : checks)
			{
				if (c == null)
				{
					continue;
				}
				n++;
				if (c.hit())
				{
					hits++;
				}
				else if (c.second < EDGE || c.second >= 60 - EDGE)
				{
					edge++;
				}
			}
		}
		int recent = 0;
		int recentMisses = 0;
		int streak = 0;
		boolean streakOpen = true;
		if (checks != null)
		{
			for (int i = checks.size() - 1; i >= 0 && recent < RECENT; i--)
			{
				Logbook.ClockCheck c = checks.get(i);
				if (c == null)
				{
					continue;
				}
				recent++;
				if (c.hit())
				{
					if (streakOpen)
					{
						streak++;
					}
				}
				else
				{
					recentMisses++;
					streakOpen = false;
				}
			}
		}
		Level level;
		if (streak >= EARN && recentMisses < DOUBT)
		{
			level = Level.TRUSTED;
		}
		else if (recentMisses >= DOUBT)
		{
			level = Level.DOUBTFUL;
		}
		else if (n < EARN)
		{
			level = Level.UNPROVEN;
		}
		else
		{
			level = Level.SHAKY;
		}
		return new ClockTrust(level, n, hits, recent, recentMisses, n - hits, edge);
	}

	public Level getLevel()
	{
		return level;
	}

	/** True when the clock may name the rotation before the spawns do. */
	public boolean trusted()
	{
		return level == Level.TRUSTED;
	}

	public int getChecks()
	{
		return checks;
	}

	public int getHits()
	{
		return hits;
	}

	public int getMisses()
	{
		return misses;
	}

	public int getRecent()
	{
		return recent;
	}

	public int getRecentMisses()
	{
		return recentMisses;
	}

	/** Misses that came within {@link #EDGE} seconds of a minute changing. */
	public int getEdgeMisses()
	{
		return edgeMisses;
	}

	/** One honest line on how the clock has done on this account. */
	public String describe()
	{
		switch (level)
		{
			case TRUSTED:
				return "The clock has called " + hits + " of your " + plural(checks, "run") + " right, so the pilot names the rotation from wave 1. The spawns still have the last word.";
			case SHAKY:
				return "The clock missed once lately (" + hits + " of " + checks + " right). The spawns name the rotation until it is right " + EARN + " runs running again.";
			case DOUBTFUL:
				String line = "The clock missed " + recentMisses + " of your last " + recent + " runs on this account, so the pilot reads the spawns only.";
				if (misses > 0 && edgeMisses == misses)
				{
					line += " Every miss came within " + EDGE + " seconds of a minute changing: go in mid-minute.";
				}
				return line;
			default:
				return checks == 0
					? "Unproven on this account: after " + EARN + " runs that it calls right, the pilot names the rotation from wave 1. Until then the spawns do."
					: "Right " + hits + " of " + plural(checks, "run") + " so far. After " + EARN + " right in a row it names the rotation from wave 1.";
		}
	}

	private static String plural(int n, String word)
	{
		return n + " " + word + (n == 1 ? "" : "s");
	}
}
