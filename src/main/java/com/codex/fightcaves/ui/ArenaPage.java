package com.codex.fightcaves.ui;

import com.codex.fightcaves.arena.Arena;
import com.codex.fightcaves.arena.ArenaLog;
import com.codex.fightcaves.arena.ArenaStats;
import com.codex.fightcaves.arena.ColosseumLoot;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The Arenas page: Inferno and Colosseum run logs. Every attempt with how far it got and how it
 * ended, your own death rate by wave, the ghost of your best run and the last attempt set against
 * it. In the Colosseum, between waves only, a plain estimate of taking the chest against going on.
 * During a wave the panel holds only a plain clock (see {@link #clockCard}).
 */
final class ArenaPage
{
	static final int WIDTH = PilotPanel.WIDTH;
	private static final int RECENT = 8;

	private ArenaPage()
	{
	}

	/** The arena the page opens on: where you stand, else the one played last. */
	static Arena preferred(View view)
	{
		if (view.arenaHere != null)
		{
			return view.arenaHere;
		}
		ArenaLog log = view.arenas;
		if (log == null)
		{
			return Arena.INFERNO;
		}
		long inf = last(log.inferno);
		long col = last(log.colosseum);
		return col > inf ? Arena.COLOSSEUM : Arena.INFERNO;
	}

	private static long last(ArenaLog.Book b)
	{
		if (b.active != null)
		{
			return Long.MAX_VALUE;
		}
		return b.runs.isEmpty() ? 0 : b.runs.get(b.runs.size() - 1).endedAt;
	}

	static StackPanel build(View view, Arena arena, IntConsumer pick)
	{
		StackPanel stack = new StackPanel(8, 8);
		if (view.state == View.State.LOGGED_OUT)
		{
			stack.add(note("No logs open", "Log in to see your Inferno and Colosseum runs."));
			return stack;
		}
		if (view.arenas == null)
		{
			stack.add(note("Arena logs are off", "Turn on Inferno and Colosseum logs in the settings to keep every attempt's splits."));
			return stack;
		}
		Segmented picker = new Segmented(false, Arena.INFERNO.getShortName(), Arena.COLOSSEUM.getShortName());
		picker.setSelected(arena.ordinal(), false);
		picker.onChange(pick);
		stack.add(picker);

		ArenaLog.Book b = view.arenas.book(arena);
		List<ArenaLog.Run> runs = b.runs;

		if (arena == Arena.COLOSSEUM && view.arenaHere == Arena.COLOSSEUM && view.arenaBetween > 0 && b.active != null)
		{
			stack.add(new SectionHeader("Between waves", "wave " + view.arenaBetween + " cleared"));
			stack.add(betweenCard(b.active, view.arenaBetween));
			if (view.lootNote)
			{
				stack.add(lootCard(ColosseumLoot.estimate(view.arenaBetween, runs)));
			}
		}
		else if (b.active != null && b.paused)
		{
			stack.add(note("Run paused", "Paused after wave " + b.active.cleared + ". The run picks up at wave " + (b.active.cleared + 1) + " when you go back in."));
		}

		int completions = b.completions();
		JPanel tiles = new JPanel(new GridLayout(2, 2, 6, 6));
		tiles.setOpaque(false);
		ArenaLog.Run best = ArenaStats.best(arena, runs);
		tiles.add(new StatTile(Format.count(runs.size()), "Attempts", runs.isEmpty() ? "none yet" : "since installing", Theme.TEXT));
		tiles.add(new StatTile(Format.count(completions), "Completions", runs.isEmpty() ? "-" : percent(completions, runs.size()) + "% of attempts", Theme.GOLD_BRIGHT));
		tiles.add(new StatTile(b.furthest() > 0 ? String.valueOf(b.furthest()) : "-", "Furthest", "of " + arena.getWaves() + " waves", Theme.EMBER));
		tiles.add(new StatTile(best == null ? "-" : Format.ticks(best.clockAt(arena.getWaves())), "Best run", best == null ? "no timed completion" : Format.day(best.endedAt), Theme.GREEN));
		stack.add(tiles);

		if (runs.isEmpty())
		{
			stack.add(Ui.wrapped(emptyText(arena), Theme.small(), Theme.TEXT_FAINT, WIDTH - 16));
			if (arena == Arena.COLOSSEUM)
			{
				stack.add(new SectionHeader("Chest values", "wiki averages"));
				stack.add(chestCard());
			}
			return stack;
		}

		ArenaStats.Ghost ghost = ArenaStats.ghost(arena, runs, null);
		stack.add(new SectionHeader("How far each got", runs.size() == 1 ? "1 attempt" : Format.count(runs.size()) + " attempts"));
		Card reach = new Card(new BorderLayout(0, 6));
		reach.add(new ReachChart(runs, arena.getWaves(), ghost == null ? 0 : ghost.run.reached), BorderLayout.CENTER);
		reach.add(Ui.wrapped(legend(arena), Theme.small(), Theme.TEXT_FAINT, WIDTH - 24), BorderLayout.SOUTH);
		stack.add(reach);

		ArenaStats.WaveRisk[] risks = ArenaStats.risks(arena, runs);
		int deaths = 0;
		for (int d : ArenaStats.deaths(arena, runs))
		{
			deaths += d;
		}
		stack.add(new SectionHeader("Your death rate by wave", deaths == 1 ? "1 death" : Format.count(deaths) + " deaths"));
		Card risk = new Card(new BorderLayout(0, 6));
		risk.add(new RiskChart(risks), BorderLayout.NORTH);
		risk.add(Ui.wrapped(riskText(risks), Theme.small(), Theme.TEXT_MUTED, WIDTH - 24), BorderLayout.CENTER);
		stack.add(risk);

		ArenaLog.Run latest = runs.get(runs.size() - 1);
		stack.add(new SectionHeader("Your ghost", ghost == null ? "none yet" : ghost.complete ? "best run" : "furthest run"));
		stack.add(ghostCard(arena, ghost));

		ArenaStats.Ghost against = ArenaStats.ghost(arena, runs, latest);
		if (against != null && latest.cleared > 0)
		{
			ArenaStats.Report report = ArenaStats.report(against, latest);
			if (report.behind != null)
			{
				stack.add(new SectionHeader("Last attempt", "against its ghost"));
				stack.add(reportCard(arena, latest, report));
			}
		}

		if (arena == Arena.COLOSSEUM)
		{
			stack.add(new SectionHeader("Chest values", "wiki averages"));
			stack.add(chestCard());
		}

		stack.add(new SectionHeader("Recent attempts"));
		Card list = new Card(new BorderLayout());
		StackPanel rows = new StackPanel(4, 0);
		for (int i = runs.size() - 1; i >= 0 && i >= runs.size() - RECENT; i--)
		{
			rows.add(row(arena, runs.get(i)));
		}
		list.add(rows, BorderLayout.CENTER);
		stack.add(list);
		return stack;
	}

	/**
	 * What every page holds while an Inferno or Colosseum wave is on: the wave and a plain run
	 * clock, nothing else. No ghost, no rate, no estimate: the wave is yours.
	 */
	static StackPanel clockCard(View view)
	{
		StackPanel stack = new StackPanel(8, 8);
		Arena arena = view.arenaHere;
		ArenaLog.Run r = view.arenas.book(arena).active;
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.label(arena.getTitle(), Theme.title(), Theme.GOLD), BorderLayout.NORTH);
		String wave = r == null ? "" : "Wave " + Math.max(r.reached, 1) + " of " + arena.getWaves();
		String clock = r == null ? "" : Format.ticks(ArenaStats.clock(arena, r));
		card.add(Ui.label(wave + (clock.isEmpty() ? "" : "  ·  " + clock), Theme.body(), Theme.TEXT), BorderLayout.CENTER);
		card.add(Ui.wrapped("Nothing else shows during a wave. The run is filed when it ends.", Theme.small(), Theme.TEXT_FAINT, WIDTH - 40), BorderLayout.SOUTH);
		stack.add(card);
		return stack;
	}

	private static Card betweenCard(ArenaLog.Run r, int cleared)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.add(Ui.label("Wave " + cleared + " of " + Arena.COLOSSEUM.getWaves() + " cleared", Theme.body(), Theme.TEXT), BorderLayout.NORTH);
		card.add(Ui.label("Run clock " + Format.ticks(ArenaStats.clock(Arena.COLOSSEUM, r)), Theme.small(), Theme.TEXT_MUTED), BorderLayout.CENTER);
		return card;
	}

	/** The take-or-go-on note, between waves only. */
	static Card lootCard(ColosseumLoot.Estimate e)
	{
		Card card = new Card(new BorderLayout(0, 6));
		card.accent(Theme.GOLD);
		card.add(Ui.label("Take the loot or go on?", Theme.title(), Theme.GOLD), BorderLayout.NORTH);
		StackPanel lines = new StackPanel(4, 0);
		for (String s : lootLines(e))
		{
			lines.add(Ui.wrapped(s, Theme.small(), Theme.TEXT, WIDTH - 40));
		}
		lines.add(Ui.wrapped("An estimate from your own runs and the wiki's average rewards, which are Grand Exchange prices and drift. It leaves out the odds of a unique and Glory. Information only.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 40));
		card.add(lines, BorderLayout.CENTER);
		return card;
	}

	/** The estimate's sentences, for the card and for tests. */
	static List<String> lootLines(ColosseumLoot.Estimate e)
	{
		List<String> out = new ArrayList<>();
		if (e == null)
		{
			out.add("The last wave is done: the chest is yours to take.");
			return out;
		}
		if (e.chest > 0)
		{
			out.add("Chest so far: about " + Format.count(e.chest) + " coins (the wiki's averages, waves 1-" + e.cleared + ").");
		}
		if (e.nextAverage > 0)
		{
			out.add("Wave " + e.next + " adds about " + Format.count(e.nextAverage) + " on average.");
		}
		switch (e.verdict)
		{
			case NO_VALUE:
				out.add("The wiki gives no average reward for wave " + (e.chest < 0 ? e.cleared : e.next) + ", so there is no estimate here."
					+ (e.next == Arena.COLOSSEUM.getWaves() ? " Clearing wave 12 guarantees Dizana's quiver." : ""));
				if (e.starts > 0)
				{
					out.add(survivedLine(e));
				}
				break;
			case TOO_FEW_RUNS:
				out.add("Your death rate on wave " + e.next + " is not known yet: " + (e.starts == 1 ? "1 of your runs" : e.starts + " of your runs")
					+ " began it, and " + ArenaStats.MIN_STARTS + " are needed.");
				break;
			default:
				out.add(survivedLine(e));
				out.add("Going on pays on average if you survive wave " + e.next + " more than " + pct(e.breakEven()) + " of the time.");
				if (e.verdict == ColosseumLoot.Verdict.GO_ON)
				{
					out.add("By your own runs, one more wave is worth about +" + Format.count(e.edge()) + " on average.");
				}
				else if (e.verdict == ColosseumLoot.Verdict.TAKE)
				{
					out.add("By your own runs, taking the chest now keeps about " + Format.count(-e.edge()) + " more on average.");
				}
				else
				{
					out.add("By your own runs it is too close to call.");
				}
				break;
		}
		return out;
	}

	private static String survivedLine(ColosseumLoot.Estimate e)
	{
		int lived = e.starts - e.deaths;
		return "You survived wave " + e.next + " in " + lived + " of " + e.starts + (e.starts == 1 ? " run" : " runs") + " (" + pct(e.survival()) + ").";
	}

	private static Card chestCard()
	{
		Card card = new Card(new BorderLayout(0, 6));
		JPanel grid = new JPanel(new GridLayout(0, 2, 8, 2));
		grid.setOpaque(false);
		for (int w = 1; w <= Arena.COLOSSEUM.getWaves(); w++)
		{
			long avg = ColosseumLoot.average(w);
			JLabel l = Ui.label("Wave " + w, Theme.small(), Theme.TEXT_MUTED);
			JLabel v = Ui.label(avg > 0 ? Format.count(avg) : "not given", Theme.small(), avg > 0 ? Theme.GOLD : Theme.TEXT_FAINT);
			v.setHorizontalAlignment(JLabel.RIGHT);
			grid.add(l);
			grid.add(v);
		}
		card.add(grid, BorderLayout.CENTER);
		card.add(Ui.wrapped("Average reward per wave, oldschool.runescape.wiki, Rewards Chest (Fortis Colosseum). Dying, teleporting or logging out in a wave loses the chest.",
			Theme.small(), Theme.TEXT_FAINT, WIDTH - 24), BorderLayout.SOUTH);
		return card;
	}

	private static Card ghostCard(Arena arena, ArenaStats.Ghost ghost)
	{
		if (ghost == null)
		{
			return note("No ghost yet", "Your first attempt timed from wave 1 becomes the ghost every later one is set against.");
		}
		String text = ghost.complete
			? "Your fastest whole run, " + Format.ticks(ghost.clockAt(arena.getWaves())) + " on " + Format.day(ghost.run.endedAt) + ". Every attempt is set against it wave by wave."
			: "Before your first completion, your furthest attempt: wave " + ghost.cleared() + " cleared in " + Format.ticks(ghost.clockAt(ghost.cleared()))
			+ " on " + Format.day(ghost.run.endedAt) + ".";
		Card card = new Card(new BorderLayout(0, 4));
		card.accent(Theme.GOLD_DIM);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		return card;
	}

	private static Card reportCard(Arena arena, ArenaLog.Run run, ArenaStats.Report r)
	{
		Card card = new Card(new BorderLayout(0, 6));
		card.add(new ArenaGhostChart(r.splits, r.slowest, arena.getWaves()), BorderLayout.NORTH);
		StringBuilder s = new StringBuilder();
		s.append(Format.ticks(Math.abs(r.behind))).append(r.behind > 0 ? " behind" : r.behind < 0 ? " ahead of" : " level with")
			.append(r.behind > 0 ? " the ghost" : r.behind < 0 ? " the ghost" : " the ghost").append(" at the end of wave ").append(r.through).append('.');
		if (!r.slowest.isEmpty())
		{
			s.append(" Most time lost: ");
			for (int i = 0; i < r.slowest.size(); i++)
			{
				ArenaStats.Split sp = r.slowest.get(i);
				s.append(i == 0 ? "" : i == r.slowest.size() - 1 ? " and " : ", ").append("wave ").append(sp.wave).append(" (").append(Format.delta(sp.lost())).append(')');
			}
			s.append('.');
		}
		card.add(Ui.wrapped(s.toString(), Theme.small(), Theme.TEXT_MUTED, WIDTH - 24), BorderLayout.CENTER);
		card.add(Ui.label(outcome(arena, run), Theme.small(), ReachChart.color(run.result)), BorderLayout.SOUTH);
		return card;
	}

	private static JPanel row(Arena arena, ArenaLog.Run r)
	{
		JPanel p = new JPanel(new BorderLayout(6, 0));
		p.setOpaque(false);
		p.setBorder(new EmptyBorder(1, 0, 1, 0));
		JLabel left = Ui.label(Format.day(r.endedAt) + "  " + outcome(arena, r), Theme.small(), ReachChart.color(r.result));
		int t = r.partial ? 0 : r.result == ArenaLog.Result.COMPLETE ? r.time() : r.clockAt(r.cleared);
		JLabel right = Ui.label(t > 0 ? Format.ticks(t) : "-", Theme.small(), Theme.TEXT_MUTED);
		p.add(left, BorderLayout.CENTER);
		p.add(right, BorderLayout.EAST);
		return p;
	}

	static String outcome(Arena arena, ArenaLog.Run r)
	{
		switch (r.result)
		{
			case COMPLETE:
				return arena == Arena.INFERNO ? "Infernal cape" : "Sol Heredit beaten";
			case DIED:
				return "Died on wave " + r.reached;
			case CLAIMED:
				return "Took the chest after " + r.cleared;
			default:
				return "Left on wave " + Math.max(1, r.reached);
		}
	}

	private static String legend(Arena arena)
	{
		return arena == Arena.INFERNO
			? "Gold: a cape. Ember: died. Faint: left. The dashed line is your ghost's reach."
			: "Gold: Sol beaten. Teal: took the chest. Ember: died. Faint: left. The dashed line is your ghost's reach.";
	}

	static String riskText(ArenaStats.WaveRisk[] risks)
	{
		List<ArenaStats.WaveRisk> worst = ArenaStats.deadliest(risks, 3);
		if (worst.isEmpty())
		{
			return "A wave's rate shows once " + ArenaStats.MIN_STARTS + " of your attempts have reached it. None has died on such a wave yet.";
		}
		StringBuilder s = new StringBuilder("Deadliest for you: ");
		for (int i = 0; i < worst.size(); i++)
		{
			ArenaStats.WaveRisk r = worst.get(i);
			s.append(i == 0 ? "" : i == worst.size() - 1 ? " and " : ", ")
				.append("wave ").append(r.wave).append(" (").append(r.deaths).append(" of ").append(r.starts).append(", ").append(pct(r.rate())).append(')');
		}
		s.append(". Waves fewer than ").append(ArenaStats.MIN_STARTS).append(" attempts reached have no rate.");
		return s.toString();
	}

	private static String emptyText(Arena arena)
	{
		return arena == Arena.INFERNO
			? "Every Inferno attempt is filed here from the game's own \"Wave\" lines: each wave's split, how far it got and how it ended."
			: "Every Colosseum attempt is filed here from the game's own wave times: each wave's split, how far it got and whether the chest was taken.";
	}

	private static Card note(String title, String text)
	{
		Card card = new Card(new BorderLayout(0, 4));
		card.add(Ui.label(title, Theme.title(), Theme.GOLD), BorderLayout.NORTH);
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, WIDTH - 40), BorderLayout.CENTER);
		return card;
	}

	private static int percent(int n, int of)
	{
		return of == 0 ? 0 : (int) Math.round(100.0 * n / of);
	}

	static String pct(double share)
	{
		return Double.isNaN(share) ? "-" : String.format(Locale.ENGLISH, "%d%%", Math.round(share * 100));
	}
}
