package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Ghost;
import com.codex.fightcaves.core.Waves;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;

/**
 * The run against its ghost across the cave's 63 waves: a gilt line for the ghost, and the run's
 * lead or deficit at the end of each wave drawn about it, ember above where the run is behind and
 * green below where it is ahead. Waves the report names are ringed, and the chart is scaled so
 * the biggest gap fills it.
 */
class GhostChart extends JComponent
{
	private final List<Ghost.Split> splits;
	private final List<Ghost.Split> marked;

	GhostChart(List<Ghost.Split> splits, List<Ghost.Split> marked, int height)
	{
		this.splits = splits;
		this.marked = marked == null ? Collections.emptyList() : marked;
		setOpaque(false);
		setPreferredSize(new Dimension(10, height));
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
		int labelH = fm.getHeight();
		float top = 4;
		float bottom = h - labelH - 2;
		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, bottom + 3, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(well);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(well);

		int most = 1;
		for (Ghost.Split s : splits)
		{
			if (s.behind != null)
			{
				most = Math.max(most, Math.abs(s.behind));
			}
		}
		float mid = (top + bottom) / 2;
		float half = (bottom - top) / 2 - 5;
		float left = 6;
		float span = w - 12;

		// The stretches of seven waves, faintly, so a wave can be placed by eye.
		g.setColor(Theme.alpha(Theme.STROKE_SOFT, 160));
		for (int s = 1; s < Waves.LAST / 7; s++)
		{
			float x = left + span * (s * 7) / (Waves.LAST - 1f);
			g.drawLine(Math.round(x), Math.round(top + 2), Math.round(x), Math.round(bottom - 1));
		}

		// The ghost: the line every wave is set against.
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(Theme.alpha(Theme.GOLD, 170));
		g.drawLine(Math.round(left), Math.round(mid), Math.round(left + span), Math.round(mid));

		Path2D line = new Path2D.Float();
		boolean open = false;
		float lastX = 0;
		float lastY = 0;
		for (Ghost.Split s : splits)
		{
			if (s.behind == null)
			{
				open = false;
				continue;
			}
			float x = left + span * (s.wave - 1) / (Waves.LAST - 1f);
			float y = mid - half * s.behind / most;
			if (!open)
			{
				line.moveTo(x, y);
				open = true;
			}
			else
			{
				// Shade between the run and the ghost: ember above (behind), green below (ahead).
				Path2D band = new Path2D.Float();
				band.moveTo(lastX, mid);
				band.lineTo(lastX, lastY);
				band.lineTo(x, y);
				band.lineTo(x, mid);
				band.closePath();
				g.setColor(Theme.alpha((lastY + y) / 2 < mid ? Theme.EMBER : Theme.GREEN, 55));
				g.fill(band);
				line.lineTo(x, y);
			}
			lastX = x;
			lastY = y;
		}
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.TEXT);
		g.draw(line);
		if (open)
		{
			Color tip = lastY < mid ? Theme.EMBER : Theme.GREEN;
			g.setColor(tip);
			g.fillOval(Math.round(lastX) - 3, Math.round(lastY) - 3, 6, 6);
		}

		g.setStroke(new BasicStroke(1.2f));
		for (Ghost.Split s : marked)
		{
			float x = left + span * (s.wave - 1) / (Waves.LAST - 1f);
			float y = s.behind == null ? mid : mid - half * s.behind / most;
			g.setColor(Theme.EMBER);
			g.drawOval(Math.round(x) - 4, Math.round(y) - 4, 8, 8);
		}

		int baseline = h - fm.getDescent() - 1;
		Ui.shadowed(g, "1", 3, baseline, Theme.TEXT_FAINT);
		String end = "Jad";
		Ui.shadowed(g, end, w - 3 - fm.stringWidth(end), baseline, Theme.TEXT_FAINT);
		String spanText = "higher is behind, to " + Format.ticks(most);
		Ui.shadowed(g, Ui.clip(spanText, fm, w - 50), (w - Math.min(w - 50, fm.stringWidth(spanText))) / 2, baseline, Theme.alpha(Theme.TEXT_FAINT, 190));
		g.dispose();
	}
}
