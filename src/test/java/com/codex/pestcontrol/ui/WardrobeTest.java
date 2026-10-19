package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.codex.pestcontrol.core.Goal;
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
		assertEquals(Wardrobe.Look.OWNED, Wardrobe.look(Reward.GLOVES, goal, owned, 0, true, false));
		// Owned shows whole even outside the goal.
		assertEquals(Wardrobe.Look.OWNED, Wardrobe.look(Reward.GLOVES, EnumSet.noneOf(Reward.class), owned, 0, true, false));
		assertEquals(Wardrobe.Look.LOCKED, Wardrobe.look(Reward.ELITE_TOP, goal, owned, 900, true, false));
		assertEquals(Wardrobe.Look.CAN_BUY, Wardrobe.look(Reward.ELITE_TOP, goal, owned, 200, true, true));
		assertEquals(Wardrobe.Look.WANTED, Wardrobe.look(Reward.ELITE_TOP, goal, owned, 199, true, true));
		// Points not read yet: nothing is called affordable.
		assertEquals(Wardrobe.Look.WANTED, Wardrobe.look(Reward.RANGER_HELM, goal, owned, 900, false, true));
		assertEquals(Wardrobe.Look.NONE, Wardrobe.look(Reward.MACE, goal, owned, 900, true, true));
		assertNotNull(Wardrobe.layout(goal, owned));
	}

	@Test
	public void theFootSaysWhenTheDiaryNotThePointsHoldsTheGoalBack()
	{
		Set<Reward> goal = EnumSet.of(Reward.TOP, Reward.ELITE_TOP);
		Set<Reward> none = EnumSet.noneOf(Reward.class);
		int cost = Reward.TOP.getCost() + Reward.ELITE_TOP.getCost();
		assertEquals("Points enough; the diary is due", Wardrobe.foot("Elite", goal, none, cost, true, false));
		assertEquals("Enough points for the rest now", Wardrobe.foot("Elite", goal, none, cost, true, true));
		assertEquals("10 points to the rest", Wardrobe.foot("Elite", goal, none, cost - 10, true, true));
		// Before the points are read, the whole cost rather than a guess at what is left.
		assertEquals(Format.number(cost) + " points for the rest", Wardrobe.foot("Elite", goal, none, 0, false, true));
		assertEquals("All of elite is yours", Wardrobe.foot("Elite", goal, goal, 0, true, true));
		assertEquals("Click a slot to set a goal", Wardrobe.foot("Elite", none, none, 0, true, true));
	}
}
