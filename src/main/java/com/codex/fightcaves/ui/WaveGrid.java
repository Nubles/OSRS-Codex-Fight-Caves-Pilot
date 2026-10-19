package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Outlook;
import com.codex.fightcaves.core.Waves;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.function.IntConsumer;
import javax.swing.JComponent;

/**
 * The road to Jad as a tablet of 63 stones, seven to a row like the splits' stretches. Each wave
 * is tinted by how it rates when it spawns: teal clean, gold pray, ember split, and Jad in blood.
 * Waves already cleared this run are dimmed, the wave on now is ringed in gold, and on the Waves
 * page any stone can be clicked to chart that wave.
 */
class WaveGrid extends JComponent
{
	private static final int COLUMNS = 7;
	private static final int ROWS = (Waves.LAST + COLUMNS - 1) / COLUMNS;
	private static final int CELL_H = 17;
	private static final int GAP = 3;

	private final String codes;
	private final int now;
	private final int chosen;
	private final IntConsumer onPick;
	private int hover;

	/**
	 * @param codes one letter per wave (see Outlook), or null when nothing is rated
	 * @param now the wave on now, dimming those before it, or 0
	 * @param chosen the wave being charted, or 0
	 * @param onPick called with a clicked wave, or null when the stones are not clickable
	 */
	WaveGrid(String codes, int now, int chosen, IntConsumer onPick)
	{
		this.codes = codes;
		this.now = now;
		this.chosen = chosen;
		this.onPick = onPick;
		setOpaque(false);
		setPreferredSize(new Dimension(10, ROWS * CELL_H + (ROWS - 1) * GAP));
		if (onPick != null)
		{
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			MouseAdapter mouse = new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
					int w = waveAt(e.getX(), e.getY());
					if (w != hover)
					{
						hover = w;
						setToolTipText(w > 0 ? tip(w) : null);
						repaint();
					}
				}

				@Override
				public void mouseExited(MouseEvent e)
				{
					hover = 0;
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e)
				{
					int w = waveAt(e.getX(), e.getY());
					if (w > 0)
					{
						onPick.accept(w);
					}
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}
	}

	private String tip(int wave)
	{
		Outlook.Rating r = rating(wave);
		return "Wave " + wave + ": " + Waves.describe(wave) + (r == null ? "" : " (" + r.getLabel().toLowerCase() + ")");
	}

	private Outlook.Rating rating(int wave)
	{
		return codes == null || wave >= Waves.JAD || wave - 1 >= codes.length() ? null : Outlook.Rating.of(codes.charAt(wave - 1));
	}

	private float cellWidth()
	{
		return (getWidth() - (COLUMNS - 1) * GAP) / (float) COLUMNS;
	}

	private int waveAt(int x, int y)
	{
		float cw = cellWidth();
		int col = (int) (x / (cw + GAP));
		int row = y / (CELL_H + GAP);
		if (col < 0 || col >= COLUMNS || row < 0 || row >= ROWS)
		{
			return 0;
		}
		int wave = row * COLUMNS + col + 1;
		return wave <= Waves.LAST ? wave : 0;
	}

	/** The tint for a rating; Jad is blood and a wave not rated yet is plain stone. */
	static Color tint(Outlook.Rating r)
	{
		if (r == null)
		{
			return Theme.STROKE;
		}
		switch (r)
		{
			case CLEAN:
				return Theme.TEAL;
			case PRAY:
				return Theme.GOLD;
			default:
				return Theme.EMBER;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		float cw = cellWidth();
		for (int wave = 1; wave <= Waves.LAST; wave++)
		{
			int col = (wave - 1) % COLUMNS;
			int row = (wave - 1) / COLUMNS;
			float x = col * (cw + GAP);
			float y = row * (CELL_H + GAP);
			boolean jad = wave == Waves.JAD;
			boolean past = now > 0 && wave < now;
			Color tint = jad ? Theme.BLOOD : tint(rating(wave));
			RoundRectangle2D cell = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, cw - 1, CELL_H - 1, 5, 5);
			g.setColor(Theme.alpha(tint, past ? 22 : rating(wave) == null && !jad ? 40 : 70));
			g.fill(cell);
			g.setColor(Theme.alpha(tint, past ? 50 : 150));
			g.draw(cell);
			if (wave == now || wave == chosen || wave == hover)
			{
				g.setColor(wave == now ? Theme.GOLD_BRIGHT : wave == chosen ? Theme.TEXT : Theme.alpha(Theme.TEXT, 140));
				g.draw(new RoundRectangle2D.Float(x - 0.5f, y - 0.5f, cw + 1, CELL_H + 1, 6, 6));
			}
			String label = jad ? "Jad" : String.valueOf(wave);
			Color text = past ? Theme.TEXT_FAINT : wave == now ? Theme.GOLD_BRIGHT : Theme.mix(Theme.TEXT, tint, 0.25);
			Ui.shadowed(g, label, Math.round(x + (cw - fm.stringWidth(label)) / 2), Math.round(y + (CELL_H + fm.getAscent()) / 2f) - 1, text);
		}
		g.dispose();
	}
}
