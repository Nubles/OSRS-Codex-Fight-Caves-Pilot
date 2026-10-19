package com.codex.fightcaves.core;

/** The three protection prayers, and which creature attacks each one answers. */
public enum Protect
{
	MELEE("Protect from Melee", "Melee"),
	MISSILES("Protect from Missiles", "Missiles"),
	MAGIC("Protect from Magic", "Magic");

	private final String name;
	private final String shortName;

	Protect(String name, String shortName)
	{
		this.name = name;
		this.shortName = shortName;
	}

	public String getName()
	{
		return name;
	}

	/** The word players use for it: Melee, Missiles or Magic. */
	public String getShortName()
	{
		return shortName;
	}

	/** The prayer that blocks an attack of this style, or null for Jad's mixed attacks. */
	public static Protect against(Monster.Style style)
	{
		switch (style)
		{
			case MELEE:
				return MELEE;
			case RANGED:
				return MISSILES;
			case MAGIC:
				return MAGIC;
			default:
				return null;
		}
	}
}
