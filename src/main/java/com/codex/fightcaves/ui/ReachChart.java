package com.codex.fightcaves.ui;

import com.codex.fightcaves.arena.ArenaLog;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * How far each attempt got, oldest on the left: one column per attempt, as tall as the waves it
 * reached. Gold where it was finished, ember where it died, teal where the chest was taken and
 * faint where it was left. A hairline marks your ghost's reach.
 */
class ReachChart extends JComponent
{
	static final int SHOWN = 30;

	private final List<ArenaLog.Run> runs;
	private final int waves;
	private final int ghostReach;

	ReachChart(List<ArenaLog.Run> runs, int waves, int ghostReach)
	{
		this.runs = runs.subList(Math.max(0, runs.size() - SHOWN), runs.size());
		this.waves = waves;
		this.ghostReach = ghostReach;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 84));
	}

	static Color color(ArenaLog.Result r)
	{
		switch (r)
		{
			case COMPLETE:
				return Theme.GOLD_BRIGHT;
			case DIED:
				return Theme.EMBER;
			case CLAIMED:
				return Theme.TEAL;
			default:
				return Theme.TEXT_FAINT;
		}
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
		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, bottom + 3, 8, 8);
		g.setColor(Theme.INK_DEEP);
		g.fill(well);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(well);

		int n = Math.max(runs.size(), 10);
		float slot = (w - 8) / (float) n;
		float bw = Math.max(2f, slot - 2f);
		for (int i = 0; i < runs.size(); i++)
		{
			ArenaLog.Run r = runs.get(i);
			float x = 4 + i * slot + (slot - bw) / 2;
			float bh = Math.max(3, (bottom - top) * Math.min(1, r.reached / (float) waves));
			Color c = color(r.result);
			g.setPaint(new GradientPaint(0, bottom - bh, c, 0, bottom, Theme.mix(c, Theme.INK_DEEP, 0.65)));
			g.fill(new RoundRectangle2D.Float(x, bottom - bh, bw, bh, 3, 3));
		}
		if (ghostReach > 0)
		{
			float y = bottom - (bottom - top) * Math.min(1, ghostReach / (float) waves);
			g.setColor(Theme.alpha(Theme.GOLD, 150));
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
			g.draw(new Line2D.Float(4, y, w - 4, y));
		}
		g.setColor(Theme.TEXT_FAINT);
		String left = runs.size() >= SHOWN ? "last " + SHOWN : "first";
		g.drawString(left, 2, h - fm.getDescent());
		String right = "wave " + waves + " at the top";
		g.drawString(right, w - fm.stringWidth(right) - 2, h - fm.getDescent());
		g.dispose();
	}
}
