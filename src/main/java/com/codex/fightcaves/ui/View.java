package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.PrayerPacer;
import com.codex.fightcaves.core.Protect;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Everything the panel shows, worked out on the client thread and handed over whole. */
public final class View
{
	/** Where the player is. */
	public enum State
	{
		LOGGED_OUT,
		OUTSIDE,
		IN_CAVE
	}

	/** What one creature will do if you stand on the chosen tile. */
	public static final class Fate
	{
		final com.codex.fightcaves.core.Monster monster;
		final String text;
		final boolean safe;

		public Fate(com.codex.fightcaves.core.Monster monster, String text, boolean safe)
		{
			this.monster = monster;
			this.text = text;
			this.safe = safe;
		}
	}

	/** Where to stand, as the panel draws it. */
	public static final class Stand
	{
		/** Where on the cave chart, as fractions of its width and height. */
		final double fx;
		final double fy;
		/** True when this is where to be for the next wave rather than the fight on now. */
		final boolean next;
		final boolean safe;
		final int steps;
		final List<Fate> fates;

		public Stand(double fx, double fy, boolean next, boolean safe, int steps, List<Fate> fates)
		{
			this.fx = fx;
			this.fy = fy;
			this.next = next;
			this.safe = safe;
			this.steps = steps;
			this.fates = fates == null ? Collections.emptyList() : fates;
		}
	}

	/** The protection prayer the fight calls for. */
	public static final class Call
	{
		final Protect prayer;
		final Protect also;
		final boolean up;
		final List<com.codex.fightcaves.core.Monster> answered;

		/**
		 * @param also a second style hitting you that the prayer cannot block, or null
		 * @param up whether you have the prayer on
		 */
		public Call(Protect prayer, Protect also, boolean up, List<com.codex.fightcaves.core.Monster> answered)
		{
			this.prayer = prayer;
			this.also = also;
			this.up = up;
			this.answered = answered == null ? Collections.emptyList() : answered;
		}

		public Protect getPrayer()
		{
			return prayer;
		}

		public boolean isUp()
		{
			return up;
		}
	}

	/** One creature's place in the kill order. */
	public static final class Kill
	{
		final com.codex.fightcaves.core.Monster monster;
		final String reason;
		final boolean reachable;

		public Kill(com.codex.fightcaves.core.Monster monster, String reason, boolean reachable)
		{
			this.monster = monster;
			this.reason = reason;
			this.reachable = reachable;
		}
	}

	final State state;
	final int wave;
	final int rotation;
	final List<Integer> candidates;
	final int decidingWave;
	final boolean clean;
	final int alive;
	final Stand stand;
	/** Where you are on the cave chart, or null. */
	final double[] you;
	final PrayerPacer.Pace pace;
	final int prayer;
	final int prayerLevel;
	final boolean paceLearned;
	/** Doses a full run takes at your rate, for the briefing outside the cave. */
	final int fullRunDoses;
	final int runTicks;
	/** Ticks ahead of (negative) or behind your best run at this wave, or null. */
	final Integer versusBest;
	final Logbook book;
	final Map<String, List<String>> corrections;
	/** The wave on now as it was seen spawning, for before the rotation is known. */
	List<Placement> observed = Collections.emptyList();
	Call call;
	List<Kill> kills = Collections.emptyList();
	/** The next wave played out before it spawns, in tiles of the cave's map square, or null. */
	com.codex.fightcaves.core.Rehearsal rehearsal;
	/** Walkable tiles of the cave's map square, [x][y], or null before the cave is read. */
	boolean[][] floor;
	/** Where you stand, in tiles of the cave's map square, or null. */
	int[] youTile;
	/** Why the gilt tile beats where you stand, or null when it does not clearly. */
	com.codex.fightcaves.core.Compare compare;
	/** The latest waves cleared this run: {wave, ticks, ticks against your best cape or MIN_VALUE}. */
	List<int[]> recentSplits = Collections.emptyList();
	/** The road to Jad for your rotation, one letter per wave (see Outlook), or null. */
	String outlook;
	/** Why the next hard wave is hard, from this visit's plan, or null. */
	String outlookReason;
	/** Your prayer against a whole run with what you carry, for the briefing outside the cave, or null. */
	PrayerPacer.Pace carried;
	/** True when the run in progress was picked up part way, so its clock is not a record. */
	boolean partial;
	/** What could land on you at once past your prayer, when it is enough to kill you; else 0. */
	int danger;
	/** The run clock when Jad's wave begins at your usual pace, or null when not known. */
	Integer jadAt;
	/** The ready check at the cave mouth, or empty. */
	List<com.codex.fightcaves.core.Ready.Line> ready = Collections.emptyList();
	/** The prayer doses in your bank as last seen, or null when the bank has not been seen. */
	com.codex.fightcaves.core.Banked banked;

	public View(State state, int wave, int rotation, List<Integer> candidates, int decidingWave, boolean clean, int alive,
		Stand stand, double[] you, PrayerPacer.Pace pace, int prayer, int prayerLevel, boolean paceLearned, int fullRunDoses,
		int runTicks, Integer versusBest, Logbook book)
	{
		this.state = state;
		this.wave = wave;
		this.rotation = rotation;
		this.candidates = candidates == null ? Collections.emptyList() : candidates;
		this.decidingWave = decidingWave;
		this.clean = clean;
		this.alive = alive;
		this.stand = stand;
		this.you = you;
		this.pace = pace;
		this.prayer = prayer;
		this.prayerLevel = prayerLevel;
		this.paceLearned = paceLearned;
		this.fullRunDoses = fullRunDoses;
		this.runTicks = runTicks;
		this.versusBest = versusBest;
		this.book = book == null ? new Logbook() : book;
		this.corrections = this.book.corrections;
	}

	public static View loggedOut()
	{
		return new View(State.LOGGED_OUT, 0, 0, null, 0, false, 0, null, null, null, 0, 0, false, 0, 0, null, null);
	}

	/** Adds the wave on now as it was seen spawning. */
	public View withObserved(List<Placement> observed)
	{
		this.observed = observed == null ? Collections.emptyList() : observed;
		return this;
	}

	/** Adds the prayer the fight calls for and the kill order. */
	public View withFight(Call call, List<Kill> kills)
	{
		this.call = call;
		this.kills = kills == null ? Collections.emptyList() : kills;
		return this;
	}

	/** Adds the next wave's rehearsal and the floor it plays out on. */
	public View withRehearsal(com.codex.fightcaves.core.Rehearsal rehearsal, boolean[][] floor, int[] youTile)
	{
		this.rehearsal = rehearsal;
		this.floor = floor;
		this.youTile = youTile;
		return this;
	}

	/** Adds why the gilt tile beats where you stand. */
	public View withCompare(com.codex.fightcaves.core.Compare compare)
	{
		this.compare = compare;
		return this;
	}

	/** Adds the latest splits of the run in progress. */
	public View withSplits(List<int[]> recentSplits)
	{
		this.recentSplits = recentSplits == null ? Collections.emptyList() : recentSplits;
		return this;
	}

	/** Adds the road to Jad and why its next hard wave is hard. */
	public View withOutlook(String outlook, String reason)
	{
		this.outlook = outlook;
		this.outlookReason = reason;
		return this;
	}

	/** Adds your prayer against a whole run, for the briefing. */
	public View withCarried(PrayerPacer.Pace carried)
	{
		this.carried = carried;
		return this;
	}

	/** Marks the run in progress as picked up part way. */
	public View withPartial(boolean partial)
	{
		this.partial = partial;
		return this;
	}

	/** Adds the eat warning: what could land at once past your prayer, or 0. */
	public View withDanger(int danger)
	{
		this.danger = danger;
		return this;
	}

	/** Adds when the run reaches Jad at your usual pace (from wave 1 outside the cave), or null. */
	public View withForecast(Integer jadAt)
	{
		this.jadAt = jadAt;
		return this;
	}

	/** Adds the ready check for the cave mouth. */
	public View withReady(List<com.codex.fightcaves.core.Ready.Line> ready)
	{
		this.ready = ready == null ? Collections.emptyList() : ready;
		return this;
	}

	/** Adds the doses in your bank, or null when the bank has not been seen. */
	public View withBanked(com.codex.fightcaves.core.Banked banked)
	{
		this.banked = banked;
		return this;
	}

	public int getDanger()
	{
		return danger;
	}

	public Call getCall()
	{
		return call;
	}

	/**
	 * True on Jad's wave inside the cave. The pilot then shows nothing to read or follow: no
	 * plaque, no chart, no prayer pacing and no splits, only a line saying it has stepped back.
	 */
	public boolean quiet()
	{
		return state == State.IN_CAVE && wave >= com.codex.fightcaves.core.Waves.JAD;
	}

	public State getState()
	{
		return state;
	}

	public int getWave()
	{
		return wave;
	}

	public int getRotation()
	{
		return rotation;
	}

	public PrayerPacer.Pace getPace()
	{
		return pace;
	}

	List<Placement> placements(int rotation, int wave)
	{
		return Logbook.placements(corrections, rotation, wave);
	}
}
