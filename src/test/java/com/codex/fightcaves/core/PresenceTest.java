package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PresenceTest
{
	@Test
	public void aLoneTickOutsideDoesNotEndTheRun()
	{
		Presence p = new Presence();
		assertEquals(Presence.Change.ENTERED, p.tick(true, true));
		assertEquals(Presence.Change.NONE, p.tick(true, false));
		assertTrue(p.isInside());
		assertEquals("back in: not a second entry", Presence.Change.NONE, p.tick(true, true));
		assertEquals(Presence.Change.NONE, p.tick(true, false));
		assertEquals(Presence.Change.LEFT, p.tick(true, false));
		assertFalse(p.isInside());
	}

	@Test
	public void aTickWithoutYourTileSaysNothing()
	{
		Presence p = new Presence();
		// Just logged in mid-run: the first tick has no tile yet, the next is in the cave.
		assertEquals(Presence.Change.NONE, p.tick(false, false));
		assertEquals(0, p.outsideFor());
		assertEquals(Presence.Change.ENTERED, p.tick(true, true));
		assertEquals(Presence.Change.NONE, p.tick(false, false));
		assertTrue(p.isInside());
	}

	@Test
	public void loggedInOutsideSettlesOnlyAfterAFewTicks()
	{
		Presence p = new Presence();
		for (int i = 1; i <= Presence.SETTLE_TICKS; i++)
		{
			assertEquals(Presence.Change.NONE, p.tick(true, false));
			assertEquals(i, p.outsideFor());
		}
		p.reset();
		assertEquals(0, p.outsideFor());
		assertFalse(p.isInside());
	}
}
