package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Rotations;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/**
 * The fifteen rotations as gilt studs in three rows. The chosen one is lit; the one the pilot
 * read from your run carries a teal mark, and rotations still in the running are ringed.
 */
class RotationPicker extends JComponent
{
	private static final int COLS = 5;
	private static final int ROW_H = 24;

	private final IntConsumer onPick;
	private int chosen = 1;
	private int detected;
	private List<Integer> candidates = Collections.emptyList();
	private int hover = -1;

	RotationPicker(IntConsumer onPick)
	{
		this.onPick = onPick;
		setOpaque(false);
		int rows = (Rotations.COUNT + COLS - 1) / COLS;
		setPreferredSize(new Dimension(10, rows * ROW_H + (rows - 1) * 4));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				int r = at(e.getX(), e.getY());
				if (r != hover)
				{
					hover = r;
					repaint();
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = -1;
				repaint();
			}

			@Override
			public void mousePressed(MouseEvent e)
			{
				int r = at(e.getX(), e.getY());
				if (r > 0)
				{
					chosen = r;
					repaint();
					onPick.accept(r);
				}
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
	}

	void show(int chosen, int detected, List<Integer> candidates)
	{
		this.chosen = chosen;
		this.detected = detected;
		this.candidates = candidates == null ? Collections.emptyList() : candidates;
		repaint();
	}

	private int at(int x, int y)
	{
		float cell = getWidth() / (float) COLS;
		int col = (int) (x / cell);
		int row = y / (ROW_H + 4);
		int r = row * COLS + col + 1;
		return col >= 0 && col < COLS && r >= 1 && r <= Rotations.COUNT ? r : -1;
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		float cell = getWidth() / (float) COLS;
		g.setFont(Theme.title());
		FontMetrics fm = g.getFontMetrics();
		for (int r = 1; r <= Rotations.COUNT; r++)
		{
			int col = (r - 1) % COLS;
			int row = (r - 1) / COLS;
			float x = col * cell + 2;
			float y = row * (ROW_H + 4);
			float cw = cell - 4;
			RoundRectangle2D stud = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, ROW_H - 1, 8, 8);
			boolean lit = r == chosen;
			boolean running = candidates.size() > 1 && candidates.contains(r);
			Color top = lit ? Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.45) : r == hover ? Theme.CARD_HOVER : Theme.CARD_TOP;
			g.setPaint(new GradientPaint(0, y, top, 0, y + ROW_H, Theme.INK_DEEP));
			g.fill(stud);
			g.setColor(lit ? Theme.GOLD : running ? Theme.alpha(Theme.TEAL, 170) : Theme.STROKE_SOFT);
			g.draw(stud);
			String n = String.valueOf(r);
			Ui.shadowed(g, n, Math.round(x + (cw - fm.stringWidth(n)) / 2), Math.round(y + (ROW_H + fm.getAscent()) / 2f - 2),
				lit ? Theme.GOLD_BRIGHT : r == detected ? Theme.TEAL : Theme.TEXT_MUTED);
			if (r == detected)
			{
				g.setColor(Theme.TEAL);
				g.fillRect(Math.round(x + cw * 0.3f), Math.round(y + ROW_H - 4), Math.round(cw * 0.4f), 2);
			}
		}
		g.dispose();
	}
}
