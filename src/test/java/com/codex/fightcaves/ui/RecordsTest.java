package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import com.codex.fightcaves.core.Logbook;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/** The Records page's tiles. */
public class RecordsTest
{
	private static void tiles(Component c, List<String> out)
	{
		if (c instanceof StatTile)
		{
			out.add(((StatTile) c).value());
		}
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				tiles(k, out);
			}
		}
	}

	@Test
	public void noRunsYetReadsTheSameInEveryTile() throws Exception
	{
		// Empty, the Runs tile said "0" beside "-" in the other three.
		javax.swing.SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel panel = new PilotPanel();
			panel.update(new View(View.State.OUTSIDE, 0, 0, null, 0, false, 0, null, null, null, 70, 70, false, 24, 0, null, new Logbook()));
			List<String> values = new ArrayList<>();
			tiles(panel.recordsPage(), values);
			assertEquals(Arrays.asList("-", "-", "-", "-"), values);
		});
	}
}
