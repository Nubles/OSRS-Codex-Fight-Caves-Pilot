package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Splits;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Where the cave hurts you: one bar for each stretch of seven waves, as tall as the damage you
 * take on an average wave there, with the worst stretch lit. A thin gold tick on each bar marks
 * the chosen run, so one run can be read against your usual.
 */
class HurtChart extends JComponent
{
	private final double[] average;
	private final double[] run;
	private final int worst;

	/**
	 * @param average damage per wave played, by stretch, across your runs
	 * @param run the chosen run's damage per wave played, by stretch, or null
	 * @param worst the stretch to light, or -1
	 */
	HurtChart(double[] average, double[] run, int worst)
	{
		this.average = average;
		this.run = run;
		this.worst = worst;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 74));
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
		double max = 1;
		for (int i = 0; i < average.length; i++)
		{
			max = Math.max(max, average[i]);
			if (run != null)
			{
				max = Math.max(max, run[i]);
			}
		}
		int top = small.getHeight() + 2;
		int chartH = h - small.getHeight() - 4 - top;
		float slot = w / (float) average.length;
		for (int i = 0; i < average.length; i++)
		{
			float x = i * slot + 2;
			float bw = slot - 4;
			int bh = average[i] <= 0 ? 2 : Math.max(4, (int) Math.round(chartH * average[i] / max));
			float base = top + chartH;
			RoundRectangle2D bar = new RoundRectangle2D.Float(x, base - bh, bw, bh, 4, 4);
			boolean lit = i == worst;
			if (average[i] <= 0)
			{
				g.setColor(Theme.STROKE_SOFT);
			}
			else
			{
				Color hi = lit ? Theme.EMBER : Theme.mix(Theme.GOLD_DIM, Theme.EMBER, 0.35);
				g.setPaint(new GradientPaint(0, base - bh, hi, 0, base, Theme.alpha(Theme.BLOOD_DEEP, 220)));
			}
			g.fill(bar);
			if (average[i] > 0)
			{
				String n = String.valueOf(Math.round(average[i]));
				Ui.shadowed(g, n, Math.round(x + (bw - small.stringWidth(n)) / 2), Math.round(base - bh - 2), lit ? Theme.TEXT : Theme.TEXT_MUTED);
			}
			if (run != null && run[i] > 0)
			{
				float y = base - Math.max(2, (float) (chartH * run[i] / max));
				g.setColor(Theme.GOLD_BRIGHT);
				g.setStroke(new java.awt.BasicStroke(2f));
				g.draw(new Line2D.Float(x + 1, y, x + bw - 1, y));
			}
			String label = String.valueOf(i * Splits.STRETCH + 1);
			g.setColor(lit ? Theme.EMBER : Theme.TEXT_FAINT);
			g.drawString(label, Math.round(x + (bw - small.stringWidth(label)) / 2), h - 2);
		}
		g.dispose();
	}
}
