package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.Spawn;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 * One wave on a line: its number in a gilt box, its creatures as small discs in their
 * prayer's colour, and where each one appears.
 */
class WaveRow extends JComponent
{
	/** How the row stands out. */
	enum Mark
	{
		NONE,
		NOW,
		NEXT,
		CHOSEN
	}

	private final int wave;
	private final List<Placement> placements;
	private final Mark mark;
	private boolean hover;

	WaveRow(int wave, List<Placement> placements, Mark mark, Runnable onClick)
	{
		this.wave = wave;
		this.placements = placements;
		this.mark = mark;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 26));
		setToolTipText("Wave " + wave + ": " + describe(placements));
		if (onClick != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

	static String describe(List<Placement> placements)
	{
		StringBuilder b = new StringBuilder();
		for (Placement p : placements)
		{
			if (b.length() > 0)
			{
				b.append(", ");
			}
			b.append(p.getMonster().getNickname()).append(' ').append(p.getSpawn().getCode());
		}
		return b.toString();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		Color accent = mark == Mark.NOW ? Theme.GOLD : mark == Mark.NEXT ? Theme.TEAL : mark == Mark.CHOSEN ? Theme.GOLD_BRIGHT : null;
		if (accent != null || hover)
		{
			RoundRectangle2D bg = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
			g.setColor(hover && accent == null ? Theme.alpha(Theme.CARD_HOVER, 200) : Theme.alpha(accent, 28));
			g.fill(bg);
			if (accent != null)
			{
				g.setColor(Theme.alpha(accent, 120));
				g.draw(bg);
			}
		}

		// The wave number in a small gilt box.
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		RoundRectangle2D box = new RoundRectangle2D.Float(4.5f, 4.5f, 24, h - 9, 6, 6);
		g.setPaint(new GradientPaint(0, 4, Theme.CARD_TOP, 0, h - 4, Theme.INK_DEEP));
		g.fill(box);
		g.setColor(accent != null ? accent : Theme.GOLD_DIM);
		g.draw(box);
		String n = String.valueOf(wave);
		Ui.shadowed(g, n, 17 - small.stringWidth(n) / 2, h / 2 + small.getAscent() / 2 - 1, accent != null ? Theme.GOLD_BRIGHT : Theme.GOLD);

		// Grouped by spawn point, so even a six-creature wave fits on one line.
		Map<Spawn, List<Monster>> groups = new EnumMap<>(Spawn.class);
		for (Placement p : placements)
		{
			groups.computeIfAbsent(p.getSpawn(), k -> new ArrayList<>()).add(p.getMonster());
		}
		int x = 35;
		int cy = h / 2;
		for (Map.Entry<Spawn, List<Monster>> e : groups.entrySet())
		{
			String code = e.getKey().getCode();
			Ui.shadowed(g, code, x, cy + small.getAscent() / 2 - 1, Theme.TEXT_MUTED);
			x += small.stringWidth(code) + 3;
			for (Monster m : e.getValue())
			{
				chip(g, m, x + 4.5, cy, 4.5);
				x += 10;
			}
			x += 4;
		}
		g.dispose();
	}

	/** A small disc for a creature in its prayer's colour. */
	static void chip(Graphics2D g, Monster m, double x, double y, double r)
	{
		Color color = Theme.of(m);
		Ellipse2D shape = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
		g.setPaint(new GradientPaint((float) x, (float) (y - r), Theme.mix(color, Color.WHITE, 0.3), (float) x, (float) (y + r), Theme.mix(color, Theme.INK_DEEP, 0.3)));
		g.fill(shape);
		g.setColor(Theme.INK_DEEP);
		g.draw(shape);
	}
}
