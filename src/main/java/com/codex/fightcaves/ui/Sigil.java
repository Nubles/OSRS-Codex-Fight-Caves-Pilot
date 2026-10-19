package com.codex.fightcaves.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The pilot's mark: a fire cape, molten gold at the collar cooling to ember and blood at the
 * hem, veined with obsidian, with a small gilt compass star pinned at its throat.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		Graphics2D g2 = (Graphics2D) g.create();
		Theme.smooth(g2);

		// The cape: narrow at the shoulders, flaring to a scalloped hem.
		Path2D cape = new Path2D.Float();
		cape.moveTo(x + 5.2f * s, y + 2.2f * s);
		cape.lineTo(x + 10.8f * s, y + 2.2f * s);
		cape.curveTo(x + 12.4f * s, y + 6f * s, x + 14.2f * s, y + 10f * s, x + 14.8f * s, y + 14.2f * s);
		cape.quadTo(x + 13.1f * s, y + 13.2f * s, x + 11.4f * s, y + 14.6f * s);
		cape.quadTo(x + 9.7f * s, y + 13.4f * s, x + 8f * s, y + 14.8f * s);
		cape.quadTo(x + 6.3f * s, y + 13.4f * s, x + 4.6f * s, y + 14.6f * s);
		cape.quadTo(x + 2.9f * s, y + 13.2f * s, x + 1.2f * s, y + 14.2f * s);
		cape.curveTo(x + 1.8f * s, y + 10f * s, x + 3.6f * s, y + 6f * s, x + 5.2f * s, y + 2.2f * s);
		cape.closePath();

		g2.setPaint(new GradientPaint(x, y + 2 * s, Theme.GOLD_BRIGHT, x, y + 9 * s, Theme.EMBER));
		g2.fill(cape);
		Shape clip = g2.getClip();
		g2.clip(cape);
		g2.setPaint(new GradientPaint(x, y + 8 * s, Theme.alpha(Theme.EMBER, 0), x, y + 15 * s, Theme.BLOOD));
		g2.fillRect(Math.round(x), Math.round(y), Math.round(size), Math.round(size));

		// Obsidian veins, the cooled crust of the lava.
		g2.setStroke(new BasicStroke(Math.max(0.7f, 0.6f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.setColor(Theme.alpha(Theme.INK_DEEP, 190));
		Path2D veins = new Path2D.Float();
		veins.moveTo(x + 6.4f * s, y + 5f * s);
		veins.lineTo(x + 5.4f * s, y + 8.2f * s);
		veins.lineTo(x + 6.8f * s, y + 10.4f * s);
		veins.lineTo(x + 5.6f * s, y + 13.4f * s);
		veins.moveTo(x + 9.8f * s, y + 5.6f * s);
		veins.lineTo(x + 10.8f * s, y + 8.8f * s);
		veins.lineTo(x + 9.4f * s, y + 11f * s);
		veins.lineTo(x + 10.6f * s, y + 13.6f * s);
		veins.moveTo(x + 6.8f * s, y + 10.4f * s);
		veins.lineTo(x + 9.4f * s, y + 11f * s);
		g2.draw(veins);
		g2.setClip(clip);

		g2.setStroke(new BasicStroke(Math.max(0.8f, 0.7f * s)));
		g2.setColor(Theme.alpha(Theme.INK_DEEP, 230));
		g2.draw(cape);

		// The gilt star at the collar.
		float cx = x + 8f * s;
		float cy = y + 2.6f * s;
		float r = 2.2f * s;
		Path2D star = new Path2D.Float();
		for (int i = 0; i < 8; i++)
		{
			double a = Math.PI / 4 * i - Math.PI / 2;
			float rr = i % 2 == 0 ? r : r * 0.42f;
			float px = cx + (float) Math.cos(a) * rr;
			float py = cy + (float) Math.sin(a) * rr;
			if (i == 0)
			{
				star.moveTo(px, py);
			}
			else
			{
				star.lineTo(px, py);
			}
		}
		star.closePath();
		g2.setColor(Theme.INK_DEEP);
		g2.setStroke(new BasicStroke(Math.max(1.2f, 1.1f * s)));
		g2.draw(star);
		g2.setPaint(new GradientPaint(cx, cy - r, Color.WHITE, cx, cy + r, Theme.GOLD));
		g2.fill(star);
		g2.dispose();
	}
}
