package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public class VoidCheckTest
{
	private static final Set<Reward> RANGER_SET = EnumSet.of(Reward.RANGER_HELM, Reward.TOP, Reward.ROBE, Reward.GLOVES);
	private static final Set<Reward> NONE = EnumSet.noneOf(Reward.class);

	@Test
	public void theFullSetWithTheRightHelmIsOn()
	{
		VoidCheck c = VoidCheck.of(RANGER_SET, Style.RANGED, RANGER_SET);
		assertEquals(VoidCheck.Kind.SET_ON, c.getKind());
		assertFalse(c.isWarning());
		assertEquals("Ranged set bonus for this game", c.getDetail());
	}

	@Test
	public void eliteTopAndRobeFillTheirSlots()
	{
		Set<Reward> elite = EnumSet.of(Reward.MAGE_HELM, Reward.ELITE_TOP, Reward.ELITE_ROBE, Reward.GLOVES);
		VoidCheck c = VoidCheck.of(elite, Style.MAGIC, elite);
		assertEquals(VoidCheck.Kind.SET_ON, c.getKind());
		assertEquals("Elite void on", c.getHeadline());
	}

	@Test
	public void aHelmForAnotherStyleGivesNothing()
	{
		Set<Reward> mage = EnumSet.of(Reward.MAGE_HELM, Reward.TOP, Reward.ROBE, Reward.GLOVES);
		Set<Reward> owned = EnumSet.copyOf(mage);
		owned.add(Reward.RANGER_HELM);
		VoidCheck c = VoidCheck.of(mage, Style.RANGED, owned);
		assertEquals(VoidCheck.Kind.WRONG_HELM, c.getKind());
		assertTrue(c.isWarning());
		assertEquals("Mage helm on a ranged weapon", c.getHeadline());
		assertEquals("Swap to your ranger helm for the set bonus", c.getDetail());
		assertEquals("Only the ranger helm helps a ranged weapon: 200 points", VoidCheck.of(mage, Style.RANGED, mage).getDetail());
	}

	@Test
	public void threePiecesOfFourGiveNoBonusAtAll()
	{
		Set<Reward> worn = EnumSet.of(Reward.MELEE_HELM, Reward.TOP, Reward.ROBE);
		VoidCheck c = VoidCheck.of(worn, Style.MELEE, worn);
		assertEquals(VoidCheck.Kind.INCOMPLETE, c.getKind());
		assertEquals("No gloves on, so no set bonus at all", c.getDetail());
		// With the gloves in the bank, it says so.
		Set<Reward> owned = EnumSet.copyOf(worn);
		owned.add(Reward.GLOVES);
		assertEquals("No gloves on, so no set bonus at all. You own the rest", VoidCheck.of(worn, Style.MELEE, owned).getDetail());
	}

	@Test
	public void noHelmNamesTheOneTheWeaponWants()
	{
		Set<Reward> worn = EnumSet.of(Reward.TOP, Reward.ROBE);
		VoidCheck c = VoidCheck.of(worn, Style.RANGED, worn);
		assertEquals("No ranger helm or gloves on, so no set bonus at all", c.getDetail());
	}

	@Test
	public void noVoidOnIsOnlyWorthSayingWithAFullSetOwned()
	{
		assertNull(VoidCheck.of(NONE, Style.MELEE, NONE));
		assertNull("a ranger set does not help a melee weapon", VoidCheck.of(NONE, Style.MELEE, RANGER_SET));
		VoidCheck c = VoidCheck.of(NONE, Style.RANGED, RANGER_SET);
		assertEquals(VoidCheck.Kind.NOT_WORN, c.getKind());
		assertFalse("not a warning: plenty play without void", c.isWarning());
	}

	@Test
	public void aSealOrMaceAloneIsNotVoidWorn()
	{
		assertNull(VoidCheck.of(EnumSet.of(Reward.SEAL, Reward.MACE), Style.MELEE, NONE));
	}
}
