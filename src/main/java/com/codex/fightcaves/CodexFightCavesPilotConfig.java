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
		keyName = "rehearse",
		name = "Rehearse the next wave",
		description = "As a wave draws to a close, play the next one out before it spawns: each creature's walk to the tile you will stand on, drawn on the floor and replayed on the panel's map of the cave",
		position = 3
	)
	default boolean rehearse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "outlook",
		name = "Road to Jad",
		description = "Rate every wave of your rotation before it comes: clean when every ranger and mager sticks behind a pillar, pray when one style reaches you, split when two styles reach you from every tile in reach. Saved for the Waves page",
		position = 4
	)
	default boolean outlook()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stuckTags",
		name = "Tag stuck creatures",
		description = "Label rangers and magers that are stuck behind a pillar and cannot hit you where you stand. Not shown on the Jad wave",
		position = 5
	)
	default boolean stuckTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "killOrder",
		name = "Kill order",
		description = "Number the creatures in the order to kill them: whatever hits you hardest for its health first, then anything stuck where you can hit it. Not shown on the Jad wave",
		position = 6
	)
	default boolean killOrder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerCall",
		name = "Prayer to use",
		description = "Name the protection prayer that blocks the most of what can hit you where you stand. Not shown on the Jad wave",
		position = 7
	)
	default boolean prayerCall()
	{
		return true;
	}

	@ConfigItem(
		keyName = "eatWarning",
		name = "Eat warning",
		description = "Say so on the plaque and the panel when what can hit you at once, past your protection prayer, could take all your hitpoints. Not shown on the Jad wave",
		position = 8
	)
	default boolean eatWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a small plaque in the game view with the wave, the rotation and how your prayer is holding up",
		position = 9
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "jadDoses",
		name = "Doses kept for Jad",
		description = "Prayer doses to hold back for the Jad wave when pacing the rest of the run",
		position = 10
	)
	@Range(min = 0, max = 28)
	default int jadDoses()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "chat",
		name = "Chat notes",
		description = "Say in the chat box when the rotation is known, when your prayer falls behind pace, and why a run ended",
		position = 11
	)
	default boolean chat()
	{
		return true;
	}
}
