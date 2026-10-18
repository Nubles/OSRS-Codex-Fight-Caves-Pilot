package com.codex.fightcaves.core;

/**
 * The cave's five spawn points, with where each sits in the cave as a fraction of its width
 * and height (0,0 is the south-west corner). The fractions only place a spawn until the pilot
 * has seen a creature appear there; after that it uses the tile it saw.
 */
public enum Spawn
{
	NW("North-west", "NW", 0.13, 0.86),
	C("Centre", "C", 0.50, 0.50),
	SE("South-east", "SE", 0.86, 0.13),
	S("South", "S", 0.50, 0.08),
	SW("South-west", "SW", 0.13, 0.13);

	private final String label;
	private final String code;
	private final double fx;
	private final double fy;

	Spawn(String label, String code, double fx, double fy)
	{
		this.label = label;
		this.code = code;
		this.fx = fx;
		this.fy = fy;
	}

	public String getLabel()
	{
		return label;
	}

	public String getCode()
	{
		return code;
	}

	public double getFx()
	{
		return fx;
	}

	public double getFy()
	{
		return fy;
	}

	public static Spawn ofCode(String code)
	{
		for (Spawn s : values())
		{
			if (s.code.equals(code))
			{
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown spawn " + code);
	}
}
