package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.PrayerPacer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Prayer on hand against prayer still needed, drawn as a long vial. The vial fills with what you
 * carry; a gilt notch marks what the rest of the run takes at your rate, so a vial that runs
 * past the notch has doses to spare and one that stops short is behind.
 */
class PaceGauge extends JComponent
{
	private final Animator fill = new Animator(this);
	private PrayerPacer.Pace pace;

	PaceGauge()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, 66));
	}

	void show(PrayerPacer.Pace pace)
	{
		this.pace = pace;
		if (pace != null)
		{
			fill.set(pace.getHave(), fill.get() > 0);
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (pace == null)
		{
			return;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		Color color = Theme.of(pace.getState());

		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String doses = Format.doses(pace.getDoses());
		g.setFont(Theme.title());
		Ui.shadowed(g, doses, 0, bold.getAscent(), Theme.TEXT);
		g.setFont(Theme.small());
		String need = "need " + pace.getDosesNeeded();
		Ui.shadowed(g, need, w - small.stringWidth(need), bold.getAscent(), Theme.TEXT_MUTED);

		int vy = bold.getHeight() + 4;
		int vh = 14;
		double scale = Math.max(1, Math.max(pace.getHave(), pace.getNeed()) * 1.08);
		RoundRectangle2D vial = new RoundRectangle2D.Float(0.5f, vy + 0.5f, w - 1, vh, vh, vh);
		g.setColor(Theme.INK_DEEP);
		g.fill(vial);
		double filled = Math.min(1, fill.get() / scale);
		if (filled > 0)
		{
			Graphics2D clip = (Graphics2D) g.create();
			clip.clip(vial);
			clip.setPaint(new GradientPaint(0, vy, Theme.mix(Theme.TEAL, Color.WHITE, 0.25), 0, vy + vh, Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.35)));
			clip.fillRect(0, vy, (int) Math.round((w - 1) * filled), vh + 1);
			// A bright meniscus at the liquid's edge.
			clip.setColor(Theme.alpha(Color.WHITE, 90));
			clip.fillRect((int) Math.round((w - 1) * filled) - 1, vy + 2, 1, vh - 3);
			clip.dispose();
		}
		g.setColor(Theme.STROKE);
		g.draw(vial);
		g.setColor(Theme.alpha(Color.WHITE, 30));
		g.drawLine(vh / 2, vy + 2, w - vh / 2, vy + 2);

		int nx = (int) Math.round((w - 1) * Math.min(1, pace.getNeed() / scale));
		Path2D notch = new Path2D.Float();
		notch.moveTo(nx - 4, vy - 4);
		notch.lineTo(nx + 4, vy - 4);
		notch.lineTo(nx, vy + 1);
		notch.closePath();
		g.setColor(Theme.GOLD_BRIGHT);
		g.fill(notch);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_BRIGHT, 200));
		g.drawLine(nx, vy + 1, nx, vy + vh);

		int ly = vy + vh + 4 + small.getAscent();
		String spare = Format.spare(pace.getSpare()) + " spare";
		Ui.shadowed(g, spare, 0, ly, color);
		String sip = "sip at " + pace.getDrinkBelow() + " or below";
		Ui.shadowed(g, Ui.clip(sip, small, w - small.stringWidth(spare) - 10), w - small.stringWidth(sip), ly, Theme.TEXT_MUTED);
		g.dispose();
	}
}
