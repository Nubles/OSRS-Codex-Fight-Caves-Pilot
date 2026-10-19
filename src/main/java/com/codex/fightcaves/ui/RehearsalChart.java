package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Chase;
import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.core.Rehearsal;
import com.codex.fightcaves.core.Spawn;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The next wave played out on a true map of the cave floor, tile by tile, before it spawns.
 * The floor and its pillars come from the game's own collision map. Each creature leaves its
 * spawn point and walks at the tile you mean to stand on the way the game moves it, at the
 * game's pace, so you see who sticks behind which pillar and who comes round to you. It loops,
 * with a pause on how the wave ends.
 */
public class RehearsalChart extends JComponent
{
	/** The walkable floor's bounds within the cave's map square, inclusive. */
	private static final int MIN_X = 3;
	private static final int MAX_X = 56;
	private static final int MIN_Y = 6;
	private static final int MAX_Y = 61;
	private static final int FRAME_MS = 40;
	private static final double TICK_MS = 600;
	/** How long the end of the wave is held before it plays again. */
	private static final double HOLD_MS = 2200;
	/** A short beat before the creatures move, while they appear. */
	private static final double LEAD_MS = 500;

	private final Timer timer;
	private boolean[][] floor;
	private Rehearsal rehearsal;
	private int[] you;
	private String caption = "";
	private double clock;
	private double frozen = -1;
	/**
	 * The stone floor, pillars and spawn rings drawn once and kept: they only change with the
	 * floor or the size, and redrawing three thousand tiles 25 times a second is wasted work.
	 */
	private BufferedImage backdrop;
	private boolean[][] backdropFloor;
	private int backdropW;
	private int backdropH;
	private double backdropScale;

	public RehearsalChart(int height)
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, height));
		setMinimumSize(new Dimension(10, height));
		timer = new Timer(FRAME_MS, e -> step());
	}

	/**
	 * @param floor walkable tiles of the cave's map square, [x][y], or null
	 * @param rehearsal the wave's walks, in tiles of the cave's map square
	 * @param you where you stand now, in the same tiles, or null
	 */
	public void show(String caption, boolean[][] floor, Rehearsal rehearsal, int[] you)
	{
		boolean fresh = rehearsal != this.rehearsal;
		this.caption = caption == null ? "" : caption;
		this.floor = floor;
		this.rehearsal = rehearsal;
		this.you = you;
		if (fresh)
		{
			clock = 0;
		}
		repaint();
	}

	/** Holds the replay on one moment, in ticks, for previews and tests. */
	public void freeze(double tick)
	{
		frozen = tick;
		timer.stop();
		repaint();
	}

	private double cycleMs()
	{
		return LEAD_MS + (rehearsal == null ? 0 : rehearsal.length()) * TICK_MS + HOLD_MS;
	}

	private void step()
	{
		if (!isShowing() || rehearsal == null)
		{
			timer.stop();
			return;
		}
		clock = (clock + FRAME_MS) % cycleMs();
		repaint();
	}

	/** The moment shown, in ticks since the wave spawned. */
	private double now()
	{
		if (frozen >= 0)
		{
			return frozen;
		}
		return Math.max(0, (clock - LEAD_MS) / TICK_MS);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		if (frozen < 0 && rehearsal != null && !timer.isRunning() && isShowing())
		{
			timer.start();
		}
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		if (w <= 0 || h <= 0)
		{
			g.dispose();
			return;
		}
		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);

		int cols = MAX_X - MIN_X + 1;
		int rows = MAX_Y - MIN_Y + 1;
		double cell = Math.min((w - 16) / (double) cols, (h - 30) / (double) rows);
		double left = (w - cell * cols) / 2;
		double top = 22 + (h - 26 - cell * rows) / 2;
		Grid map = new Grid(left, top, cell);

		paintBackdrop(g, w, h, frame, map);
		java.awt.Shape clip = g.getClip();
		g.clip(frame);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		double t = now();
		Rehearsal r = rehearsal;
		if (r != null)
		{
			// Trails first, so every creature sits on top of every trail.
			for (Rehearsal.Track track : r.getTracks())
			{
				paintTrail(g, map, track, t);
			}
			int[] stand = r.getStand();
			paintStand(g, map.x(stand[0] + 0.5), map.y(stand[1] + 0.5));
			for (Rehearsal.Track track : r.getTracks())
			{
				paintCreature(g, map, track, t);
			}
		}
		if (you != null)
		{
			double x = map.x(you[0] + 0.5);
			double y = map.y(you[1] + 0.5);
			g.setColor(Theme.alpha(Theme.TEAL, 70));
			g.fill(new Ellipse2D.Double(x - 4.5, y - 4.5, 9, 9));
			g.setColor(Theme.TEAL);
			g.fill(new Ellipse2D.Double(x - 2.2, y - 2.2, 4.4, 4.4));
		}

		g.setClip(clip);
		g.setFont(Theme.small());
		Ui.shadowed(g, caption, 9, 4 + small.getAscent(), Theme.GOLD);
		if (r != null)
		{
			int tick = (int) Math.min(r.length(), Math.floor(t));
			String clockText = "TICK " + tick + " / " + r.length();
			Ui.shadowed(g, clockText, w - 9 - small.stringWidth(clockText), 4 + small.getAscent(), Theme.TEXT_MUTED);
		}
		g.setColor(Theme.STROKE_SOFT);
		g.draw(frame);
		g.dispose();
	}

	/** Draws the still part of the chart from the kept image, redrawing it only when the floor or size changes. */
	private void paintBackdrop(Graphics2D g, int w, int h, RoundRectangle2D frame, Grid map)
	{
		AffineTransform tx = g.getTransform();
		double scale = Math.max(1, tx.getScaleX());
		if (backdrop == null || backdropFloor != floor || backdropW != w || backdropH != h || backdropScale != scale)
		{
			backdrop = new BufferedImage((int) Math.ceil(w * scale), (int) Math.ceil(h * scale), BufferedImage.TYPE_INT_ARGB);
			Graphics2D b = backdrop.createGraphics();
			Theme.smooth(b);
			b.scale(scale, scale);
			b.setColor(Theme.INK_DEEP);
			b.fill(frame);
			b.setClip(frame);
			b.setPaint(new RadialGradientPaint(w / 2f, h * 0.55f, Math.max(w, h) * 0.7f, new float[]{0, 1},
				new Color[]{Theme.alpha(Theme.BLOOD_DEEP, 110), Theme.alpha(Theme.INK_DEEP, 0)}));
			b.fillRect(0, 0, w, h);
			paintFloor(b, map);
			b.setColor(Theme.alpha(Theme.GOLD_DIM, 120));
			b.setStroke(new BasicStroke(1f));
			for (Spawn s : Spawn.values())
			{
				double cx = map.x(s.getTileX() + 1);
				double cy = map.y(s.getTileY() + 1);
				double r = map.cell * 2.6;
				b.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
			}
			b.dispose();
			backdropFloor = floor;
			backdropW = w;
			backdropH = h;
			backdropScale = scale;
		}
		g.drawImage(backdrop, 0, 0, w, h, null);
	}

	/** Open floor as warm stone; pillars and walls as the dark between, edged in gold. */
	private void paintFloor(Graphics2D g, Grid map)
	{
		boolean[][] f = floor;
		if (f == null)
		{
			return;
		}
		g.setPaint(new GradientPaint(0, (float) map.top, Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.14),
			0, (float) (map.top + map.cell * (MAX_Y - MIN_Y + 1)), Theme.CARD));
		Path2D open = new Path2D.Double();
		for (int x = MIN_X; x <= MAX_X; x++)
		{
			for (int y = MIN_Y; y <= MAX_Y; y++)
			{
				if (open(f, x, y))
				{
					open.append(new Rectangle2D.Double(map.x(x), map.y(y + 1), map.cell + 0.3, map.cell + 0.3), false);
				}
			}
		}
		g.fill(open);
		// The rim of every pillar and wall, where open floor meets rock.
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 170));
		for (int x = MIN_X; x <= MAX_X; x++)
		{
			for (int y = MIN_Y; y <= MAX_Y; y++)
			{
				if (!open(f, x, y))
				{
					continue;
				}
				double x0 = map.x(x);
				double x1 = map.x(x + 1);
				double y0 = map.y(y);
				double y1 = map.y(y + 1);
				if (!open(f, x - 1, y))
				{
					g.draw(new Line2D.Double(x0, y0, x0, y1));
				}
				if (!open(f, x + 1, y))
				{
					g.draw(new Line2D.Double(x1, y0, x1, y1));
				}
				if (!open(f, x, y - 1))
				{
					g.draw(new Line2D.Double(x0, y0, x1, y0));
				}
				if (!open(f, x, y + 1))
				{
					g.draw(new Line2D.Double(x0, y1, x1, y1));
				}
			}
		}
	}

	private static boolean open(boolean[][] f, int x, int y)
	{
		return x >= 0 && y >= 0 && x < f.length && y < f[x].length && f[x][y];
	}

	private static double[] centre(Grid map, Monster m, int[] tile)
	{
		return new double[]{map.x(tile[0] + m.getSize() / 2.0), map.y(tile[1] + m.getSize() / 2.0)};
	}

	/** Where a creature is at a moment between ticks, gliding from one tile to the next. */
	private static double[] position(Grid map, Rehearsal.Track track, double t)
	{
		int i = (int) Math.floor(t);
		double[] a = centre(map, track.getMonster(), track.at(i));
		double[] b = centre(map, track.getMonster(), track.at(i + 1));
		double k = t - i;
		return new double[]{a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k};
	}

	private static void paintTrail(Graphics2D g, Grid map, Rehearsal.Track track, double t)
	{
		List<int[]> path = track.getPath();
		if (path.size() < 2)
		{
			return;
		}
		Color color = Theme.of(track.getMonster());
		// The way still to go, dotted and faint.
		Path2D ahead = new Path2D.Double();
		double[] p0 = centre(map, track.getMonster(), path.get(0));
		ahead.moveTo(p0[0], p0[1]);
		for (int i = 1; i < path.size(); i++)
		{
			double[] p = centre(map, track.getMonster(), path.get(i));
			ahead.lineTo(p[0], p[1]);
		}
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{1.5f, 2.5f}, 0));
		g.setColor(Theme.alpha(color, 110));
		g.draw(ahead);
		// The way walked, solid.
		Path2D walked = new Path2D.Double();
		walked.moveTo(p0[0], p0[1]);
		int last = (int) Math.min(path.size() - 1, Math.floor(t));
		for (int i = 1; i <= last; i++)
		{
			double[] p = centre(map, track.getMonster(), path.get(i));
			walked.lineTo(p[0], p[1]);
		}
		double[] now = position(map, track, Math.min(t, path.size() - 1));
		walked.lineTo(now[0], now[1]);
		g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(color, 200));
		g.draw(walked);
	}

	private static void paintCreature(Graphics2D g, Grid map, Rehearsal.Track track, double t)
	{
		Monster m = track.getMonster();
		boolean done = t >= track.getTicks();
		double[] p = position(map, track, Math.min(t, track.getPath().size() - 1));
		double r = Math.max(3.2, m.getSize() * map.cell / 2.0);
		Color color = Theme.of(m);
		if (done)
		{
			// How the walk ends: a teal halo when it sticks, a pulsing ember one when it hits.
			Color halo = track.attacks() ? Theme.EMBER : Theme.TEAL;
			double pulse = track.attacks() ? 2 + 2 * Math.abs(Math.sin((t - track.getTicks()) * Math.PI)) : 2.5;
			g.setColor(Theme.alpha(halo, 90));
			g.fill(new Ellipse2D.Double(p[0] - r - pulse, p[1] - r - pulse, (r + pulse) * 2, (r + pulse) * 2));
		}
		Ellipse2D body = new Ellipse2D.Double(p[0] - r, p[1] - r, r * 2, r * 2);
		g.setPaint(new GradientPaint((float) p[0], (float) (p[1] - r), Theme.mix(color, Color.WHITE, 0.35),
			(float) p[0], (float) (p[1] + r), Theme.mix(color, Theme.INK_DEEP, 0.3)));
		g.fill(body);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.INK_DEEP);
		g.draw(body);
		if (done && track.getEnd() == Chase.End.STUCK)
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String label = track.isInReach() ? "safespot" : "stuck";
			int x = (int) Math.round(p[0] - fm.stringWidth(label) / 2.0);
			int y = (int) Math.round(p[1] - r - 3);
			Ui.shadowed(g, label, x, y, Theme.TEAL);
		}
	}

	private static void paintStand(Graphics2D g, double x, double y)
	{
		g.setPaint(new RadialGradientPaint((float) x, (float) y, 11, new float[]{0, 1},
			new Color[]{Theme.alpha(Theme.TEAL, 120), Theme.alpha(Theme.TEAL, 0)}));
		g.fill(new Ellipse2D.Double(x - 11, y - 11, 22, 22));
		Path2D diamond = new Path2D.Double();
		diamond.moveTo(x, y - 5);
		diamond.lineTo(x + 5, y);
		diamond.lineTo(x, y + 5);
		diamond.lineTo(x - 5, y);
		diamond.closePath();
		g.setPaint(new GradientPaint((float) x, (float) (y - 5), Color.WHITE, (float) x, (float) (y + 5), Theme.TEAL));
		g.fill(diamond);
		g.setColor(Theme.INK_DEEP);
		g.draw(diamond);
	}

	/** Tiles of the map square to pixels, north up. */
	private static final class Grid
	{
		final double left;
		final double top;
		final double cell;

		Grid(double left, double top, double cell)
		{
			this.left = left;
			this.top = top;
			this.cell = cell;
		}

		double x(double tileX)
		{
			return left + (tileX - MIN_X) * cell;
		}

		/** The pixel row of a tile edge: tile y counts up from the south. */
		double y(double tileY)
		{
			return top + (MAX_Y + 1 - tileY) * cell;
		}
	}
}
