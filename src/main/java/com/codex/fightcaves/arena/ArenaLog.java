package com.codex.fightcaves.arena;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The Inferno and Colosseum run logs for one account, saved as one blob under its own key beside
 * the Fight Cave logbook. Every attempt is kept with its wave splits, how far it got and how it
 * ended; the run in progress is kept too, so an Inferno paused between waves picks up again.
 */
public final class ArenaLog
{
	/** Attempts kept per arena; the oldest go first. */
	public static final int KEEP = 300;

	/** How an attempt ended. */
	public enum Result
	{
		/** The last wave was cleared: TzKal-Zuk or Sol Heredit beaten. */
		COMPLETE,
		/** You died on the wave you were on. */
		DIED,
		/** Colosseum: you ended the run between waves and took the chest. */
		CLAIMED,
		/** You left (teleported, walked or logged out) without dying or claiming. */
		LEFT
	}

	/** One attempt, finished or in progress. */
	public static final class Run
	{
		/** The first wave seen; 1 for a run watched from the start. */
		public int firstWave;
		/** The furthest wave started. */
		public int reached;
		/** Waves cleared: reached, or one less when the run ended on a wave. */
		public int cleared;
		/** Ticks each wave took, wave 1 first; 0 for a wave not timed. */
		public List<Integer> splits = new ArrayList<>();
		/** Ticks spent in the arena (Inferno), or the sum of the game's wave times (Colosseum). */
		public int ticks;
		/** The game's own total for a finished run, in ticks, or 0 when it did not say. */
		public int gameTicks;
		public long startedAt;
		public long endedAt;
		public Result result;
		/** True when the run was picked up part way, so its times are not a record. */
		public boolean partial;
		/** In progress: ticks on the wave on now, kept so a paused Inferno times it whole. */
		public int waveTicks;

		int onWave()
		{
			return waveTicks;
		}

		/** Ticks from wave 1 through {@code wave}, or 0 when a wave there went untimed. */
		public int clockAt(int wave)
		{
			if (partial || wave < 1 || wave > splits.size())
			{
				return 0;
			}
			int sum = 0;
			for (int w = 1; w <= wave; w++)
			{
				Integer t = splits.get(w - 1);
				if (t == null || t <= 0)
				{
					return 0;
				}
				sum += t;
			}
			return sum;
		}

		/** Waves cleared from wave 1 with every one timed. */
		public int timedThrough()
		{
			if (partial)
			{
				return 0;
			}
			int n = 0;
			while (n < splits.size() && n < cleared && splits.get(n) != null && splits.get(n) > 0)
			{
				n++;
			}
			return n;
		}

		/** The run's time: the game's own when it gave one, else the pilot's. */
		public int time()
		{
			return gameTicks > 0 ? gameTicks : ticks;
		}
	}

	/** One arena's attempts. */
	public static final class Book
	{
		public List<Run> runs = new ArrayList<>();
		/** The attempt in progress, or null. */
		public Run active;
		/** True when the Inferno said it was paused: leaving then does not end the run. */
		public boolean paused;

		public int completions()
		{
			int n = 0;
			for (Run r : runs)
			{
				if (r.result == Result.COMPLETE)
				{
					n++;
				}
			}
			return n;
		}

		public int furthest()
		{
			int n = 0;
			for (Run r : runs)
			{
				n = Math.max(n, r.reached);
			}
			return n;
		}
	}

	public Book inferno = new Book();
	public Book colosseum = new Book();

	public Book book(Arena arena)
	{
		return arena == Arena.INFERNO ? inferno : colosseum;
	}

	/** Reads a saved log, or a fresh one when there is none or it cannot be read. */
	public static ArenaLog read(Gson gson, String json)
	{
		ArenaLog log = null;
		if (json != null && !json.isEmpty())
		{
			try
			{
				log = gson.fromJson(json, ArenaLog.class);
			}
			catch (JsonParseException | IllegalStateException e)
			{
				log = null;
			}
		}
		if (log == null)
		{
			log = new ArenaLog();
		}
		log.inferno = clean(log.inferno);
		log.colosseum = clean(log.colosseum);
		return log;
	}

	private static Book clean(Book b)
	{
		if (b == null)
		{
			b = new Book();
		}
		if (b.runs == null)
		{
			b.runs = new ArrayList<>();
		}
		b.runs.removeIf(r -> r == null || r.result == null);
		for (Run r : b.runs)
		{
			cleanRun(r);
		}
		if (b.active != null)
		{
			cleanRun(b.active);
		}
		return b;
	}

	private static void cleanRun(Run r)
	{
		if (r.splits == null)
		{
			r.splits = new ArrayList<>();
		}
		r.splits.replaceAll(t -> t == null || t < 0 ? 0 : t);
		r.firstWave = Math.max(1, r.firstWave);
		r.reached = Math.max(r.reached, r.cleared);
	}

	/** A deep copy, for the panel to read while the client thread goes on writing. */
	public ArenaLog copy(Gson gson)
	{
		return gson.fromJson(gson.toJson(this), ArenaLog.class);
	}
}
