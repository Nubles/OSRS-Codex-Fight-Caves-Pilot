package com.codex.fightcaves.ui;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/** What one creature will do if you stand on the gilt tile: stuck behind a pillar, or on its way. */
class FateRow extends JComponent
{
	private final View.Fate fate;

	FateRow(View.Fate fate)
	{
		this.fate = fate;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 18));
		setToolTipText(fate.monster.getName() + " (level " + fate.monster.getLevel() + "): " + fate.text);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int h = getHeight();
		WaveRow.chip(g, fate.monster, 7, h / 2.0, 5.5);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int base = h / 2 + small.getAscent() / 2 - 1;
		String name = fate.monster.getNickname();
		Ui.shadowed(g, name, 18, base, Theme.TEXT);
		int x = 18 + small.stringWidth(name) + 6;
		Ui.shadowed(g, Ui.clip(fate.text, small, getWidth() - x), x, base, fate.safe ? Theme.TEAL : Theme.EMBER);
		g.dispose();
	}
}
