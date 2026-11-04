package com.codex.fightcaves.arena;

/**
 * The two wave arenas the pilot keeps run logs for, beside the Fight Cave. Each is known by its
 * map region, read from your own tile mapped back out of the instance, as RuneLite's own plugins
 * read them.
 */
public enum Arena
{
	/**
	 * The Inferno. Region 9043 is RuneLite's own (TimersAndBuffsPlugin.INFERNO_REGION_ID and
	 * DiscordGameEventType.MG_INFERNO). "There are 69 waves in total." and "On the 69th and final
	 * wave, TzKal-Zuk will break free from the northern wall." (oldschool.runescape.wiki/w/The_Inferno)
	 */
	INFERNO("The Inferno", "Inferno", 9043, 69),
	/**
	 * The Fortis Colosseum. Region 7216 is RuneLite's own (DiscordGameEventType.MG_FORTIS_COLOSSEUM;
	 * the lobby is 7316, MG_FORTIS_COLOSSEUM_LOBBY). "players can take on up to twelve waves of
	 * opponents, culminating with the final wave against Sol Heredit."
	 * (oldschool.runescape.wiki/w/Fortis_Colosseum)
	 */
	COLOSSEUM("Fortis Colosseum", "Colosseum", 7216, 12);

	private final String title;
	private final String shortName;
	private final int region;
	private final int waves;

	Arena(String title, String shortName, int region, int waves)
	{
		this.title = title;
		this.shortName = shortName;
		this.region = region;
		this.waves = waves;
	}

	public String getTitle()
	{
		return title;
	}

	public String getShortName()
	{
		return shortName;
	}

	public int getRegion()
	{
		return region;
	}

	/** Waves in a whole run, the last one the boss's. */
	public int getWaves()
	{
		return waves;
	}

	/** The arena with this map region, or null. */
	public static Arena ofRegion(int region)
	{
		for (Arena a : values())
		{
			if (a.region == region)
			{
				return a;
			}
		}
		return null;
	}
}
