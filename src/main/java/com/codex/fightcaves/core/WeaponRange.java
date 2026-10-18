package com.codex.fightcaves.core;

import java.util.Locale;

/**
 * How far you can attack from, read from the name of what you wield. The pilot uses it to
 * tell a creature stuck where you can still hit it from one that is merely stuck. Ranges are
 * the weapons' standard ones, without the two extra tiles of the long range style, so a tile
 * the pilot calls in reach always is.
 */
public final class WeaponRange
{
	/** What the pilot assumes when it cannot tell: a crossbow or bow. */
	public static final int DEFAULT = 7;

	private WeaponRange()
	{
	}

	/** @param name the wielded item's name, or null with nothing wielded */
	public static int of(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return 1;
		}
		String n = name.toLowerCase(Locale.ROOT);
		if (n.contains("blowpipe"))
		{
			return 5;
		}
		if (n.contains("chinchompa"))
		{
			return 9;
		}
		if (n.endsWith(" dart") || n.contains(" dart(") || n.endsWith("knife") || n.contains("knife(") || n.contains("thrownaxe"))
		{
			return n.contains("dart") ? 3 : 4;
		}
		if (n.contains("ballista"))
		{
			return 9;
		}
		if (n.contains("armadyl crossbow") || n.contains("karil's crossbow"))
		{
			return 8;
		}
		if (n.contains("crossbow"))
		{
			return 7;
		}
		if (n.contains("longbow") || n.contains("dark bow") || n.contains("twisted bow") || n.contains("comp ")
			|| n.contains("composite") || n.contains("faerdhinen") || n.contains("crystal bow") || n.contains("craw's bow")
			|| n.contains("webweaver"))
		{
			return n.contains("craw's") || n.contains("webweaver") ? 9 : 10;
		}
		if (n.contains("shortbow") || n.endsWith(" bow") || n.equals("bow") || n.contains(" bow ("))
		{
			return 7;
		}
		// Powered staves fire their own spell from 7 tiles; other staves and wands autocast from 10.
		if (n.contains("trident") || n.contains("sanguinesti") || n.contains("tumeken") || n.contains("thammaron")
			|| n.contains("accursed sceptre") || n.contains("warped sceptre") || n.contains("eye of ayak"))
		{
			return 7;
		}
		if (n.contains("staff") || n.contains("wand") || n.contains("sceptre") || n.contains("kodai") || n.contains("nightmare staff"))
		{
			return 10;
		}
		if (n.contains("halberd"))
		{
			return 2;
		}
		return 1;
	}
}
