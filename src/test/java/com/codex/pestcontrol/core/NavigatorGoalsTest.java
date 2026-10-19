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

	/**
	 * Written by Codex Navigator's own GoalBoard.of and Gson (Navigator 71c07ee): a level goal in focus,
	 * a diary and a quest, at level 52 in every skill. Gson writes an apostrophe as an escape.
	 */
	private static final String REAL_BOARD = "{\"version\":1,\"focus\":\"skill:Thieving 70\",\"goals\":[{\"key\":\"skill:Thieving 70\",\"type\":\"skill\","
		+ "\"title\":\"70 Thieving\",\"complete\":false,\"progress\":0.17,\"next\":\"Train Thieving to 70\",\"hoursLeft\":5.0,"
		+ "\"skills\":{\"Thieving\":70},\"quests\":[]},{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard Diary\","
		+ "\"complete\":false,\"progress\":0.31,\"next\":\"Start Shield of Arrav\",\"hoursLeft\":59.5,\"skills\":{\"Cooking\":53,"
		+ "\"Fishing\":53,\"Hunter\":59,\"Ranged\":60,\"Farming\":65,\"Smithing\":68,\"Magic\":66,\"Thieving\":72,\"Agility\":65,"
		+ "\"Runecraft\":65},\"quests\":[\"The Grand Tree\",\"Tree Gnome Village\",\"Monkey Madness I\",\"Family Crest\","
		+ "\"Shield of Arrav\",\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\","
		+ "\"Druidic Ritual\",\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\","
		+ "\"Waterfall Quest\",\"Legends\\u0027 Quest\",\"Regicide\",\"Roving Elves\",\"Big Chompy Bird Hunting\","
		+ "\"Sheep Herder\",\"Mourning\\u0027s End Part I\",\"Watchtower\"]},{\"key\":\"quest:Dragon Slayer II\","
		+ "\"type\":\"quest\",\"title\":\"Dragon Slayer II\",\"complete\":false,\"progress\":0.28,\"next\":\"Start Shield of Arrav\","
		+ "\"hoursLeft\":65.0,\"skills\":{\"Cooking\":53,\"Woodcutting\":55,\"Thieving\":60,\"Crafting\":62,\"Agility\":60,"
		+ "\"Smithing\":70,\"Mining\":68,\"Magic\":75,\"Fishing\":53},\"quests\":[\"Family Crest\",\"Shield of Arrav\","
		+ "\"Lost City\",\"Merlin\\u0027s Crystal\",\"Dragon Slayer I\",\"Heroes\\u0027 Quest\",\"Druidic Ritual\","
		+ "\"Jungle Potion\",\"Shilo Village\",\"Plague City\",\"Biohazard\",\"Underground Pass\",\"Waterfall Quest\","
		+ "\"Legends\\u0027 Quest\",\"The Fremennik Trials\",\"Rune Mysteries\",\"Lunar Diplomacy\",\"Death Plateau\","
		+ "\"Troll Stronghold\",\"Eadgar\\u0027s Ruse\",\"Dream Mentor\",\"Gertrude\\u0027s Cat\",\"Icthlarin\\u0027s Little Helper\","
		+ "\"A Tail of Two Cats\",\"The Restless Ghost\",\"Ernest the Chicken\",\"Priest in Peril\",\"Animal Magnetism\","
		+ "\"Ghosts Ahoy\",\"The Dig Site\",\"Bone Voyage\",\"X Marks the Spot\",\"Client of Kourend\",\"Dragon Slayer II\"]},"
		+ "{\"key\":\"skill:Attack 70\",\"type\":\"skill\",\"title\":\"70 Attack\",\"complete\":false,\"progress\":0.17,"
		+ "\"next\":\"Train Attack to 70\",\"hoursLeft\":11.0,\"skills\":{\"Attack\":70},\"quests\":[]},{\"key\":\"diary:Western Provinces Hard\","
		+ "\"type\":\"diary\",\"title\":\"Western Provinces Hard\",\"complete\":false,\"progress\":0.0,\"next\":\"Western Provinces Hard\","
		+ "\"hoursLeft\":0.0,\"skills\":{},\"quests\":[]}]}";

	@Test
	public void readsTheBoardNavigatorReallyWrites()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (Experience.Skill s : Experience.Skill.values())
		{
			levels.put(s.getTitle(), 52);
		}
		List<NavigatorGoals.Need> needs = NavigatorGoals.needs(new Gson(), REAL_BOARD, levels);
		assertEquals(4, needs.size());
		// No focus here: the focus is a Thieving level, which the shop does not sell.
		assertEquals("Ardougne Hard Diary", needs.get(0).getGoal());
		assertEquals("Ranged", needs.get(0).getSkill());
		assertEquals(60, needs.get(0).getLevel());
		assertEquals("Magic", needs.get(1).getSkill());
		assertEquals(66, needs.get(1).getLevel());
		assertEquals("70 Attack", needs.get(2).getGoal());
		assertEquals("Attack", needs.get(2).getSkill());
		assertEquals(70, needs.get(2).getLevel());
		assertEquals("Dragon Slayer II", needs.get(3).getGoal());
		assertEquals(75, needs.get(3).getLevel());
	}
}
