package com.codex.fightcaves.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Numbers, times and dates the way the panel writes them. */
public final class Format
{
	private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

	private Format()
	{
	}

	/** Game ticks as a clock: 42:07, or 1:02:07 past an hour. */
	public static String ticks(int ticks)
	{
		long seconds = Math.round(ticks * 0.6);
		long h = seconds / 3600;
		long m = (seconds % 3600) / 60;
		long s = seconds % 60;
		if (h > 0)
		{
			return String.format(Locale.ENGLISH, "%d:%02d:%02d", h, m, s);
		}
		return String.format(Locale.ENGLISH, "%d:%02d", m, s);
	}

	/** A signed difference in ticks: "+0:12" behind, "-0:30" ahead. */
	public static String delta(int ticks)
	{
		return (ticks < 0 ? "-" : "+") + ticks(Math.abs(ticks));
	}

	public static String day(long epochMs)
	{
		return SHORT.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}

	/** "1 dose", "3 doses" */
	public static String doses(int n)
	{
		return n + (n == 1 ? " dose" : " doses");
	}

	/** "1 wave", "3 waves" */
	public static String waves(int n)
	{
		return n + (n == 1 ? " wave" : " waves");
	}

	/** Spare doses with a sign, rounded toward caution. */
	public static String spare(double doses)
	{
		long n = doses >= 0 ? (long) Math.floor(doses) : -(long) Math.ceil(-doses);
		return (n >= 0 ? "+" : "") + n;
	}

	/** How long ago something was seen: "just now", "12 minutes ago", "3 hours ago", "2 days ago". */
	public static String ago(long thenMs, long nowMs)
	{
		long minutes = Math.max(0, nowMs - thenMs) / 60_000L;
		if (minutes < 1)
		{
			return "just now";
		}
		if (minutes < 60)
		{
			return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
		}
		long hours = minutes / 60;
		if (hours < 48)
		{
			return hours + (hours == 1 ? " hour ago" : " hours ago");
		}
		return (hours / 24) + " days ago";
	}
}
