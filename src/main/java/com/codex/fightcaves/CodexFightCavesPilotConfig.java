package com.codex.fightcaves;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CodexFightCavesPilotConfig.GROUP)
public interface CodexFightCavesPilotConfig extends Config
{
	String GROUP = "codexfightcavespilot";

	@ConfigItem(
		keyName = "standTile",
		name = "Mark where to stand",
		description = "Gild the tile where the fewest creatures can hit you, keeping rangers and magers stuck behind pillars where possible",
		position = 0
	)
	default boolean standTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "followFight",
		name = "Follow the fight",
		description = "Keep re-working where to stand from where the creatures really are during a wave, not only from where they spawn",
		position = 1
	)
	default boolean followFight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nextSpawns",
		name = "Show the next wave",
		description = "Outline where the next wave's creatures will appear, and where to be standing when they do, as a wave draws to a close",
		position = 2
	)
	default boolean nextSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stuckTags",
		name = "Tag stuck creatures",
		description = "Label rangers and magers that are stuck behind a pillar and cannot hit you where you stand",
		position = 3
	)
	default boolean stuckTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a small plaque in the game view with the wave, the rotation and how your prayer is holding up",
		position = 4
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "jadDoses",
		name = "Doses kept for Jad",
		description = "Prayer doses to hold back for the Jad wave when pacing the rest of the run",
		position = 5
	)
	@Range(min = 0, max = 28)
	default int jadDoses()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "chat",
		name = "Chat notes",
		description = "Say in the chat box when the rotation is known and when your prayer falls behind pace",
		position = 6
	)
	default boolean chat()
	{
		return true;
	}
}
