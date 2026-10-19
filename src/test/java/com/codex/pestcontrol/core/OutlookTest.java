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
	public void unknownPointsStillGiveTheCostButNoGames()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.ALL.getRewards()), EnumSet.noneOf(Reward.class), -1, 5, 4, false);
		assertFalse(o.isPointsKnown());
		assertEquals(1250, o.getRemaining());
		assertEquals(-1, o.getGames());
		assertEquals(-1, o.getMinutes(), 0);
	}

	@Test
	public void presetsAreRecognised()
	{
		assertEquals(Goal.ELITE, Goal.matching(EnumSet.copyOf(Goal.ELITE.getRewards())));
		assertEquals(null, Goal.matching(EnumSet.of(Reward.MACE)));
	}

	@Test
	public void thePointsLightTheSetThatSpendsTheMostOfThem()
	{
		// 400 points toward a top (250) and two helms (200 each): the dearest piece first bought the
		// top and left 150, short of either helm. Both helms spend all 400, so they come first.
		Outlook o = Outlook.of(EnumSet.of(Reward.TOP, Reward.MELEE_HELM, Reward.RANGER_HELM), EnumSet.noneOf(Reward.class), 400, 4, 3, true);
		assertEquals(EnumSet.of(Reward.MELEE_HELM, Reward.RANGER_HELM), o.buyableNow(null));
		java.util.List<Outlook.Milestone> m = o.milestones(null);
		assertEquals(0, m.get(0).getGames());
		assertEquals(0, m.get(1).getGames());
		assertEquals(Reward.TOP, m.get(2).getReward());
		assertEquals(650, m.get(2).getTotal());
		assertEquals((int) Math.ceil(250 / 4.0), m.get(2).getGames());
		assertTrue(o.buyableNow(null).contains(o.getNextBuy()));
	}

	@Test
	public void theBestSetLeavesLockedPiecesOut()
	{
		// The elite top would make 400 with the helm, but the diary keeps it locked: the helm and
		// the gloves (350) are the most the points buy.
		java.util.Set<Reward> locked = EnumSet.of(Reward.ELITE_TOP);
		Outlook o = Outlook.of(EnumSet.of(Reward.ELITE_TOP, Reward.RANGER_HELM, Reward.GLOVES), EnumSet.noneOf(Reward.class), 400, 4, 3, true);
		assertEquals(EnumSet.of(Reward.RANGER_HELM, Reward.GLOVES), o.buyableNow(locked));
		assertEquals(Reward.ELITE_TOP, o.milestones(locked).get(2).getReward());
	}

	@Test
	public void gamesAreNotCountedBeforeThePointsAreRead()
	{
		// Unread points are not zero points: a player boarding with 600 is not 213 games away.
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.noneOf(Reward.class), -1, 4, 4, false);
		assertFalse(o.isPointsKnown());
		assertEquals(-1, o.getGames());
		assertFalse(o.isGamesKnown());
		for (Outlook.Milestone m : o.milestones(null))
		{
			assertEquals(m.getReward() + " is not timed", -1, m.getGames());
		}
		assertTrue(Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.noneOf(Reward.class), 0, 4, 4, false).isGamesKnown());
	}
}
