package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class WardrobeTest
{
	private static Reward at(Wardrobe.Slot[] slots, int col, int row)
	{
		for (Wardrobe.Slot s : slots)
		{
			if (s.col == col && s.row == row)
			{
				return s.reward;
			}
		}
		throw new AssertionError("no slot at " + col + "," + row);
	}

	@Test
	public void everyRewardHasOneSlotWhereTheGameWearsIt()
	{
		Wardrobe.Slot[] slots = Wardrobe.layout(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.noneOf(Reward.class));
		Set<Reward> shown = new HashSet<>();
		for (Wardrobe.Slot s : slots)
		{
			if (s.reward != null)
			{
				assertEquals("one slot each", true, shown.add(s.reward));
			}
		}
		// Every reward but the elite pieces, which take the body and legs only when wanted.
		assertEquals(Reward.values().length - 2, shown.size());
		assertEquals(Reward.SEAL, at(slots, 1, 1));
		assertEquals(Reward.MACE, at(slots, 0, 2));
		assertEquals(Reward.TOP, at(slots, 1, 2));
		assertEquals(Reward.ROBE, at(slots, 1, 3));
		assertEquals(Reward.GLOVES, at(slots, 0, 4));
	}

	@Test
	public void eliteTakesTheBodyAndLegsWhenWantedOrOwned()
	{
		Wardrobe.Slot[] wanted = Wardrobe.layout(EnumSet.copyOf(Goal.ELITE.getRewards()), EnumSet.noneOf(Reward.class));
		assertEquals(Reward.ELITE_TOP, at(wanted, 1, 2));
		assertEquals(Reward.ELITE_ROBE, at(wanted, 1, 3));
		Wardrobe.Slot[] owned = Wardrobe.layout(EnumSet.noneOf(Reward.class), EnumSet.of(Reward.ELITE_TOP));
		assertEquals(Reward.ELITE_TOP, at(owned, 1, 2));
		assertEquals(Reward.ROBE, at(owned, 1, 3));
	}

	@Test
	public void aSlotLooksAsTheRewardStands()
	{
		Set<Reward> goal = EnumSet.copyOf(Goal.ELITE.getRewards());
		Set<Reward> owned = EnumSet.of(Reward.GLOVES);
		Set<Reward> none = EnumSet.noneOf(Reward.class);
		assertEquals(Wardrobe.Look.OWNED, Wardrobe.look(Reward.GLOVES, goal, owned, none, false));
		// Owned shows whole even outside the goal.
		assertEquals(Wardrobe.Look.OWNED, Wardrobe.look(Reward.GLOVES, EnumSet.noneOf(Reward.class), owned, none, false));
		assertEquals(Wardrobe.Look.LOCKED, Wardrobe.look(Reward.ELITE_TOP, goal, owned, EnumSet.of(Reward.ELITE_TOP), false));
		assertEquals(Wardrobe.Look.CAN_BUY, Wardrobe.look(Reward.ELITE_TOP, goal, owned, EnumSet.of(Reward.ELITE_TOP), true));
		assertEquals(Wardrobe.Look.WANTED, Wardrobe.look(Reward.ELITE_TOP, goal, owned, none, true));
		assertEquals(Wardrobe.Look.NONE, Wardrobe.look(Reward.MACE, goal, owned, EnumSet.of(Reward.MACE), true));
		assertNotNull(Wardrobe.layout(goal, owned));
	}

	@Test
	public void onlyWhatThePointsBuyTogetherIsLit()
	{
		// 412 points toward the melee set: each of the top, robe, mace and helm costs 200 to 250, so
		// each alone was lit as buyable. Together they buy the top, and nothing after it.
		Set<Reward> goal = EnumSet.copyOf(Goal.MELEE.getRewards());
		Set<Reward> owned = EnumSet.of(Reward.GLOVES);
		Outlook o = Outlook.of(goal, owned, 412, 4.2, 3, true);
		Set<Reward> buyable = o.buyableNow(EnumSet.noneOf(Reward.class));
		assertEquals(EnumSet.of(Reward.TOP), buyable);
		assertEquals(o.getNextBuy(), Reward.TOP);
		assertEquals(Wardrobe.Look.CAN_BUY, Wardrobe.look(Reward.TOP, goal, owned, buyable, false));
		assertEquals(Wardrobe.Look.WANTED, Wardrobe.look(Reward.ROBE, goal, owned, buyable, false));
		assertEquals(Wardrobe.Look.WANTED, Wardrobe.look(Reward.MELEE_HELM, goal, owned, buyable, false));
		assertEquals("Teal: buy the top now", Wardrobe.foot("Melee", goal, owned, buyable, 412, true, false));
		// Enough for two: both lit, and the foot counts them.
		Set<Reward> two = Outlook.of(goal, owned, 450, 4.2, 3, true).buyableNow(EnumSet.noneOf(Reward.class));
		assertEquals(2, two.size());
		assertEquals("Teal: these 2 can be bought now", Wardrobe.foot("Melee", goal, owned, two, 450, true, false));
		// Points not read yet: nothing is called affordable.
		assertTrue(Outlook.of(goal, owned, -1, 4.2, 3, true).buyableNow(EnumSet.noneOf(Reward.class)).isEmpty());
	}

	@Test
	public void theFootSaysWhenTheDiaryNotThePointsHoldsTheGoalBack()
	{
		Set<Reward> goal = EnumSet.of(Reward.TOP, Reward.ELITE_TOP);
		Set<Reward> none = EnumSet.noneOf(Reward.class);
		Set<Reward> top = EnumSet.of(Reward.TOP);
		int cost = Reward.TOP.getCost() + Reward.ELITE_TOP.getCost();
		assertEquals("Points enough; the diary is due", Wardrobe.foot("Elite", goal, none, top, cost, true, false));
		assertEquals("Enough points for the rest now", Wardrobe.foot("Elite", goal, none, goal, cost, true, true));
		// The top alone, with the elite top still to save for: the foot points at what can be bought.
		assertEquals("Teal: buy the top now", Wardrobe.foot("Elite", goal, none, top, cost - 10, true, false));
		assertEquals("Not enough points for a piece yet", Wardrobe.foot("Elite", goal, none, none, 10, true, true));
		assertEquals("The rest needs the hard diary", Wardrobe.foot("Elite", goal, top, none, 10, true, false));
		// Points not read yet.
		assertEquals(Format.number(cost) + " points for the rest", Wardrobe.foot("Elite", goal, none, none, 0, false, true));
		assertEquals("All of elite is yours", Wardrobe.foot("Elite", goal, goal, none, 0, true, true));
		assertEquals("Click a slot to set a goal", Wardrobe.foot("Elite", none, none, none, 0, true, true));
	}
}
