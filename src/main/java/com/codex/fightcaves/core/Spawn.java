package com.codex.fightcaves.core;

/**
 * The cave's five spawn points. Each is the tile a creature's south-west corner appears on,
 * counted from the south-west corner of the cave's map square (region 9551), so it holds for
 * every copy of the cave.
 */
public enum Spawn
{
	NW("North-west", "NW", 13, 49),
	C("Centre", "C", 32, 32),
	SE("South-east", "SE", 51, 27),
	S("South", "S", 37, 17),
	SW("South-west", "SW", 14, 17);

	private final String label;
	private final String code;
	private final int tileX;
	private final int tileY;

	Spawn(String label, String code, int tileX, int tileY)
	{
		this.label = label;
		this.code = code;
		this.tileX = tileX;
		this.tileY = tileY;
	}

	public String getLabel()
	{
		return label;
	}

	public String getCode()
	{
		return code;
	}

	/** The spawn tile, counted from the west edge of the cave's map square. */
	public int getTileX()
	{
		return tileX;
	}

	/** The spawn tile, counted from the south edge of the cave's map square. */
	public int getTileY()
	{
		return tileY;
	}

	/** Where the spawn sits on the cave chart, as fractions of the floor's width and height. */
	public double getFx()
	{
		return CaveMap.fx(tileX + 1);
	}

	public double getFy()
	{
		return CaveMap.fy(tileY + 1);
	}

	/** True when every code in a saved list names a spawn point, so none would throw on reading. */
	public static boolean areCodes(java.util.List<String> codes)
	{
		if (codes == null)
		{
			return false;
		}
		for (String code : codes)
		{
			boolean known = false;
			for (Spawn s : values())
			{
				known |= s.code.equals(code);
			}
			if (!known)
			{
				return false;
			}
		}
		return true;
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
