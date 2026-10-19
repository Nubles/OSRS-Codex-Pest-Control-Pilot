package com.codex.pestcontrol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(CodexPestControlConfig.GROUP)
public interface CodexPestControlConfig extends Config
{
	String GROUP = "codexpestcontrol";

	/** Which lander the goal's estimate assumes. */
	enum Lander
	{
		BEST_FOR_MY_LEVEL,
		NOVICE,
		INTERMEDIATE,
		VETERAN
	}

	/** Where the plaque shows. */
	enum PlaqueMode
	{
		IN_GAMES,
		AT_THE_OUTPOST,
		NEVER
	}

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a plaque in the game view with what to work next. At the outpost it also shows on the island and in the lander, with your points toward the goal",
		position = 0
	)
	default PlaqueMode plaque()
	{
		return PlaqueMode.AT_THE_OUTPOST;
	}

	@ConfigItem(
		keyName = "markPortal",
		name = "Mark the portal",
		description = "Outline the portal to work next in the world and on the minimap",
		position = 1
	)
	default boolean markPortal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markCandidates",
		name = "Mark possible next portals",
		description = "While no shield is down, faintly outline every portal whose shield could drop next",
		position = 2
	)
	default boolean markCandidates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "knightWarn",
		name = "Defend knight below",
		description = "When the Void Knight's health falls to this or lower and no shield is down, advise defending him. 0 turns it off",
		position = 3
	)
	@Range(min = 0, max = 200)
	default int knightWarn()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "lander",
		name = "Lander for estimates",
		description = "Which lander the games-left estimate assumes until your own games have been measured",
		position = 4
	)
	default Lander lander()
	{
		return Lander.BEST_FOR_MY_LEVEL;
	}

	@ConfigItem(
		keyName = "announce",
		name = "Announce affordable rewards",
		description = "After a game, say in the chat box when your points first cover a piece of your goal",
		position = 5
	)
	default boolean announce()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gameSummary",
		name = "Game summary in chat",
		description = "After a game, write its points and damage, how far the goal is and the sitting's points an hour in the chat box",
		position = 6
	)
	default boolean gameSummary()
	{
		return true;
	}
}
