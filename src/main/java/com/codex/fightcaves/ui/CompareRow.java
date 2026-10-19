package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Compare;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 * One creature on the tile you are on and on the gilt tile, side by side: "hits now" against
 * "stuck". The header row names the two columns.
 */
class CompareRow extends JComponent
{
	static final int COLUMN = 50;

	private final Compare.Row row;

	/** @param row the creature, or null for the header */
	CompareRow(Compare.Row row)
	{
		this.row = row;
		setOpaque(false);
		setPreferredSize(new Dimension(10, row == null ? 13 : 18));
		if (row != null)
		{
			setToolTipText(row.getMonster().getName() + ": " + row.getHere() + " where you are, " + row.getThere() + " on the gilt tile");
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int base = h / 2 + small.getAscent() / 2 - 1;
		int hereRight = w - COLUMN - 6;
		int thereRight = w;
		if (row == null)
		{
			right(g, small, "HERE", hereRight, base, Theme.GOLD_DIM);
			right(g, small, "GILT", thereRight, base, Theme.GOLD_DIM);
			g.dispose();
			return;
		}
		WaveRow.chip(g, row.getMonster(), 7, h / 2.0, 5.5);
		String name = row.getMonster().getNickname();
		Ui.shadowed(g, Ui.clip(name, small, hereRight - COLUMN - 18), 18, base, Theme.TEXT);
		right(g, small, row.getHere(), hereRight, base, row.isHereSafe() ? Theme.TEAL : Theme.EMBER);
		right(g, small, row.getThere(), thereRight, base, row.isThereSafe() ? Theme.TEAL : Theme.EMBER);
		g.dispose();
	}

	private static void right(Graphics2D g, FontMetrics fm, String text, int right, int base, Color color)
	{
		String t = Ui.clip(text, fm, COLUMN);
		Ui.shadowed(g, t, right - fm.stringWidth(t), base, color);
	}
}
