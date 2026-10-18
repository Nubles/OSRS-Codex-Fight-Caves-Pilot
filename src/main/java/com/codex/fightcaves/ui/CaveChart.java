package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.Spawn;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;

/**
 * A chart of the cave, north up: the five spawn points, a wave's creatures standing on them as
 * discs in their prayer's colour and sized by their bulk, and where to stand. A second, fainter
 * wave can be drawn underneath, so the wave now and the wave to come read together.
 */
public class CaveChart extends JComponent
{
	private static final double[][] OUTLINE = {
		{0.06, 0.12}, {0.30, 0.02}, {0.62, 0.03}, {0.93, 0.07}, {0.98, 0.32}, {0.91, 0.56},
		{0.96, 0.80}, {0.78, 0.97}, {0.46, 0.94}, {0.16, 0.98}, {0.02, 0.80}, {0.07, 0.50}
	};
	/** Cooled lava seams across the floor, for texture only. */
	private static final double[][][] SEAMS = {
		{{0.22, 0.30}, {0.34, 0.38}, {0.40, 0.33}},
		{{0.62, 0.70}, {0.70, 0.62}, {0.80, 0.66}},
		{{0.30, 0.74}, {0.38, 0.66}},
		{{0.70, 0.30}, {0.76, 0.40}, {0.70, 0.46}}
	};

	private String caption = "";
	private String corner = "";
	private List<Placement> wave = Collections.emptyList();
	private List<Placement> faint = Collections.emptyList();
	private double[] stand;
	private boolean standNext;
	private double[] you;

	public CaveChart(int height)
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, height));
		setMinimumSize(new Dimension(10, height));
	}

	/**
	 * @param wave the creatures drawn in full
	 * @param faint creatures drawn as outlines beneath, such as the wave still on the floor
	 * @param stand where to stand as fractions of the cave, or null
	 * @param you where you are as fractions of the cave, or null
	 */
	public void show(String caption, String corner, List<Placement> wave, List<Placement> faint, double[] stand, boolean standNext, double[] you)
	{
		this.caption = caption == null ? "" : caption;
		this.corner = corner == null ? "" : corner;
		this.wave = wave == null ? Collections.emptyList() : wave;
		this.faint = faint == null ? Collections.emptyList() : faint;
		this.stand = stand;
		this.standNext = standNext;
		this.you = you;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();

		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setColor(Theme.INK_DEEP);
		g.fill(frame);
		g.setClip(frame);
		g.setPaint(new RadialGradientPaint(w / 2f, h * 0.55f, Math.max(w, h) * 0.7f, new float[]{0, 1},
			new Color[]{Theme.alpha(Theme.BLOOD_DEEP, 120), Theme.alpha(Theme.INK_DEEP, 0)}));
		g.fillRect(0, 0, w, h);

		float left = 12;
		float top = 22;
		float cw = w - 24;
		float ch = h - 32;

		Path2D cave = new Path2D.Float();
		int n = OUTLINE.length;
		Point2D first = mid(OUTLINE[n - 1], OUTLINE[0], left, top, cw, ch);
		cave.moveTo(first.getX(), first.getY());
		for (int i = 0; i < n; i++)
		{
			Point2D ctrl = at(OUTLINE[i], left, top, cw, ch);
			Point2D end = mid(OUTLINE[i], OUTLINE[(i + 1) % n], left, top, cw, ch);
			cave.quadTo(ctrl.getX(), ctrl.getY(), end.getX(), end.getY());
		}
		cave.closePath();
		g.setPaint(new GradientPaint(0, top, Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.12), 0, top + ch, Theme.CARD));
		g.fill(cave);

		g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.EMBER, 70));
		for (double[][] seam : SEAMS)
		{
			Path2D p = new Path2D.Float();
			Point2D s0 = at(seam[0], left, top, cw, ch);
			p.moveTo(s0.getX(), s0.getY());
			for (int i = 1; i < seam.length; i++)
			{
				Point2D si = at(seam[i], left, top, cw, ch);
				p.lineTo(si.getX(), si.getY());
			}
			g.draw(p);
		}
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 200));
		g.draw(cave);

		// Spawn points: an engraved ring and the point's letters.
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		for (Spawn s : Spawn.values())
		{
			Point2D p = at(new double[]{s.getFx(), s.getFy()}, left, top, cw, ch);
			float r = 10;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 160));
			g.fill(new Ellipse2D.Double(p.getX() - r, p.getY() - r, r * 2, r * 2));
			g.setColor(Theme.alpha(Theme.GOLD_DIM, 150));
			g.draw(new Ellipse2D.Double(p.getX() - r, p.getY() - r, r * 2, r * 2));
			g.setColor(Theme.TEXT_FAINT);
			String code = s.getCode();
			g.drawString(code, (float) (p.getX() - small.stringWidth(code) / 2.0), (float) (p.getY() + r + small.getAscent() + 1));
		}

		paintWave(g, faint, true, left, top, cw, ch);
		paintWave(g, wave, false, left, top, cw, ch);

		if (you != null)
		{
			Point2D p = at(you, left, top, cw, ch);
			g.setColor(Theme.alpha(Theme.TEAL, 60));
			g.fill(new Ellipse2D.Double(p.getX() - 6, p.getY() - 6, 12, 12));
			g.setColor(Theme.TEAL);
			g.fill(new Ellipse2D.Double(p.getX() - 3, p.getY() - 3, 6, 6));
			g.setColor(Theme.INK_DEEP);
			g.draw(new Ellipse2D.Double(p.getX() - 3, p.getY() - 3, 6, 6));
		}
		if (stand != null)
		{
			paintStand(g, at(stand, left, top, cw, ch), standNext);
		}

		g.setClip(null);
		g.setFont(Theme.small());
		Ui.shadowed(g, caption, 9, 4 + small.getAscent(), Theme.GOLD);
		if (!corner.isEmpty())
		{
			Ui.shadowed(g, corner, w - 9 - small.stringWidth(corner), 4 + small.getAscent(), Theme.TEXT_MUTED);
		}
		g.setColor(Theme.STROKE_SOFT);
		g.draw(frame);
		g.dispose();
	}

	private static void paintWave(Graphics2D g, List<Placement> list, boolean faint, float left, float top, float cw, float ch)
	{
		Map<Spawn, List<Monster>> at = new EnumMap<>(Spawn.class);
		for (Placement p : list)
		{
			at.computeIfAbsent(p.getSpawn(), k -> new ArrayList<>()).add(p.getMonster());
		}
		for (Map.Entry<Spawn, List<Monster>> e : at.entrySet())
		{
			Point2D c = at(new double[]{e.getKey().getFx(), e.getKey().getFy()}, left, top, cw, ch);
			List<Monster> ms = e.getValue();
			ms.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
			for (int i = ms.size() - 1; i >= 0; i--)
			{
				// Several creatures at one spawn fan out so each can be read.
				double a = -Math.PI / 2 + i * 2.1;
				double off = i == 0 ? 0 : 9;
				double x = c.getX() + Math.cos(a) * off;
				double y = c.getY() + Math.sin(a) * off;
				disc(g, ms.get(i), x, y, faint);
			}
		}
	}

	static void disc(Graphics2D g, Monster m, double x, double y, boolean faint)
	{
		double r = 5 + m.getSize() * 1.4;
		Color color = Theme.of(m);
		Ellipse2D shape = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
		if (faint)
		{
			g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2.5f, 2.5f}, 0));
			g.setColor(Theme.alpha(color, 150));
			g.draw(shape);
			g.setStroke(new BasicStroke(1f));
			return;
		}
		g.setColor(Theme.alpha(color, 50));
		g.fill(new Ellipse2D.Double(x - r - 3, y - r - 3, r * 2 + 6, r * 2 + 6));
		g.setPaint(new GradientPaint((float) x, (float) (y - r), Theme.mix(color, Color.WHITE, 0.35), (float) x, (float) (y + r), Theme.mix(color, Theme.INK_DEEP, 0.35)));
		g.fill(shape);
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(Theme.INK_DEEP);
		g.draw(shape);
		g.setStroke(new BasicStroke(1f));
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		String label = String.valueOf(m.getLevel());
		if (fm.stringWidth(label) < r * 2 + 4)
		{
			g.setColor(Theme.INK_DEEP);
			g.drawString(label, (float) (x - fm.stringWidth(label) / 2.0), (float) (y + fm.getAscent() / 2.0 - 1));
		}
	}

	private static void paintStand(Graphics2D g, Point2D p, boolean next)
	{
		Color color = next ? Theme.TEAL : Theme.GOLD_BRIGHT;
		double x = p.getX();
		double y = p.getY();
		g.setPaint(new RadialGradientPaint((float) x, (float) y, 14, new float[]{0, 1},
			new Color[]{Theme.alpha(color, 110), Theme.alpha(color, 0)}));
		g.fill(new Ellipse2D.Double(x - 14, y - 14, 28, 28));
		Path2D diamond = new Path2D.Double();
		diamond.moveTo(x, y - 6);
		diamond.lineTo(x + 6, y);
		diamond.lineTo(x, y + 6);
		diamond.lineTo(x - 6, y);
		diamond.closePath();
		g.setPaint(new GradientPaint((float) x, (float) (y - 6), Color.WHITE, (float) x, (float) (y + 6), color));
		g.fill(diamond);
		g.setColor(Theme.INK_DEEP);
		g.draw(diamond);
	}

	private static Point2D at(double[] f, float left, float top, float cw, float ch)
	{
		return new Point2D.Double(left + f[0] * cw, top + (1 - f[1]) * ch);
	}

	private static Point2D mid(double[] a, double[] b, float left, float top, float cw, float ch)
	{
		return at(new double[]{(a[0] + b[0]) / 2, (a[1] + b[1]) / 2}, left, top, cw, ch);
	}
}
