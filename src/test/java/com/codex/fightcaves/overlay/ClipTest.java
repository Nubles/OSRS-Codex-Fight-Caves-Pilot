package com.codex.fightcaves.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.codex.fightcaves.core.RotationClock;
import com.codex.fightcaves.ui.Theme;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * RuneLite's overlay renderer compares the clip after each overlay and fails on one left cleared,
 * every frame. The plaques must hand the graphics back as they found it: clip, transform, font,
 * paint, stroke, composite and hints.
 */
public class ClipTest
{
	private static final Rectangle CLIP = new Rectangle(3, 4, 300, 60);

	private static Graphics2D graphics(BufferedImage img)
	{
		Graphics2D g = img.createGraphics();
		g.setClip(CLIP);
		g.translate(2, 1);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
		g.setPaint(Color.MAGENTA);
		g.setStroke(new BasicStroke(3f));
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		return g;
	}

	private static void assertUnchanged(Graphics2D g, Shape clip, AffineTransform t, Font f, Paint p, Stroke s, Composite c, Object aa)
	{
		assertNotNull("clip cleared", g.getClip());
		assertEquals(clip, g.getClip());
		assertEquals(t, g.getTransform());
		assertEquals(f, g.getFont());
		assertEquals(p, g.getPaint());
		assertEquals(s, g.getStroke());
		assertEquals(c, g.getComposite());
		assertEquals(aa, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
	}

	@Test
	public void thePilotPlaqueLeavesTheGraphicsAsItFoundThem()
	{
		PilotPlaque plaque = new PilotPlaque();
		plaque.show("Wave 24", "Rotation 9", "18 doses (+3)", Theme.GREEN, "Ghost -0:14", Theme.GREEN);
		Graphics2D g = graphics(new BufferedImage(400, 80, BufferedImage.TYPE_INT_ARGB));
		Shape clip = g.getClip();
		AffineTransform t = g.getTransform();
		Font f = g.getFont();
		Paint p = g.getPaint();
		Stroke s = g.getStroke();
		Composite c = g.getComposite();
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		assertNotNull(plaque.render(g));
		assertUnchanged(g, clip, t, f, p, s, c, aa);
		g.dispose();
	}

	@Test
	public void theClockPlaqueLeavesTheGraphicsAsItFoundThem()
	{
		ClockPlaque plaque = new ClockPlaque();
		RotationClock.Reading reading = new RotationClock.Reading(12, 30);
		plaque.show(reading, System.currentTimeMillis(), null, null);
		Graphics2D g = graphics(new BufferedImage(400, 80, BufferedImage.TYPE_INT_ARGB));
		Shape clip = g.getClip();
		AffineTransform t = g.getTransform();
		Font f = g.getFont();
		Paint p = g.getPaint();
		Stroke s = g.getStroke();
		Composite c = g.getComposite();
		Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		assertNotNull(plaque.render(g));
		assertUnchanged(g, clip, t, f, p, s, c, aa);
		g.dispose();
	}
}
