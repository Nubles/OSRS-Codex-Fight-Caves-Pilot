package com.codex.fightcaves.ui;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * Your hitpoints over a fall's last ten seconds: one line from left to right, a tick a step,
 * filled beneath in blood that deepens toward the end, with the first and last figures named.
 */
class FallTrail extends JComponent
{
	private final List<Integer> hp;

	FallTrail(List<Integer> hp)
	{
		this.hp = hp;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 52));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (hp == null || hp.size() < 2)
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int w = getWidth();
		int h = getHeight();
		int max = 1;
		for (int v : hp)
		{
			max = Math.max(max, v);
		}
		String first = String.valueOf(hp.get(0));
		int left = small.stringWidth(first) + 6;
		int right = w - 4;
		int top = 4;
		int bottom = h - small.getHeight() - 3;
		float step = (right - left) / (float) (hp.size() - 1);

		g.setColor(Theme.STROKE_SOFT);
		g.drawLine(left, bottom, right, bottom);

		Path2D line = new Path2D.Float();
		Path2D area = new Path2D.Float();
		area.moveTo(left, bottom);
		for (int i = 0; i < hp.size(); i++)
		{
			float x = left + i * step;
			float y = bottom - (bottom - top) * Math.max(0, hp.get(i)) / (float) max;
			if (i == 0)
			{
				line.moveTo(x, y);
			}
			else
			{
				line.lineTo(x, y);
			}
			area.lineTo(x, y);
		}
		area.lineTo(right, bottom);
		area.closePath();
		g.setPaint(new GradientPaint(left, 0, Theme.alpha(Theme.EMBER, 40), right, 0, Theme.alpha(Theme.BLOOD, 120)));
		g.fill(area);
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(left, 0, Theme.EMBER, right, 0, Theme.BLOOD));
		g.draw(line);

		Ui.shadowed(g, first, 0, top + small.getAscent(), Theme.TEXT_MUTED);
		String caption = "HITPOINTS, LAST " + Math.round(hp.size() * 0.6) + "S";
		Ui.shadowed(g, caption, left, h - 2, Theme.TEXT_FAINT);
		g.dispose();
	}
}
