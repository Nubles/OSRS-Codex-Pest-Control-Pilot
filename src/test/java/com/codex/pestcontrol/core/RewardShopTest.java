package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class RewardShopTest
{
	@Test
	public void readsTheShopsWordsLoosely()
	{
		assertEquals(Reward.TOP, Reward.inShop("Void Knight Top", -1));
		assertEquals(Reward.ROBE, Reward.inShop("<col=ff9040>Void knight robes</col> (250 pts)", -1));
		assertEquals(Reward.MAGE_HELM, Reward.inShop("Void Knight Mage Helm", -1));
		assertEquals(Reward.MAGE_HELM, Reward.inShop("Void mage helm", -1));
		assertEquals(Reward.GLOVES, Reward.inShop("Void Knight Gloves", -1));
		assertEquals(Reward.SEAL, Reward.inShop("Void seal(8)", -1));
		assertEquals(Reward.MACE, Reward.inShop("Void knight mace", -1));
	}

	@Test
	public void eliteIsNeverReadAsThePlainPiece()
	{
		assertEquals(Reward.ELITE_TOP, Reward.inShop("Elite void top", -1));
		assertEquals(Reward.ELITE_ROBE, Reward.inShop("Elite Void Knight Robe", -1));
	}

	@Test
	public void readsThePictureWhenTheWordsSayNothing()
	{
		assertEquals(Reward.RANGER_HELM, Reward.inShop("", 11664));
		assertEquals(Reward.TOP, Reward.inShop(null, 8839));
	}

	@Test
	public void otherLinesAreLeftAlone()
	{
		assertNull(Reward.inShop("Attack - 100 points", -1));
		assertNull(Reward.inShop("Herb pack", 995));
		assertNull(Reward.inShop("", -1));
		assertNull(Reward.inShop("Void", -1));
	}
}
