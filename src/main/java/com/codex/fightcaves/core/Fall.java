package com.codex.fightcaves.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why a run ended, read from its last few seconds. Every tick of a wave before Jad's is kept
 * briefly (hitpoints, prayer, the protection prayer up and what could hit you from where you
 * stood), and when you fall the last of them are read for the likeliest cause: prayer running
 * out, two attack styles reaching you at once, the wrong prayer up, no prayer up, or simply being
 * worn down. It is a look back once the run is over; nothing is said during a wave, and a fall to
 * Jad is not reviewed.
 */
public final class Fall
{
	/** Ticks kept: about ten seconds. */
	static final int KEEP = 16;
	/** Ticks the cause is read from: the last six seconds or so. */
	static final int WINDOW = 10;

	/** Why the run ended. */
	public enum Cause
	{
		PRAYER_OUT,
		TWO_STYLES,
		WRONG_PRAYER,
		NO_PRAYER,
		WORN_DOWN
	}

	/** One tick of a wave. */
	public static final class Frame
	{
		final int hp;
		final int prayer;
		final Protect up;
		final List<Threat> threats;
		final int damage;

		/**
		 * @param up the protection prayer up, or null
		 * @param threats what could hit you soon from where you stood
		 * @param damage damage you took this tick
		 */
		public Frame(int hp, int prayer, Protect up, List<Threat> threats, int damage)
		{
			this.hp = hp;
			this.prayer = prayer;
			this.up = up;
			this.threats = threats == null ? new ArrayList<>() : threats;
			this.damage = damage;
		}
	}

	/** The verdict on a fall. */
	public static final class Review
	{
		private final Cause cause;
		private final String text;
		private final List<Integer> hp;

		Review(Cause cause, String text, List<Integer> hp)
		{
			this.cause = cause;
			this.text = text;
			this.hp = hp;
		}

		public Cause getCause()
		{
			return cause;
		}

		/** One or two sentences on what happened. */
		public String getText()
		{
			return text;
		}

		/** Your hitpoints over the last ticks, oldest first. */
		public List<Integer> getHp()
		{
			return hp;
		}
	}

	private final Deque<Frame> frames = new ArrayDeque<>();

	/**
	 * A tick of a wave before Jad's. Once a tick shows your hitpoints at nothing, the ticks after
	 * it (the death animation, prayers switched off, creatures still about) are not kept: they
	 * would read as having fought on with no prayer up.
	 */
	public void add(Frame f)
	{
		Frame last = frames.peekLast();
		if (last != null && last.hp <= 0)
		{
			return;
		}
		frames.addLast(f);
		while (frames.size() > KEEP)
		{
			frames.removeFirst();
		}
	}

	/** A new wave, a new visit or a finished run: what came before tells nothing about the next fall. */
	public void clear()
	{
		frames.clear();
	}

	/**
	 * Why you fell, from the ticks kept.
	 *
	 * @param doses prayer and restore doses still carried
	 * @return the review, or null when no tick was kept (a fall to Jad, or straight after logging in)
	 */
	public Review review(int doses)
	{
		if (frames.isEmpty())
		{
			return null;
		}
		List<Frame> all = new ArrayList<>(frames);
		List<Frame> window = all.subList(Math.max(0, all.size() - WINDOW), all.size());
		List<Integer> hp = new ArrayList<>();
		for (Frame f : all)
		{
			hp.add(f.hp);
		}
		int damage = 0;
		List<Frame> threatened = new ArrayList<>();
		for (Frame f : window)
		{
			damage += f.damage;
			if (!f.threats.isEmpty())
			{
				threatened.add(f);
			}
		}
		int half = Math.max(1, (threatened.size() + 1) / 2);

		boolean prayerOut = false;
		for (Frame f : window)
		{
			prayerOut |= f.prayer <= 0 && !f.threats.isEmpty();
		}
		if (prayerOut)
		{
			return new Review(Cause.PRAYER_OUT, doses > 0
				? "Prayer ran out with " + doses + (doses == 1 ? " dose" : " doses") + " still in your pack. Sip at the level the prayer gauge gives."
				: "Prayer ran out with no doses left. The supply check at the cave mouth says what a run takes.", hp);
		}

		int split = 0;
		int wrong = 0;
		int bare = 0;
		Map<Protect, Set<String>> who = new EnumMap<>(Protect.class);
		Map<Protect, Integer> weight = new EnumMap<>(Protect.class);
		for (Frame f : threatened)
		{
			EnumSet<Protect> styles = EnumSet.noneOf(Protect.class);
			Map<Protect, Integer> here = new EnumMap<>(Protect.class);
			for (Threat t : f.threats)
			{
				styles.add(t.getStyle());
				here.merge(t.getStyle(), t.getMonster().getMaxHit(), Integer::sum);
				weight.merge(t.getStyle(), t.getMonster().getMaxHit(), Integer::sum);
				who.computeIfAbsent(t.getStyle(), k -> new LinkedHashSet<>()).add(noun(t.getMonster()));
			}
			if (styles.size() >= 2)
			{
				split++;
			}
			Protect worst = heaviest(here);
			if (f.up == null)
			{
				bare++;
			}
			else if (worst != null && worst != f.up)
			{
				wrong++;
			}
		}
		Protect heavy = heaviest(weight);
		if (threatened.size() >= 2 && split >= half)
		{
			List<String> names = new ArrayList<>();
			for (Protect p : byWeight(weight))
			{
				// The first creature seen hitting with each style names it.
				names.add("the " + who.get(p).iterator().next());
			}
			return new Review(Cause.TWO_STYLES, capital(and(names.subList(0, Math.min(2, names.size())))) + " reached you at once, and one prayer"
				+ " cannot block both. A tile where one of them is stuck splits them: the gilt tile looks for it.", hp);
		}
		if (heavy != null && bare >= half && bare >= wrong)
		{
			return new Review(Cause.NO_PRAYER, "No protection prayer was up against " + the(who.get(heavy))
				+ ". " + heavy.getName() + " blocks it.", hp);
		}
		if (heavy != null && wrong >= half)
		{
			Protect up = lastUp(threatened);
			return new Review(Cause.WRONG_PRAYER, (up == null ? "The wrong prayer" : up.getName()) + " was up while " + the(who.get(heavy))
				+ " hit you. " + heavy.getName() + " blocks it.", hp);
		}
		String from = heavy == null ? "" : ", mostly from " + the(who.get(heavy));
		return new Review(Cause.WORN_DOWN, "Worn down: " + damage + " damage in the last " + Math.max(1, Math.round(window.size() * 0.6))
			+ " seconds" + from + ". Eating earlier keeps a hit from finishing you.", hp);
	}

	/** "bat", "ranger", "melee creature": what players call each creature, for a sentence. */
	static String noun(Monster m)
	{
		return m == Monster.YT_MEJKOT ? "melee creature" : m.getNickname().toLowerCase();
	}

	private static Protect heaviest(Map<Protect, Integer> weight)
	{
		List<Protect> order = byWeight(weight);
		return order.isEmpty() ? null : order.get(0);
	}

	private static List<Protect> byWeight(Map<Protect, Integer> weight)
	{
		List<Protect> order = new ArrayList<>(weight.keySet());
		order.sort((a, b) -> Integer.compare(weight.get(b), weight.get(a)));
		return order;
	}

	private static Protect lastUp(List<Frame> frames)
	{
		for (int i = frames.size() - 1; i >= 0; i--)
		{
			if (frames.get(i).up != null)
			{
				return frames.get(i).up;
			}
		}
		return null;
	}

	/** "the mager", "the ranger and the bat". */
	private static String the(Set<String> names)
	{
		List<String> list = new ArrayList<>();
		for (String n : names == null ? new LinkedHashSet<String>() : names)
		{
			list.add("the " + n);
		}
		return list.isEmpty() ? "it" : and(list);
	}

	private static String and(List<String> parts)
	{
		if (parts.size() <= 1)
		{
			return parts.isEmpty() ? "" : parts.get(0);
		}
		return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
