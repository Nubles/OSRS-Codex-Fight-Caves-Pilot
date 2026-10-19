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

	@Test
	public void aWaveCallOnTheTickYouArePlacedEntersAtOnce()
	{
		Presence p = new Presence();
		// The call comes between ticks: you enter there and then, and the tick itself adds nothing.
		assertTrue(p.enterNow());
		assertTrue(p.isInside());
		assertEquals(Presence.Change.NONE, p.tick(true, true));
		assertFalse("already in", p.enterNow());
		// It also ends a lone tick outside.
		p.tick(true, false);
		p.enterNow();
		assertEquals(0, p.outsideFor());
		assertEquals(Presence.Change.NONE, p.tick(true, false));
		assertTrue(p.isInside());
	}

	@Test
	public void aCallHeardBeforeYouAreSeenInsideIsHeldAFewTicks()
	{
		EarlyCall c = new EarlyCall();
		c.hear(1, 100);
		assertEquals(1, c.take(101));
		assertEquals("taken once", 0, c.take(101));
		c.hear(5, 100);
		assertEquals(5, c.take(100 + EarlyCall.HOLD_TICKS));
		// A wave called elsewhere (the Inferno) and never followed in is dropped.
		c.hear(12, 100);
		assertEquals(0, c.take(101 + EarlyCall.HOLD_TICKS));
		assertEquals(0, new EarlyCall().take(5));
	}
}
