package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

public class ExperienceTest
{
	@Test
	public void aPointBuysTheShopsRate()
	{
		// 70 squared is 4,900; over 600 is 8 (rounded down); times 35.
		assertEquals(280, Experience.perPoint(Experience.Skill.ATTACK, 70));
		assertEquals(256, Experience.perPoint(Experience.Skill.MAGIC, 70));
		assertEquals(144, Experience.perPoint(Experience.Skill.PRAYER, 70));
		// 99 squared is 9,801: 16 a step.
		assertEquals(560, Experience.perPoint(Experience.Skill.STRENGTH, 99));
		assertEquals(0, Experience.perPoint(Experience.Skill.ATTACK, 24));
		assertEquals(35, Experience.perPoint(Experience.Skill.ATTACK, 25));
	}

	@Test
	public void bigBundlesPayABonus()
	{
		// 123 points at 280 each: 100 with 10% (30,800), 20 with 1% (2 x 2,828), 3 alone (840).
		assertEquals(30_800 + 2 * 2_828 + 840, Experience.forPoints(Experience.Skill.ATTACK, 70, 123));
		assertEquals(0, Experience.forPoints(Experience.Skill.ATTACK, 20, 123));
		assertEquals(0, Experience.forPoints(Experience.Skill.ATTACK, 70, 0));
	}

	@Test
	public void onlyPointsTheGoalLeavesAreSpare()
	{
		Outlook bought = Outlook.of(EnumSet.of(Reward.MACE), EnumSet.of(Reward.MACE), 300, 5, 10, true);
		assertEquals(300, Experience.spare(bought));
		Outlook saving = Outlook.of(EnumSet.of(Reward.TOP), EnumSet.noneOf(Reward.class), 100, 5, 10, true);
		assertEquals(0, Experience.spare(saving));
		Outlook covered = Outlook.of(EnumSet.of(Reward.TOP), EnumSet.noneOf(Reward.class), Reward.TOP.getCost() + 40, 5, 10, true);
		assertEquals(40, Experience.spare(covered));
		assertEquals(0, Experience.spare(Outlook.of(EnumSet.noneOf(Reward.class), EnumSet.noneOf(Reward.class), -1, 5, 10, true)));
	}

	@Test
	public void theMostExperienceFirstAndLockedSkillsLast()
	{
		List<Experience.Line> lines = Experience.of(new int[]{70, 70, 20, 80, 90, 1, 70}, 100);
		assertEquals(Experience.Skill.RANGED, lines.get(0).getSkill());
		assertEquals(Experience.Skill.HITPOINTS, lines.get(1).getSkill());
		assertTrue(lines.get(lines.size() - 1).isLocked());
		assertTrue(lines.get(lines.size() - 2).isLocked());
	}
}
