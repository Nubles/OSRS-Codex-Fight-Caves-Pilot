package com.codex.fightcaves.overlay;

import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.ui.Theme;
import com.codex.fightcaves.ui.Ui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Draws on the cave floor: the gilt tile to stand on (teal while it is where to be for the wave
 * about to spawn), dashed outlines where the next wave will appear, a numbered seal over each
 * creature giving its place in the kill order, and a tag over any ranger or mager stuck behind
 * a pillar.
 */
public class CaveOverlay extends Overlay
{
	// Everything the frame draws with is made once here, so a frame allocates no colours, strokes or labels.
	private static final Monster[] MONSTERS = Monster.values();
	private static final Color[] GHOST_FILL = new Color[MONSTERS.length];
	private static final Color[] GHOST_LINE = new Color[MONSTERS.length];
	private static final Color[] TRAIL_COLOR = new Color[MONSTERS.length];
	private static final String[] GHOST_LABEL = new String[MONSTERS.length];
	private static final BasicStroke GHOST_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 4}, 0);
	private static final BasicStroke STAND_SHADOW = new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke STAND_LINE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke SEAL_FIRST = new BasicStroke(1.8f);
	private static final BasicStroke SEAL = new BasicStroke(1.2f);
	private static final BasicStroke PILL = new BasicStroke(1f);
	private static final Color SHADOW_70 = Theme.alpha(Color.BLACK, 70);
	private static final Color SHADOW_120 = Theme.alpha(Color.BLACK, 120);
	private static final Color STAND_FILL = Theme.alpha(Theme.GOLD_BRIGHT, 55);
	private static final Color STAND_NEXT_FILL = Theme.alpha(Theme.TEAL, 55);
	private static final Color SEAL_FILL = Theme.alpha(Theme.INK_DEEP, 210);
	private static final Color PILL_FILL = Theme.alpha(Theme.INK_DEEP, 190);
	private static final Color PILL_LINE = Theme.alpha(Theme.TEAL, 200);
	private static final String[] RANKS = new String[16];

	static
	{
		for (Monster m : MONSTERS)
		{
			Color c = Theme.of(m);
			GHOST_FILL[m.ordinal()] = Theme.alpha(c, 30);
			GHOST_LINE[m.ordinal()] = Theme.alpha(c, 210);
			TRAIL_COLOR[m.ordinal()] = Theme.alpha(c, 220);
			GHOST_LABEL[m.ordinal()] = m.getNickname() + " " + m.getLevel();
		}
		for (int i = 0; i < RANKS.length; i++)
		{
			RANKS[i] = String.valueOf(i);
		}
	}

	private final Client client;
	private volatile Marks marks = Marks.NONE;

	@Inject
	public CaveOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(Overlay.PRIORITY_HIGH);
	}

	public void show(Marks marks)
	{
		this.marks = marks == null ? Marks.NONE : marks;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Marks m = marks;
		WorldView wv = client.getTopLevelWorldView();
		if (m == Marks.NONE || wv == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.small());
		if (m.rehearsal != null)
		{
			paintRehearsal(g, wv, m.rehearsal);
		}
		for (Marks.Ghost ghost : m.ghosts)
		{
			int size = ghost.monster.getSize();
			LocalPoint sw = LocalPoint.fromScene(ghost.x, ghost.y, wv);
			if (sw == null)
			{
				continue;
			}
			// The area's polygon is centred on its middle, which for even sizes falls between tiles.
			LocalPoint centre = new LocalPoint(sw.getX() + (size - 1) * 64, sw.getY() + (size - 1) * 64, wv);
			Polygon poly = Perspective.getCanvasTileAreaPoly(client, centre, size);
			if (poly == null)
			{
				continue;
			}
			int k = ghost.monster.ordinal();
			Color color = Theme.of(ghost.monster);
			g.setColor(GHOST_FILL[k]);
			g.fill(poly);
			g.setStroke(GHOST_STROKE);
			g.setColor(GHOST_LINE[k]);
			g.draw(poly);
			String label = GHOST_LABEL[k];
			Point text = Perspective.getCanvasTextLocation(client, g, centre, label, 0);
			if (text != null)
			{
				Ui.shadowed(g, label, text.getX(), text.getY(), color);
			}
		}

		if (m.stand != null)
		{
			LocalPoint lp = LocalPoint.fromScene(m.stand.x, m.stand.y, wv);
			Polygon poly = lp == null ? null : Perspective.getCanvasTilePoly(client, lp);
			if (poly != null)
			{
				Color color = m.stand.next ? Theme.TEAL : Theme.GOLD_BRIGHT;
				g.setColor(m.stand.next ? STAND_NEXT_FILL : STAND_FILL);
				g.fill(poly);
				g.setStroke(STAND_SHADOW);
				g.setColor(SHADOW_120);
				g.draw(poly);
				g.setStroke(STAND_LINE);
				g.setColor(color);
				g.draw(poly);
				String label = m.stand.next ? "Stand: next wave" : m.stand.safe ? "Stand: safe" : "Stand";
				Point text = Perspective.getCanvasTextLocation(client, g, lp, label, 0);
				if (text != null)
				{
					Ui.shadowed(g, label, text.getX(), text.getY() - 6, color);
				}
			}
		}

		FontMetrics fm = g.getFontMetrics();
		// Ranked creatures first, then any stuck one without a rank, each once.
		for (NPC npc : m.ranks.keySet())
		{
			paintTag(g, fm, npc, m.ranks.get(npc), m.stuck.contains(npc));
		}
		for (NPC npc : m.stuck)
		{
			if (!m.ranks.containsKey(npc))
			{
				paintTag(g, fm, npc, null, true);
			}
		}
		return null;
	}

	/** A creature's kill-order seal and its "Stuck" tag, side by side over its head. */
	private static void paintTag(Graphics2D g, FontMetrics fm, NPC npc, Integer rank, boolean stuck)
	{
		if (npc == null || npc.isDead())
		{
			return;
		}
		String label = stuck ? "Stuck" : "";
		Point anchor = npc.getCanvasTextLocation(g, label.isEmpty() ? "0" : label, npc.getLogicalHeight() + 40);
		if (anchor == null)
		{
			return;
		}
		int d = fm.getHeight() + 3;
		int textWidth = label.isEmpty() ? 0 : fm.stringWidth(label) + 8;
		int total = (rank != null ? d : 0) + (rank != null && stuck ? 3 : 0) + textWidth;
		int x = anchor.getX() + (label.isEmpty() ? fm.stringWidth("0") / 2 : fm.stringWidth(label) / 2) - total / 2;
		int top = anchor.getY() - fm.getAscent() - 2;
		if (rank != null)
		{
			// A gilt seal with the creature's place in the kill order.
			Color seal = rank == 1 ? Theme.GOLD_BRIGHT : Theme.GOLD;
			g.setColor(SEAL_FILL);
			g.fillOval(x, top, d, d);
			g.setStroke(rank == 1 ? SEAL_FIRST : SEAL);
			g.setColor(seal);
			g.drawOval(x, top, d, d);
			String n = rank >= 0 && rank < RANKS.length ? RANKS[rank] : String.valueOf(rank);
			Ui.shadowed(g, n, x + (d - fm.stringWidth(n)) / 2 + 1, top + (d + fm.getAscent()) / 2 - 1, seal);
			x += d + 3;
		}
		if (stuck)
		{
			int py = top + (d - fm.getHeight() - 1) / 2;
			int ph = fm.getHeight() + 1;
			g.setColor(PILL_FILL);
			g.fillRoundRect(x, py, textWidth, ph, 6, 6);
			g.setStroke(PILL);
			g.setColor(PILL_LINE);
			g.drawRoundRect(x, py, textWidth, ph, 6, 6);
			Ui.shadowed(g, label, x + 4, py + fm.getAscent(), Theme.TEAL);
		}
	}

	private static final BasicStroke TRAIL_SHADOW = new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final BasicStroke TRAIL = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2, 6}, 0);
	private static final BasicStroke RING_SHADOW = new BasicStroke(2f);
	private static final BasicStroke RING = new BasicStroke(1.4f);

	/**
	 * Each creature's walk from its spawn to where it stops, as a dotted line on the floor in its
	 * prayer's colour, ending in a ring: teal where it sticks, ember where it starts hitting you.
	 */
	private void paintRehearsal(Graphics2D g, WorldView wv, com.codex.fightcaves.core.Rehearsal r)
	{
		int plane = wv.getPlane();
		for (com.codex.fightcaves.core.Rehearsal.Track t : r.getTracks())
		{
			java.util.List<int[]> path = t.getPath();
			if (path.size() < 2)
			{
				continue;
			}
			int size = t.getMonster().getSize();
			java.awt.geom.Path2D line = new java.awt.geom.Path2D.Double();
			Point last = null;
			boolean started = false;
			for (int[] tile : path)
			{
				LocalPoint sw = LocalPoint.fromScene(tile[0], tile[1], wv);
				if (sw == null)
				{
					continue;
				}
				LocalPoint centre = new LocalPoint(sw.getX() + (size - 1) * 64, sw.getY() + (size - 1) * 64, wv);
				Point p = Perspective.localToCanvas(client, centre, plane);
				if (p == null)
				{
					continue;
				}
				if (!started)
				{
					line.moveTo(p.getX(), p.getY());
					started = true;
				}
				else
				{
					line.lineTo(p.getX(), p.getY());
				}
				last = p;
			}
			if (!started || last == null)
			{
				continue;
			}
			g.setStroke(TRAIL_SHADOW);
			g.setColor(SHADOW_70);
			g.draw(line);
			g.setStroke(TRAIL);
			g.setColor(TRAIL_COLOR[t.getMonster().ordinal()]);
			g.draw(line);
			Color end = t.attacks() ? Theme.EMBER : Theme.TEAL;
			g.setStroke(RING_SHADOW);
			g.setColor(SHADOW_120);
			g.drawOval(last.getX() - 6, last.getY() - 6, 12, 12);
			g.setStroke(RING);
			g.setColor(end);
			g.drawOval(last.getX() - 5, last.getY() - 5, 10, 10);
			String label = t.attacks() ? (t.getTicks() == 0 ? "at once" : t.getTicks() + (t.getTicks() == 1 ? " tick" : " ticks")) : t.isInReach() ? "safespot" : "stuck";
			FontMetrics fm = g.getFontMetrics();
			Ui.shadowed(g, label, last.getX() - fm.stringWidth(label) / 2, last.getY() + 7 + fm.getAscent(), end);
		}
	}
}
