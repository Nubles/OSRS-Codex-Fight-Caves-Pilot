package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.PrayerPacer;
import com.codex.fightcaves.core.Rotations;
import com.codex.fightcaves.core.Waves;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The pilot's side panel: the crest, then three pages. Pilot is the run in progress: the cave
 * chart, where to stand and why, prayer against the waves left and what comes next. Waves is
 * every wave of every rotation, to study before going in. Records is how your runs have gone.
 */
public class PilotPanel extends PluginPanel
{
	static final int WIDTH = 200;
	private static final String[] TABS = {"pilot", "waves", "records"};
	private static final int AHEAD = 5;
	private static final int RECENT = 8;

	private final Crest crest = new Crest();
	private final Segmented tabs = new Segmented(true, "Pilot", "Waves", "Records");
	private final CardLayout cards = new CardLayout();
	private final JPanel content = new JPanel(cards);
	private final JScrollPane pilot = scroller();
	private final JScrollPane waves = scroller();
	private final JScrollPane records = scroller();

	private View view = View.loggedOut();
	/** The rotation and wave being studied on the Waves page. */
	private int studyRotation = 1;
	private int studyWave = 1;
	private boolean studyTouched;

	public PilotPanel()
	{
		super(false);
		setLayout(new BorderLayout());
		setBackground(Theme.INK);

		JPanel north = new JPanel(new BorderLayout());
		north.setOpaque(false);
		north.add(crest, BorderLayout.NORTH);
		JPanel tabHolder = new JPanel(new BorderLayout());
		tabHolder.setOpaque(false);
		tabHolder.setBorder(new EmptyBorder(8, 8, 2, 8));
		tabHolder.add(tabs, BorderLayout.CENTER);
		north.add(tabHolder, BorderLayout.SOUTH);
		add(north, BorderLayout.NORTH);

		content.setOpaque(false);
		content.add(pilot, TABS[0]);
		content.add(waves, TABS[1]);
		content.add(records, TABS[2]);
		add(content, BorderLayout.CENTER);
		tabs.onChange(this::showTab);
		rebuild();
	}

	private static JScrollPane scroller()
	{
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new java.awt.Dimension(8, 0));
		return scroll;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		boolean rotationChanged = view.rotation != this.view.rotation;
		this.view = view;
		if (!studyTouched || rotationChanged && view.rotation > 0)
		{
			studyRotation = view.rotation > 0 ? view.rotation : studyRotation;
			studyWave = view.wave > 0 ? view.wave : studyWave;
		}
		updateCrest();
		rebuild();
	}

	/** Opens one tab, for previews and tests. */
	public void showTab(int index)
	{
		tabs.setSelected(index, false);
		cards.show(content, TABS[index]);
	}

	/** Chooses what the Waves page shows, for previews and tests. */
	public void study(int rotation, int wave)
	{
		studyRotation = rotation;
		studyWave = wave;
		studyTouched = true;
		rebuildWaves();
	}

	private void updateCrest()
	{
		switch (view.state)
		{
			case LOGGED_OUT:
				crest.update("TZHAAR FIGHT CAVE", "-", "", "Log in to fly the cave", Theme.TEXT_MUTED, Theme.GOLD_DIM, 0);
				return;
			case OUTSIDE:
				Logbook b = view.book;
				String status = b.bestTicks > 0 ? "Best cape " + Format.ticks(b.bestTicks) : b.furthest > 0 ? "Furthest: wave " + b.furthest : "Ready when you are";
				crest.update("TZHAAR FIGHT CAVE", b.capes > 0 ? String.valueOf(b.capes) : "63", b.capes > 0 ? (b.capes == 1 ? "cape" : "capes") : "waves",
					status, Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3), Theme.GOLD, 0);
				return;
			default:
				break;
		}
		String figure = view.wave > 0 ? String.valueOf(view.wave) : "-";
		String status;
		java.awt.Color color = Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.35);
		if (view.wave == Waves.JAD)
		{
			status = "TzTok-Jad. Watch him, not the panel";
			color = Theme.BLOOD;
		}
		else if (view.pace != null && view.pace.getState() == PrayerPacer.State.SHORT)
		{
			status = "Prayer is behind pace";
			color = Theme.BLOOD;
		}
		else if (view.rotation > 0)
		{
			status = Waves.describe(view.wave);
		}
		else if (view.wave > 0)
		{
			status = "Reading the rotation";
			color = Theme.TEAL;
		}
		else
		{
			status = "Waiting for the first wave";
		}
		crest.update(view.rotation > 0 ? "ROTATION " + view.rotation : "TZHAAR FIGHT CAVE", figure, "/ " + Waves.LAST, status, color,
			view.wave == Waves.JAD ? Theme.BLOOD : Theme.GOLD, view.wave / (double) Waves.LAST);
	}

	private void rebuild()
	{
		rebuildPilot();
		rebuildWaves();
		rebuildRecords();
	}

	private static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.add(Ui.label(title, Theme.title(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		return card;
	}

	private static void replace(JScrollPane scroll, JComponent stack)
	{
		scroll.setViewportView(stack);
		scroll.revalidate();
		scroll.repaint();
	}

	// The Pilot page.

	private void rebuildPilot()
	{
		StackPanel stack = new StackPanel(8, 8);
		if (view.state == View.State.LOGGED_OUT)
		{
			stack.add(note("The cave is dark", "Log in and Codex Fight Caves Pilot reads your rotation from the first spawns, marks where to stand each wave and paces your prayer against the waves left."));
			replace(pilot, stack);
			return;
		}
		if (view.state == View.State.OUTSIDE)
		{
			buildBriefing(stack);
			replace(pilot, stack);
			return;
		}

		int wave = Math.max(1, view.wave);
		boolean next = view.stand != null && view.stand.next;
		int shown = next ? Math.min(Waves.LAST, wave + 1) : wave;
		List<Placement> bright = view.rotation > 0 ? view.placements(view.rotation, shown) : view.observed;
		List<Placement> faint = Collections.emptyList();
		if (view.rotation > 0)
		{
			faint = next ? view.placements(view.rotation, wave) : view.placements(view.rotation, Math.min(Waves.LAST, wave + 1));
			if (shown == Waves.LAST && !next)
			{
				faint = Collections.emptyList();
			}
		}
		CaveChart chart = new CaveChart(170);
		String caption = view.wave == 0 ? "WAITING FOR WAVE 1" : next ? "NEXT  WAVE " + shown : "WAVE " + shown;
		chart.show(caption, view.rotation > 0 ? "ROTATION " + view.rotation : "ROTATION ?", bright, faint,
			view.stand == null ? null : new double[]{view.stand.fx, view.stand.fy}, next, view.you);
		stack.add(chart);

		stack.add(rotationCard());

		if (view.stand != null)
		{
			stack.add(new SectionHeader("Where to stand", next ? "for wave " + shown : "now"));
			Card card = new Card(new BorderLayout(0, 4));
			StackPanel rows = new StackPanel(2, 0);
			for (View.Fate f : view.stand.fates)
			{
				rows.add(new FateRow(f));
			}
			card.add(rows, BorderLayout.CENTER);
			String foot = view.stand.safe ? "Every ranger and mager is stuck from here" : "The gilt tile is the safest within reach";
			if (view.stand.steps > 0)
			{
				foot += ", " + view.stand.steps + (view.stand.steps == 1 ? " step away" : " steps away");
			}
			card.add(Ui.wrapped(foot, Theme.small(), view.stand.safe ? Theme.TEAL : Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
			card.accent(view.stand.safe ? Theme.TEAL : Theme.GOLD);
			stack.add(card);
		}

		if (view.call != null || !view.kills.isEmpty())
		{
			stack.add(new SectionHeader("The fight", "from where you stand"));
			Card card = new Card(new BorderLayout(0, 6));
			if (view.call != null)
			{
				card.add(callBlock(view.call), BorderLayout.NORTH);
			}
			if (!view.kills.isEmpty())
			{
				StackPanel rows = new StackPanel(2, 0);
				rows.add(Ui.label("KILL ORDER", Theme.small(), Theme.GOLD_DIM));
				for (int i = 0; i < view.kills.size(); i++)
				{
					rows.add(new KillRow(i + 1, view.kills.get(i)));
				}
				card.add(rows, BorderLayout.CENTER);
			}
			card.accent(view.call != null ? Theme.of(view.call.prayer) : Theme.GOLD);
			stack.add(card);
		}

		if (view.pace != null)
		{
			stack.add(new SectionHeader("Prayer", view.paceLearned ? "your rate" : "first guess"));
			Card card = new Card(new BorderLayout(0, 6));
			PaceGauge gauge = new PaceGauge();
			gauge.show(view.pace);
			card.add(gauge, BorderLayout.CENTER);
			card.accent(Theme.of(view.pace.getState()));
			stack.add(card);
		}

		if (view.rotation > 0 && wave < Waves.LAST)
		{
			stack.add(new SectionHeader("Coming up", Format.waves(Waves.JAD - wave) + " to Jad"));
			Card card = new Card(new BorderLayout());
			StackPanel rows = new StackPanel(2, 0);
			for (int w = wave; w <= Math.min(Waves.LAST, wave + AHEAD - 1); w++)
			{
				rows.add(new WaveRow(w, view.placements(view.rotation, w), w == wave ? WaveRow.Mark.NOW : w == wave + 1 ? WaveRow.Mark.NEXT : WaveRow.Mark.NONE, null));
			}
			card.add(rows, BorderLayout.CENTER);
			stack.add(card);
		}

		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		String vs = view.versusBest == null ? "no cape yet" : Format.delta(view.versusBest) + " vs best";
		tiles.add(new StatTile(Format.ticks(view.runTicks), "Run time", vs,
			view.versusBest == null ? Theme.GOLD : view.versusBest <= 0 ? Theme.GREEN : Theme.EMBER));
		tiles.add(new StatTile(view.prayer + "/" + view.prayerLevel, "Prayer", view.pace == null ? "" : "sip at " + view.pace.getDrinkBelow(), Theme.TEAL));
		stack.add(tiles);
		replace(pilot, stack);
	}

	/** The protection prayer to use: its name in the colour of what it answers, and whether it is on. */
	private static JComponent callBlock(View.Call call)
	{
		JPanel block = new JPanel(new BorderLayout(0, 1));
		block.setOpaque(false);
		java.awt.Color color = Theme.of(call.prayer);
		block.add(Ui.label(call.prayer.getName(), Theme.title(), color), BorderLayout.NORTH);
		List<String> names = new java.util.ArrayList<>();
		for (com.codex.fightcaves.core.Monster m : call.answered)
		{
			String name = m.getNickname().toLowerCase();
			if (!names.contains(name))
			{
				names.add(name);
			}
		}
		String text = (call.up ? "On. " : "Switch to it. ") + "Blocks the " + String.join(" and the ", names) + ".";
		if (call.also != null)
		{
			text += " " + call.also.getShortName() + " is hitting too: step to the gilt tile to lose it.";
		}
		block.add(Ui.wrapped(text, Theme.small(), call.up ? Theme.TEXT_MUTED : Theme.mix(Theme.TEXT_MUTED, color, 0.5), WIDTH - 40), BorderLayout.CENTER);
		return block;
	}

	private Card rotationCard()
	{
		Card card = new Card(new BorderLayout(0, 2));
		if (view.rotation > 0)
		{
			card.add(Ui.label("Rotation " + view.rotation, Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
			String text = view.clean ? "Read from the spawns. Every wave so far matched it." : "Read from the spawns. One wave disagreed, so keep an eye on the next spawns.";
			card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
			card.accent(view.clean ? Theme.TEAL : Theme.EMBER);
			return card;
		}
		card.add(Ui.label("Reading the rotation", Theme.title(), Theme.TEAL), BorderLayout.NORTH);
		String text;
		if (view.wave == 0 || view.candidates.size() == Rotations.COUNT)
		{
			text = "Fight as normal. The first spawns tell the pilot which of the 15 rotations this is.";
		}
		else
		{
			text = "Rotation " + join(view.candidates) + "." + (view.decidingWave > 0 ? " Wave " + view.decidingWave + " settles it." : "");
		}
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		card.accent(Theme.TEAL);
		return card;
	}

	private static String join(List<Integer> list)
	{
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < list.size(); i++)
		{
			if (i > 0)
			{
				b.append(i == list.size() - 1 ? " or " : ", ");
			}
			b.append(list.get(i));
		}
		return b.toString();
	}

	private void buildBriefing(StackPanel stack)
	{
		Logbook b = view.book;
		stack.add(note("At the cave mouth", "Step in and the pilot takes over: it reads your rotation from the first spawns, gilds the tile where rangers and magers cannot see you, and paces your prayer against the waves left."));
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(view.fullRunDoses > 0 ? String.valueOf(view.fullRunDoses) : "-", "Full run", view.paceLearned ? "doses, your rate" : "doses, a guess", Theme.TEAL));
		tiles.add(new StatTile(b.bestTicks > 0 ? Format.ticks(b.bestTicks) : "-", "Best cape", b.capes > 0 ? Format.waves(b.capes).replace("wave", "cape") : "none yet", Theme.GOLD));
		stack.add(tiles);
		if (!b.runs.isEmpty())
		{
			stack.add(new SectionHeader("Last run"));
			Card card = new Card(new BorderLayout());
			card.add(new RunRow(b.runs.get(b.runs.size() - 1)), BorderLayout.CENTER);
			stack.add(card);
		}
		stack.add(new SectionHeader("How it reads the cave"));
		stack.add(Ui.wrapped("Wave 1 narrows the 15 rotations to three and wave 2 nearly always settles it; rotations 7 and 8 wait for wave 4. Creatures are coloured by the prayer that answers them: ember melee, green range, violet magic.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
	}

	// The Waves page.

	private void rebuildWaves()
	{
		StackPanel stack = new StackPanel(8, 8);
		stack.add(new SectionHeader("Rotation", view.rotation > 0 ? "yours is " + view.rotation : null));
		RotationPicker picker = new RotationPicker(r ->
		{
			studyRotation = r;
			studyTouched = true;
			rebuildWaves();
		});
		picker.show(studyRotation, view.rotation, view.candidates);
		stack.add(picker);

		CaveChart chart = new CaveChart(160);
		int nextWave = Math.min(Waves.LAST, studyWave + 1);
		chart.show("WAVE " + studyWave, "ROTATION " + studyRotation, view.placements(studyRotation, studyWave),
			studyWave < Waves.LAST ? view.placements(studyRotation, nextWave) : null, null, false, null);
		stack.add(chart);
		stack.add(Ui.wrapped(Waves.describe(studyWave) + (studyWave < Waves.LAST ? ". Wave " + nextWave + " is outlined." : "."),
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));

		stack.add(new SectionHeader("Every wave", "click to chart"));
		Card card = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(1, 0);
		for (int w = 1; w <= Waves.LAST; w++)
		{
			final int wave = w;
			WaveRow.Mark mark = w == studyWave ? WaveRow.Mark.CHOSEN
				: view.state == View.State.IN_CAVE && studyRotation == view.rotation && w == view.wave ? WaveRow.Mark.NOW : WaveRow.Mark.NONE;
			rows.add(new WaveRow(w, view.placements(studyRotation, w), mark, () ->
			{
				studyWave = wave;
				studyTouched = true;
				rebuildWaves();
			}));
		}
		card.add(rows, BorderLayout.CENTER);
		stack.add(card);
		java.awt.Point keep = waves.getViewport().getViewPosition();
		replace(waves, stack);
		waves.getViewport().setViewPosition(keep);
	}

	// The Records page.

	private void rebuildRecords()
	{
		StackPanel stack = new StackPanel(8, 8);
		Logbook b = view.book;
		if (view.state == View.State.LOGGED_OUT)
		{
			stack.add(note("No records open", "Log in to see your runs."));
			replace(records, stack);
			return;
		}
		JPanel tiles = new JPanel(new GridLayout(2, 2, 6, 6));
		tiles.setOpaque(false);
		tiles.add(new StatTile(String.valueOf(b.capes), "Capes", b.attempts > 0 ? Math.round(100.0 * b.capes / b.attempts) + "% of runs" : "", Theme.GOLD_BRIGHT));
		tiles.add(new StatTile(String.valueOf(b.attempts), "Runs", "since installing", Theme.TEXT));
		tiles.add(new StatTile(b.bestTicks > 0 ? Format.ticks(b.bestTicks) : "-", "Best time", "caped runs", Theme.GREEN));
		tiles.add(new StatTile(b.furthest > 0 ? String.valueOf(b.furthest) : "-", "Furthest", "wave reached", Theme.EMBER));
		stack.add(tiles);

		stack.add(new SectionHeader("Where runs end"));
		Card deaths = new Card(new BorderLayout());
		deaths.add(new DeathChart(b.deaths), BorderLayout.CENTER);
		stack.add(deaths);

		if (b.runs.isEmpty())
		{
			stack.add(Ui.wrapped("Finished runs are filed here: capes, falls and walk-outs, with rotation, time and doses drunk.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		}
		else
		{
			stack.add(new SectionHeader("Recent runs"));
			Card card = new Card(new BorderLayout());
			StackPanel rows = new StackPanel(4, 0);
			for (int i = b.runs.size() - 1; i >= 0 && i >= b.runs.size() - RECENT; i--)
			{
				rows.add(new RunRow(b.runs.get(i)));
			}
			card.add(rows, BorderLayout.CENTER);
			stack.add(card);
		}
		replace(records, stack);
	}
}
