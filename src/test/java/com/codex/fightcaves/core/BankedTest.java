package com.codex.fightcaves.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BankedTest
{
	private static final Gson GSON = new Gson();

	@Test
	public void countsEveryDoseThatRestoresPrayer()
	{
		assertEquals(4, Banked.doses(2434));
		assertEquals(1, Banked.doses(143));
		assertEquals(3, Banked.doses(3026));
		assertEquals(2, Banked.doses(10929));
		// A shark is no dose.
		assertEquals(0, Banked.doses(385));
		Map<Integer, Long> bank = new HashMap<>();
		bank.put(2434, 30L);
		bank.put(3024, 10L);
		bank.put(141, 1L);
		bank.put(385, 200L);
		// A placeholder holds none.
		bank.put(139, 0L);
		assertEquals(30 * 4 + 10 * 4 + 2, Banked.count(bank));
	}

	@Test
	public void readsBankStewardsOwnFixtureUnchanged() throws IOException
	{
		// osrs-codex-bank-steward src/test/resources/com/codex/steward/core/bank-steward-bankV1.json, byte for byte.
		Banked b = Banked.steward(GSON, fixture("bank-steward-bankV1.json"));
		assertSame(Banked.Source.BANK_STEWARD, b.getSource());
		assertEquals(1_760_000_000_000L, b.getSavedAt());
		// Thirty four-dose prayer potions.
		assertEquals(120, b.getDoses());
		assertEquals(5, b.runs(24));
	}

	@Test
	public void aDamagedSnapshotReadsAsNone()
	{
		assertNull(Banked.steward(GSON, null));
		assertNull(Banked.steward(GSON, ""));
		assertNull(Banked.steward(GSON, "not json {"));
		assertNull(Banked.steward(GSON, "[1,2]"));
		assertNull(Banked.steward(GSON, "{\"items\":[]}"));
		// Odd entries are skipped; the rest still count.
		Banked b = Banked.steward(GSON, "{\"items\":{\"x\":3,\"2434\":\"many\",\"3024\":2}}");
		assertEquals(8, b.getDoses());
		assertEquals(0, b.getSavedAt());
	}

	@Test
	public void theNewerLookWins()
	{
		Banked own = new Banked(40, 2000, Banked.Source.PILOT);
		Banked steward = new Banked(80, 3000, Banked.Source.BANK_STEWARD);
		assertSame(steward, Banked.newest(own, steward));
		assertSame(own, Banked.newest(own, new Banked(80, 2000, Banked.Source.BANK_STEWARD)));
		assertSame(own, Banked.newest(own, null));
		assertSame(steward, Banked.newest(null, steward));
		assertNull(Banked.newest(null, null));
		assertEquals(0, own.runs(0));
		assertEquals(1, own.runs(24));
	}

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = BankedTest.class.getResourceAsStream(name))
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[4096];
			for (int n; (n = in.read(buf)) > 0; )
			{
				out.write(buf, 0, n);
			}
			return new String(out.toByteArray(), StandardCharsets.UTF_8);
		}
	}
}
