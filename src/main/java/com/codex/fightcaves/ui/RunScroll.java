package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.RunStory;
import com.codex.fightcaves.core.Waves;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A run kept as a scroll: the cape (or the wave it fell on) in gilt, the run's time in large
 * figures, and the whole run as a skyline of 63 waves, each as tall as the wave took and lit by
 * how it went against every run before it: bright gold where it was your fastest yet, teal
 * where it came close, ember along the foot for the damage it cost. A few lines under it say
 * what made the run worth keeping. It is drawn after a run, never during one.
 */
class RunScroll extends JPanel
{
	private static final int FACE_H = 188;
	private static final int SKY_H = 34;
	private static final int HURT_H = 6;

	private final RunStory story;

	RunScroll(RunStory story)
	{
		super(new BorderLayout(0, 4));
		this.story = story;
		setOpaque(false);
		setBorder(new EmptyBorder(0, 12, 12, 12));
		add(new Face(), BorderLayout.NORTH);
		StackPanel lines = new StackPanel(3, 0);
		for (String line : lines(story))
		{
			JComponent label = Ui.wrapped(line, Theme.small(), line.startsWith("A new best") || line.startsWith("Your first")
				? Theme.GOLD : Theme.TEXT_MUTED, PilotPanel.WIDTH - 30);
			lines.add(label);
		}
		add(lines, BorderLayout.CENTER);
	}

	/** "8th fire cape", "First fire cape", "Fell on wave 55", "Left on wave 31". */
	static String title(RunStory s)
	{
		Logbook.Run run = s.getRun();
		if (run.result == Logbook.Result.CAPE)
		{
			if (s.getCapeNumber() < 0)
			{
				return "Fire cape";
			}
			return s.getCapeNumber() <= 1 ? "First fire cape" : ordinal(s.getCapeNumber()) + " fire cape";
		}
		return (run.result == Logbook.Result.DIED ? "Fell on wave " : "Left on wave ") + run.wave;
	}

	static String ordinal(int n)
	{
		int tens = n % 100;
		String suffix = tens >= 11 && tens <= 13 ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
		return Format.count(n) + suffix;
	}

	/** What makes the run worth keeping, a short line each, most telling first. */
	static List<String> lines(RunStory s)
	{
		List<String> out = new ArrayList<>();
		Logbook.Run run = s.getRun();
		if (run.result == Logbook.Result.CAPE)
		{
			if (run.partial)
			{
				out.add("Picked up part way, so its time is not a record.");
			}
			else if (s.getVersusOldBest() != null && s.getVersusOldBest() < 0)
			{
				out.add("A new best cape, " + Format.ticks(-s.getVersusOldBest()) + " under the old one.");
			}
			else if (s.getVersusOldBest() != null && s.getVersusOldBest() == 0)
			{
				out.add("A new best cape, level with the old one to the tick.");
			}
			else if (s.getVersusOldBest() == null && s.getCapeNumber() == 1)
			{
				out.add("Your first fire cape. Every cape after races this one.");
			}
			else if (s.getVersusLastCape() != null)
			{
				int d = s.getVersusLastCape();
				int off = s.getVersusOldBest();
				out.add(d == off ? Format.ticks(off) + " off your best cape, the one just before it."
					: Format.ticks(Math.abs(d)) + (d <= 0 ? " faster" : " slower") + " than the cape before it, " + Format.ticks(off) + " off your best.");
			}
		}
		else if (s.isFurthestYet())
		{
			out.add("Your furthest yet.");
		}
		int best = s.getBestWaves();
		if (best > 0)
		{
			out.add((best == 1 ? "1 wave" : best + " waves") + (s.isPartialRecord() ? " at your fastest on record" : " at your fastest yet") + ", in bright gold.");
		}
		int worst = 0;
		for (int w = 1; w <= Waves.LAST; w++)
		{
			if (s.hurt(w) > 0 && (worst == 0 || s.hurt(w) > s.hurt(worst)))
			{
				worst = w;
			}
		}
		if (worst > 0)
		{
			out.add("Hurt most on wave " + worst + ", which took " + s.hurt(worst) + ".");
		}
		return out;
	}

	/** A wave's colour on the skyline. */
	static Color tint(RunStory.Tier tier)
	{
		switch (tier)
		{
			case BEST:
				return Theme.GOLD_BRIGHT;
			case NEAR:
				return Theme.TEAL;
			case PLAIN:
			case FIRST:
				return Theme.mix(Theme.STROKE, Theme.TEXT_FAINT, 0.55);
			case ENDED:
				return Theme.BLOOD;
			default:
				return Theme.STROKE;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		boolean cape = story.getRun().result == Logbook.Result.CAPE;
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, h, Theme.INK_DEEP));
		g.fill(shape);
		// The cave's glow from below, warmer for a cape.
		g.setClip(shape);
		g.setPaint(new RadialGradientPaint(w / 2f, 64, w * 0.62f, new float[]{0, 1},
			new Color[]{Theme.alpha(cape ? Theme.EMBER : Theme.BLOOD_DEEP, cape ? 46 : 40), Theme.alpha(Theme.INK_DEEP, 0)}));
		g.fillRect(0, 0, w, h);
		g.setClip(null);

		// A double gilt rule with a diamond at each corner, like the edge of an illuminated page.
		Color rim = cape ? Theme.GOLD : Theme.GOLD_DIM;
		g.setColor(Theme.alpha(rim, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(rim, 60));
		g.draw(new RoundRectangle2D.Float(3.5f, 3.5f, w - 8, h - 8, Theme.RADIUS - 3, Theme.RADIUS - 3));
		for (float[] c : new float[][]{{6, 6}, {w - 7, 6}, {6, h - 7}, {w - 7, h - 7}})
		{
			Path2D d = new Path2D.Float();
			d.moveTo(c[0], c[1] - 3);
			d.lineTo(c[0] + 3, c[1]);
			d.lineTo(c[0], c[1] + 3);
			d.lineTo(c[0] - 3, c[1]);
			d.closePath();
			g.setColor(rim);
			g.fill(d);
		}
		g.dispose();
	}

	/** The scroll's face: the emblem, the title, the time and the skyline. */
	private final class Face extends JComponent
	{
		private int hover;

		Face()
		{
			setOpaque(false);
			setPreferredSize(new Dimension(10, FACE_H));
			MouseAdapter mouse = new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
					int wave = waveAt(e.getX(), e.getY());
					if (wave != hover)
					{
						hover = wave;
						setToolTipText(wave > 0 ? tip(wave) : null);
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					if (hover != 0)
					{
						hover = 0;
						repaint();
					}
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		private float slot()
		{
			return getWidth() / (float) Waves.LAST;
		}

		private int skyTop()
		{
			return 124;
		}

		private int waveAt(int x, int y)
		{
			if (y < skyTop() - 4 || y > skyTop() + SKY_H + 8)
			{
				return 0;
			}
			int wave = (int) (x / slot()) + 1;
			return wave >= 1 && wave <= Waves.LAST ? wave : 0;
		}

		private String tip(int wave)
		{
			RunStory.Tier tier = story.tier(wave);
			String name = wave == Waves.JAD ? "Jad" : "Wave " + wave;
			switch (tier)
			{
				case UNPLAYED:
					return name + ": not reached";
				case ENDED:
					return name + ": the run ended here" + (story.hurt(wave) > 0 ? ", " + story.hurt(wave) + " damage taken" : "");
				case UNTIMED:
					return name + ": not timed";
				default:
					String t = name + ": " + Format.ticks(story.ticks(wave));
					if (story.best(wave) > 0)
					{
						t += tier == RunStory.Tier.BEST ? (story.isPartialRecord() ? ", your fastest on record" : ", your fastest yet") : ", best before " + Format.ticks(story.best(wave));
					}
					return t + (story.hurt(wave) > 0 ? ", " + story.hurt(wave) + " damage" : "");
			}
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			Logbook.Run run = story.getRun();
			boolean cape = run.result == Logbook.Result.CAPE;
			g.setFont(Theme.small());
			FontMetrics small = g.getFontMetrics();

			String heading = "TZHAAR FIGHT CAVE";
			g.setColor(Theme.GOLD_DIM);
			g.drawString(heading, (w - small.stringWidth(heading)) / 2, 18);

			// The emblem: the cape for a cape, otherwise the wave it ended on in a dark seal.
			int es = 30;
			float ex = (w - es) / 2f;
			int ey = 24;
			if (cape)
			{
				g.setPaint(new RadialGradientPaint(w / 2f, ey + es * 0.7f, es * 0.8f, new float[]{0, 1},
					new Color[]{Theme.alpha(Theme.EMBER, 90), Theme.alpha(Theme.EMBER, 0)}));
				g.fill(new Rectangle2D.Float(ex - 10, ey - 6, es + 20, es + 12));
				Sigil.paint(g, ex, ey, es);
			}
			else
			{
				RoundRectangle2D seal = new RoundRectangle2D.Float(ex, ey + 2, es, es - 4, 8, 8);
				g.setColor(Theme.INK_DEEP);
				g.fill(seal);
				g.setColor(Theme.alpha(run.result == Logbook.Result.DIED ? Theme.BLOOD : Theme.TEXT_MUTED, 170));
				g.draw(seal);
				g.setFont(Theme.title());
				FontMetrics bold = g.getFontMetrics();
				String n = String.valueOf(run.wave);
				Ui.shadowed(g, n, Math.round(ex + (es - bold.stringWidth(n)) / 2f), ey + 2 + (es - 4 + bold.getAscent()) / 2 - 1,
					run.result == Logbook.Result.DIED ? Theme.BLOOD : Theme.TEXT_MUTED);
			}

			g.setFont(Theme.title());
			FontMetrics bold = g.getFontMetrics();
			String title = Ui.clip(title(story), bold, w);
			Ui.shadowed(g, title, (w - bold.stringWidth(title)) / 2, 72,
				cape ? Theme.GOLD_BRIGHT : run.result == Logbook.Result.DIED ? Theme.mix(Theme.TEXT, Theme.BLOOD, 0.45) : Theme.TEXT);

			// The time in large figures, measured first so it sits centred.
			String time = Format.ticks(run.ticks);
			int timeW = (bold.stringWidth(time) + 1) * 2;
			Crest.paintLarge(g, time, (w - timeW) / 2, 78, cape && !run.partial ? Theme.GOLD : Theme.TEXT_MUTED);

			g.setFont(Theme.small());
			String sub = (run.rotation > 0 ? "Rotation " + run.rotation : "Rotation unknown")
				+ (run.dosesUsed > 0 ? "  ·  " + Format.doses(run.dosesUsed) : "")
				+ (run.endedAt > 0 ? "  ·  " + Format.day(run.endedAt) : "");
			sub = Ui.clip(sub, small, w);
			g.setColor(Theme.TEXT_MUTED);
			g.drawString(sub, (w - small.stringWidth(sub)) / 2, 114);

			paintSkyline(g, w, small);
			g.dispose();
		}

		private void paintSkyline(Graphics2D g, int w, FontMetrics small)
		{
			int top = skyTop();
			int base = top + SKY_H;
			float slot = slot();
			float bw = Math.max(1f, slot - 1f);
			int longest = story.longestWave();
			int worst = story.worstHurt();
			// A faint floor line so unplayed waves still read as part of the cave.
			g.setColor(Theme.alpha(Theme.STROKE, 120));
			g.drawLine(0, base, w, base);
			for (int wave = 1; wave <= Waves.LAST; wave++)
			{
				RunStory.Tier tier = story.tier(wave);
				float x = (wave - 1) * slot;
				int bh;
				switch (tier)
				{
					case UNPLAYED:
						bh = 0;
						break;
					case UNTIMED:
						bh = 3;
						break;
					case ENDED:
						bh = SKY_H;
						break;
					default:
						bh = Math.max(3, Math.round(SKY_H * story.ticks(wave) / (float) longest));
				}
				if (bh > 0)
				{
					Color c = tint(tier);
					if (wave == Waves.JAD && tier != RunStory.Tier.BEST && tier != RunStory.Tier.ENDED)
					{
						c = Theme.mix(c, Theme.BLOOD, 0.5);
					}
					boolean lit = wave == hover;
					g.setPaint(new GradientPaint(0, base - bh, lit ? Theme.TEXT : c, 0, base, Theme.alpha(c, tier == RunStory.Tier.BEST ? 150 : 90)));
					g.fill(new Rectangle2D.Float(x, base - bh, bw, bh));
				}
				if (worst > 0 && story.hurt(wave) > 0)
				{
					// The damage the wave cost, hanging below the floor in ember: deeper where it hurt more.
					float share = story.hurt(wave) / (float) worst;
					int dh = Math.max(1, Math.round(HURT_H * share));
					g.setColor(Theme.alpha(Theme.EMBER, 90 + Math.round(165 * share)));
					g.fill(new Rectangle2D.Float(x, base + 2, bw, dh));
				}
			}
			// Stretch marks every seven waves, as the splits are read.
			g.setColor(Theme.alpha(Theme.GOLD_DIM, 90));
			for (int s = 7; s < Waves.LAST; s += 7)
			{
				int x = Math.round(s * slot) - 1;
				g.drawLine(x, base + HURT_H + 4, x, base + HURT_H + 6);
			}
			g.setColor(Theme.TEXT_FAINT);
			g.drawString("Wave 1", 0, base + HURT_H + 17);
			String jad = "Jad";
			g.setColor(story.tier(Waves.JAD) == RunStory.Tier.UNPLAYED ? Theme.TEXT_FAINT : Theme.mix(Theme.TEXT_FAINT, Theme.BLOOD, 0.6));
			g.drawString(jad, w - small.stringWidth(jad), base + HURT_H + 17);
			g.setStroke(new BasicStroke(1f));
		}
	}
}
