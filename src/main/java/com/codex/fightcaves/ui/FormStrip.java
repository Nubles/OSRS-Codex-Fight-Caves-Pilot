package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Form;
import com.codex.fightcaves.core.Logbook;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Your last ten runs as a row of badges, oldest on the left, in the same marks as the recent
 * runs list: a gilt cape sigil for a cape, the wave in blood for a fall, the wave in grey for a
 * run you walked out of. Capes in a row up to the latest share one gilt underline.
 */
class FormStrip extends JComponent
{
	private static final int BADGE = 16;
	private static final int HEIGHT = BADGE + 8;

	private final Form form;

	FormStrip(Form form)
	{
		this.form = form;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int slots = Form.SIZE;
		// Always ten slots wide, so a strip with three runs lines up with one that has ten; empty
		// slots on the left are faint outlines still to be filled.
		float step = (w - BADGE) / (float) (slots - 1);
		int shown = form.getPips().size();
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		for (int s = 0; s < slots; s++)
		{
			float x = s * step;
			RoundRectangle2D badge = new RoundRectangle2D.Float(x + 0.5f, 0.5f, BADGE - 1, BADGE - 1, 5, 5);
			int i = s - (slots - shown);
			if (i < 0)
			{
				g.setColor(Theme.STROKE_SOFT);
				g.draw(badge);
				continue;
			}
			Form.Pip pip = form.getPips().get(i);
			boolean cape = pip.getResult() == Logbook.Result.CAPE;
			Color color = cape ? Theme.GOLD_BRIGHT : pip.getResult() == Logbook.Result.DIED ? Theme.BLOOD : Theme.TEXT_MUTED;
			g.setColor(cape ? Theme.alpha(Theme.GOLD, 40) : Theme.INK_DEEP);
			g.fill(badge);
			g.setColor(Theme.alpha(color, cape ? 200 : 150));
			g.draw(badge);
			if (cape)
			{
				Sigil.paint(g, x + 1, 1, BADGE - 2);
			}
			else
			{
				String n = String.valueOf(pip.getWave());
				Ui.shadowed(g, n, Math.round(x + BADGE / 2f - fm.stringWidth(n) / 2f), BADGE / 2 + fm.getAscent() / 2 - 1, color);
			}
		}
		int streak = Math.min(form.getStreak(), shown);
		if (streak >= 2)
		{
			float from = (slots - streak) * step;
			float to = (slots - 1) * step + BADGE;
			g.setColor(Theme.GOLD);
			g.fill(new RoundRectangle2D.Float(from + 2, HEIGHT - 4, to - from - 4, 2, 2, 2));
		}
		g.dispose();
	}
}
