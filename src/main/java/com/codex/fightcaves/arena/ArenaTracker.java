package com.codex.fightcaves.arena;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Follows Inferno and Colosseum attempts from the game's own chat lines and files each one in the
 * {@link ArenaLog}: when each wave began and ended, how far the attempt got and how it ended. It
 * only reads; it never says anything about a wave while it is on.
 *
 * <p>The lines it reads, and where each is known from:
 * <ul>
 * <li>{@code Wave: N}: RuneLite's TimersAndBuffsPlugin (TZHAAR_WAVE_MESSAGE), which reads it in both
 * the Fight Cave and the Inferno ("The first wave message of the inferno comes six seconds after the
 * ingame timer starts counting").</li>
 * <li>{@code Your TzKal-Zuk kill count is: N.} and {@code Duration: T}: RuneLite's
 * ChatCommandsPluginTest (testZukKill, testZukNewPb).</li>
 * <li>{@code The Inferno has been paused. You may now log out.}: RuneLite's TimersAndBuffsPlugin
 * (TZHAAR_PAUSED_MESSAGE).</li>
 * <li>{@code Search the chest nearby to retrieve your earned rewards!}: RuneLite's ScreenshotPlugin,
 * which takes it as the Colosseum's end.</li>
 * <li>{@code Wave N completed! Wave duration: T} and {@code Colosseum duration: T}: unconfirmed, from
 * the Fortis Colosseum plugin (LlemonDuck, BSD-2, SplitsTracker) and Blert (ColosseumChallenge); no
 * wiki page or RuneLite source quotes them.</li>
 * </ul>
 */
public final class ArenaTracker
{
	/** Ticks outside the arena, logged in, before an open attempt is taken as over. */
	static final int SETTLE_TICKS = 5;
	/** Ticks after a run is filed in which the game's own total for it may still arrive. */
	static final int DURATION_GRACE = 10;

	private static final Pattern TAGS = Pattern.compile("<[^>]*>");
	static final Pattern WAVE = Pattern.compile("^Wave: (\\d+)");
	static final Pattern ZUK_KC = Pattern.compile("^Your TzKal-Zuk kill count is");
	static final Pattern DURATION = Pattern.compile("^Duration: ([0-9:.]+)");
	static final Pattern PAUSED = Pattern.compile("^The Inferno has been paused\\. You may now log out\\.");
	static final Pattern COLO_WAVE_DONE = Pattern.compile("^Wave (\\d+) completed! Wave duration: ([0-9:.]+)");
	static final Pattern COLO_DURATION = Pattern.compile("^Colosseum duration: ([0-9:.]+)");
	static final String COLO_CHEST = "Search the chest nearby to retrieve your earned rewards!";

	private ArenaLog log = new ArenaLog();
	private int tick;
	private int outside;
	/** The run filed last, its arena and the tick, for the game's total that follows it. */
	private ArenaLog.Run lastFiled;
	private Arena lastFiledArena;
	private int lastFiledTick = Integer.MIN_VALUE / 2;
	/** Colosseum: a wave has been cleared and the next not yet begun. */
	private boolean between;
	private boolean changed;

	public void load(ArenaLog log)
	{
		this.log = log == null ? new ArenaLog() : log;
		outside = 0;
		lastFiled = null;
		between = colosseumOpen() && this.log.colosseum.active.cleared >= this.log.colosseum.active.reached;
	}

	public ArenaLog getLog()
	{
		return log;
	}

	/** True once since the last call when something worth saving changed. */
	public boolean takeChanged()
	{
		boolean c = changed;
		changed = false;
		return c;
	}

	/**
	 * A game message. Returns true when it was one of the arenas' own lines, which the Fight Cave
	 * must then not read as its own.
	 *
	 * @param here the arena you stand in, or null
	 */
	public boolean chat(String message, Arena here, long now)
	{
		String text = TAGS.matcher(message).replaceAll("").trim();
		if (here == Arena.INFERNO)
		{
			Matcher m = WAVE.matcher(text);
			if (m.find())
			{
				infernoWave(Integer.parseInt(m.group(1)), now);
				return true;
			}
			if (PAUSED.matcher(text).find())
			{
				log.inferno.paused = true;
				changed = true;
				return true;
			}
		}
		if (ZUK_KC.matcher(text).find() && log.inferno.active != null)
		{
			ArenaLog.Run r = log.inferno.active;
			setSplit(r, r.reached, r.onWave());
			r.cleared = Arena.INFERNO.getWaves();
			r.reached = Arena.INFERNO.getWaves();
			finish(Arena.INFERNO, ArenaLog.Result.COMPLETE, now);
			return true;
		}
		Matcher d = DURATION.matcher(text);
		if (d.find() && lastFiledArena == Arena.INFERNO && tick - lastFiledTick <= DURATION_GRACE
			&& lastFiled != null && lastFiled.result == ArenaLog.Result.COMPLETE)
		{
			lastFiled.gameTicks = ticksOf(d.group(1));
			changed = true;
			return true;
		}
		if (here == Arena.COLOSSEUM)
		{
			Matcher done = COLO_WAVE_DONE.matcher(text);
			if (done.find())
			{
				coloWaveDone(Integer.parseInt(done.group(1)), ticksOf(done.group(2)), now);
				return true;
			}
			Matcher w = WAVE.matcher(text);
			if (w.find())
			{
				coloWaveStart(Integer.parseInt(w.group(1)), now);
				return true;
			}
			Matcher total = COLO_DURATION.matcher(text);
			if (total.find())
			{
				ArenaLog.Run r = log.colosseum.active != null ? log.colosseum.active
					: lastFiledArena == Arena.COLOSSEUM && tick - lastFiledTick <= DURATION_GRACE ? lastFiled : null;
				if (r != null)
				{
					r.gameTicks = ticksOf(total.group(1));
					changed = true;
				}
				return true;
			}
			if (text.contains(COLO_CHEST))
			{
				ArenaLog.Run r = log.colosseum.active;
				if (r != null && r.cleared < Arena.COLOSSEUM.getWaves())
				{
					// The run was ended between waves and the chest taken.
					r.reached = r.cleared;
					finish(Arena.COLOSSEUM, ArenaLog.Result.CLAIMED, now);
				}
				return true;
			}
		}
		return false;
	}

	/** One logged-in game tick, standing in {@code here} (null outside both arenas). */
	public void tick(Arena here, long now)
	{
		tick++;
		Arena open = openArena();
		if (open == null)
		{
			outside = 0;
			return;
		}
		ArenaLog.Book b = log.book(open);
		if (here == open)
		{
			outside = 0;
			if (open == Arena.INFERNO)
			{
				b.active.ticks++;
				b.active.waveTicks++;
			}
			else if (!between)
			{
				b.active.waveTicks++;
			}
			return;
		}
		outside++;
		if (outside >= SETTLE_TICKS && !(open == Arena.INFERNO && b.paused))
		{
			// Gone without dying, finishing or claiming: teleported, walked out or logged out.
			finish(open, ArenaLog.Result.LEFT, now);
		}
	}

	/** You took damage. In the Colosseum, between waves, that means the next wave has begun. */
	public void damage(Arena here, long now)
	{
		if (here != Arena.COLOSSEUM)
		{
			return;
		}
		ArenaLog.Run r = log.colosseum.active;
		if (r == null)
		{
			// The first wave began; its own call may not have come.
			start(Arena.COLOSSEUM, 1, now);
			between = false;
			return;
		}
		if (between)
		{
			between = false;
			r.reached = Math.max(r.reached, r.cleared + 1);
			r.waveTicks = 0;
			changed = true;
		}
	}

	/** You died, standing in {@code here}. */
	public void death(Arena here, long now)
	{
		if (here == null || log.book(here).active == null)
		{
			return;
		}
		ArenaLog.Run r = log.book(here).active;
		if (here == Arena.COLOSSEUM)
		{
			r.reached = Math.max(r.reached, r.cleared + 1);
		}
		r.cleared = Math.min(r.cleared, r.reached - 1);
		finish(here, ArenaLog.Result.DIED, now);
	}

	/** The arena with an attempt open, or null. */
	public Arena openArena()
	{
		return log.inferno.active != null ? Arena.INFERNO : log.colosseum.active != null ? Arena.COLOSSEUM : null;
	}

	/**
	 * True while a wave is on in {@code here}: an Inferno run that is not paused, or a Colosseum
	 * wave between its start and its end. Nothing but a plain clock is shown then.
	 */
	public boolean inWave(Arena here)
	{
		if (here == null || here != openArena())
		{
			return false;
		}
		return here == Arena.INFERNO ? !log.inferno.paused : !between;
	}

	/** Colosseum waves cleared while you stand between waves with the run open, or 0. */
	public int betweenWaves(Arena here)
	{
		return here == Arena.COLOSSEUM && colosseumOpen() && between ? log.colosseum.active.cleared : 0;
	}

	private boolean colosseumOpen()
	{
		return log.colosseum.active != null;
	}

	private void infernoWave(int n, long now)
	{
		ArenaLog.Book b = log.inferno;
		b.paused = false;
		ArenaLog.Run r = b.active;
		if (r != null && n == 1 && r.reached > 1)
		{
			// A new run while one was still open: the last ended out of sight.
			finish(Arena.INFERNO, ArenaLog.Result.LEFT, now);
			r = null;
		}
		if (r == null)
		{
			start(Arena.INFERNO, n, now);
			return;
		}
		if (n <= r.reached)
		{
			return;
		}
		if (n == r.reached + 1)
		{
			setSplit(r, r.reached, r.onWave());
		}
		r.cleared = n - 1;
		r.reached = n;
		r.waveTicks = 0;
		changed = true;
	}

	private void coloWaveStart(int n, long now)
	{
		ArenaLog.Run r = log.colosseum.active;
		if (r == null)
		{
			start(Arena.COLOSSEUM, n, now);
		}
		else if (n > r.reached || between)
		{
			r.reached = Math.max(r.reached, n);
			r.waveTicks = 0;
			changed = true;
		}
		between = false;
	}

	private void coloWaveDone(int n, int gameTicks, long now)
	{
		ArenaLog.Run r = log.colosseum.active;
		if (r == null)
		{
			r = start(Arena.COLOSSEUM, n, now);
		}
		else if (r.cleared == 0 && n > 1)
		{
			// Begun by a hit, but the first wave timed is a later one: picked up part way.
			r.firstWave = n;
			r.partial = true;
		}
		else if (n > r.cleared + 1)
		{
			r.partial = true;
		}
		setSplit(r, n, gameTicks > 0 ? gameTicks : r.onWave());
		r.cleared = Math.max(r.cleared, n);
		r.reached = Math.max(r.reached, n);
		r.waveTicks = 0;
		int sum = 0;
		for (int t : r.splits)
		{
			sum += t;
		}
		r.ticks = sum;
		changed = true;
		if (n >= Arena.COLOSSEUM.getWaves())
		{
			between = false;
			finish(Arena.COLOSSEUM, ArenaLog.Result.COMPLETE, now);
		}
		else
		{
			between = true;
		}
	}

	private ArenaLog.Run start(Arena arena, int wave, long now)
	{
		ArenaLog.Run r = new ArenaLog.Run();
		r.firstWave = Math.max(1, wave);
		r.reached = r.firstWave;
		r.cleared = r.firstWave - 1;
		r.partial = r.firstWave > 1;
		r.startedAt = now;
		log.book(arena).active = r;
		log.book(arena).paused = false;
		outside = 0;
		changed = true;
		return r;
	}

	private void finish(Arena arena, ArenaLog.Result result, long now)
	{
		ArenaLog.Book b = log.book(arena);
		ArenaLog.Run r = b.active;
		if (r == null)
		{
			return;
		}
		r.result = result;
		r.endedAt = now;
		r.waveTicks = 0;
		if (arena == Arena.COLOSSEUM)
		{
			int sum = 0;
			for (int t : r.splits)
			{
				sum += t;
			}
			r.ticks = sum;
		}
		b.runs.add(r);
		while (b.runs.size() > ArenaLog.KEEP)
		{
			b.runs.remove(0);
		}
		b.active = null;
		b.paused = false;
		if (arena == Arena.COLOSSEUM)
		{
			between = false;
		}
		lastFiled = r;
		lastFiledArena = arena;
		lastFiledTick = tick;
		outside = 0;
		changed = true;
	}

	private static void setSplit(ArenaLog.Run r, int wave, int ticks)
	{
		if (wave < 1)
		{
			return;
		}
		List<Integer> s = r.splits;
		while (s.size() < wave)
		{
			s.add(0);
		}
		s.set(wave - 1, Math.max(0, ticks));
	}

	/**
	 * A game time such as "0:46.20", "104:31" or "1:02:03" as game ticks of 0.6 seconds
	 * (oldschool.runescape.wiki/w/Game_tick), rounded to the nearest; 0 when it cannot be read.
	 */
	static int ticksOf(String time)
	{
		if (time == null || time.isEmpty())
		{
			return 0;
		}
		String[] parts = time.split(":");
		double seconds = 0;
		try
		{
			for (String p : parts)
			{
				seconds = seconds * 60 + Double.parseDouble(p);
			}
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
		return (int) Math.round(seconds / 0.6);
	}
}
