package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import com.codex.fightcaves.core.Ghost;
import com.codex.fightcaves.core.Logbook;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Test;

/** The run report's lines, as the panel writes them. */
public class ReportTest
{
	private static Logbook.Run run(Logbook.Result result, int waves, int each)
	{
		Logbook.Run r = new Logbook.Run();
		r.result = result;
		r.wave = result == Logbook.Result.CAPE ? 63 : waves + 1;
		for (int w = 1; w <= waves; w++)
		{
			r.splits.add(each);
			r.doses.add(0);
			r.food.add(0);
		}
		r.ticks = waves * each;
		r.endedAt = waves;
		return r;
	}

	@Test
	public void linesNameTheWavesInOrderOfCost()
	{
		Logbook.Run g = run(Logbook.Result.CAPE, 63, 10);
		Logbook.Run r = run(Logbook.Result.DIED, 50, 10);
		r.splits.set(45, 73);
		r.splits.set(49, 45);
		r.doses.set(45, 3);
		r.food.set(45, 1);
		r.food.set(30, 2);
		Ghost.Report rep = Ghost.of(java.util.Arrays.asList(g, r), r).report(r);
		assertEquals("Most time lost: wave 46 (+0:38) and wave 50 (+0:21).", PilotPanel.slowLine(rep.getSlowest()));
		assertEquals("Most supplies: wave 46 (3 doses and 1 food, the ghost nothing) and wave 31 (+2).", PilotPanel.hungryLine(rep.getHungriest()));
		assertEquals("No wave slower than the ghost's.", PilotPanel.slowLine(Collections.<Ghost.Split>emptyList()));
		assertEquals("1 dose", PilotPanel.supplies(1, 0));
		assertEquals("2 food", PilotPanel.supplies(0, 2));
	}

	@Test
	public void noGhostNoReport() throws Exception
	{
		Logbook b = new Logbook();
		Logbook.Run only = run(Logbook.Result.DIED, 10, 10);
		b.runs.add(only);
		Object[] card = new Object[2];
		SwingUtilities.invokeAndWait(() ->
		{
			card[0] = PilotPanel.reportCard(b, only);
			b.runs.add(0, run(Logbook.Result.CAPE, 63, 10));
			card[1] = PilotPanel.reportCard(b, only);
		});
		assertNull(card[0]);
		assertNotNull(card[1]);
	}
}
