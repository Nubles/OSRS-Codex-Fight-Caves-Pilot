package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class FallTest
{
	private static List<Threat> threats(Object... pairs)
	{
		List<Threat> list = new ArrayList<>();
		for (int i = 0; i + 1 < pairs.length; i += 2)
		{
			list.add(new Threat((Monster) pairs[i], (Protect) pairs[i + 1]));
		}
		return list;
	}

	private static Fall fall(int ticks, int hp, int prayer, Protect up, List<Threat> threats, int damage)
	{
		Fall f = new Fall();
		for (int i = 0; i < ticks; i++)
		{
			f.add(new Fall.Frame(Math.max(0, hp - i * damage), prayer, up, threats, damage));
		}
		return f;
	}

	@Test
	public void prayerRunningOutComesFirst()
	{
		Fall f = fall(8, 70, 20, Protect.MAGIC, threats(Monster.KET_ZEK, Protect.MAGIC), 6);
		f.add(new Fall.Frame(20, 0, Protect.MAGIC, threats(Monster.KET_ZEK, Protect.MAGIC), 30));
		Fall.Review r = f.review(3);
		assertSame(Fall.Cause.PRAYER_OUT, r.getCause());
		assertEquals("Prayer ran out with 3 doses still in your pack. Sip at the level the prayer gauge gives.", r.getText());
		assertEquals(9, r.getHp().size());
		assertTrue(f.review(0).getText().startsWith("Prayer ran out with no doses left"));
	}

	@Test
	public void twoStylesAtOnceIsNamed()
	{
		Fall.Review r = fall(10, 80, 50, Protect.MAGIC,
			threats(Monster.KET_ZEK, Protect.MAGIC, Monster.TOK_XIL, Protect.MISSILES), 8).review(10);
		assertSame(Fall.Cause.TWO_STYLES, r.getCause());
		assertTrue(r.getText(), r.getText().startsWith("The mager and the ranger reached you at once"));
	}

	@Test
	public void theWrongPrayerAndNoPrayerAreToldApart()
	{
		Fall.Review wrong = fall(10, 70, 40, Protect.MELEE, threats(Monster.KET_ZEK, Protect.MAGIC), 7).review(8);
		assertSame(Fall.Cause.WRONG_PRAYER, wrong.getCause());
		assertEquals("Protect from Melee was up while the mager hit you. Protect from Magic blocks it.", wrong.getText());
		Fall.Review bare = fall(10, 70, 40, null, threats(Monster.TOK_XIL, Protect.MISSILES), 7).review(8);
		assertSame(Fall.Cause.NO_PRAYER, bare.getCause());
		assertEquals("No protection prayer was up against the ranger. Protect from Missiles blocks it.", bare.getText());
	}

	@Test
	public void otherwiseYouWereWornDown()
	{
		// Praying melee against the melee creature, with bats chipping in.
		Fall.Review r = fall(12, 60, 40, Protect.MELEE,
			threats(Monster.YT_MEJKOT, Protect.MELEE, Monster.TZ_KIH, Protect.MELEE), 5).review(8);
		assertSame(Fall.Cause.WORN_DOWN, r.getCause());
		assertEquals("Worn down: 50 damage in the last 6 seconds, mostly from the melee creature and the bat."
			+ " Eating earlier keeps a hit from finishing you.", r.getText());
		assertEquals("only the last ten seconds are kept", Fall.KEEP, fall(40, 60, 40, null, Collections.emptyList(), 1).review(0).getHp().size());
	}

	@Test
	public void nothingKeptIsNoReview()
	{
		Fall f = new Fall();
		assertNull(f.review(4));
		f.add(new Fall.Frame(50, 40, null, Arrays.asList(), 0));
		f.clear();
		assertNull(f.review(4));
	}

	@Test
	public void dangerCountsOnlyWhatPrayerLetsThrough()
	{
		List<Threat> t = threats(Monster.KET_ZEK, Protect.MAGIC, Monster.TOK_XIL, Protect.MISSILES);
		assertEquals(0, Threat.danger(t, Protect.MAGIC, 20));
		assertEquals(13, Threat.danger(t, Protect.MAGIC, 13));
		assertEquals(67, Threat.danger(t, null, 60));
		assertEquals(0, Threat.danger(t, null, 0));
	}
}
