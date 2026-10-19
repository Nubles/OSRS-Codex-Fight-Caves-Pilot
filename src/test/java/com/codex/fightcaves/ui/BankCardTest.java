package com.codex.fightcaves.ui;

import static org.junit.Assert.assertEquals;
import com.codex.fightcaves.core.Banked;
import org.junit.Test;

public class BankCardTest
{
	private static final long NOW = 1_760_000_000_000L;

	@Test
	public void saysHowManyWholeRunsTheBankCovers()
	{
		Banked own = new Banked(186, NOW - 3 * 3_600_000L, Banked.Source.PILOT);
		assertEquals("4 runs in the bank", PilotPanel.bankTitle(own, 39));
		assertEquals("186 doses of prayer potion, super restore and sanfew serum, against about 39 a run at your rate,"
			+ " Jad's share included. Counted 3 hours ago.", PilotPanel.bankText(own, 39, NOW));
		assertEquals("1 run in the bank", PilotPanel.bankTitle(new Banked(40, NOW, Banked.Source.PILOT), 39));
		assertEquals("Under a run in the bank", PilotPanel.bankTitle(new Banked(20, NOW, Banked.Source.PILOT), 39));
		assertEquals("No prayer doses banked", PilotPanel.bankTitle(new Banked(0, NOW, Banked.Source.PILOT), 39));
	}

	@Test
	public void namesBankStewardWhenItsSnapshotIsTheNewer()
	{
		Banked steward = new Banked(1, NOW - 2 * 86_400_000L, Banked.Source.BANK_STEWARD);
		assertEquals("1 dose of prayer potion, super restore and sanfew serum, against about 39 a run at your rate,"
			+ " Jad's share included. From Codex Bank Steward, 2 days ago.", PilotPanel.bankText(steward, 39, NOW));
		assertEquals("1 dose of prayer potion, super restore and sanfew serum. From Codex Bank Steward.",
			PilotPanel.bankText(new Banked(1, 0, Banked.Source.BANK_STEWARD), 0, NOW));
	}

	@Test
	public void agesReadPlainly()
	{
		assertEquals("just now", Format.ago(NOW - 20_000, NOW));
		assertEquals("1 minute ago", Format.ago(NOW - 60_000, NOW));
		assertEquals("59 minutes ago", Format.ago(NOW - 59 * 60_000, NOW));
		assertEquals("1 hour ago", Format.ago(NOW - 61 * 60_000, NOW));
		assertEquals("47 hours ago", Format.ago(NOW - 47 * 3_600_000L, NOW));
		assertEquals("2 days ago", Format.ago(NOW - 48 * 3_600_000L, NOW));
		// A clock set back reads as just now, not as a time to come.
		assertEquals("just now", Format.ago(NOW + 60_000, NOW));
	}
}
