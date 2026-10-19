package com.codex.fightcaves.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A look over what you bring before you step in: the protection prayers your level allows, how
 * far your weapon reaches, the ammunition it needs and the food you carry. Each line is either
 * fine or a warning, said plainly. Prayer doses have their own supply check.
 */
public final class Ready
{
	/** Protection prayer levels: magic, missiles, melee. */
	static final int MAGIC_LEVEL = 37;
	static final int MISSILES_LEVEL = 40;
	static final int MELEE_LEVEL = 43;
	/** Below this much ammunition the quiver is called low. */
	static final int LOW_AMMO = 100;

	/** Foods carried into the cave, by the start of their name. Potions such as brews count as food. */
	private static final String[] FOODS = {
		"shark", "manta ray", "anglerfish", "dark crab", "sea turtle", "tuna potato", "monkfish", "cooked karambwan",
		"saradomin brew", "lobster", "swordfish", "bass", "potato with cheese", "egg potato", "mushroom potato",
		"pineapple pizza", "anchovy pizza", "meat pizza", "1/2 ", "cooked moonlight antelope", "cooked sunlight antelope",
	};

	/** One line of the check. */
	public static final class Line
	{
		private final boolean ok;
		private final String text;

		public Line(boolean ok, String text)
		{
			this.ok = ok;
			this.text = text;
		}

		public boolean isOk()
		{
			return ok;
		}

		public String getText()
		{
			return text;
		}
	}

	private Ready()
	{
	}

	/**
	 * @param prayerLevel your Prayer level, not boosted
	 * @param weapon what you wield, or null for nothing
	 * @param reach how far it attacks from, see {@link WeaponRange}
	 * @param ammo ammunition in the quiver slot
	 * @param food pieces of food carried
	 * @param doses prayer doses carried; when there are some, the supply check says whether they are enough
	 * @return the lines, or none when your levels have not loaded yet (just logged in)
	 */
	public static List<Line> check(int prayerLevel, String weapon, int reach, int ammo, int food, int doses)
	{
		if (prayerLevel <= 0)
		{
			return Collections.emptyList();
		}
		List<Line> lines = new ArrayList<>();
		lines.add(prayer(prayerLevel));
		if (reach <= 1)
		{
			lines.add(new Line(false, "A melee weapon: a ranger or mager stuck behind a pillar cannot be hit, so it waits there until you go to it."));
		}
		else
		{
			lines.add(new Line(true, "Your weapon reaches " + reach + " tiles, so a stuck creature can be hit from where you stand."));
		}
		if (needsAmmo(weapon))
		{
			if (ammo <= 0)
			{
				lines.add(new Line(false, "No ammunition in your quiver."));
			}
			else if (ammo < LOW_AMMO)
			{
				lines.add(new Line(false, "Only " + ammo + " ammunition in your quiver."));
			}
			else
			{
				lines.add(new Line(true, ammo + " ammunition in your quiver."));
			}
		}
		lines.add(food <= 0 ? new Line(false, "No food carried.")
			: new Line(true, food + (food == 1 ? " piece" : " pieces") + " of food carried."));
		if (doses <= 0)
		{
			lines.add(new Line(false, "No prayer potions or restores carried."));
		}
		return lines;
	}

	private static Line prayer(int level)
	{
		if (level >= MELEE_LEVEL)
		{
			return new Line(true, "Prayer " + level + ": all three protection prayers.");
		}
		if (level >= MISSILES_LEVEL)
		{
			return new Line(false, "Prayer " + level + ": no Protect from Melee until " + MELEE_LEVEL + ", so the Yt-MejKot and anything beside you hit through.");
		}
		if (level >= MAGIC_LEVEL)
		{
			return new Line(false, "Prayer " + level + ": no Protect from Missiles until " + MISSILES_LEVEL + ", so every Tok-Xil hits through.");
		}
		return new Line(false, "Prayer " + level + ": no protection prayers until " + MAGIC_LEVEL + ".");
	}

	/** True for a bow or crossbow that fires from the quiver; bows with their own charges do not. */
	public static boolean needsAmmo(String weapon)
	{
		if (weapon == null)
		{
			return false;
		}
		String n = weapon.toLowerCase(Locale.ROOT);
		if (n.contains("crystal bow") || n.contains("faerdhinen") || n.contains("craw's") || n.contains("webweaver")
			|| n.contains("venator") || n.contains("blowpipe") || n.contains("chinchompa"))
		{
			return false;
		}
		// The Seercull is a shortbow without the word, and the atlatl throws darts from the quiver.
		return n.contains("crossbow") || n.contains("seercull") || n.contains("atlatl") || n.contains("ballista") || n.endsWith(" bow") || n.contains("bow (") || n.contains("shortbow")
			|| n.contains("longbow") || n.contains("dark bow") || n.contains("twisted bow") || n.contains("comp bow") || n.contains("composite bow");
	}

	/** True when an item, by name, is food for the cave. */
	public static boolean isFood(String name)
	{
		if (name == null)
		{
			return false;
		}
		String n = name.toLowerCase(Locale.ROOT);
		for (String f : FOODS)
		{
			if (n.startsWith(f))
			{
				return true;
			}
		}
		return false;
	}
}
