package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Monster;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.RunStory;
import com.codex.fightcaves.core.Spawn;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import javax.swing.JComponent;
import org.junit.Test;

/**
 * The panel's cards and charts clip their glow to their own rounded edge and then put the clip
 * back as it was. Clearing it instead would let them paint past the part of the panel being
 * redrawn.
 */
public class ClipTest
{
	/** Paints a component through a clip and checks nothing landed outside it. */
	private static void paintsInsideItsClip(JComponent c)
	{
		c.setSize(200, 120);
		c.doLayout();
		BufferedImage img = new BufferedImage(200, 120, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle inside = new Rectangle(0, 0, 200, 30);
		g.setClip(inside);
		c.paint(g);
		assertEquals(inside, g.getClip());
		g.dispose();
		for (int y = 30; y < 120; y++)
		{
			for (int x = 0; x < 200; x++)
			{
				assertEquals("painted past the clip at " + x + "," + y, 0, img.getRGB(x, y) >>> 24);
			}
		}
	}

	@Test
	public void aCardPaintsOnlyInsideItsClip()
	{
		Card card = new Card(new BorderLayout());
		card.accent(Theme.GOLD);
		paintsInsideItsClip(card);
	}

	@Test
	public void theRunScrollPaintsOnlyInsideItsClip()
	{
		Logbook b = new Logbook();
		Logbook.Active a = b.begin(1, 0);
		a.wave = 12;
		a.ticks = 900;
		for (int w = 1; w < 12; w++)
		{
			a.splits.add(60);
		}
		Logbook.Run run = b.finish(Logbook.Result.DIED);
		paintsInsideItsClip(new RunScroll(RunStory.of(b, run)));
	}

	@Test
	public void theCaveChartPaintsOnlyInsideItsClip()
	{
		CaveChart chart = new CaveChart(120);
		chart.show("WAVE 1", "ROTATION 4", Collections.singletonList(new Placement(Monster.TZ_KIH, Spawn.S)), null);
		paintsInsideItsClip(chart);
	}
}
