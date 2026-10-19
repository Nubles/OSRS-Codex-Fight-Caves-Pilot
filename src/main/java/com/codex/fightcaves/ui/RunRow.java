package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * One past run: how it ended, on which rotation, how long it took and when. On the Records page
 * a run can be clicked to set its splits against your best cape; the chosen one is lit.
 */
class RunRow extends JComponent
{
	private final Logbook.Run run;
	private final boolean selected;
	private boolean hover;

	RunRow(Logbook.Run run)
	{
		this(run, false, null);
	}

	RunRow(Logbook.Run run, boolean selected, Runnable onClick)
	{
		this.run = run;
		this.selected = selected;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 30));
		if (onClick != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(selected ? "These splits are shown above" : "Show this run's splits against your best cape");
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					hover = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = false;
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					if (contains(e.getPoint()))
					{
						onClick.run();
					}
				}
			});
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		boolean cape = run.result == Logbook.Result.CAPE;
		Color color = cape ? Theme.GOLD_BRIGHT : run.result == Logbook.Result.DIED ? Theme.BLOOD : Theme.TEXT_MUTED;
		if (selected || hover)
		{
			g.setColor(Theme.alpha(Theme.GOLD, selected ? 34 : 18));
			g.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
		}

		RoundRectangle2D badge = new RoundRectangle2D.Float(0.5f, 3.5f, 24, h - 7, 6, 6);
		g.setColor(Theme.INK_DEEP);
		g.fill(badge);
		g.setColor(Theme.alpha(color, 160));
		g.draw(badge);
		if (cape)
		{
			Sigil.paint(g, 3, (h - 18) / 2f, 18);
		}
		else
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String n = String.valueOf(run.wave);
			Ui.shadowed(g, n, 13 - fm.stringWidth(n) / 2, h / 2 + fm.getAscent() / 2 - 1, color);
		}

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String what = cape ? "Fire cape" : run.result == Logbook.Result.DIED ? "Fell on wave " + run.wave : "Left on wave " + run.wave;
		Ui.shadowed(g, what, 32, 12, color);
		String when = Format.day(run.endedAt);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(when, w - small.stringWidth(when), 12);
		String detail = (run.rotation > 0 ? "Rotation " + run.rotation : "Rotation unknown") + "  " + Format.ticks(run.ticks)
			+ (run.partial && run.firstWave > 1 ? " from " + run.firstWave : "")
			+ (run.dosesUsed > 0 ? "  " + Format.doses(run.dosesUsed) : "");
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(detail, small, w - 32), 32, h - 5);
		g.dispose();
	}
}
