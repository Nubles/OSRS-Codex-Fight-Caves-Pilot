package com.codex.fightcaves.core;

import java.util.List;

/**
 * Your own best rotations, from the runs the logbook keeps: the one you caped fastest, and the
 * one that ends in a cape most often. With the rotation clock they become "your fastest, 9, comes
 * up in 3:12".
 */
public final class RotationPicks
{
	/** Runs on a rotation before its cape rate is worth a word. */
	public static final int STEADY_RUNS = 2;

	private final int fastest;
	private final RotationRecord fastestRecord;
	private final int steadiest;
	private final RotationRecord steadiestRecord;

	private RotationPicks(int fastest, RotationRecord fastestRecord, int steadiest, RotationRecord steadiestRecord)
	{
		this.fastest = fastest;
		this.fastestRecord = fastestRecord;
		this.steadiest = steadiest;
		this.steadiestRecord = steadiestRecord;
	}

	public static RotationPicks of(List<Logbook.Run> runs)
	{
		int fastest = 0;
		RotationRecord fastestRecord = null;
		int steadiest = 0;
		RotationRecord steadiestRecord = null;
		double bestScore = -1;
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			RotationRecord rec = RotationRecord.of(runs == null ? java.util.Collections.emptyList() : runs, r);
			if (rec.getBestTicks() > 0 && (fastestRecord == null || rec.getBestTicks() < fastestRecord.getBestTicks()))
			{
				fastest = r;
				fastestRecord = rec;
			}
			if (rec.getRuns() >= STEADY_RUNS && rec.getCapes() > 0)
			{
				// Capes over runs, pulled toward a half so two from two does not outrank nine from ten.
				double score = (rec.getCapes() + 1.0) / (rec.getRuns() + 2.0);
				if (score > bestScore)
				{
					bestScore = score;
					steadiest = r;
					steadiestRecord = rec;
				}
			}
		}
		return new RotationPicks(fastest, fastestRecord, steadiest, steadiestRecord);
	}

	/** The rotation of your fastest timed cape, or 0. */
	public int getFastest()
	{
		return fastest;
	}

	public RotationRecord getFastestRecord()
	{
		return fastestRecord;
	}

	/** The rotation that ends in a cape most often (two runs or more), or 0. */
	public int getSteadiest()
	{
		return steadiest;
	}

	public RotationRecord getSteadiestRecord()
	{
		return steadiestRecord;
	}

	public boolean isEmpty()
	{
		return fastest == 0 && steadiest == 0;
	}

	/** True when a rotation is one of your picks. */
	public boolean favours(int rotation)
	{
		return rotation > 0 && (rotation == fastest || rotation == steadiest);
	}
}
