package com.codex.fightcaves.overlay;

import com.codex.fightcaves.ui.Theme;
import com.codex.fightcaves.ui.Ui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
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
			Color color = Theme.of(ghost.monster);
			g.setColor(Theme.alpha(color, 30));
			g.fill(poly);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 4}, 0));
			g.setColor(Theme.alpha(color, 210));
			g.draw(poly);
			String label = ghost.monster.getNickname() + " " + ghost.monster.getLevel();
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
				g.setColor(Theme.alpha(color, 55));
				g.fill(poly);
				g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(Theme.alpha(Color.BLACK, 120));
				g.draw(poly);
				g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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

		java.util.Set<NPC> tagged = new java.util.LinkedHashSet<>(m.ranks.keySet());
		tagged.addAll(m.stuck);
		FontMetrics fm = g.getFontMetrics();
		for (NPC npc : tagged)
		{
			if (npc == null || npc.isDead())
			{
				continue;
			}
			Integer rank = m.ranks.get(npc);
			boolean stuck = m.stuck.contains(npc);
			String label = stuck ? "Stuck" : "";
			Point anchor = npc.getCanvasTextLocation(g, label.isEmpty() ? "0" : label, npc.getLogicalHeight() + 40);
			if (anchor == null)
			{
				continue;
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
				g.setColor(Theme.alpha(Theme.INK_DEEP, 210));
				g.fillOval(x, top, d, d);
				g.setStroke(new BasicStroke(rank == 1 ? 1.8f : 1.2f));
				g.setColor(seal);
				g.drawOval(x, top, d, d);
				String n = String.valueOf(rank);
				Ui.shadowed(g, n, x + (d - fm.stringWidth(n)) / 2 + 1, top + (d + fm.getAscent()) / 2 - 1, seal);
				x += d + 3;
			}
			if (stuck)
			{
				Rectangle pill = new Rectangle(x, top + (d - fm.getHeight() - 1) / 2, textWidth, fm.getHeight() + 1);
				g.setColor(Theme.alpha(Theme.INK_DEEP, 190));
				g.fillRoundRect(pill.x, pill.y, pill.width, pill.height, 6, 6);
				g.setStroke(new BasicStroke(1f));
				g.setColor(Theme.alpha(Theme.TEAL, 200));
				g.drawRoundRect(pill.x, pill.y, pill.width, pill.height, 6, 6);
				Ui.shadowed(g, label, pill.x + 4, pill.y + fm.getAscent(), Theme.TEAL);
			}
		}
		return null;
	}
}
