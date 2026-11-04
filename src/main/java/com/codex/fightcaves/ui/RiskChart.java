package com.codex.fightcaves.ui;

import com.codex.fightcaves.arena.ArenaStats;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Your own death rate on every wave, wave 1 on the left: each column as tall as the share of the
 * attempts that began that wave and died on it. A wave too few attempts reached has only a faint
 * stub, so a rate built on one or two runs never looks like a wall.
 */
class RiskChart extends JComponent
{
	private final ArenaStats.WaveRisk[] risks;

	RiskChart(ArenaStats.WaveRisk[] risks)
	{
		this.risks = risks;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 84));
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
		float top = 4;
		int n = risks.length - 1;
		float slot = w / (float) n;
		float bw = Math.max(1.5f, slot - (n > 20 ? 1f : 4f));
		for (int wave = 1; wave <= n; wave++)
		{
			ArenaStats.WaveRisk r = risks[wave];
			float x = (wave - 1) * slot + (slot - bw) / 2;
			if (!r.known() || r.deaths == 0)
			{
				g.setColor(r.known() ? Theme.alpha(Theme.GREEN, 90) : Theme.STROKE_SOFT);
				g.fill(new RoundRectangle2D.Float(x, bottom - 2, bw, 2, 1, 1));
				continue;
			}
			float bh = Math.max(3, (bottom - top) * (float) r.rate());
			g.setPaint(new GradientPaint(0, bottom - bh, Theme.EMBER, 0, bottom, Theme.BLOOD_DEEP));
			g.fill(new RoundRectangle2D.Float(x, bottom - bh, bw, bh, n > 20 ? 1 : 3, n > 20 ? 1 : 3));
		}
		g.setColor(Theme.TEXT_FAINT);
		int step = n > 20 ? 10 : 1;
		for (int wave = 1; wave <= n; wave++)
		{
			if (wave != 1 && wave % step != 0 && wave != n)
			{
				continue;
			}
			if (n > 20 && wave != 1 && wave != n && n - wave < 10)
			{
				continue;
			}
			String s = String.valueOf(wave);
			float cx = (wave - 0.5f) * slot;
			int x = Math.round(cx - fm.stringWidth(s) / 2f);
			x = Math.max(0, Math.min(w - fm.stringWidth(s), x));
			g.drawString(s, x, h - fm.getDescent());
		}
		g.dispose();
	}
}
