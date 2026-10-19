package com.codex.fightcaves.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** One line of the ready check: a small lozenge, teal when fine and ember when not, and the line. */
class CheckRow extends JPanel
{
	CheckRow(boolean ok, String text, int width)
	{
		super(new BorderLayout(6, 0));
		setOpaque(false);
		JPanel mark = new JPanel(new BorderLayout());
		mark.setOpaque(false);
		mark.add(new Lozenge(ok ? Theme.TEAL : Theme.EMBER, ok), BorderLayout.NORTH);
		add(mark, BorderLayout.WEST);
		add(Ui.wrapped(text, Theme.small(), ok ? Theme.TEXT_MUTED : Theme.TEXT, width - 16), BorderLayout.CENTER);
		setToolTipText(text);
	}

	/** A diamond, filled when fine and drawn hollow with a bar through it when not. */
	private static final class Lozenge extends JComponent
	{
		private final Color color;
		private final boolean ok;

		Lozenge(Color color, boolean ok)
		{
			this.color = color;
			this.ok = ok;
			setPreferredSize(new Dimension(10, 13));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			float cx = 5f;
			float cy = 7.5f;
			Path2D.Float d = new Path2D.Float();
			d.moveTo(cx, cy - 4);
			d.lineTo(cx + 4, cy);
			d.lineTo(cx, cy + 4);
			d.lineTo(cx - 4, cy);
			d.closePath();
			g.setColor(color);
			if (ok)
			{
				g.fill(d);
			}
			else
			{
				g.draw(d);
				g.fillRect(Math.round(cx) - 1, Math.round(cy) - 1, 2, 2);
			}
			g.dispose();
		}
	}
}
