package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Points spent on combat experience at the Void Knights' reward shop, for points the goal does not
 * need. Per the OSRS Wiki, each point buys floor(level squared / 600) times 35 experience in
 * Attack, Strength, Defence or Hitpoints, 32 in Ranged or Magic and 18 in Prayer; a skill must be
 * at level 25 to buy any; and buying 100 points at once adds 10%, 10 at once 1%.
 */
public final class Experience
{
	/** The lowest level the shop sells experience at. */
	public static final int MIN_LEVEL = 25;

	/** The skills the shop sells experience in, with the experience a point buys per level-squared step. */
	public enum Skill
	{
		ATTACK("Attack", 35),
		STRENGTH("Strength", 35),
		DEFENCE("Defence", 35),
		HITPOINTS("Hitpoints", 35),
		RANGED("Ranged", 32),
		MAGIC("Magic", 32),
		PRAYER("Prayer", 18);

		private final String title;
		private final int factor;

		Skill(String title, int factor)
		{
			this.title = title;
			this.factor = factor;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/** What spare points buy in one skill. */
	public static final class Line
	{
		private final Skill skill;
		private final int level;
		private final long xp;

		Line(Skill skill, int level, long xp)
		{
			this.skill = skill;
			this.level = level;
			this.xp = xp;
		}

		public Skill getSkill()
		{
			return skill;
		}

		public int getLevel()
		{
			return level;
		}

		/** Experience the points buy; 0 below level 25. */
		public long getXp()
		{
			return xp;
		}

		public boolean isLocked()
		{
			return level < MIN_LEVEL;
		}
	}

	private Experience()
	{
	}

	/** Experience one point buys at a level, before any bundle bonus; 0 below level 25. */
	public static int perPoint(Skill skill, int level)
	{
		if (level < MIN_LEVEL)
		{
			return 0;
		}
		int capped = Math.min(99, level);
		return capped * capped / 600 * skill.factor;
	}

	/** Total experience a level starts at, the game's own table; 0 for level 1 or below. */
	public static long xpFor(int level)
	{
		long points = 0;
		for (int lvl = 1; lvl < Math.min(level, 127); lvl++)
		{
			points += (long) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
		}
		return points / 4;
	}

	/** The level a total of experience makes, up to 99. */
	public static int levelFor(long xp)
	{
		int level = 1;
		while (level < 99 && xpFor(level + 1) <= xp)
		{
			level++;
		}
		return level;
	}

	/**
	 * Points it takes to reach a level by buying experience alone, in the biggest bundles that do
	 * not buy more than is needed, and with each purchase paid at the level it is made at.
	 *
	 * @param xp experience in the skill now
	 * @return the points, 0 when the level is already reached, or -1 below level 25, where the shop sells nothing
	 */
	public static int pointsTo(Skill skill, long xp, int target)
	{
		long goal = xpFor(Math.min(99, target));
		int points = 0;
		long have = xp;
		while (have < goal)
		{
			int per = perPoint(skill, levelFor(have));
			if (per == 0)
			{
				return -1;
			}
			long left = goal - have;
			if (left >= per * 110L)
			{
				points += 100;
				have += per * 110L;
			}
			else if (left >= per * 101L / 10)
			{
				points += 10;
				have += per * 101L / 10;
			}
			else
			{
				points += 1;
				have += per;
			}
		}
		return points;
	}

	/** Experience the points buy when spent in the biggest bundles: hundreds, then tens, then ones. */
	public static long forPoints(Skill skill, int level, int points)
	{
		int per = perPoint(skill, level);
		if (per == 0 || points <= 0)
		{
			return 0;
		}
		long hundreds = points / 100;
		long tens = points % 100 / 10;
		long ones = points % 10;
		// Each bundle is paid out whole, rounded down, as the shop pays one purchase at a time.
		return hundreds * (per * 110L) + tens * (per * 101L / 10) + ones * per;
	}

	/**
	 * Points the goal leaves spare: everything held once the goal is bought or complete, else none.
	 *
	 * @return 0 when points are not known or the goal still needs them all
	 */
	public static int spare(Outlook outlook)
	{
		if (outlook == null || !outlook.isPointsKnown())
		{
			return 0;
		}
		return Math.max(0, outlook.getPoints() - outlook.getCost());
	}

	/**
	 * What the points buy in every skill, the most experience first and skills below 25 last.
	 *
	 * @param levels each skill's real level, in {@link Skill} order
	 */
	public static List<Line> of(int[] levels, int points)
	{
		List<Line> out = new ArrayList<>();
		for (Skill s : Skill.values())
		{
			int level = levels != null && s.ordinal() < levels.length ? levels[s.ordinal()] : 0;
			out.add(new Line(s, level, forPoints(s, level, points)));
		}
		out.sort((a, b) -> a.isLocked() != b.isLocked() ? (a.isLocked() ? 1 : -1)
			: a.xp != b.xp ? Long.compare(b.xp, a.xp) : a.skill.compareTo(b.skill));
		return Collections.unmodifiableList(out);
	}
}
