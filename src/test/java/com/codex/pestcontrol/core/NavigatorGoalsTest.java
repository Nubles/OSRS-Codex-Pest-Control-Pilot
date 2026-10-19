package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class NavigatorGoalsTest
{
	/** A board as Codex Navigator writes it (GoalBoard, version 1), extra fields and all. */
	private static final String BOARD = "{\"version\":1,\"focus\":\"quest:Song of the Elves\",\"goals\":["
		+ "{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard\",\"complete\":false,\"progress\":0.4,"
		+ "\"next\":\"Train Thieving to 72\",\"hoursLeft\":6.5,\"skills\":{\"Thieving\":72,\"Magic\":66},\"quests\":[]},"
		+ "{\"key\":\"quest:Song of the Elves\",\"type\":\"quest\",\"title\":\"Song of the Elves\",\"complete\":false,\"progress\":0.2,"
		+ "\"next\":\"Train Prayer to 70\",\"hoursLeft\":30.0,\"skills\":{\"Prayer\":70,\"Agility\":70,\"Magic\":70},\"quests\":[]},"
		+ "{\"key\":\"quest:Done Deal\",\"type\":\"quest\",\"title\":\"Done Deal\",\"complete\":true,\"progress\":1.0,"
		+ "\"hoursLeft\":0.0,\"skills\":{\"Attack\":99},\"quests\":[],\"somethingNew\":[1,2]}]}";

	@Test
	public void theShopsSkillsAreReadFocusFirst()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		levels.put("Attack", 70);
		levels.put("Magic", 64);
		levels.put("Prayer", 52);
		List<NavigatorGoals.Need> needs = NavigatorGoals.needs(new Gson(), BOARD, levels);
		assertEquals(4 - 1, needs.size());
		assertEquals("Song of the Elves", needs.get(0).getGoal());
		assertTrue(needs.get(0).isFocus());
		assertEquals("Prayer", needs.get(0).getSkill());
		assertEquals(70, needs.get(0).getLevel());
		assertEquals("Magic", needs.get(1).getSkill());
		assertEquals("Ardougne Hard", needs.get(2).getGoal());
		assertEquals(66, needs.get(2).getLevel());
	}

	@Test
	public void noBoardOrABadOneIsNoGoals()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		levels.put("Prayer", 1);
		assertTrue(NavigatorGoals.needs(new Gson(), null, levels).isEmpty());
		assertTrue(NavigatorGoals.needs(new Gson(), "[1,2", levels).isEmpty());
		assertTrue(NavigatorGoals.needs(new Gson(), "{}", levels).isEmpty());
	}
}
