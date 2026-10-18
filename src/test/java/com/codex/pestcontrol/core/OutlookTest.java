package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import org.junit.Test;

public class OutlookTest
{
	@Test
	public void voidSetCostsEightHundredFifty()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.noneOf(Reward.class), 100, 4, 4, false);
		assertEquals(850, o.getCost());
		assertEquals(750, o.getRemaining());
		assertEquals(188, o.getGames());
		assertEquals(752, o.getMinutes(), 0.001);
		assertEquals(null, o.getNextBuy());
	}

	@Test
	public void ownedPiecesCostNothing()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.RANGER.getRewards()), EnumSet.of(Reward.TOP, Reward.ROBE), 300, 5, 4, true);
		assertEquals(350, o.getCost());
		assertEquals(50, o.getRemaining());
		assertEquals(10, o.getGames());
		assertEquals(Reward.RANGER_HELM, o.getNextBuy());
		assertFalse(o.isComplete());
	}

	@Test
	public void affordableAndComplete()
	{
		Outlook o = Outlook.of(EnumSet.of(Reward.SEAL), EnumSet.noneOf(Reward.class), 20, 3, 4, false);
		assertTrue(o.isAffordable());
		assertEquals(0, o.getGames());
		Outlook done = Outlook.of(EnumSet.of(Reward.SEAL), EnumSet.of(Reward.SEAL), 20, 3, 4, false);
		assertTrue(done.isComplete());
		assertEquals(1, done.progress(), 0);
	}

	@Test
	public void unknownPointsCountFromZero()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.ALL.getRewards()), EnumSet.noneOf(Reward.class), -1, 5, 4, false);
		assertFalse(o.isPointsKnown());
		assertEquals(1250, o.getRemaining());
		assertEquals(250, o.getGames());
	}

	@Test
	public void presetsAreRecognised()
	{
		assertEquals(Goal.ELITE, Goal.matching(EnumSet.copyOf(Goal.ELITE.getRewards())));
		assertEquals(null, Goal.matching(EnumSet.of(Reward.MACE)));
	}
}
