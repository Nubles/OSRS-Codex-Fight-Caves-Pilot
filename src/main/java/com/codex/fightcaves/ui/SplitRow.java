package com.codex.fightcaves.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 * One line of splits: what it covers on the left, then up to three columns of times, the last
 * one a signed difference in green when ahead and ember when behind. A header row names the
 * columns in gold.
 */
class SplitRow extends JComponent
{
	static final int COLUMN = 42;

	private final String label;
	private final String[] cells;
	private final Integer delta;
	private final boolean hasDelta;
	private final boolean header;
	private final Color labelColor;

	/**
	 * @param cells the plain columns, left to right
	 * @param delta ticks behind (positive) or ahead for the last column, or null for a dash
	 */
	SplitRow(String label, String[] cells, Integer delta, Color labelColor)
	{
		this(label, cells, delta, true, false, labelColor);
	}

	/** A row of plain times with no difference column. */
	static SplitRow plain(String label, Color labelColor, String... cells)
	{
		return new SplitRow(label, cells, null, false, false, labelColor);
	}

	private SplitRow(String label, String[] cells, Integer delta, boolean hasDelta, boolean header, Color labelColor)
	{
		this.label = label;
		this.cells = cells;
		this.delta = delta;
		this.hasDelta = hasDelta;
		this.header = header;
		this.labelColor = labelColor;
		setOpaque(false);
		setPreferredSize(new Dimension(10, header ? 13 : 16));
	}

	/** The row naming the columns, the last of which is the difference. */
	static SplitRow header(String label, String... columns)
	{
		return new SplitRow(label, columns, null, false, true, Theme.GOLD_DIM);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int base = h / 2 + fm.getAscent() / 2 - 1;
		int columns = hasDelta ? cells.length + 1 : cells.length;
		int x0 = w - columns * COLUMN;
		Ui.shadowed(g, Ui.clip(label, fm, x0 - 4), 0, base, labelColor);
		for (int i = 0; i < cells.length; i++)
		{
			String c = cells[i] == null ? "-" : cells[i];
			int right = x0 + (i + 1) * COLUMN;
			Color color = header ? Theme.GOLD_DIM : c.equals("-") ? Theme.TEXT_FAINT : Theme.TEXT_MUTED;
			Ui.shadowed(g, c, right - fm.stringWidth(c), base, color);
		}
		if (hasDelta)
		{
			String d = delta == null ? "-" : delta == 0 ? "even" : Format.delta(delta);
			Color color = delta == null ? Theme.TEXT_FAINT : delta <= 0 ? Theme.GREEN : Theme.EMBER;
			Ui.shadowed(g, d, w - fm.stringWidth(d), base, color);
		}
		g.dispose();
	}
}
