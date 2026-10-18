package com.codex.fightcaves.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/** One creature's place in the kill order: a gilt seal with its number, the creature and why. */
class KillRow extends JComponent
{
	private final int rank;
	private final View.Kill kill;

	KillRow(int rank, View.Kill kill)
	{
		this.rank = rank;
		this.kill = kill;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 18));
		setToolTipText(kill.monster.getName() + " (level " + kill.monster.getLevel() + "): " + kill.reason);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int h = getHeight();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int base = h / 2 + small.getAscent() / 2 - 1;

		Color seal = !kill.reachable ? Theme.TEXT_FAINT : rank == 1 ? Theme.GOLD_BRIGHT : Theme.GOLD;
		double r = 6.5;
		Ellipse2D ring = new Ellipse2D.Double(7 - r, h / 2.0 - r, r * 2, r * 2);
		g.setColor(Theme.INK_DEEP);
		g.fill(ring);
		g.setStroke(new BasicStroke(rank == 1 && kill.reachable ? 1.6f : 1f));
		g.setColor(seal);
		g.draw(ring);
		String n = String.valueOf(rank);
		Ui.shadowed(g, n, 7 - small.stringWidth(n) / 2 + 1, base, seal);

		WaveRow.chip(g, kill.monster, 23, h / 2.0, 5.5);
		String name = kill.monster.getNickname();
		Ui.shadowed(g, name, 33, base, kill.reachable ? Theme.TEXT : Theme.TEXT_MUTED);
		int x = 33 + small.stringWidth(name) + 6;
		Ui.shadowed(g, Ui.clip(kill.reason, small, getWidth() - x), x, base, kill.reachable ? Theme.TEXT_MUTED : Theme.TEXT_FAINT);
		g.dispose();
	}
}
