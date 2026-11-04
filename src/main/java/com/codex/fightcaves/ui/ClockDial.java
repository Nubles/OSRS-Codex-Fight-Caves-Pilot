package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.RotationClock;
import com.codex.fightcaves.core.RotationPicks;
import com.codex.fightcaves.core.RotationRecord;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The rotation clock on the panel: a dial of the cave's 16 minute cycle with the turn on now lit
 * in gold and your best rotations marked green, the rotation you would get if you went in now at
 * its heart, and beside it the next few rotations with when each comes on and your own best on
 * it. It runs its own seconds on from the last reading of the game's clock.
 */
class ClockDial extends JComponent
{
	private static final int DIAL = 84;
	private static final int ROWS = 5;
	private static final int ROW = 15;

	private final RotationClock.Reading reading;
	private final long readAt;
	private final RotationPicks picks;
	private final String[] records = new String[16];
	/** A fixed moment to draw, for previews; else the clock runs on from the reading. */
	private final boolean frozen;
	private Timer timer;

	ClockDial(RotationClock.Reading reading, long readAt, RotationPicks picks, List<Logbook.Run> runs, boolean frozen)
	{
		this.reading = reading;
		this.readAt = readAt;
		this.picks = picks;
		this.frozen = frozen;
		for (int r = 1; r <= 15; r++)
		{
			RotationRecord rec = RotationRecord.of(runs, r);
			records[r] = rec.getBestTicks() > 0 ? "best " + Format.ticks(rec.getBestTicks())
				: rec.getRuns() > 0 ? "furthest " + rec.getFurthest() : "";
		}
		setOpaque(false);
		int lines = picks == null ? 0 : (picks.getFastest() > 0 ? 1 : 0) + (picks.getSteadiest() > 0 && picks.getSteadiest() != picks.getFastest() ? 1 : 0);
		setPreferredSize(new Dimension(10, DIAL + 8 + lines * 14 + (lines > 0 ? 6 : 0) + 16 + ROWS * ROW));
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		if (!frozen && timer == null)
		{
			timer = new Timer(500, e -> repaint());
			timer.start();
		}
	}

	@Override
	public void removeNotify()
	{
		if (timer != null)
		{
			timer.stop();
			timer = null;
		}
		super.removeNotify();
	}

	/** The clock now: the reading run on by the time since it was taken. */
	RotationClock.Reading now()
	{
		if (frozen)
		{
			return reading;
		}
		long since = Math.max(0, System.currentTimeMillis() - readAt) / 1000;
		return reading.plus((int) Math.min(since, 600));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		RotationClock.Reading r = now();
		int w = getWidth();
		paintDial(g, r, 0, 0);

		// Beside the dial: the rotation now and its countdown.
		int x = DIAL + 12;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString("IF YOU GO IN NOW", x, 14);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		boolean favoured = picks != null && picks.favours(r.rotation());
		Ui.shadowed(g, "Rotation " + r.rotation(), x, 16 + bold.getAscent(), favoured ? Theme.GREEN : Theme.GOLD_BRIGHT);
		g.setFont(Theme.small());
		boolean changing = ClockText.changing(r);
		g.setColor(changing ? Theme.EMBER : Theme.TEAL);
		int y = 34 + small.getAscent();
		g.drawString(changing ? "changing now: wait" : "for " + ClockText.clock(r.secondsLeft()) + " more", x, y);
		g.setColor(Theme.TEXT_FAINT);
		String record = records[r.rotation()];
		g.drawString(record.isEmpty() ? "no run of yours on it" : "yours: " + record, x, y + 14);
		if (favoured)
		{
			g.setColor(Theme.GREEN);
			g.drawString(r.rotation() == picks.getFastest() ? "your fastest" : "your steadiest", x, y + 28);
		}

		// Your best rotations and when they come up.
		y = DIAL + 8;
		if (picks != null && picks.getFastest() > 0)
		{
			y += 12;
			drawPick(g, r, "Your fastest", picks.getFastest(), 0, y, w);
		}
		if (picks != null && picks.getSteadiest() > 0 && picks.getSteadiest() != picks.getFastest())
		{
			y += 14;
			drawPick(g, r, "Your steadiest", picks.getSteadiest(), 0, y, w);
		}
		if (picks != null && !picks.isEmpty())
		{
			y += 6;
		}

		// The timetable: the next rotations, when each comes on, and your best on it.
		y += 4;
		g.setColor(Theme.STROKE_SOFT);
		g.drawLine(0, y, w, y);
		y += 12;
		g.setColor(Theme.GOLD_DIM);
		g.drawString("NEXT", 0, y - 1);
		List<RotationClock.Slot> slots = RotationClock.upcoming(r.getMinutes(), r.getSeconds(), ROWS + 1);
		for (int i = 1; i < slots.size(); i++)
		{
			RotationClock.Slot s = slots.get(i);
			y += ROW;
			boolean mine = picks != null && picks.favours(s.getRotation());
			g.setFont(Theme.small());
			String n = String.valueOf(s.getRotation());
			Ui.shadowed(g, n, 14 - small.stringWidth(n), y, mine ? Theme.GREEN : Theme.GOLD);
			String rec = records[s.getRotation()];
			if (!rec.isEmpty())
			{
				g.setColor(mine ? Theme.alpha(Theme.GREEN, 200) : Theme.TEXT_FAINT);
				g.drawString(rec, 24, y);
			}
			String when = "in " + ClockText.clock(s.getStartsIn());
			g.setColor(i == 1 ? Theme.TEAL : Theme.TEXT_MUTED);
			g.drawString(when, w - small.stringWidth(when), y);
		}
		g.dispose();
	}

	private void drawPick(Graphics2D g, RotationClock.Reading r, String who, int rotation, int x, int y, int w)
	{
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int wait = RotationClock.secondsUntil(rotation, r.getMinutes(), r.getSeconds());
		diamond(g, x + 3, y - 4, Theme.GREEN);
		String text = who + ", " + rotation;
		g.setColor(Theme.TEXT);
		g.drawString(text, x + 11, y);
		String when = wait == 0 ? "on now" : "in " + ClockText.clock(wait);
		g.setColor(wait == 0 ? Theme.GREEN : Theme.GOLD);
		g.drawString(when, w - fm.stringWidth(when), y);
	}

	private static void diamond(Graphics2D g, int cx, int cy, Color c)
	{
		java.awt.geom.Path2D d = new java.awt.geom.Path2D.Float();
		d.moveTo(cx, cy - 3);
		d.lineTo(cx + 3, cy);
		d.lineTo(cx, cy + 3);
		d.lineTo(cx - 3, cy);
		d.closePath();
		g.setColor(c);
		g.fill(d);
	}

	/** The cycle as a ring of 16 minutes, read clockwise from the top. */
	private void paintDial(Graphics2D g, RotationClock.Reading r, int x, int y)
	{
		double cx = x + DIAL / 2.0;
		double cy = y + DIAL / 2.0;
		double outer = DIAL / 2.0 - 2;
		double ring = 9;
		double mid = outer - ring / 2;

		// The face.
		g.setPaint(new java.awt.RadialGradientPaint(new java.awt.geom.Point2D.Double(cx, cy - 6), (float) outer,
			new float[]{0f, 1f}, new Color[]{Theme.CARD_HOVER, Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Double(cx - outer, cy - outer, outer * 2, outer * 2));

		int now = Math.floorMod(r.getMinutes(), RotationClock.CYCLE);
		int nowRotation = r.rotation();
		g.setStroke(new BasicStroke((float) ring, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		for (int m = 0; m < RotationClock.CYCLE; m++)
		{
			int rot = RotationClock.rotationAt(m);
			boolean on = rot == nowRotation;
			boolean mine = picks != null && picks.favours(rot);
			Color c = on ? Theme.GOLD_BRIGHT : mine ? Theme.alpha(Theme.GREEN, 190) : Theme.alpha(Theme.GOLD_DIM, 150);
			g.setColor(c);
			// Rotation 4 spans minutes 15 and 0: no gap between its two halves.
			boolean joinNext = RotationClock.rotationAt(m + 1) == rot;
			double start = 90 - m * 22.5 - 1;
			double extent = -(22.5 - (joinNext ? 0 : 2));
			g.draw(new Arc2D.Double(cx - mid, cy - mid, mid * 2, mid * 2, start, extent, Arc2D.OPEN));
		}

		// The hand: where in the cycle the clock stands, to the second, from the hub to the rim.
		double turn = (now + r.getSeconds() / 60.0) / RotationClock.CYCLE;
		double angle = Math.toRadians(turn * 360 - 90);
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double tip = outer + 2;
		double tail = outer - ring - 6;
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Color.BLACK, 150));
		g.draw(new Line2D.Double(cx + cos * tail, cy + sin * tail, cx + cos * tip, cy + sin * tip));
		g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.TEXT);
		g.draw(new Line2D.Double(cx + cos * tail, cy + sin * tail, cx + cos * tip, cy + sin * tip));
		g.setColor(Theme.GOLD_BRIGHT);
		g.fill(new Ellipse2D.Double(cx + cos * tail - 2, cy + sin * tail - 2, 4, 4));

		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 140));
		g.draw(new Ellipse2D.Double(cx - outer - 1.5, cy - outer - 1.5, outer * 2 + 3, outer * 2 + 3));
		g.setColor(Theme.alpha(Theme.STROKE, 220));
		double inner = outer - ring - 1;
		g.draw(new Ellipse2D.Double(cx - inner, cy - inner, inner * 2, inner * 2));

		// The rotation now, at the heart.
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		String n = String.valueOf(nowRotation);
		Ui.shadowed(g, n, (int) Math.round(cx - bold.stringWidth(n) / 2.0), (int) Math.round(cy + bold.getAscent() / 2.0 - 4), Theme.GOLD_BRIGHT);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String t = ClockText.clock(r.secondsLeft());
		g.setColor(ClockText.changing(r) ? Theme.EMBER : Theme.TEAL);
		g.drawString(t, (int) Math.round(cx - small.stringWidth(t) / 2.0), (int) Math.round(cy + 13));
	}
}
