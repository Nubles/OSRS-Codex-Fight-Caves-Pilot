package com.codex.fightcaves.core;

import java.util.Objects;

/** One creature of a wave and the spawn point it appears at. */
public final class Placement
{
	private final Monster monster;
	private final Spawn spawn;

	public Placement(Monster monster, Spawn spawn)
	{
		this.monster = monster;
		this.spawn = spawn;
	}

	public Monster getMonster()
	{
		return monster;
	}

	public Spawn getSpawn()
	{
		return spawn;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Placement))
		{
			return false;
		}
		Placement p = (Placement) o;
		return monster == p.monster && spawn == p.spawn;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(monster, spawn);
	}

	@Override
	public String toString()
	{
		return monster.getNickname() + "@" + spawn.getCode();
	}
}
