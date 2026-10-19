package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Waves;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Where runs end, as a row of embers: one bar for each stretch of seven waves and one for Jad,
 * taller where more runs have fallen, so the wall in your runs shows at a glance.
 */
class DeathChart extends JComponent
{
	private static final int SPAN = 7;

	private final int[] bars;
	private final String[] labels;

	DeathChart(int[] deaths)
	{
		int stretches = (Waves.JAD - 1 + SPAN - 1) / SPAN;
		bars = new int[stretches + 1];
		labels = new String[stretches + 1];
		for (int i = 0; i < stretches; i++)
		{
			int from = i * SPAN + 1;
			int to = Math.min(Waves.JAD - 1, from + SPAN - 1);
			for (int w = from; w <= to; w++)
			{
				bars[i] += deaths[w];
			}
			labels[i] = String.valueOf(from);
		}
		bars[stretches] = deaths[Waves.JAD];
		labels[stretches] = "Jad";
		setOpaque(false);
		setPreferredSize(new Dimension(10, 78));
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
		int max = 1;
		for (int b : bars)
		{
			max = Math.max(max, b);
		}
		int chartH = h - small.getHeight() - 4;
		float slot = w / (float) bars.length;
		for (int i = 0; i < bars.length; i++)
		{
			float x = i * slot + 2;
			float bw = slot - 4;
			// The tallest bar leaves room above it for its count.
			int room = chartH - small.getAscent() - 3;
			int bh = bars[i] == 0 ? 2 : Math.max(4, Math.round(room * bars[i] / (float) max));
			boolean jad = i == bars.length - 1;
			RoundRectangle2D bar = new RoundRectangle2D.Float(x, chartH - bh, bw, bh, 4, 4);
			if (bars[i] == 0)
			{
				g.setColor(Theme.STROKE_SOFT);
			}
			else
			{
				g.setPaint(new GradientPaint(0, chartH - bh, jad ? Theme.BLOOD : Theme.EMBER, 0, chartH, Theme.BLOOD_DEEP));
			}
			g.fill(bar);
			if (bars[i] > 0)
			{
				String n = String.valueOf(bars[i]);
				Ui.shadowed(g, n, Math.round(x + (bw - small.stringWidth(n)) / 2), chartH - bh - 2, Theme.TEXT);
			}
			String label = labels[i];
			g.setColor(jad ? Theme.BLOOD : Theme.TEXT_FAINT);
			g.drawString(label, Math.round(x + (bw - small.stringWidth(label)) / 2), h - 2);
		}
		g.dispose();
	}
}
