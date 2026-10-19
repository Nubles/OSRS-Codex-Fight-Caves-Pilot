package com.codex.fightcaves.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Map;
import net.runelite.api.gameval.ItemID;

/**
 * The prayer doses in your bank, as it was last seen, and how many runs they cover. The pilot
 * counts them on every look at the bank, and also reads Codex Bank Steward's shared snapshot, so
 * a fresh install knows your stock before you open the bank. The newer of the two is used.
 *
 * <p>Bank Steward saves {@code codexbanksteward/bankV1} in the RuneLite profile, read here and
 * never written:
 * {@code {"version":1,"savedAt":1760000000000,"items":{"995":1234567,"2434":30},"truncated":false}}.</p>
 */
public final class Banked
{
	public static final String STEWARD_GROUP = "codexbanksteward";
	public static final String STEWARD_KEY = "bankV1";

	/** Where a count came from. */
	public enum Source
	{
		/** The pilot's own look at the bank. */
		PILOT,
		/** Codex Bank Steward's shared snapshot. */
		BANK_STEWARD
	}

	/** Every potion that restores prayer in the cave, four doses down to one. */
	private static final int[][] POTIONS = {
		{ItemID._4DOSEPRAYERRESTORE, ItemID._3DOSEPRAYERRESTORE, ItemID._2DOSEPRAYERRESTORE, ItemID._1DOSEPRAYERRESTORE},
		{ItemID._4DOSE2RESTORE, ItemID._3DOSE2RESTORE, ItemID._2DOSE2RESTORE, ItemID._1DOSE2RESTORE},
		{ItemID.SANFEW_SALVE_4_DOSE, ItemID.SANFEW_SALVE_3_DOSE, ItemID.SANFEW_SALVE_2_DOSE, ItemID.SANFEW_SALVE_1_DOSE},
	};

	private final long doses;
	private final long savedAt;
	private final Source source;

	public Banked(long doses, long savedAt, Source source)
	{
		this.doses = Math.max(0, doses);
		this.savedAt = Math.max(0, savedAt);
		this.source = source;
	}

	/** Doses of prayer an item holds: a prayer potion, super restore or sanfew serum, else 0. */
	public static int doses(int itemId)
	{
		for (int[] potion : POTIONS)
		{
			for (int i = 0; i < potion.length; i++)
			{
				if (potion[i] == itemId)
				{
					return 4 - i;
				}
			}
		}
		return 0;
	}

	/** The doses among a bank's items, item id to how many. */
	public static long count(Map<Integer, Long> items)
	{
		long n = 0;
		for (Map.Entry<Integer, Long> e : items.entrySet())
		{
			if (e.getKey() != null && e.getValue() != null && e.getValue() > 0)
			{
				n += doses(e.getKey()) * Math.min(e.getValue(), Integer.MAX_VALUE);
			}
		}
		return n;
	}

	/** Bank Steward's snapshot, or null when there is none or it cannot be read. */
	public static Banked steward(Gson gson, String json)
	{
		if (json == null || json.trim().isEmpty())
		{
			return null;
		}
		JsonObject root;
		try
		{
			JsonElement e = gson.fromJson(json, JsonElement.class);
			root = e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		}
		catch (JsonParseException | IllegalStateException | NumberFormatException e)
		{
			return null;
		}
		JsonElement list = root == null ? null : root.get("items");
		if (list == null || !list.isJsonObject())
		{
			return null;
		}
		long n = 0;
		for (Map.Entry<String, JsonElement> e : list.getAsJsonObject().entrySet())
		{
			Integer id = integer(e.getKey());
			Long qty = quantity(e.getValue());
			if (id != null && qty != null && qty > 0)
			{
				n += doses(id) * qty;
			}
		}
		Long savedAt = number(root.get("savedAt"));
		return new Banked(n, savedAt == null ? 0 : savedAt, Source.BANK_STEWARD);
	}

	/** The newer of two counts, either of which may be null. The pilot's own wins a tie. */
	public static Banked newest(Banked own, Banked steward)
	{
		return newest(own, steward, Long.MAX_VALUE);
	}

	/**
	 * The newer of two counts as of {@code now}. A time more than a day past now (a clock set
	 * wrong when it was saved) reads as unknown, so it neither wins for ever nor reads as "just now".
	 */
	public static Banked newest(Banked own, Banked steward, long now)
	{
		own = own == null ? null : own.seenBy(now);
		steward = steward == null ? null : steward.seenBy(now);
		if (own == null)
		{
			return steward;
		}
		if (steward == null)
		{
			return own;
		}
		return steward.savedAt > own.savedAt ? steward : own;
	}

	/** This count, with its time read as unknown when it is more than a day past {@code now}. */
	private Banked seenBy(long now)
	{
		return savedAt > now + 86_400_000L && now != Long.MAX_VALUE ? new Banked(doses, 0, source) : this;
	}

	/** Whole runs the doses cover, at so many doses a run; 0 when the need is not known. */
	public long runs(int perRun)
	{
		return perRun <= 0 ? 0 : doses / perRun;
	}

	public long getDoses()
	{
		return doses;
	}

	/** When the bank was seen, in epoch milliseconds; 0 when not known. */
	public long getSavedAt()
	{
		return savedAt;
	}

	public Source getSource()
	{
		return source;
	}

	/** A count no bigger than the biggest stack the game holds, or null when it is not a number. */
	private static Long quantity(JsonElement e)
	{
		Long n = number(e);
		return n == null ? null : Math.min(n, Integer.MAX_VALUE);
	}

	/**
	 * A whole number, or null when it is not one. A figure past what a long holds reads as the
	 * most a long holds, not wrapped round to a small or negative one.
	 */
	private static Long number(JsonElement e)
	{
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return null;
		}
		try
		{
			java.math.BigDecimal d = e.getAsBigDecimal();
			if (d.compareTo(java.math.BigDecimal.valueOf(Long.MAX_VALUE)) > 0)
			{
				return Long.MAX_VALUE;
			}
			if (d.compareTo(java.math.BigDecimal.valueOf(Long.MIN_VALUE)) < 0)
			{
				return Long.MIN_VALUE;
			}
			return d.longValue();
		}
		catch (NumberFormatException | ArithmeticException ex)
		{
			return null;
		}
	}

	private static Integer integer(String s)
	{
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
}
