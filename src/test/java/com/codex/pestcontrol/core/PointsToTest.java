package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PointsToTest
{
	@Test
	public void theExperienceTableIsTheGames()
	{
		assertEquals(0, Experience.xpFor(1));
		assertEquals(83, Experience.xpFor(2));
		assertEquals(737_627, Experience.xpFor(70));
		assertEquals(13_034_431, Experience.xpFor(99));
		assertEquals(1, Experience.levelFor(82));
		assertEquals(70, Experience.levelFor(814_444));
		assertEquals(99, Experience.levelFor(200_000_000));
	}

	@Test
	public void pointsToALevelBuyTheBiggestBundlesThatFit()
	{
		// Prayer 70 to 71 is 76,818 xp at 144 a point: four hundreds (15,840 each), nine tens (1,454) and three ones.
		assertEquals(493, Experience.pointsTo(Experience.Skill.PRAYER, Experience.xpFor(70), 71));
		assertEquals(0, Experience.pointsTo(Experience.Skill.PRAYER, Experience.xpFor(71), 70));
		assertEquals("below 25 the shop sells nothing", -1, Experience.pointsTo(Experience.Skill.ATTACK, Experience.xpFor(20), 30));
	}

	@Test
	public void eachPurchaseIsPaidAtTheLevelItIsMadeAt()
	{
		// Points to go 60 to 80 in Attack cost less than at the level-60 rate throughout.
		int points = Experience.pointsTo(Experience.Skill.ATTACK, Experience.xpFor(60), 80);
		long atSixty = (Experience.xpFor(80) - Experience.xpFor(60)) / (Experience.perPoint(Experience.Skill.ATTACK, 60) * 110L / 100);
		assertEquals(true, points < atSixty);
	}
}
