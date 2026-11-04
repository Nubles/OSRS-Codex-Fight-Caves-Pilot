package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Banked;
import com.codex.fightcaves.core.Damage;
import com.codex.fightcaves.core.Form;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Placement;
import com.codex.fightcaves.core.PrayerPacer;
import com.codex.fightcaves.core.Ready;
import com.codex.fightcaves.core.RotationRecord;
import com.codex.fightcaves.core.Rotations;
import com.codex.fightcaves.core.RunStory;
import com.codex.fightcaves.core.Splits;
import com.codex.fightcaves.core.Waves;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The pilot's side panel: the crest, then three pages. Pilot is the run in progress: the
 * rotation, the run against its ghost, the supplies each wave took, prayer against the waves left
 * and what comes next. Waves is every wave of every rotation, to study before going in. Records is
 * how your runs have gone, with the report on each against its ghost. Nothing on it says how to
 * fight: no tile, no prayer to use, no attack timing.
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
	/** The run whose splits the Records page sets against your best cape, by when it ended; 0 for the latest. */
	private long comparedRun;
	/** Whether the panel is open in the sidebar; the plugin skips rebuilding it while it is not. */
	private volatile boolean open;
	private Runnable onOpen;
	/** Set by the plugin: true while Jad's wave is on, so the panel opens on its stepped-back card. */
	private volatile boolean jadWave;
	/** The crest's line on Jad's wave; the card below says the rest. */
	static final String STEPPED_BACK = "The pilot has stepped back";

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

	/** Called when the panel is opened, so the plugin can bring it up to date. */
	public void onOpen(Runnable action)
	{
		onOpen = action;
	}

	/** True while the panel is open in the sidebar. Safe to read from any thread. */
	public boolean isOpen()
	{
		return open || isShowing();
	}

	/** Tells the panel whether Jad's wave is on. Safe to call from any thread. */
	public void jadWave(boolean on)
	{
		jadWave = on;
	}

	@Override
	public void onActivate()
	{
		open = true;
		if (jadWave)
		{
			// Opened on Jad's wave: what it drew when last open must not show, not even until the
			// plugin's fresh view arrives, so every page steps back before the panel is painted.
			stepBack();
		}
		Runnable r = onOpen;
		if (r != null)
		{
			r.run();
		}
	}

	@Override
	public void onDeactivate()
	{
		open = false;
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		boolean rotationChanged = view.rotation != this.view.rotation;
		this.view = view;
		if (!studyTouched || rotationChanged && view.rotation > 0)
		{
			studyRotation = view.rotation > 0 ? view.rotation : studyRotation;
			// Jad's wave is not charted for you while you fight it.
			studyWave = view.wave > 0 && !view.quiet() ? view.wave : studyWave;
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
				crest.update("TZHAAR FIGHT CAVE", b.capes > 0 ? Format.count(b.capes) : "63", b.capes > 0 ? (b.capes == 1 ? "cape" : "capes") : "waves",
					status, Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.3), Theme.GOLD, 0);
				return;
			default:
				break;
		}
		String figure = view.wave > 0 ? String.valueOf(view.wave) : "-";
		String status;
		java.awt.Color color = Theme.mix(Theme.TEXT_MUTED, Theme.GOLD, 0.35);
		if (view.quiet())
		{
			stepBackCrest();
			return;
		}
		if (view.wave == Waves.JAD)
		{
			status = STEPPED_BACK;
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
		crest.update(view.rotation > 0 ? "ROTATION " + view.rotation : "TZHAAR FIGHT CAVE", figure, view.wave == Waves.LAST ? LAST_WAVE : "/ " + Waves.LAST, status, color,
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

	/**
	 * Jad's wave is yours alone: every page holds just this note until it ends, so there is
	 * nothing on the panel to read or follow, not even a chart of where he stands.
	 */
	private void stepBack()
	{
		stepBackCrest();
		quiet(pilot);
		quiet(waves);
		quiet(records);
	}

	/** The crest's word beside 63 on the last wave. */
	static final String LAST_WAVE = "final wave";

	/** The crest on Jad's wave: the wave alone, not even the rotation. */
	private void stepBackCrest()
	{
		// "63 / 63" would say the same figure twice: the last wave is named instead.
		crest.update("TZHAAR FIGHT CAVE", String.valueOf(Waves.JAD), LAST_WAVE, STEPPED_BACK, Theme.BLOOD, Theme.BLOOD, 1.0);
	}

	/** True when a page holds only the stepped-back card, for tests. */
	/** The Records page, for tests. */
	JScrollPane recordsPage()
	{
		return records;
	}

	boolean steppedBack(int tab)
	{
		JScrollPane page = tab == 0 ? pilot : tab == 1 ? waves : records;
		java.awt.Component c = page.getViewport().getView();
		return c != null && QUIET.equals(c.getName());
	}

	private static final String QUIET = "quiet";

	private static void quiet(JScrollPane page)
	{
		StackPanel stack = new StackPanel(8, 8);
		stack.setName(QUIET);
		Card card = note("TzTok-Jad", "Nothing is shown on this wave. Watch him, not the panel. Your run is filed the moment the wave ends.");
		card.accent(Theme.BLOOD);
		stack.add(card);
		replace(page, stack);
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
			stack.add(note("The cave is dark", "Log in and Codex Fight Caves Pilot reads your rotation from the first spawns, races your best run wave by wave and counts what each wave costs you."));
			replace(pilot, stack);
			return;
		}
		if (view.state == View.State.OUTSIDE)
		{
			buildBriefing(stack);
			replace(pilot, stack);
			return;
		}

		if (view.quiet())
		{
			quiet(pilot);
			return;
		}

		int wave = Math.max(1, view.wave);
		stack.add(rotationCard());

		if (view.wave > 1)
		{
			buildGhost(stack, wave);
		}
		buildSupplies(stack, wave);

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
			// What each wave holds, as any rotation table says; where it spawns is left to the Waves
			// page, so nothing during a run points at a spot on the floor.
			StackPanel rows = new StackPanel(2, 0);
			for (int w = wave; w <= Math.min(Waves.LAST, wave + AHEAD - 1); w++)
			{
				JPanel row = new JPanel(new BorderLayout(6, 0));
				row.setOpaque(false);
				java.awt.Color tone = w == wave ? Theme.GOLD_BRIGHT : Theme.TEXT_MUTED;
				JLabel number = Ui.label(String.valueOf(w), Theme.small(), w == wave ? Theme.GOLD_BRIGHT : Theme.GOLD_DIM);
				number.setPreferredSize(new java.awt.Dimension(18, number.getPreferredSize().height));
				row.add(number, BorderLayout.WEST);
				row.add(Ui.label(w == Waves.JAD ? "TzTok-Jad" : Waves.describe(w), Theme.small(), tone), BorderLayout.CENTER);
				rows.add(row);
			}
			card.add(rows, BorderLayout.CENTER);
			if (view.jadAt != null)
			{
				card.add(Ui.wrapped("At your usual pace, Jad at about " + Format.ticks(view.jadAt) + " on the run clock.",
					Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
			}
			stack.add(card);
		}

		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		String vs = view.partial ? "picked up part way" : view.versusBest == null ? "no ghost yet" : Format.delta(view.versusBest) + " on ghost";
		tiles.add(new StatTile(Format.ticks(view.runTicks), "Run time", vs,
			view.versusBest == null ? Theme.GOLD : view.versusBest <= 0 ? Theme.GREEN : Theme.EMBER));
		tiles.add(new StatTile(view.prayer + "/" + view.prayerLevel, "Prayer", view.pace == null ? "" : Format.doses(view.pace.getDoses()) + " carried", Theme.TEAL));
		stack.add(tiles);
		replace(pilot, stack);
	}

	/**
	 * The run against its ghost: where it stands at the end of the last wave cleared, the whole
	 * run so far as a line about the ghost's, and the last few waves set against the same waves of
	 * the ghost. Times and nothing else: it never says how to fight a wave.
	 */
	private void buildGhost(StackPanel stack, int wave)
	{
		List<com.codex.fightcaves.core.Ghost.Split> splits = view.ghostSplits;
		if (splits.isEmpty())
		{
			if (!view.partial)
			{
				stack.add(new SectionHeader("Wave ghost"));
				stack.add(Ui.wrapped("Your first run timed from wave 1 becomes the ghost to race; a cape takes its place once you have one.",
					Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
			}
			return;
		}
		stack.add(new SectionHeader("Wave ghost", view.ghostCape ? "your best cape" : "your furthest run"));
		Card card = new Card(new BorderLayout(0, 4));
		Integer behind = view.versusBest;
		StackPanel top = new StackPanel(2, 0);
		if (behind != null)
		{
			java.awt.Color tone = behind <= 0 ? Theme.GREEN : Theme.EMBER;
			top.add(Ui.label(Format.delta(behind) + (behind <= 0 ? " ahead" : " behind"), Theme.title(), tone));
			top.add(Ui.wrapped("Against the ghost at the end of wave " + (wave - 1) + ".", Theme.small(), Theme.TEXT_MUTED, WIDTH - 40));
		}
		else
		{
			top.add(Ui.label("Racing the ghost", Theme.title(), Theme.GOLD));
			top.add(Ui.wrapped(view.partial ? "This run was picked up part way, so its clock is not set against the ghost; each wave still is."
				: "The ghost fell before this wave, so only your own clock runs from here.", Theme.small(), Theme.TEXT_MUTED, WIDTH - 40));
		}
		card.add(top, BorderLayout.NORTH);
		card.add(new GhostChart(splits, null, 58), BorderLayout.CENTER);
		StackPanel rows = new StackPanel(1, 0);
		rows.add(SplitRow.header("", "TIME", "GHOST", ""));
		for (int i = splits.size() - 1; i >= 0 && i >= splits.size() - 3; i--)
		{
			com.codex.fightcaves.core.Ghost.Split sp = splits.get(i);
			rows.add(new SplitRow("Wave " + sp.wave, new String[]{sp.ticks > 0 ? Format.ticks(sp.ticks) : null,
				sp.ghostTicks > 0 ? Format.ticks(sp.ghostTicks) : null}, sp.lost(), Theme.TEXT));
		}
		card.add(rows, BorderLayout.SOUTH);
		card.accent(behind == null ? Theme.GOLD : behind <= 0 ? Theme.GREEN : Theme.EMBER);
		stack.add(card);
	}

	/** Supplies this run and on the wave on now, beside the ghost's on the same wave. */
	private void buildSupplies(StackPanel stack, int wave)
	{
		stack.add(new SectionHeader("Supplies", "this run"));
		Card card = new Card(new BorderLayout(0, 2));
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(Format.count(view.runDoses), view.runDoses == 1 ? "Dose" : "Doses", "drunk this run", Theme.TEAL));
		tiles.add(new StatTile(Format.count(view.runFood), "Food", "eaten this run", Theme.GOLD));
		card.add(tiles, BorderLayout.NORTH);
		String line = "Wave " + wave + ": " + supplies(view.waveDoses, view.waveFood);
		if (view.ghostWaveDoses >= 0)
		{
			line += "; the ghost used " + supplies(view.ghostWaveDoses, view.ghostWaveFood) + " on it.";
		}
		else
		{
			line += ".";
		}
		card.add(Ui.wrapped(line, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		stack.add(card);
	}

	/** "2 doses and 1 food", "nothing". */
	static String supplies(int doses, int food)
	{
		if (doses == 0 && food == 0)
		{
			return "nothing";
		}
		String d = doses == 0 ? null : Format.doses(doses);
		String f = food == 0 ? null : Format.count(food) + " food";
		return d == null ? f : f == null ? d : d + " and " + f;
	}

	private Card rotationCard()
	{
		Card card = new Card(new BorderLayout(0, 2));
		if (view.rotation > 0 && view.fromClock)
		{
			card.add(Ui.label("Rotation " + view.rotation, Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
			String text = "From the clock as you went in, which has been right on your runs. The spawns have the last word"
				+ (view.decidingWave > 0 ? " and will confirm it by wave " + view.decidingWave + "." : ".");
			card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
			if (view.book != null)
			{
				card.add(Ui.wrapped(recordLine(RotationRecord.of(view.book.runs, view.rotation)), Theme.small(), Theme.GOLD_DIM, WIDTH - 40), BorderLayout.SOUTH);
			}
			card.accent(Theme.GOLD);
			return card;
		}
		if (view.rotation > 0)
		{
			card.add(Ui.label("Rotation " + view.rotation, Theme.title(), Theme.GOLD_BRIGHT), BorderLayout.NORTH);
			String text = view.clean ? "Read from the spawns. Every wave so far matched it." : "Read from the spawns. One wave disagreed, so keep an eye on the next spawns.";
			text += clockVerdict();
			card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
			if (view.book != null)
			{
				card.add(Ui.wrapped(recordLine(RotationRecord.of(view.book.runs, view.rotation)), Theme.small(), Theme.GOLD_DIM, WIDTH - 40), BorderLayout.SOUTH);
			}
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
		if (view.clockSaid > 0)
		{
			text += " The clock said " + (view.clockAlt > 0 ? view.clockSaid + " or " + view.clockAlt : String.valueOf(view.clockSaid))
				+ (view.clockTrust != null && view.clockTrust.trusted() ? "." : "; it is still earning trust on this account, so the spawns decide.");
		}
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		card.accent(Theme.TEAL);
		return card;
	}

	/** What the spawns made of the clock's call as you went in, or nothing with no call. */
	private String clockVerdict()
	{
		if (view.clockSaid <= 0 || view.clockHit == null)
		{
			return "";
		}
		if (view.clockHit)
		{
			return " The clock called it as you went in.";
		}
		return " The clock said " + (view.clockAlt > 0 ? view.clockSaid + " or " + view.clockAlt : String.valueOf(view.clockSaid))
			+ "; the spawns win, and the miss is counted against it.";
	}

	/** "Rotation 9 so far: 2 capes from 3 runs, best 45:12. Wave 46 ended 2 of them." */
	static String recordLine(RotationRecord r)
	{
		if (r.getRuns() == 0)
		{
			return "No run of yours on this rotation yet.";
		}
		String line;
		if (r.getRuns() == 1)
		{
			if (r.getCapes() == 1)
			{
				line = r.getBestTicks() > 0 ? "You caped it once before, in " + Format.ticks(r.getBestTicks()) + "." : "You caped it once before.";
			}
			else
			{
				line = r.getWall() > 0 ? "Your one run on it fell on wave " + r.getWall() + "." : "Your one run on it ended on wave " + r.getFurthest() + ".";
			}
			return line;
		}
		if (r.getCapes() > 0)
		{
			line = "Your runs on it: " + (r.getCapes() == 1 ? "1 cape" : r.getCapes() + " capes") + " from " + r.getRuns()
				+ (r.getBestTicks() > 0 ? ", best " + Format.ticks(r.getBestTicks()) : "") + ".";
		}
		else
		{
			line = "Your runs on it: " + r.getRuns() + ", furthest wave " + r.getFurthest() + ".";
		}
		if (r.getWallFalls() >= 2)
		{
			line += " Wave " + r.getWall() + " ended " + r.getWallFalls() + " of them.";
		}
		return line;
	}

	private static String lowerFirst(String s)
	{
		return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
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
		stack.add(note("At the cave mouth", "Step in and the pilot reads your rotation from the first spawns, races your best run's ghost wave by wave and counts the doses and food each wave takes. The fighting is yours."));
		if (view.clockOn)
		{
			buildClock(stack, b);
		}
		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		tiles.add(new StatTile(view.fullRunDoses > 0 ? String.valueOf(view.fullRunDoses) : "-", "Doses a run", view.paceLearned ? "at your rate" : "a first guess", Theme.TEAL));
		// Your capes and best cape are in the crest just above; the tile gives how often a run ends in one.
		tiles.add(new StatTile(b.attempts > 0 ? Form.percent(b.capes, b.attempts) + "%" : "-", "Cape rate",
			b.attempts > 0 ? "in " + Format.count(b.attempts) + (b.attempts == 1 ? " run" : " runs") : "no runs yet", Theme.GOLD));
		stack.add(tiles);
		if (view.jadAt != null)
		{
			stack.add(Ui.wrapped("At your usual pace a run reaches Jad in about " + Format.ticks(view.jadAt) + ".", Theme.small(), Theme.TEXT_MUTED, WIDTH - 16));
		}
		if (view.carried != null)
		{
			stack.add(supplyCard(view.carried));
		}
		if (!view.ready.isEmpty())
		{
			stack.add(readyCard(view.ready));
		}
		stack.add(bankCard(view.banked, view.fullRunDoses, System.currentTimeMillis()));
		if (!b.runs.isEmpty())
		{
			// The scroll carries its own date, so the heading does not say it twice.
			stack.add(new SectionHeader("Last run"));
			Logbook.Run last = b.runs.get(b.runs.size() - 1);
			stack.add(new RunScroll(RunStory.of(b, last)));
			if (last.fall != null)
			{
				stack.add(fallCard(last));
			}
			Card report = reportCard(b, last);
			if (report != null)
			{
				stack.add(report);
			}
		}
		stack.add(new SectionHeader("How it reads the cave"));
		stack.add(Ui.wrapped("Wave 1 narrows the 15 rotations to three and wave 2 nearly always settles it; rotations 7 and 8 wait for wave 4. Creatures are coloured by the prayer that answers them: ember melee, green range, violet magic.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
	}

	/**
	 * The rotation clock: at the cave mouth, the dial with the rotation you would get now, your
	 * best rotations' waits and the next few; anywhere else outside, a word on where to see it.
	 * Beneath, how the clock has done against the spawns on this account.
	 */
	private void buildClock(StackPanel stack, Logbook b)
	{
		com.codex.fightcaves.core.ClockTrust trust = view.clockTrust;
		stack.add(new SectionHeader("Rotation clock", clockTag(trust)));
		Card card = new Card(new BorderLayout(0, 8));
		if (view.clockReading != null)
		{
			card.add(new ClockDial(view.clockReading, view.clockReadAt, view.picks, b.runs, view.clockFrozen), BorderLayout.CENTER);
		}
		else
		{
			card.add(Ui.label("Away from the cave", Theme.title(), Theme.GOLD), BorderLayout.NORTH);
			card.add(Ui.wrapped("At the cave mouth in Mor Ul Rek the pilot reads the game's clock and shows which rotation you would get if you went in, and when your best ones come up.",
				Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.CENTER);
		}
		if (trust != null)
		{
			Color words = trust.getLevel() == com.codex.fightcaves.core.ClockTrust.Level.DOUBTFUL ? Theme.EMBER : Theme.TEXT_MUTED;
			card.add(Ui.wrapped(trust.describe(), Theme.small(), words, WIDTH - 40), BorderLayout.SOUTH);
		}
		card.accent(clockColor(trust));
		if (view.clockReading != null)
		{
			card.highlighted(true);
		}
		stack.add(card);
	}

	/** The clock's record in a word or two, for its heading. */
	static String clockTag(com.codex.fightcaves.core.ClockTrust trust)
	{
		if (trust == null || trust.getChecks() == 0)
		{
			return "unproven";
		}
		return trust.getHits() + " of " + trust.getChecks() + " right";
	}

	private static Color clockColor(com.codex.fightcaves.core.ClockTrust trust)
	{
		if (trust == null)
		{
			return Theme.TEXT_MUTED;
		}
		switch (trust.getLevel())
		{
			case TRUSTED:
				return Theme.GREEN;
			case DOUBTFUL:
				return Theme.EMBER;
			case SHAKY:
				return Theme.GOLD;
			default:
				return Theme.TEXT_MUTED;
		}
	}

	/** The ready check: each line fine or not, under a title that says whether all are. */
	private static Card readyCard(List<Ready.Line> lines)
	{
		int warnings = 0;
		for (Ready.Line l : lines)
		{
			warnings += l.isOk() ? 0 : 1;
		}
		Card card = new Card(new BorderLayout(0, 4));
		Color tone = warnings == 0 ? Theme.TEAL : Theme.EMBER;
		card.add(Ui.label(warnings == 0 ? "Ready to go in" : warnings == 1 ? "One thing to check" : warnings + " things to check",
			Theme.title(), tone), BorderLayout.NORTH);
		StackPanel rows = new StackPanel(3, 0);
		for (Ready.Line l : lines)
		{
			rows.add(new CheckRow(l.isOk(), l.getText(), WIDTH - 40));
		}
		card.add(rows, BorderLayout.CENTER);
		card.accent(tone);
		return card;
	}

	/** How many runs the potions in your bank cover, or how to have them counted. */
	static Card bankCard(Banked banked, int perRun, long now)
	{
		Card card = new Card(new BorderLayout(0, 2));
		if (banked == null)
		{
			card.add(Ui.label("In the bank", Theme.title(), Theme.GOLD), BorderLayout.NORTH);
			card.add(Ui.wrapped("Open your bank once and the pilot counts how many runs your prayer potions and restores cover.",
				Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.CENTER);
			return card;
		}
		card.add(Ui.label(bankTitle(banked, perRun), Theme.title(), banked.runs(perRun) > 0 ? Theme.GOLD : Theme.EMBER), BorderLayout.NORTH);
		card.add(Ui.wrapped(bankText(banked, perRun, now), Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		return card;
	}

	static String bankTitle(Banked banked, int perRun)
	{
		long runs = banked.runs(perRun);
		if (banked.getDoses() == 0)
		{
			return "No prayer doses banked";
		}
		if (runs == 0)
		{
			return "Under a run in the bank";
		}
		return (runs == 1 ? "1 run" : Format.count(runs) + " runs") + " in the bank";
	}

	/** "120 doses in the bank, at about 24 a run. Counted 3 hours ago." */
	static String bankText(Banked banked, int perRun, long now)
	{
		String seen = banked.getSavedAt() <= 0 ? "" : (banked.getSource() == Banked.Source.BANK_STEWARD
			? " From Codex Bank Steward, " + Format.ago(banked.getSavedAt(), now) + "."
			: " Counted " + Format.ago(banked.getSavedAt(), now) + ".");
		if (banked.getSource() == Banked.Source.BANK_STEWARD && banked.getSavedAt() <= 0)
		{
			seen = " From Codex Bank Steward.";
		}
		String text = Format.doses(banked.getDoses()) + " of prayer potion, super restore and sanfew serum";
		if (perRun > 0)
		{
			text += ", against about " + perRun + " a run at your rate, Jad's share included.";
		}
		else
		{
			text += ".";
		}
		return text + seen;
	}

	/** What you carry against a whole run: enough, with how many spare, or how many short. */
	private static Card supplyCard(PrayerPacer.Pace p)
	{
		Card card = new Card(new BorderLayout(0, 2));
		card.add(Ui.label(supplyTitle(p), Theme.title(), Theme.of(p.getState())), BorderLayout.NORTH);
		card.add(Ui.wrapped(supplyText(p), Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		card.accent(Theme.of(p.getState()));
		return card;
	}

	static String supplyTitle(PrayerPacer.Pace p)
	{
		switch (p.getState())
		{
			case SHORT:
				return "Short for a full run";
			case TIGHT:
				return "Cutting it close";
			default:
				return "Enough for a full run";
		}
	}

	/** "You carry 24 doses; the waves and Jad take about 20, leaving 3 spare." */
	static String supplyText(PrayerPacer.Pace p)
	{
		double spare = p.getSpare();
		String text = "You carry " + Format.doses(p.getDoses()) + "; the waves and Jad take about " + p.getDosesNeeded();
		if (spare >= 1)
		{
			return text + ", leaving " + (long) Math.floor(spare) + " spare.";
		}
		if (spare > -1)
		{
			return text + ", with nothing to spare: one more potion gives room for a slow wave.";
		}
		return text + ", so bring about " + Format.doses((int) Math.ceil(-spare)) + " more.";
	}

	// The Waves page.

	private void rebuildWaves()
	{
		if (view.quiet())
		{
			quiet(waves);
			return;
		}
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
		if (view.state != View.State.LOGGED_OUT && view.book != null)
		{
			stack.add(Ui.wrapped(recordLine(RotationRecord.of(view.book.runs, studyRotation)), Theme.small(), Theme.GOLD_DIM, WIDTH - 16));
		}

		CaveChart chart = new CaveChart(160);
		int nextWave = Math.min(Waves.LAST, studyWave + 1);
		chart.show("WAVE " + studyWave, "ROTATION " + studyRotation, view.placements(studyRotation, studyWave),
			studyWave < Waves.LAST ? view.placements(studyRotation, nextWave) : null);
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
		if (view.quiet())
		{
			quiet(records);
			return;
		}
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
		// The crest above already gives your capes and, at the cave mouth, your best cape, so the
		// tiles say what it does not: how often a run ends in a cape and what a cape usually takes.
		int usual = Form.usualCape(b);
		int timed = Form.timedCapes(b);
		String usualNote = usual < 0 ? "no cape yet" : view.state != View.State.OUTSIDE && b.bestTicks > 0
			? "best " + Format.ticks(b.bestTicks) : timed == 1 ? "your one timed cape" : "middle cape";
		tiles.add(new StatTile(b.attempts > 0 ? Form.percent(b.capes, b.attempts) + "%" : "-", "Cape rate", b.attempts > 0 ? "of every run" : "no runs yet", Theme.GOLD_BRIGHT));
		tiles.add(new StatTile(b.attempts > 0 ? Format.count(b.attempts) : "-", "Runs", "since installing", Theme.TEXT));
		tiles.add(new StatTile(usual < 0 ? "-" : Format.ticks(usual), "Usual cape", usualNote, Theme.GREEN));
		tiles.add(new StatTile(b.furthest > 0 ? String.valueOf(b.furthest) : "-", "Furthest", "wave reached", Theme.EMBER));
		stack.add(tiles);

		Form form = Form.of(b);
		if (form != null)
		{
			stack.add(new SectionHeader("Form", "latest on the right"));
			Card card = new Card(new BorderLayout(0, 6));
			card.add(new FormStrip(form), BorderLayout.NORTH);
			card.add(Ui.wrapped(form.line(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 24), BorderLayout.CENTER);
			stack.add(card);
		}

		if (!b.runs.isEmpty())
		{
			Logbook.Run run = comparedRun(b);
			boolean latest = run == b.runs.get(b.runs.size() - 1);
			stack.add(new SectionHeader("The run", latest ? "latest" : "picked below"));
			stack.add(new RunScroll(RunStory.of(b, run)));
			buildFall(stack, b);
			Card report = reportCard(b, run);
			if (report != null)
			{
				stack.add(new SectionHeader("Run report", "against its ghost"));
				stack.add(report);
			}
			buildSplits(stack, b);
			buildHurt(stack, b);
		}

		int falls = 0;
		for (int d : b.deaths)
		{
			falls += d;
		}
		if (falls > 0)
		{
			// Only once a run has fallen: an empty chart says nothing.
			stack.add(new SectionHeader("Where runs end", falls == 1 ? "1 fall" : Format.count(falls) + " falls"));
			Card deaths = new Card(new BorderLayout());
			deaths.add(new DeathChart(b.deaths), BorderLayout.CENTER);
			stack.add(deaths);
		}

		if (b.runs.isEmpty())
		{
			stack.add(Ui.wrapped("Finished runs are filed here: capes, falls and walk-outs, with rotation, time and doses drunk, and for a fall before Jad, what ended it.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		}
		else
		{
			stack.add(new SectionHeader("Recent runs"));
			Card card = new Card(new BorderLayout());
			StackPanel rows = new StackPanel(4, 0);
			Logbook.Run compared = comparedRun(b);
			for (int i = b.runs.size() - 1; i >= 0 && i >= b.runs.size() - RECENT; i--)
			{
				Logbook.Run run = b.runs.get(i);
				final long ended = run.endedAt;
				rows.add(new RunRow(run, run == compared, () ->
				{
					comparedRun = pick(ended, b);
					rebuildRecords();
				}));
			}
			card.add(rows, BorderLayout.CENTER);
			stack.add(card);
		}
		java.awt.Point keep = records.getViewport().getViewPosition();
		replace(records, stack);
		records.getViewport().setViewPosition(keep);
	}

	/** Choosing the latest run again goes back to following whatever run is latest. */
	private static long pick(long ended, Logbook b)
	{
		return b.runs.get(b.runs.size() - 1).endedAt == ended ? 0 : ended;
	}

	/** The run the splits are set against: the one picked from the list, otherwise the latest. */
	private Logbook.Run comparedRun(Logbook b)
	{
		if (comparedRun != 0)
		{
			for (Logbook.Run r : b.runs)
			{
				if (r.endedAt == comparedRun)
				{
					return r;
				}
			}
		}
		return b.runs.isEmpty() ? null : b.runs.get(b.runs.size() - 1);
	}

	/** Splits by stretches of seven: your best cape, your best ever and the chosen run against the cape. */
	private void buildSplits(StackPanel stack, Logbook b)
	{
		Logbook.Run run = comparedRun(b);
		Splits splits = Splits.of(b.runs, run);
		boolean cape = splits.getPbTicks() > 0;
		boolean latest = run == b.runs.get(b.runs.size() - 1);
		// The scroll above already says where the run ended.
		stack.add(new SectionHeader("Splits", latest ? "last run" : "run of " + Format.day(run.endedAt)));
		Card card = new Card(new BorderLayout(0, 6));
		StackPanel rows = new StackPanel(1, 0);
		rows.add(cape ? SplitRow.header("WAVES", "CAPE", "BEST", "RUN") : SplitRow.header("WAVES", "BEST", "RUN"));
		for (Splits.Stretch s : splits.getStretches())
		{
			String best = s.getBest() > 0 ? Format.ticks(s.getBest()) : null;
			String ran = s.getRun() > 0 ? Format.ticks(s.getRun()) : null;
			boolean gold = s.getRun() > 0 && s.getRun() == s.getBest();
			java.awt.Color label = gold ? Theme.GOLD_BRIGHT : s.getRun() > 0 ? Theme.TEXT : Theme.TEXT_FAINT;
			if (cape)
			{
				rows.add(new SplitRow(s.label(), new String[]{Format.ticks(s.getPb()), best}, s.versusPb(), label));
			}
			else
			{
				rows.add(SplitRow.plain(s.label(), label, best, ran));
			}
		}
		card.add(rows, BorderLayout.CENTER);
		card.add(Ui.wrapped(splitsNote(splits, cape), Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
		card.accent(Theme.GOLD);
		stack.add(card);
	}

	/** Why the chosen run fell, when it fell before Jad: the cause in a sentence and the hitpoints trail. */
	private void buildFall(StackPanel stack, Logbook b)
	{
		Logbook.Run run = comparedRun(b);
		if (run == null || run.fall == null)
		{
			return;
		}
		stack.add(new SectionHeader("The fall", "why it ended"));
		stack.add(fallCard(run));
	}

	static Card fallCard(Logbook.Run run)
	{
		Card card = new Card(new BorderLayout(0, 6));
		if (run.fallHp != null && run.fallHp.size() > 1)
		{
			card.add(new FallTrail(run.fallHp), BorderLayout.NORTH);
		}
		card.add(Ui.wrapped(run.fall, Theme.small(), Theme.TEXT, WIDTH - 40), BorderLayout.CENTER);
		card.accent(Theme.BLOOD);
		return card;
	}

	/** Where the cave hurts you: damage per wave by stretch across your runs, with the chosen run marked. */
	private void buildHurt(StackPanel stack, Logbook b)
	{
		double[] average = Damage.perWaveByStretch(b.runs);
		if (Damage.worstStretch(average) < 0)
		{
			return;
		}
		Logbook.Run run = comparedRun(b);
		double[] mine = run == null ? null : Damage.perWaveByStretch(Collections.singletonList(run));
		int worst = Damage.worstStretch(average);
		stack.add(new SectionHeader("Where it hurts", "damage a wave"));
		Card card = new Card(new BorderLayout(0, 6));
		card.add(new HurtChart(average, mine, worst), BorderLayout.CENTER);
		int from = worst * Splits.STRETCH + 1;
		String label = from + "-" + (from + Splits.STRETCH - 1 >= Waves.JAD ? "Jad" : String.valueOf(from + Splits.STRETCH - 1));
		String note = "Waves " + label + " hurt most, about " + Math.round(average[worst]) + " damage a wave.";
		String summary = Damage.summary(run);
		if (!summary.isEmpty())
		{
			note += " This run: " + lowerFirst(summary);
		}
		card.add(Ui.wrapped(note + " The gold tick is the run picked below.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
		card.accent(Theme.EMBER);
		stack.add(card);
	}

	private static String splitsNote(Splits splits, boolean cape)
	{
		String gold = " A gilt stretch is your best ever.";
		if (!cape)
		{
			return splits.getSumOfBest() > 0 ? "Every wave at its best adds up to " + Format.ticks(splits.getSumOfBest()) + "." + gold
				: "Times per stretch fill in as runs get through them; with a cape the run is raced against it." + gold;
		}
		if (splits.onTheTable() <= 0)
		{
			return "Your best cape is your best on every wave." + gold;
		}
		String note = "Every wave at its best adds up to " + Format.ticks(splits.getSumOfBest()) + ", " + Format.ticks(splits.onTheTable())
			+ " under your best cape.";
		Splits.Stretch most = splits.mostToGain();
		if (most != null)
		{
			note += " Most to win back in waves " + most.label() + " (" + Format.ticks(most.toGain()) + ").";
		}
		return note;
	}

	/**
	 * The report on a run against its ghost (your best run other than it): the run as a line about
	 * the ghost's, the waves where it lost the most time and the most supplies, and the totals.
	 * Null when there is no ghost to set it against or the run cleared no wave the ghost did.
	 */
	static Card reportCard(Logbook b, Logbook.Run run)
	{
		com.codex.fightcaves.core.Ghost ghost = com.codex.fightcaves.core.Ghost.of(b.runs, run);
		if (ghost == null || run == null)
		{
			return null;
		}
		com.codex.fightcaves.core.Ghost.Report r = ghost.report(run);
		if (r.getThrough() == 0)
		{
			return null;
		}
		Card card = new Card(new BorderLayout(0, 4));
		StackPanel top = new StackPanel(2, 0);
		Integer behind = r.getBehind();
		java.awt.Color tone = behind == null ? Theme.GOLD : behind <= 0 ? Theme.GREEN : Theme.EMBER;
		String through = r.getThrough() == Waves.JAD ? "Jad" : "wave " + r.getThrough();
		top.add(Ui.label(behind == null ? "Against the ghost" : Format.delta(behind) + (behind <= 0 ? " ahead" : " behind"), Theme.title(), tone));
		top.add(Ui.wrapped((behind == null ? "Wave by wave against " : "On the clock at " + through + ", against ")
			+ (ghost.isCape() ? "your best cape" : "your furthest run") + ".", Theme.small(), Theme.TEXT_MUTED, WIDTH - 40));
		card.add(top, BorderLayout.NORTH);
		card.add(new GhostChart(r.getSplits(), r.getSlowest(), 64), BorderLayout.CENTER);
		StackPanel lines = new StackPanel(3, 0);
		lines.add(Ui.wrapped(slowLine(r.getSlowest()), Theme.small(), r.getSlowest().isEmpty() ? Theme.GREEN : Theme.mix(Theme.TEXT, Theme.EMBER, 0.4), WIDTH - 40));
		lines.add(Ui.wrapped(hungryLine(r.getHungriest()), Theme.small(), r.getHungriest().isEmpty() ? Theme.GREEN : Theme.TEXT_MUTED, WIDTH - 40));
		lines.add(Ui.wrapped("Through " + through + ": " + supplies(r.getDoses(), r.getFood()) + " used, the ghost "
			+ supplies(r.getGhostDoses(), r.getGhostFood()) + ".", Theme.small(), Theme.TEXT_FAINT, WIDTH - 40));
		card.add(lines, BorderLayout.SOUTH);
		card.accent(tone);
		return card;
	}

	/** "Most time lost: wave 46 (+0:38), wave 52 (+0:21) and wave 38 (+0:12)." */
	static String slowLine(List<com.codex.fightcaves.core.Ghost.Split> slow)
	{
		if (slow.isEmpty())
		{
			return "No wave slower than the ghost's.";
		}
		List<String> parts = new java.util.ArrayList<>();
		for (com.codex.fightcaves.core.Ghost.Split s : slow)
		{
			parts.add("wave " + s.wave + " (" + Format.delta(s.lost()) + ")");
		}
		return "Most time lost: " + list(parts) + ".";
	}

	/** "Most supplies: wave 61 (3 doses and 1 food, the ghost 1 dose), wave 52 (+2) and wave 40 (+1)." */
	static String hungryLine(List<com.codex.fightcaves.core.Ghost.Split> hungry)
	{
		if (hungry.isEmpty())
		{
			return "No wave took more supplies than the ghost's.";
		}
		List<String> parts = new java.util.ArrayList<>();
		for (com.codex.fightcaves.core.Ghost.Split s : hungry)
		{
			// The worst in full, the rest by how far past the ghost they went.
			parts.add(parts.isEmpty() ? "wave " + s.wave + " (" + supplies(s.doses, s.food) + ", the ghost " + supplies(s.ghostDoses, s.ghostFood) + ")"
				: "wave " + s.wave + " (+" + s.overSupplies() + ")");
		}
		return "Most supplies: " + list(parts) + ".";
	}

	private static String list(List<String> parts)
	{
		if (parts.size() == 1)
		{
			return parts.get(0);
		}
		return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
	}
}
