package com.codex.fightcaves.ui;

import com.codex.fightcaves.arena.ArenaStats;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * A finished attempt against its ghost: the ghost is the gilt middle line, and the attempt's clock
 * against it at the end of each wave is drawn about it, ember above where it was behind and green
 * below where it was ahead. The waves that lost the most time are ringed.
 */
class ArenaGhostChart extends JComponent
{
	private final List<ArenaStats.Split> splits;
	private final List<ArenaStats.Split> marked;
	private final int waves;

	ArenaGhostChart(List<ArenaStats.Split> splits, List<ArenaStats.Split> marked, int waves)
	{
		this.splits = splits;
		this.marked = marked;
		this.waves = waves;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 66));
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
		float bottom = h - fm.getHeight() - 2;
		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, bottom + 3, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(well);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(well);
		float mid = (bottom + 4) / 2f;
		float half = (bottom - 4) / 2f - 3;
		int most = 1;
		for (ArenaStats.Split s : splits)
		{
			if (s.behind != null)
			{
				most = Math.max(most, Math.abs(s.behind));
			}
		}
		g.setColor(Theme.alpha(Theme.GOLD, 170));
		g.setStroke(new BasicStroke(1.2f));
		g.draw(new Line2D.Float(6, mid, w - 6, mid));

		float span = w - 12;
		Path2D line = new Path2D.Float();
		boolean started = false;
		float lastX = 0;
		float lastY = 0;
		for (ArenaStats.Split s : splits)
		{
			if (s.behind == null)
			{
				started = false;
				continue;
			}
			float x = 6 + span * (s.wave - 0.5f) / waves;
			float y = mid - half * s.behind / (float) most;
			if (!started)
			{
				line.moveTo(x, y);
				started = true;
			}
			else
			{
				line.lineTo(x, y);
			}
			lastX = x;
			lastY = y;
		}
		Color end = lastY <= mid ? Theme.EMBER : Theme.GREEN;
		g.setColor(end);
		g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.draw(line);
		for (ArenaStats.Split s : marked)
		{
			if (s.behind == null)
			{
				continue;
			}
			float x = 6 + span * (s.wave - 0.5f) / waves;
			float y = mid - half * s.behind / (float) most;
			g.setColor(Theme.EMBER);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(new Ellipse2D.Float(x - 4, y - 4, 8, 8));
		}
		if (lastX > 0)
		{
			g.setColor(end);
			g.fill(new Ellipse2D.Float(lastX - 2.5f, lastY - 2.5f, 5, 5));
		}
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("wave 1", 2, h - fm.getDescent());
		String r = "wave " + waves;
		g.drawString(r, w - fm.stringWidth(r) - 2, h - fm.getDescent());
		g.dispose();
	}
}
