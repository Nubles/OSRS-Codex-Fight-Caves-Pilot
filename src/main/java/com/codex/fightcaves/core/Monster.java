package com.codex.fightcaves.core;

/**
 * The cave's creatures, with what the pilot needs to plan around them: how big they are,
 * how they hit and from how far, and how much of the fight they are.
 */
public enum Monster
{
	TZ_KIH("Tz-Kih", "Bat", 22, 1, 10, Style.MELEE, 1, 1),
	TZ_KEK("Tz-Kek", "Blob", 45, 2, 20, Style.MELEE, 1, 2),
	TOK_XIL("Tok-Xil", "Ranger", 90, 3, 40, Style.RANGED, 10, 4),
	YT_MEJKOT("Yt-MejKot", "Melee", 180, 4, 80, Style.MELEE, 1, 5),
	KET_ZEK("Ket-Zek", "Mager", 360, 5, 160, Style.MAGIC, 10, 9),
	TZTOK_JAD("TzTok-Jad", "Jad", 702, 5, 250, Style.MIXED, 10, 12);

	/** How a creature fights. */
	public enum Style
	{
		MELEE,
		RANGED,
		MAGIC,
		MIXED
	}

	private final String name;
	private final String nickname;
	private final int level;
	private final int size;
	private final int hitpoints;
	private final Style style;
	private final int range;
	private final int threat;

	Monster(String name, String nickname, int level, int size, int hitpoints, Style style, int range, int threat)
	{
		this.name = name;
		this.nickname = nickname;
		this.level = level;
		this.size = size;
		this.hitpoints = hitpoints;
		this.style = style;
		this.range = range;
		this.threat = threat;
	}

	public String getName()
	{
		return name;
	}

	/** What players call it: Bat, Blob, Ranger, Melee, Mager, Jad. */
	public String getNickname()
	{
		return nickname;
	}

	public int getLevel()
	{
		return level;
	}

	/** Tiles along each side. */
	public int getSize()
	{
		return size;
	}

	public int getHitpoints()
	{
		return hitpoints;
	}

	public Style getStyle()
	{
		return style;
	}

	/** How far it can attack from, in tiles; 1 for melee. */
	public int getRange()
	{
		return range;
	}

	/** How much it matters to keep it off you, used to weigh where to stand. */
	public int getThreat()
	{
		return threat;
	}

	/** Attacks from a distance, so standing out of its sight is what keeps it off you. */
	public boolean isRanged()
	{
		return range > 1;
	}

	/**
	 * The wave creature with this name and combat level, or null. A Tz-Kek's level 22 halves
	 * after it splits are not wave spawns, so they are left out.
	 */
	public static Monster of(String name, int level)
	{
		if (name == null)
		{
			return null;
		}
		for (Monster m : values())
		{
			if (m.name.equalsIgnoreCase(name) && m.level == level)
			{
				return m;
			}
		}
		return null;
	}
}
