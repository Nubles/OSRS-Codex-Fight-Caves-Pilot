package com.codex.fightcaves.ui;

import com.codex.fightcaves.core.Banked;
import com.codex.fightcaves.core.Damage;
import com.codex.fightcaves.core.Form;
import com.codex.fightcaves.core.Logbook;
import com.codex.fightcaves.core.Outlook;
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
	/** Kept across rebuilds so the rehearsal plays on instead of restarting each time the panel redraws. */
	private final RehearsalChart rehearsalChart = new RehearsalChart(196);
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

	/** Holds the rehearsal on one moment, in ticks, for previews and tests. */
	public void freezeRehearsal(double tick)
	{
		rehearsalChart.freeze(tick);
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

		if (view.quiet())
		{
			quiet(pilot);
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
		if (view.rehearsal != null && view.floor != null)
		{
			// The next wave played out on the real floor before it spawns.
			rehearsalChart.show("REHEARSING WAVE " + Math.min(Waves.LAST, wave + 1), view.floor, view.rehearsal, view.youTile);
			stack.add(rehearsalChart);
			stack.add(Ui.wrapped(view.rehearsal.headline(), Theme.small(),
				view.rehearsal.firstHit() < 0 ? Theme.TEAL : Theme.TEXT_MUTED, WIDTH - 16));
		}
		else
		{
			CaveChart chart = new CaveChart(170);
			String caption = view.wave == 0 ? "WAITING FOR WAVE 1" : next ? "NEXT  WAVE " + shown : "WAVE " + shown;
			chart.show(caption, view.rotation > 0 ? "ROTATION " + view.rotation : "ROTATION ?", bright, faint,
				view.stand == null ? null : new double[]{view.stand.fx, view.stand.fy}, next, view.you);
			stack.add(chart);
		}

		stack.add(rotationCard());

		if (view.stand != null)
		{
			stack.add(new SectionHeader("Where to stand", next ? "for wave " + shown : "now"));
			Card card = new Card(new BorderLayout(0, 4));
			StackPanel rows = new StackPanel(2, 0);
			if (view.compare != null)
			{
				// Where you are against the gilt tile, creature by creature.
				rows.add(new CompareRow(null));
				for (com.codex.fightcaves.core.Compare.Row r : view.compare.getRows())
				{
					rows.add(new CompareRow(r));
				}
			}
			else
			{
				for (View.Fate f : view.stand.fates)
				{
					rows.add(new FateRow(f));
				}
			}
			card.add(rows, BorderLayout.CENTER);
			String foot = view.stand.safe ? "Every ranger and mager is stuck from here" : "The gilt tile is the safest within reach";
			if (view.stand.steps > 0)
			{
				foot += ", " + view.stand.steps + (view.stand.steps == 1 ? " step away" : " steps away");
			}
			StackPanel foots = new StackPanel(3, 0);
			if (view.compare != null)
			{
				String why = next ? "When wave " + shown + " spawns: " + lowerFirst(view.compare.getHeadline()) : view.compare.getHeadline();
				foots.add(Ui.wrapped(why, Theme.small(), Theme.mix(Theme.TEXT_MUTED, Theme.EMBER, 0.35), WIDTH - 40));
			}
			foots.add(Ui.wrapped(foot + ".", Theme.small(), view.stand.safe ? Theme.TEAL : Theme.TEXT_FAINT, WIDTH - 40));
			card.add(foots, BorderLayout.SOUTH);
			card.accent(view.stand.safe ? Theme.TEAL : Theme.GOLD);
			stack.add(card);
		}

		if (view.call != null || !view.kills.isEmpty() || view.danger > 0)
		{
			stack.add(new SectionHeader("The fight", "from where you stand"));
			Card card = new Card(new BorderLayout(0, 6));
			StackPanel top = new StackPanel(4, 0);
			if (view.danger > 0)
			{
				top.add(dangerBlock(view.danger));
			}
			if (view.call != null)
			{
				top.add(callBlock(view.call));
			}
			card.add(top, BorderLayout.NORTH);
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
			card.accent(view.danger > 0 ? Theme.BLOOD : view.call != null ? Theme.of(view.call.prayer) : Theme.GOLD);
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
			if (view.jadAt != null)
			{
				card.add(Ui.wrapped("At your usual pace, Jad at about " + Format.ticks(view.jadAt) + " on the run clock.",
					Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
			}
			stack.add(card);
		}

		if (view.rotation > 0 && view.outlook != null && wave < Waves.JAD)
		{
			buildRoad(stack, wave);
		}

		if (!view.recentSplits.isEmpty())
		{
			boolean racing = view.recentSplits.get(0)[2] != Integer.MIN_VALUE;
			stack.add(new SectionHeader("Splits", racing ? "against your best cape" : "this run"));
			Card card = new Card(new BorderLayout());
			StackPanel rows = new StackPanel(1, 0);
			rows.add(racing ? SplitRow.header("", "TIME", "CAPE", "") : SplitRow.header("", "TIME"));
			for (int[] split : view.recentSplits)
			{
				String label = "Wave " + split[0];
				if (racing)
				{
					rows.add(new SplitRow(label, new String[]{Format.ticks(split[1]), Format.ticks(split[1] - split[2])}, split[2], Theme.TEXT));
				}
				else
				{
					rows.add(SplitRow.plain(label, Theme.TEXT, Format.ticks(split[1])));
				}
			}
			card.add(rows, BorderLayout.CENTER);
			stack.add(card);
		}

		JPanel tiles = new JPanel(new GridLayout(1, 2, 6, 0));
		tiles.setOpaque(false);
		String vs = view.partial ? "picked up part way" : view.versusBest == null ? "no cape yet" : Format.delta(view.versusBest) + " vs best";
		tiles.add(new StatTile(Format.ticks(view.runTicks), "Run time", vs,
			view.versusBest == null ? Theme.GOLD : view.versusBest <= 0 ? Theme.GREEN : Theme.EMBER));
		tiles.add(new StatTile(view.prayer + "/" + view.prayerLevel, "Prayer", view.pace == null ? "" : "sip at " + view.pace.getDrinkBelow(), Theme.TEAL));
		stack.add(tiles);
		replace(pilot, stack);
	}

	/** The road to Jad: every wave of the rotation rated, and a line on the next hard one. */
	private void buildRoad(StackPanel stack, int wave)
	{
		String codes = view.outlook;
		int splits = Outlook.counted(codes, wave, Outlook.Rating.SPLIT);
		int prays = Outlook.counted(codes, wave, Outlook.Rating.PRAY);
		stack.add(new SectionHeader("Road to Jad", splits + " split, " + prays + " pray ahead"));
		Card card = new Card(new BorderLayout(0, 6));
		card.add(new WaveGrid(codes, wave, 0, null), BorderLayout.NORTH);
		StackPanel lines = new StackPanel(3, 0);
		lines.add(Ui.wrapped(roadLine(codes, wave, view.outlookReason), Theme.small(),
			splits > 0 ? Theme.mix(Theme.TEXT_MUTED, Theme.EMBER, 0.35) : Theme.TEAL, WIDTH - 40));
		lines.add(Ui.wrapped("Teal is clean: rangers and magers stick. Gold is pray: one style reaches you. Ember is split: two do.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 40));
		card.add(lines, BorderLayout.CENTER);
		card.accent(splits > 0 ? Theme.EMBER : Theme.TEAL);
		stack.add(card);
	}

	/** "Wave 46 is the next split, in 3 waves. The mager sees you..." */
	static String roadLine(String codes, int wave, String reason)
	{
		int next = Outlook.nextAtLeast(codes, wave, Outlook.Rating.SPLIT);
		if (next == 0)
		{
			int pray = Outlook.nextAtLeast(codes, wave, Outlook.Rating.PRAY);
			if (pray == 0)
			{
				return codes != null && codes.indexOf(Outlook.UNKNOWN) >= 0 && codes.indexOf(Outlook.UNKNOWN) < Waves.JAD - 1
					? "Rating the waves ahead." : "Every wave left before Jad is clean.";
			}
			return "No split wave left before Jad. Next to pray through: wave " + pray + "." + (reason == null ? "" : " " + reason);
		}
		int in = next - wave;
		String when = in == 1 ? "the next wave" : "in " + in + " waves";
		return "Wave " + next + " is the next split, " + when + "." + (reason == null ? " No tile in reach keeps both styles off you." : " " + reason);
	}

	/** The protection prayer to use: its name in the colour of what it answers, and whether it is on. */
	/** "Eat now": what could land at once past your prayer is enough to finish you. */
	private static JComponent dangerBlock(int danger)
	{
		StackPanel block = new StackPanel(1, 0);
		block.add(Ui.label("EAT NOW", Theme.title(), Theme.BLOOD));
		block.add(Ui.wrapped("Up to " + danger + " can land at once past your prayer, enough to finish you.", Theme.small(), Theme.TEXT_MUTED, WIDTH - 40));
		return block;
	}

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
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		card.accent(Theme.TEAL);
		return card;
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
		stack.add(note("At the cave mouth", "Step in and the pilot takes over: it reads your rotation from the first spawns, gilds the tile where rangers and magers cannot see you, and paces your prayer against the waves left."));
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
		}
		stack.add(new SectionHeader("How it reads the cave"));
		stack.add(Ui.wrapped("Wave 1 narrows the 15 rotations to three and wave 2 nearly always settles it; rotations 7 and 8 wait for wave 4. Creatures are coloured by the prayer that answers them: ember melee, green range, violet magic.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
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

		String codes = studyRotation == view.rotation && view.outlook != null ? view.outlook
			: Outlook.saved(view.book.outlooks, studyRotation, 0);
		int now = view.state == View.State.IN_CAVE && studyRotation == view.rotation ? view.wave : 0;
		stack.add(new SectionHeader("Road to Jad", "click to chart"));
		stack.add(new WaveGrid(codes, now, studyWave, w ->
		{
			studyWave = w;
			studyTouched = true;
			rebuildWaves();
		}));
		if (codes == null)
		{
			stack.add(Ui.wrapped("Each wave is rated once you have fought this rotation with the road to Jad on.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
		}

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
		tiles.add(new StatTile(Format.count(b.attempts), "Runs", "since installing", Theme.TEXT));
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
}
