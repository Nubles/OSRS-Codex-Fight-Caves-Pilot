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

	final State state;
	final int wave;
	final int rotation;
	final List<Integer> candidates;
	final int decidingWave;
	final boolean clean;
	final int alive;
	final PrayerPacer.Pace pace;
	final int prayer;
	final int prayerLevel;
	final boolean paceLearned;
	/** Doses a full run takes at your rate, for the briefing outside the cave. */
	final int fullRunDoses;
	final int runTicks;
	/** Ticks ahead of (negative) or behind your best run's ghost at the end of the last wave cleared, or null. */
	final Integer versusBest;
	final Logbook book;
	final Map<String, List<String>> corrections;
	/** The wave on now as it was seen spawning, for before the rotation is known. */
	List<Placement> observed = Collections.emptyList();
	/** Whether the ghost is a cape (else your furthest run), and the waves it cleared; 0 with no ghost. */
	boolean ghostCape;
	int ghostCleared;
	/** The run's cleared waves against the ghost, wave 1 first, or empty with no ghost. */
	List<com.codex.fightcaves.core.Ghost.Split> ghostSplits = Collections.emptyList();
	/** Doses drunk and food eaten this run and on the wave on now, and the ghost's on this wave (-1 with no ghost). */
	int runDoses;
	int runFood;
	int waveDoses;
	int waveFood;
	int ghostWaveDoses = -1;
	int ghostWaveFood = -1;
	/** Your prayer against a whole run with what you carry, for the briefing outside the cave, or null. */
	PrayerPacer.Pace carried;
	/** True when the run in progress was picked up part way, so its clock is not a record. */
	boolean partial;
	/** The run clock when Jad's wave begins at your usual pace, or null when not known. */
	Integer jadAt;
	/** The ready check at the cave mouth, or empty. */
	List<com.codex.fightcaves.core.Ready.Line> ready = Collections.emptyList();
	/** The prayer doses in your bank as last seen, or null when the bank has not been seen. */
	com.codex.fightcaves.core.Banked banked;

	/** The rotation clock is on (the setting). */
	boolean clockOn;
	/** The game's clock at the cave mouth and when it was read, or null away from the cave. */
	com.codex.fightcaves.core.RotationClock.Reading clockReading;
	long clockReadAt;
	/** Draw the clock at its reading rather than running it on, for previews. */
	boolean clockFrozen;
	/** Your fastest and steadiest rotations, and how far the clock has earned trust here. */
	com.codex.fightcaves.core.RotationPicks picks;
	com.codex.fightcaves.core.ClockTrust clockTrust;
	/** In the cave: the rotation shown came from the clock, not yet the spawns. */
	boolean fromClock;
	/** In the cave: what the clock said as you went in (and the other side of a change), or 0. */
	int clockSaid;
	int clockAlt;
	/** In the cave: whether the spawns bore the clock out, or null until they settle. */
	Boolean clockHit;

	public View(State state, int wave, int rotation, List<Integer> candidates, int decidingWave, boolean clean, int alive,
		PrayerPacer.Pace pace, int prayer, int prayerLevel, boolean paceLearned, int fullRunDoses,
		int runTicks, Integer versusBest, Logbook book)
	{
		this.state = state;
		this.wave = wave;
		this.rotation = rotation;
		this.candidates = candidates == null ? Collections.emptyList() : candidates;
		this.decidingWave = decidingWave;
		this.clean = clean;
		this.alive = alive;
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
		return new View(State.LOGGED_OUT, 0, 0, null, 0, false, 0, null, 0, 0, false, 0, 0, null, null);
	}

	/** Adds the wave on now as it was seen spawning. */
	public View withObserved(List<Placement> observed)
	{
		this.observed = observed == null ? Collections.emptyList() : observed;
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

	/** Adds the run against its ghost: whether it is a cape, the waves it cleared, and every cleared wave against it. */
	public View withGhost(boolean cape, int cleared, List<com.codex.fightcaves.core.Ghost.Split> splits)
	{
		ghostCape = cape;
		ghostCleared = cleared;
		ghostSplits = splits == null ? Collections.emptyList() : splits;
		return this;
	}

	/** Adds the supplies used this run and on this wave, and the ghost's on this wave (-1 with no ghost). */
	public View withSupplies(int runDoses, int runFood, int waveDoses, int waveFood, int ghostWaveDoses, int ghostWaveFood)
	{
		this.runDoses = runDoses;
		this.runFood = runFood;
		this.waveDoses = waveDoses;
		this.waveFood = waveFood;
		this.ghostWaveDoses = ghostWaveDoses;
		this.ghostWaveFood = ghostWaveFood;
		return this;
	}

	/** Adds the rotation clock as seen outside the cave: the reading is null away from the cave mouth. */
	public View withClock(com.codex.fightcaves.core.RotationClock.Reading reading, long readAt,
		com.codex.fightcaves.core.RotationPicks picks, com.codex.fightcaves.core.ClockTrust trust)
	{
		clockOn = true;
		clockReading = reading;
		clockReadAt = readAt;
		this.picks = picks;
		clockTrust = trust;
		return this;
	}

	/** Holds the clock at its reading, for previews and tests. */
	public View frozenClock()
	{
		clockFrozen = true;
		return this;
	}

	/**
	 * Adds what the clock said as you went in, whether the rotation shown is the clock's alone,
	 * and whether the spawns bore it out (null until they settle).
	 */
	public View withEntryClock(int said, int alt, boolean fromClock, Boolean hit, com.codex.fightcaves.core.ClockTrust trust)
	{
		clockOn = true;
		clockSaid = said;
		clockAlt = alt;
		this.fromClock = fromClock;
		clockHit = hit;
		clockTrust = trust;
		return this;
	}

	public boolean isFromClock()
	{
		return fromClock;
	}

	/** Ticks behind (positive) or ahead of the ghost at the end of the last wave cleared, or null. */
	public Integer getVersusBest()
	{
		return versusBest;
	}

	public List<com.codex.fightcaves.core.Ghost.Split> getGhostSplits()
	{
		return ghostSplits;
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
