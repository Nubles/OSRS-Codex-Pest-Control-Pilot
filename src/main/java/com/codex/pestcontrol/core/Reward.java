package com.codex.pestcontrol.core;

/**
 * The Void Knights' rewards that a goal can be made of, with their cost in commendation
 * points and every item that counts as already owning one (broken and ornamented copies too).
 * After the first id each piece lists its broken copy, its trouver-locked copy, the copy made with
 * the Shattered Relics void ornament kit and that one locked, then the locked copies broken and
 * mangled on death.
 */
public enum Reward
{
	TOP("Void knight top", "Top", 250, false, 8839, 20465, 24177, 26463, 27000, 33476, 33478),
	ROBE("Void knight robe", "Robe", 250, false, 8840, 20469, 24179, 26465, 27001, 33484, 33486),
	GLOVES("Void knight gloves", "Gloves", 150, false, 8842, 20475, 24182, 26467, 27002, 33492, 33494),
	MELEE_HELM("Void melee helm", "Melee helm", 200, false, 11665, 20481, 24185, 26477, 27007, 33464, 33466),
	RANGER_HELM("Void ranger helm", "Ranger helm", 200, false, 11664, 20479, 24184, 26475, 27006, 33472, 33474),
	MAGE_HELM("Void mage helm", "Mage helm", 200, false, 11663, 20477, 24183, 26473, 27005, 33468, 33470),
	ELITE_TOP("Elite void top", "Elite top", 200, true, 13072, 20467, 24178, 26469, 27003, 33480, 33482),
	ELITE_ROBE("Elite void robe", "Elite robe", 200, true, 13073, 20471, 24180, 26471, 27004, 33488, 33490),
	MACE("Void knight mace", "Mace", 250, false, 8841, 20473, 24181),
	SEAL("Void seal", "Seal", 10, false, 11666, 11667, 11668, 11669, 11670, 11671, 11672, 11673);

	private final String title;
	private final String shortTitle;
	private final int cost;
	private final boolean elite;
	private final int[] items;

	Reward(String title, String shortTitle, int cost, boolean elite, int... items)
	{
		this.title = title;
		this.shortTitle = shortTitle;
		this.cost = cost;
		this.elite = elite;
		this.items = items;
	}

	public String getTitle()
	{
		return title;
	}

	public String getShortTitle()
	{
		return shortTitle;
	}

	public int getCost()
	{
		return cost;
	}

	/** Elite pieces need the hard Western Provinces diary. */
	public boolean isElite()
	{
		return elite;
	}

	/** The item to draw for it. */
	public int getIcon()
	{
		return items[0];
	}

	public boolean isOwnedBy(int itemId)
	{
		for (int id : items)
		{
			if (id == itemId)
			{
				return true;
			}
		}
		return false;
	}

	/** Pieces worn as void equipment, which need level 42 combat skills and 22 Prayer. */
	public boolean isVoidGear()
	{
		return this != SEAL;
	}

	public static Reward named(String name)
	{
		for (Reward r : values())
		{
			if (r.name().equals(name))
			{
				return r;
			}
		}
		return null;
	}
}
