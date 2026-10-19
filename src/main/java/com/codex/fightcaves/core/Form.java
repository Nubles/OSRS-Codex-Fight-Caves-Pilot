package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your form: the last ten runs, oldest first, the way a league table shows a team's last games.
 * Each is a cape, a fall (with its wave) or a run you walked out of. Under them one line sets
 * the capes of those runs against your rate over every run, and counts the capes in a row up to
 * the latest: "Last 10: 6 capes (60%), against 40% over all 20 runs. 3 in a row."
 *
 * <p>It reads only the runs the logbook already keeps. Runs you walked out of count as runs,
 * as they do in the logbook's own cape rate, so the two rates are measured alike.
 */
public final class Form
{
	/** How many runs the strip shows. */
	public static final int SIZE = 10;

	/** One run on the strip. */
	public static final class Pip
	{
		private final Logbook.Result result;
		private final int wave;

		Pip(Logbook.Result result, int wave)
		{
			this.result = result;
			this.wave = wave;
		}

		public Logbook.Result getResult()
		{
			return result;
		}

		/** The wave it ended on. */
		public int getWave()
		{
			return wave;
		}
	}

	private final List<Pip> pips;
	private final int capes;
	private final int streak;
	private final int allRuns;
	private final int allCapes;

	private Form(List<Pip> pips, int capes, int streak, int allRuns, int allCapes)
	{
		this.pips = Collections.unmodifiableList(pips);
		this.capes = capes;
		this.streak = streak;
		this.allRuns = allRuns;
		this.allCapes = allCapes;
	}

	/** The form in a logbook, or null before its first finished run. */
	public static Form of(Logbook book)
	{
		if (book == null || book.runs.isEmpty())
		{
			return null;
		}
		List<Logbook.Run> runs = book.runs;
		int from = Math.max(0, runs.size() - SIZE);
		List<Pip> pips = new ArrayList<>();
		int capes = 0;
		for (int i = from; i < runs.size(); i++)
		{
			Logbook.Run r = runs.get(i);
			Logbook.Result result = r.result == null ? Logbook.Result.LEFT : r.result;
			pips.add(new Pip(result, r.wave));
			capes += result == Logbook.Result.CAPE ? 1 : 0;
		}
		int streak = 0;
		for (int i = runs.size() - 1; i >= 0 && runs.get(i).result == Logbook.Result.CAPE; i--)
		{
			streak++;
		}
		// The logbook lets old runs go, but its totals count every run.
		int allRuns = Math.max(book.attempts, runs.size());
		int allCapes = Math.max(0, Math.min(book.capes, allRuns));
		return new Form(pips, capes, streak, allRuns, allCapes);
	}

	public List<Pip> getPips()
	{
		return pips;
	}

	public int getCapes()
	{
		return capes;
	}

	/** Capes in a row up to the latest run; 0 when the latest was not a cape. */
	public int getStreak()
	{
		return streak;
	}

	/**
	 * The middle of your timed capes' times, in ticks (the faster middle one for an even count),
	 * or -1 before the first. Capes picked up part way never took their whole time, so are left out.
	 */
	public static int usualCape(Logbook book)
	{
		List<Integer> t = new ArrayList<>();
		for (Logbook.Run r : book.runs)
		{
			if (r.result == Logbook.Result.CAPE && !r.partial && r.ticks > 0)
			{
				t.add(r.ticks);
			}
		}
		if (t.isEmpty())
		{
			return -1;
		}
		Collections.sort(t);
		return t.get((t.size() - 1) / 2);
	}

	/** How many timed capes {@link #usualCape} is the middle of. */
	public static int timedCapes(Logbook book)
	{
		int n = 0;
		for (Logbook.Run r : book.runs)
		{
			n += r.result == Logbook.Result.CAPE && !r.partial && r.ticks > 0 ? 1 : 0;
		}
		return n;
	}

	/** Whole percent, rounded, never 100 unless every run was a cape and never 0 unless none was. */
	public static int percent(int part, int whole)
	{
		if (whole <= 0 || part <= 0)
		{
			return 0;
		}
		if (part >= whole)
		{
			return 100;
		}
		int p = (int) Math.round(100.0 * part / whole);
		return Math.max(1, Math.min(99, p));
	}

	/**
	 * "Last 10: 6 capes (60%), against 40% over all 20 runs. 3 in a row." Until there are more
	 * runs than the strip holds the two rates are the same, so only the strip's is given.
	 */
	public String line()
	{
		int n = pips.size();
		StringBuilder b = new StringBuilder();
		b.append(n == 1 ? "Your one run: " : "Last " + n + ": ");
		b.append(capes == 0 ? "no capes" : capes == 1 ? "1 cape" : capes + " capes");
		if (n > 1)
		{
			b.append(" (").append(percent(capes, n)).append("%)");
		}
		if (allRuns > n)
		{
			b.append(", against ").append(percent(allCapes, allRuns)).append("% over all ")
				.append(String.format(java.util.Locale.ENGLISH, "%,d", allRuns)).append(" runs");
		}
		b.append('.');
		if (streak >= 2)
		{
			b.append(' ').append(streak).append(" in a row.");
		}
		return b.toString();
	}
}
