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

	@ConfigSection(name = "During a run", description = "The plaque and the wave ghost; none of it shows on the Jad wave, and none of it says how to fight", position = 0)
	String WAVE = "wave";

	@ConfigSection(name = "Planning and chat", description = "Prayer pacing and the chat box", position = 1)
	String PLAN = "plan";

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a small plaque in the game view with the wave, the rotation and how your prayer is holding up, never on the Jad wave",
		section = WAVE,
		position = 0
	)
	default boolean plaque()
	{
		return true;
	}

	@ConfigItem(
		keyName = "ghost",
		name = "Wave ghost on the plaque",
		description = "Race your best run: the plaque shows how far ahead or behind its ghost you were at the end of the last wave, never on the Jad wave",
		section = WAVE,
		position = 1
	)
	default boolean ghost()
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
		keyName = "chat",
		name = "Notes in chat",
		description = "Say in the chat box when your rotation is known, when your prayer falls behind pace and why a run ended",
		section = PLAN,
		position = 1
	)
	default boolean chat()
	{
		return true;
	}

	@ConfigSection(name = "Rotation clock", description = "Which rotation you would get if you went in now, by the game's clock", position = 2)
	String CLOCK = "clock";

	@ConfigItem(
		keyName = "rotationClock",
		name = "Rotation clock",
		description = "Near the cave, show which rotation you would get if you went in now and when your best rotations come up; once it has been right on your account, name the rotation from wave 1. The spawns always have the last word",
		section = CLOCK,
		position = 0
	)
	default boolean rotationClock()
	{
		return true;
	}

	@ConfigSection(
		name = "Field notes",
		description = "A local record for checking the plugin's guesses",
		position = 99,
		closedByDefault = true
	)
	String FIELD_NOTES = "fieldNotesSection";

	@ConfigItem(
		keyName = "fieldNotes",
		name = "Keep field notes",
		description = "Write what Fight Caves Pilot reads and decides to its folder in .runelite/plugin-data on your computer, for checking its guesses; nothing is sent anywhere",
		section = FIELD_NOTES,
		position = 0
	)
	default boolean fieldNotes()
	{
		return false;
	}
}
