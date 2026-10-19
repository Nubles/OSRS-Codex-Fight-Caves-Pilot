package com.codex.fightcaves;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexFightCavesPilotPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexFightCavesPilotPlugin.class);
		RuneLite.main(args);
	}
}
