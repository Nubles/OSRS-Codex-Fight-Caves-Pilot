package com.codex.fightcaves.core;

/**
 * A creature on the cave floor: what it is and the south-west tile of its body. The fall review
 * reads what could reach you from these after a run has ended; nothing is shown during a wave.
 */
public final class Foe
{
	final Monster monster;
	final int x;
	final int y;

	/** @param x the south-west tile of its body */
	public Foe(Monster monster, int x, int y)
	{
		this.monster = monster;
		this.x = x;
		this.y = y;
	}

	public Monster getMonster()
	{
		return monster;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}
}
