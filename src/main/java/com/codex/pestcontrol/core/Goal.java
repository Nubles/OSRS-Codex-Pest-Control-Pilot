package com.codex.pestcontrol.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** The ready-made goals: the usual void sets. Any set of rewards can be a goal too. */
public enum Goal
{
	MELEE("Void melee set", "Melee", Reward.TOP, Reward.ROBE, Reward.GLOVES, Reward.MELEE_HELM),
	RANGER("Void ranger set", "Ranger", Reward.TOP, Reward.ROBE, Reward.GLOVES, Reward.RANGER_HELM),
	MAGE("Void mage set", "Mage", Reward.TOP, Reward.ROBE, Reward.GLOVES, Reward.MAGE_HELM),
	ALL("Void, all three helms", "All", Reward.TOP, Reward.ROBE, Reward.GLOVES, Reward.MELEE_HELM, Reward.RANGER_HELM, Reward.MAGE_HELM),
	ELITE("Elite void", "Elite", Reward.ELITE_TOP, Reward.ELITE_ROBE, Reward.GLOVES, Reward.RANGER_HELM);

	private final String title;
	private final String shortTitle;
	private final List<Reward> rewards;

	Goal(String title, String shortTitle, Reward... rewards)
	{
		this.title = title;
		this.shortTitle = shortTitle;
		this.rewards = Collections.unmodifiableList(Arrays.asList(rewards));
	}

	public String getTitle()
	{
		return title;
	}

	public String getShortTitle()
	{
		return shortTitle;
	}

	public List<Reward> getRewards()
	{
		return rewards;
	}

	/** The preset these rewards make up exactly, or null for a goal of your own. */
	public static Goal matching(Set<Reward> chosen)
	{
		for (Goal g : values())
		{
			if (EnumSet.copyOf(g.rewards).equals(chosen))
			{
				return g;
			}
		}
		return null;
	}
}
