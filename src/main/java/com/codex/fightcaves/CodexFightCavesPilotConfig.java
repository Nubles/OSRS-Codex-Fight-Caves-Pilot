package com.codex.fightcaves;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(CodexFightCavesPilotConfig.GROUP)
public interface CodexFightCavesPilotConfig extends Config
{
	String GROUP = "codexfightcavespilot";

	@ConfigSection(name = "Where to stand", description = "The gilt tile, and the next wave played out before it spawns", position = 0)
	String STAND = "stand";

	@ConfigSection(name = "During a wave", description = "What the pilot marks and says while a wave is on; none of it shows on the Jad wave", position = 1)
	String WAVE = "wave";

	@ConfigSection(name = "Planning and chat", description = "Prayer pacing, the road to Jad and the chat box", position = 2)
	String PLAN = "plan";

	@ConfigItem(
		keyName = "standTile",
		name = "Mark where to stand",
		description = "Gild the tile where the fewest creatures can hit you, with rangers and magers kept behind pillars where it can be done",
		section = STAND,
		position = 0
	)
	default boolean standTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "followFight",
		name = "Follow the fight",
		description = "Keep working out where to stand from where the creatures really are during a wave, not only from where they spawned",
		section = STAND,
		position = 1
	)
	default boolean followFight()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nextSpawns",
		name = "Show the next wave",
		description = "As a wave draws to a close, outline where the next one appears and where to be standing when it does",
		section = STAND,
		position = 2
	)
	default boolean nextSpawns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rehearse",
		name = "Rehearse the next wave",
		description = "As a wave draws to a close, play the next one out on the floor and on the panel's map: each creature's walk to the tile you will stand on",
		section = STAND,
		position = 3
	)
	default boolean rehearse()
	{
		return true;
	}

	@ConfigItem(
		keyName = "stuckTags",
		name = "Tag stuck creatures",
		description = "Label rangers and magers stuck behind a pillar that cannot hit you where you stand, never on the Jad wave",
		section = WAVE,
		position = 0
	)
	default boolean stuckTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "killOrder",
		name = "Kill order",
		description = "Number the creatures in the order to kill them, hardest hitter for its health first, never on the Jad wave",
		section = WAVE,
		position = 1
	)
	default boolean killOrder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerCall",
		name = "Prayer to use",
		description = "Name the protection prayer that blocks most of what can hit you where you stand, never on the Jad wave",
		section = WAVE,
		position = 2
	)
	default boolean prayerCall()
	{
		return true;
	}

	@ConfigItem(
		keyName = "eatWarning",
		name = "Eat warning",
		description = "Warn on the plaque and the panel when what can hit you at once, past your prayer, could take all your hitpoints, never on the Jad wave",
		section = WAVE,
		position = 3
	)
	default boolean eatWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a small plaque in the game view with the wave, the rotation and how your prayer is holding up, never on the Jad wave",
		section = WAVE,
		position = 4
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "jadDoses",
		name = "Doses kept for Jad",
		description = "Prayer doses to hold back for the Jad wave when pacing your prayer through the waves before it",
		section = PLAN,
		position = 0
	)
	@Range(min = 0, max = 28)
	default int jadDoses()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "outlook",
		name = "Road to Jad",
		description = "Rate each wave of your rotation before it comes (clean, pray or split) and keep the ratings for the Waves page",
		section = PLAN,
		position = 1
	)
	default boolean outlook()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chat",
		name = "Notes in chat",
		description = "Say in the chat box when your rotation is known, when your prayer falls behind pace and why a run ended",
		section = PLAN,
		position = 2
	)
	default boolean chat()
	{
		return true;
	}
}
