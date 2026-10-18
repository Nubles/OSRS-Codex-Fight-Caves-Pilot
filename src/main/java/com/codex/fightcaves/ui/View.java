package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.PrayerPacer;
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
