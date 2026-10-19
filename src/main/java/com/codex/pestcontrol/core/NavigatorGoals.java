package com.codex.pestcontrol.core;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The combat levels Codex Navigator's pinned goals still need, for the experience the reward shop sells. Navigator saves its goal board
 * for companion plugins under the RS profile key {@code codexnavigator.goalBoard} (fields are
 * only ever added); this keeps each unfinished goal's title and levels. Read only, and with
 * Navigator not installed there is no board and nothing is shown.
 */
public final class NavigatorGoals
{
	public static final String GROUP = "codexnavigator";
	public static final String KEY = "goalBoard";

	/** One pinned goal, as saved. Field names are Navigator's. */
	static final class Entry
	{
		String key;
		String title;
		boolean complete;
		Map<String, Integer> skills;
	}

	/** Navigator's saved board, as far as it is read here. */
	static final class Board
	{
		String focus;
		List<Entry> goals;
	}

	/** A level one goal needs. */
	public static final class Need
	{
		private final String goal;
		private final String skill;
		private final int level;
		private final boolean focus;

		Need(String goal, String skill, int level, boolean focus)
		{
			this.goal = goal;
			this.skill = skill;
			this.level = level;
			this.focus = focus;
		}

		/** The skill as Navigator names it, such as "Prayer". */
		public String getSkill()
		{
			return skill;
		}

		/** The goal's title, such as "Song of the Elves". */
		public String getGoal()
		{
			return goal;
		}

		public int getLevel()
		{
			return level;
		}

		/** The goal Navigator has in focus. */
		public boolean isFocus()
		{
			return focus;
		}
	}

	private NavigatorGoals()
	{
	}

	/**
	 * @param json the board as Navigator saved it, or null
	 * @param levels each skill's level now by name, such as "Prayer"; skills not listed are not asked about
	 * @return every unfinished goal's need above the level now in those skills, the focus goal first, then lowest level first
	 */
	public static List<Need> needs(Gson gson, String json, Map<String, Integer> levels)
	{
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}
		Board board;
		try
		{
			board = gson.fromJson(json, Board.class);
		}
		catch (RuntimeException | StackOverflowError e)
		{
			// Cut short, not JSON, or garbage nested deeper than Gson reads.
			return Collections.emptyList();
		}
		if (board == null || board.goals == null)
		{
			return Collections.emptyList();
		}
		List<Need> out = new ArrayList<>();
		for (Entry e : board.goals)
		{
			if (e == null || e.complete || e.skills == null)
			{
				continue;
			}
			for (Map.Entry<String, Integer> s : e.skills.entrySet())
			{
				Integer need = s.getValue();
				String skill = canonical(levels, s.getKey());
				if (skill == null || need == null || need <= levels.get(skill) || need > 99)
				{
					continue;
				}
				String title = e.title == null || e.title.isEmpty() ? "A Navigator goal" : e.title;
				out.add(new Need(title, skill, need, e.key != null && e.key.equals(board.focus)));
			}
		}
		out.sort((a, b) -> a.focus != b.focus ? (a.focus ? -1 : 1) : Integer.compare(a.level, b.level));
		return out;
	}

	/** The skill's name as the levels spell it, matching however Navigator capitalised it; null when not asked about. */
	private static String canonical(Map<String, Integer> levels, String name)
	{
		if (name == null)
		{
			return null;
		}
		for (String k : levels.keySet())
		{
			if (k.equalsIgnoreCase(name))
			{
				return k;
			}
		}
		return null;
	}
}
