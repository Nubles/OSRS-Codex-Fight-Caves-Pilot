package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class ReadyTest
{
	@Test
	public void aWellPackedRangerIsAllClear()
	{
		List<Ready.Line> lines = Ready.check(70, "Rune crossbow", 7, 600, 12, 20);
		assertEquals(4, lines.size());
		for (Ready.Line l : lines)
		{
			assertTrue(l.getText(), l.isOk());
		}
		assertEquals("Prayer 70: all three protection prayers.", lines.get(0).getText());
		assertEquals("600 ammunition in your quiver.", lines.get(2).getText());
		assertEquals("12 pieces of food carried.", lines.get(3).getText());
	}

	@Test
	public void eachProtectionPrayerHasItsLevel()
	{
		assertTrue(Ready.check(43, "Toxic blowpipe", 5, 0, 5, 20).get(0).isOk());
		Ready.Line l = Ready.check(42, "Toxic blowpipe", 5, 0, 5, 20).get(0);
		assertFalse(l.isOk());
		assertTrue(l.getText(), l.getText().contains("Protect from Melee until 43"));
		assertTrue(Ready.check(39, null, 1, 0, 0, 20).get(0).getText().contains("Protect from Missiles until 40"));
		assertTrue(Ready.check(20, null, 1, 0, 0, 20).get(0).getText().contains("no protection prayers until 37"));
	}

	@Test
	public void aBlowpipeNeedsNoQuiverButABowDoes()
	{
		// Prayer, weapon and food: the blowpipe holds its own darts.
		assertEquals(3, Ready.check(70, "Toxic blowpipe", 5, 0, 5, 20).size());
		List<Ready.Line> bow = Ready.check(70, "Magic shortbow", 7, 0, 5, 20);
		assertEquals("No ammunition in your quiver.", bow.get(2).getText());
		assertFalse(bow.get(2).isOk());
		assertEquals("Only 40 ammunition in your quiver.", Ready.check(70, "Dragon crossbow", 7, 40, 5, 20).get(2).getText());
		assertTrue(Ready.needsAmmo("Twisted bow"));
		assertFalse(Ready.needsAmmo("Toxic blowpipe"));
		assertFalse(Ready.needsAmmo("Bow of faerdhinen (c)"));
		assertFalse(Ready.needsAmmo("Crystal bow"));
		assertFalse(Ready.needsAmmo("Craw's bow"));
		assertFalse(Ready.needsAmmo("Rune scimitar"));
		assertTrue(Ready.needsAmmo("Armadyl crossbow"));
	}

	@Test
	public void aMeleeWeaponAndNoFoodAreFlagged()
	{
		List<Ready.Line> lines = Ready.check(70, "Abyssal whip", 1, 0, 0, 20);
		assertFalse(lines.get(1).isOk());
		assertTrue(lines.get(1).getText().startsWith("A melee weapon"));
		assertEquals("No food carried.", lines.get(2).getText());
		assertEquals("1 piece of food carried.", Ready.check(70, "Abyssal whip", 1, 0, 1, 20).get(2).getText());
	}

	@Test
	public void noPotionsAtAllIsFlagged()
	{
		List<Ready.Line> lines = Ready.check(70, "Toxic blowpipe", 5, 0, 10, 0);
		assertEquals("No prayer potions or restores carried.", lines.get(lines.size() - 1).getText());
		assertFalse(lines.get(lines.size() - 1).isOk());
	}

	@Test
	public void nothingIsCheckedBeforeYourLevelsLoad()
	{
		assertTrue(Ready.check(0, "Rune crossbow", 7, 0, 0, 20).isEmpty());
	}

	@Test
	public void knowsTheCavesFoods()
	{
		assertTrue(Ready.isFood("Shark"));
		assertTrue(Ready.isFood("Saradomin brew(4)"));
		assertTrue(Ready.isFood("Cooked karambwan"));
		assertTrue(Ready.isFood("1/2 anchovy pizza"));
		assertFalse(Ready.isFood("Prayer potion(4)"));
		assertFalse(Ready.isFood("Raw shark"));
		assertFalse(Ready.isFood(null));
	}
}
