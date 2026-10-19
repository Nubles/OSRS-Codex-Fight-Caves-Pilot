package com.codex.fightcaves.overlay;

import com.codex.fightcaves.core.Monster;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.api.NPC;

/** What the cave overlay draws this frame, handed over whole by the plugin. */
public final class Marks
{
	public static final Marks NONE = new Marks(null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

	/** The tile to stand on, in scene coordinates. */
	public static final class Stand
	{
		final int x;
		final int y;
		final boolean next;
		final boolean safe;

		/** @param next true when this is where to be for the wave about to spawn */
		public Stand(int x, int y, boolean next, boolean safe)
		{
			this.x = x;
			this.y = y;
			this.next = next;
			this.safe = safe;
		}
	}

	/** Where a creature of the next wave will appear, by its south-west scene tile. */
	public static final class Ghost
	{
		final Monster monster;
		final int x;
		final int y;

		public Ghost(Monster monster, int x, int y)
		{
			this.monster = monster;
			this.x = x;
			this.y = y;
		}
	}

	/** The next wave's rehearsal, in scene tiles, drawn as each creature's walk on the floor, or null. */
	final com.codex.fightcaves.core.Rehearsal rehearsal;
	final Stand stand;
	final List<Ghost> ghosts;
	final List<NPC> stuck;
	/** Where each creature comes in the kill order, from 1. */
	final Map<NPC, Integer> ranks;

	public Marks(Stand stand, List<Ghost> ghosts, List<NPC> stuck, Map<NPC, Integer> ranks)
	{
		this(stand, ghosts, stuck, ranks, null);
	}

	public Marks(Stand stand, List<Ghost> ghosts, List<NPC> stuck, Map<NPC, Integer> ranks, com.codex.fightcaves.core.Rehearsal rehearsal)
	{
		this.rehearsal = rehearsal;
		this.stand = stand;
		this.ghosts = ghosts;
		this.stuck = stuck;
		this.ranks = ranks;
	}
}
