package com.codex.fightcaves.overlay;

import com.codex.fightcaves.ui.Sigil;
import com.codex.fightcaves.ui.Theme;
import com.codex.fightcaves.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A slim gilt plaque in the game view: the wave and rotation in the gold of the crest, how
 * prayer is holding up in its pace colour, and the protection prayer the fight calls for. It
 * draws what the plugin hands it.
 */
public class PilotPlaque extends Overlay
{
	private static final int PAD = 7;

	private volatile Shown shown;

	private static final class Shown
	{
		final String figure;
		final String note;
		final String prayer;
		final Color prayerColor;
		final String call;
		final Color callColor;

		Shown(String figure, String note, String prayer, Color prayerColor, String call, Color callColor)
		{
			this.figure = figure;
			this.note = note;
			this.prayer = prayer;
			this.prayerColor = prayerColor;
			this.call = call;
			this.callColor = callColor;
		}
	}

	public PilotPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/**
	 * @param figure the big text, such as "Wave 24", or null to hide the plaque
	 * @param note quieter text beside it, such as the rotation
	 * @param prayer the prayer pace, or null
	 * @param call the protection prayer the fight calls for, such as "Pray Magic", or null
	 */
	public void show(String figure, String note, String prayer, Color prayerColor, String call, Color callColor)
	{
		shown = figure == null ? null : new Shown(figure, note, prayer, prayerColor, call, callColor);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int icon = 16;
		int width = PAD + icon + 6 + bold.stringWidth(s.figure) + 8 + small.stringWidth(s.note) + PAD;
		if (s.prayer != null)
		{
			width += 10 + small.stringWidth(s.prayer);
		}
		if (s.call != null)
		{
			width += 10 + small.stringWidth(s.call) + 8;
		}
		int height = Math.max(icon, bold.getHeight()) + PAD;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, height, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);

		int x = PAD;
		Sigil.paint(g, x, (height - icon) / 2f, icon);
		x += icon + 6;
		int base = (height + bold.getAscent()) / 2 - 2;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.figure, x, base, Theme.GOLD_BRIGHT);
		x += bold.stringWidth(s.figure) + 8;
		g.setFont(Theme.small());
		Ui.shadowed(g, s.note, x, base, Theme.TEXT_MUTED);
		if (s.prayer != null)
		{
			x += small.stringWidth(s.note) + 10;
			Ui.shadowed(g, s.prayer, x, base, s.prayerColor);
			x += small.stringWidth(s.prayer);
		}
		if (s.call != null)
		{
			// The prayer to use sits in its own lozenge, in the colour of what it answers.
			x += 10;
			int w = small.stringWidth(s.call) + 8;
			int h = small.getHeight() + 2;
			int top = (height - h) / 2;
			g.setColor(Theme.alpha(s.callColor, 40));
			g.fillRoundRect(x - 4, top, w, h, 8, 8);
			g.setColor(Theme.alpha(s.callColor, 190));
			g.drawRoundRect(x - 4, top, w, h, 8, 8);
			Ui.shadowed(g, s.call, x, base, s.callColor);
		}
		return new Dimension(width, height);
	}
}
