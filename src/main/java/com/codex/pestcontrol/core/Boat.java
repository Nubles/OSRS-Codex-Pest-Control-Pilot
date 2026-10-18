package com.codex.pestcontrol.core;

/** The three landers: the combat level to board and the points a won game gives. */
public enum Boat
{
	NOVICE("Novice", 40, 3),
	INTERMEDIATE("Intermediate", 70, 4),
	VETERAN("Veteran", 100, 5);

	private final String title;
	private final int combat;
	private final int points;

	Boat(String title, int combat, int points)
	{
		this.title = title;
		this.combat = combat;
		this.points = points;
	}

	public String getTitle()
	{
		return title;
	}

	public int getCombat()
	{
		return combat;
	}

	public int getPoints()
	{
		return points;
	}

	/** The best lander a combat level can board. */
	public static Boat forCombat(int level)
	{
		if (level >= VETERAN.combat)
		{
			return VETERAN;
		}
		if (level >= INTERMEDIATE.combat)
		{
			return INTERMEDIATE;
		}
		return NOVICE;
	}

	/** The lander a won game's points came from, or null for any other number. */
	public static Boat forPoints(int points)
	{
		for (Boat b : values())
		{
			if (b.points == points)
			{
				return b;
			}
		}
		return null;
	}

	public static Boat named(String name)
	{
		for (Boat b : values())
		{
			if (b.name().equals(name))
			{
				return b;
			}
		}
		return null;
	}
}
