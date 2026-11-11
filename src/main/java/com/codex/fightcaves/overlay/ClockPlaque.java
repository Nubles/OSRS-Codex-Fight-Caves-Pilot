package com.codex.fightcaves.overlay;

import com.codex.fightcaves.core.ClockTrust;
import com.codex.fightcaves.core.RotationClock;
import com.codex.fightcaves.core.RotationPicks;
import com.codex.fightcaves.ui.ClockText;
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
 * The rotation clock at the cave mouth: the rotation you would get if you went in now in the
 * crest's gold, the next one and when it comes on in a lozenge, your best rotation's wait
 * beneath, and a hairline that burns down as the minute runs out. The seconds run on between
 * game ticks from the last reading of the game's clock.
 */
public class ClockPlaque extends Overlay
{
	private static final int PAD = 7;
	private static final int ICON = 16;
	/** How far past the last reading the plaque runs the clock on by itself. */
	private static final int RUN_ON = 3;
	private static final java.awt.image.BufferedImage MARK = Sigil.icon(ICON);
	private static final Color RIM = Theme.alpha(Theme.GOLD, 150);

	private volatile Shown shown;

	/** What the plaque shows, as read on the last tick; the render runs its clock on. */
	static final class Shown
	{
		final RotationClock.Reading reading;
		final long readAt;
		final RotationPicks picks;
		final String doubt;
		/** The rotation now, the lozenge and the line under it at the reading, for tests. */
		final int rotation;
		final String next;
		final String line;

		Shown(RotationClock.Reading reading, long readAt, RotationPicks picks, String doubt)
		{
			this.reading = reading;
			this.readAt = readAt;
			this.picks = picks;
			this.doubt = doubt;
			this.rotation = reading.rotation();
			this.next = ClockText.next(reading);
			this.line = doubt != null ? doubt : ClockText.pick(reading, picks);
		}
	}

	public ClockPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/**
	 * @param reading the game's clock, or null to hide the plaque
	 * @param readAt when it was read, in epoch milliseconds
	 */
	public void show(RotationClock.Reading reading, long readAt, RotationPicks picks, ClockTrust trust)
	{
		shown = reading == null ? null : new Shown(reading, readAt, picks, ClockText.doubt(trust));
	}

	public boolean isShowing()
	{
		return shown != null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		long since = System.currentTimeMillis() - s.readAt;
		int run = (int) Math.max(0, Math.min(RUN_ON, since / 1000));
		// Drawn on a copy: the clip, hints, font and paint the plaque sets never reach the renderer.
		Graphics2D copy = (Graphics2D) g.create();
		try
		{
			return paint(copy, s.reading.plus(run), s.picks, s.doubt);
		}
		finally
		{
			copy.dispose();
		}
	}

	/** Draws the plaque at a moment of the clock; also used for previews. */
	public static Dimension paint(Graphics2D g, RotationClock.Reading r, RotationPicks picks, String doubt)
	{
		Theme.smooth(g);
		String title = "Rotation " + r.rotation();
		String note = "if you go in now";
		boolean changing = ClockText.changing(r);
		String lozenge = changing ? "Changing: " + ClockText.next(r) : "Next " + ClockText.next(r);
		Color tone = changing ? Theme.EMBER : Theme.TEAL;
		String line = doubt != null ? doubt : ClockText.pick(r, picks);
		Color lineColor = doubt != null ? Theme.EMBER : Theme.GOLD;

		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int lozengeW = small.stringWidth(lozenge) + 10;
		int top = PAD + ICON + 6 + bold.stringWidth(title) + 6 + small.stringWidth(note) + 10 + lozengeW + PAD;
		int width = Math.max(top, line == null ? 0 : PAD * 2 + ICON + 6 + small.stringWidth(line));
		int row = Math.max(ICON, bold.getHeight()) + 2;
		int height = PAD + row + (line == null ? 0 : small.getHeight() + 1) + PAD - 1;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 230), 0, height, Theme.alpha(Theme.INK_DEEP, 230)));
		g.fill(shape);

		// The turn burning down: a gilt hairline along the foot, as long as the turn has left.
		int whole = r.secondsLeft() + r.secondsIn();
		double left = whole <= 0 ? 0 : r.secondsLeft() / (double) whole;
		java.awt.Shape clip = g.getClip();
		g.clip(shape);
		g.setColor(Theme.alpha(Theme.STROKE, 200));
		g.fillRect(1, height - 3, width - 2, 2);
		g.setColor(Theme.alpha(changing ? Theme.EMBER : Theme.GOLD, 220));
		g.fillRect(1, height - 3, (int) Math.round((width - 2) * left), 2);
		g.setClip(clip);
		g.setColor(RIM);
		g.draw(shape);

		int x = PAD;
		int rowTop = PAD - 2;
		g.drawImage(MARK, x, rowTop + (row - ICON) / 2, null);
		x += ICON + 6;
		int base = rowTop + (row + bold.getAscent()) / 2 - 1;
		g.setFont(Theme.title());
		boolean favoured = picks != null && picks.favours(r.rotation());
		Ui.shadowed(g, title, x, base, favoured ? Theme.GREEN : Theme.GOLD_BRIGHT);
		x += bold.stringWidth(title) + 6;
		g.setFont(Theme.small());
		Ui.shadowed(g, note, x, base, Theme.TEXT_MUTED);
		x += small.stringWidth(note) + 10;

		int h = small.getHeight() + 2;
		int lt = rowTop + (row - h) / 2;
		g.setColor(Theme.alpha(tone, 40));
		g.fillRoundRect(x, lt, lozengeW, h, 8, 8);
		g.setColor(Theme.alpha(tone, 190));
		g.drawRoundRect(x, lt, lozengeW, h, 8, 8);
		Ui.shadowed(g, lozenge, x + 5, base, tone);

		if (line != null)
		{
			Ui.shadowed(g, line, PAD + ICON + 6, rowTop + row + small.getAscent() + 1, lineColor);
		}
		return new Dimension(width, height);
	}
}
