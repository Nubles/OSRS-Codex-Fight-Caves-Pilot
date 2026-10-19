package com.codex.fightcaves.core;

/**
 * Paces prayer against the waves still to come. The cave's waves grow by a fixed ladder, so
 * what is left of a run is known exactly in hitpoints; the pacer learns how many prayer points
 * you spend per hitpoint of wave and turns that into the points the rest of the run needs.
 * Points on hand are your prayer now plus every dose you carry.
 *
 * Only waves with a ranger or mager in them teach the pacer, because those are the waves
 * where protection prayers are up; early waves fought without prayer would teach it to
 * expect far too little of the later ones.
 */
public final class PrayerPacer
{
	/** Points per hitpoint of wave before anything is learned: protection up the whole wave. */
	public static final double PRIOR_RATE = 0.10;
	/** How many hitpoints of waves the prior counts as, so a single odd wave cannot swing it. */
	static final double PRIOR_WEIGHT = 1500;
	/** Each learned wave fades older ones a little, so the pacer follows your gear as it improves. */
	static final double FADE = 0.985;

	/** How the run's prayer is holding up. */
	public enum State
	{
		/** Well more than the rest of the run needs. */
		AMPLE,
		/** Enough, with a little to spare. */
		ON_PACE,
		/** About even: little room for mistakes. */
		TIGHT,
		/** Not enough for the rest of the run at your usual rate. */
		SHORT
	}

	private double spent;
	private double hitpoints;

	public PrayerPacer()
	{
		this(PRIOR_RATE * PRIOR_WEIGHT, PRIOR_WEIGHT);
	}

	/** Restores a pacer from its saved sums. */
	public PrayerPacer(double spent, double hitpoints)
	{
		this.spent = spent;
		this.hitpoints = hitpoints;
	}

	public double getSpent()
	{
		return spent;
	}

	public double getHitpoints()
	{
		return hitpoints;
	}

	/** Prayer points you spend per hitpoint of wave, as learned. */
	public double rate()
	{
		return hitpoints <= 0 ? PRIOR_RATE : spent / hitpoints;
	}

	/** True once enough of your own waves are in the rate to outweigh the starting guess. */
	public boolean learned()
	{
		return hitpoints > PRIOR_WEIGHT * 2;
	}

	/**
	 * Learns from a finished wave.
	 *
	 * @param points prayer points drained during the wave
	 */
	public void learn(int wave, int points)
	{
		if (!teaches(wave) || points < 0)
		{
			return;
		}
		spent = spent * FADE + points;
		hitpoints = hitpoints * FADE + Waves.hitpoints(wave);
	}

	/** Waves with a ranger or mager in them, where protection prayers are expected to be up. */
	public static boolean teaches(int wave)
	{
		if (wave >= Waves.JAD)
		{
			return false;
		}
		for (Monster m : Waves.of(wave))
		{
			if (m.isRanged())
			{
				return true;
			}
		}
		return false;
	}

	/** Hitpoints of the waves from this one on that need prayer, the ones the rate is learned on. */
	public static int prayerHitpointsFrom(int wave)
	{
		int hp = 0;
		for (int w = Math.max(1, wave); w < Waves.JAD; w++)
		{
			if (teaches(w))
			{
				hp += Waves.hitpoints(w);
			}
		}
		return hp;
	}

	/** Points one dose restores: a quarter of your Prayer level plus 7, or 8 for super restores. */
	public static int restore(int level, boolean superRestore)
	{
		return level / 4 + (superRestore ? 8 : 7);
	}

	/**
	 * Works out the pace.
	 *
	 * @param wave the wave being fought, or about to be
	 * @param points prayer points now
	 * @param level your Prayer level, the most a dose fills you to
	 * @param prayerDoses prayer potion doses carried
	 * @param restoreDoses super restore and sanfew doses carried
	 * @param jadDoses doses to hold back for Jad
	 */
	public Pace pace(int wave, int points, int level, int prayerDoses, int restoreDoses, int jadDoses)
	{
		int perPrayer = restore(level, false);
		int perRestore = restore(level, true);
		int doses = prayerDoses + restoreDoses;
		double perDose = doses == 0 ? perPrayer : (prayerDoses * perPrayer + restoreDoses * perRestore) / (double) doses;
		int need = (int) Math.ceil(rate() * prayerHitpointsFrom(wave) + jadDoses * perPrayer);
		int have = points + prayerDoses * perPrayer + restoreDoses * perRestore;
		double spare = (have - need) / perDose;
		State state;
		if (spare >= Math.max(4, doses * 0.25))
		{
			state = State.AMPLE;
		}
		else if (spare >= 1)
		{
			state = State.ON_PACE;
		}
		else if (spare >= -2)
		{
			state = State.TIGHT;
		}
		else
		{
			state = State.SHORT;
		}
		int dosesNeeded = (int) Math.ceil(Math.max(0, need - points) / perDose);
		int drinkBelow = Math.max(1, level - perPrayer + 1);
		return new Pace(state, doses, dosesNeeded, need, have, spare, drinkBelow, perPrayer);
	}

	/** Where the run's prayer stands. */
	public static final class Pace
	{
		private final State state;
		private final int doses;
		private final int dosesNeeded;
		private final int need;
		private final int have;
		private final double spare;
		private final int drinkBelow;
		private final int perDose;

		Pace(State state, int doses, int dosesNeeded, int need, int have, double spare, int drinkBelow, int perDose)
		{
			this.state = state;
			this.doses = doses;
			this.dosesNeeded = dosesNeeded;
			this.need = need;
			this.have = have;
			this.spare = spare;
			this.drinkBelow = drinkBelow;
			this.perDose = perDose;
		}

		public State getState()
		{
			return state;
		}

		/** Doses carried. */
		public int getDoses()
		{
			return doses;
		}

		/** Doses the rest of the run is expected to take, on top of the prayer you have now. */
		public int getDosesNeeded()
		{
			return dosesNeeded;
		}

		/** Points the rest of the run is expected to take. */
		public int getNeed()
		{
			return need;
		}

		/** Points on hand: prayer now plus every dose. */
		public int getHave()
		{
			return have;
		}

		/** Doses to spare; negative when short. */
		public double getSpare()
		{
			return spare;
		}

		/**
		 * Sip at or below this many points: a dose drunk any higher fills you past your level
		 * and the overflow is wasted.
		 */
		public int getDrinkBelow()
		{
			return drinkBelow;
		}

		public int getPerDose()
		{
			return perDose;
		}
	}
}
