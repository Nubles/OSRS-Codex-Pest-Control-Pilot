package com.codex.pestcontrol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Range;

@ConfigGroup(CodexPestControlConfig.GROUP)
public interface CodexPestControlConfig extends Config
{
	String GROUP = "codexpestcontrol";

	/** Which lander the goal's estimate assumes. */
	enum Lander
	{
		BEST_FOR_MY_LEVEL("Best for my level"),
		NOVICE("Novice"),
		INTERMEDIATE("Intermediate"),
		VETERAN("Veteran");

		private final String shown;

		Lander(String shown)
		{
			this.shown = shown;
		}

		@Override
		public String toString()
		{
			return shown;
		}
	}

	/** Where the plaque shows. */
	enum PlaqueMode
	{
		IN_GAMES("In games"),
		AT_THE_OUTPOST("Games and outpost"),
		NEVER("Never");

		private final String shown;

		PlaqueMode(String shown)
		{
			this.shown = shown;
		}

		@Override
		public String toString()
		{
			return shown;
		}
	}

	@ConfigSection(
		name = "In game",
		description = "What Pest Control Pilot draws over the game",
		position = 0
	)
	String inGame = "inGame";

	@ConfigSection(
		name = "Chat",
		description = "What Pest Control Pilot says in the chat box",
		position = 1
	)
	String chat = "chat";

	@ConfigSection(
		name = "Alerts",
		description = "When Pest Control Pilot sends a notification, useful with the client in the background",
		position = 2
	)
	String alerts = "alerts";

	@ConfigSection(
		name = "Goal and card",
		description = "How the goal is counted and what the void card shows",
		position = 3
	)
	String goal = "goal";

	@ConfigItem(
		keyName = "plaque",
		name = "Pilot plaque",
		description = "Show a plaque in the game view with what to work next, and if you like at the outpost and in the lander with your points toward the goal",
		position = 0,
		section = inGame
	)
	default PlaqueMode plaque()
	{
		return PlaqueMode.AT_THE_OUTPOST;
	}

	@ConfigItem(
		keyName = "markPortal",
		name = "Mark the portal",
		description = "Outline the portal to work next in the world and on the minimap",
		position = 1,
		section = inGame
	)
	default boolean markPortal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markCandidates",
		name = "Mark possible next portals",
		description = "While no shield is down, faintly outline every portal whose shield could drop next",
		position = 2,
		section = inGame
	)
	default boolean markCandidates()
	{
		return true;
	}

	@ConfigItem(
		keyName = "knightWarn",
		name = "Defend knight below",
		description = "Advise defending the Void Knight when his health falls to this or lower and no shield is down; 0 turns it off",
		position = 3,
		section = inGame
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
		position = 0,
		section = goal
	)
	default Lander lander()
	{
		return Lander.BEST_FOR_MY_LEVEL;
	}

	@ConfigItem(
		keyName = "announce",
		name = "Affordable pieces in chat",
		description = "After a game, say in the chat box when your points first buy a piece of your goal",
		position = 1,
		section = chat
	)
	default boolean announce()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gameSummary",
		name = "Game summary in chat",
		description = "After a game, write its points and damage, how far the goal is and the sitting's points an hour in the chat box",
		position = 0,
		section = chat
	)
	default boolean gameSummary()
	{
		return true;
	}

	@ConfigItem(
		keyName = "voidCheck",
		name = "Void check on the plaque",
		description = "Before a game, say on the plaque when the void you wear gives no set bonus, such as a piece missing or a helm for another style",
		position = 4,
		section = inGame
	)
	default boolean voidCheck()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nameOnCard",
		name = "Name on cards",
		description = "Write your character's name on the void card; turn off to share it without it",
		position = 1,
		section = goal
	)
	default boolean nameOnCard()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markShop",
		name = "Mark the reward shop",
		description = "In the Void Knights' reward shop, outline the pieces of your goal, the ones your points buy now in teal",
		position = 5,
		section = inGame
	)
	default boolean markShop()
	{
		return true;
	}

	@ConfigItem(
		keyName = "notifyStart",
		name = "Game start",
		description = "Notify when a game begins, so you can wait in the lander with the client in the background",
		position = 0,
		section = alerts
	)
	default Notification notifyStart()
	{
		return Notification.ON;
	}

	@ConfigItem(
		keyName = "notifyShield",
		name = "Shield drop",
		description = "Notify when a portal's shield drops, naming its gate and the portal due next",
		position = 1,
		section = alerts
	)
	default Notification notifyShield()
	{
		return Notification.OFF;
	}

	@ConfigItem(
		keyName = "notifyKnight",
		name = "Knight in danger",
		description = "Notify once a game when the Void Knight's health falls to the Defend knight below setting",
		position = 2,
		section = alerts
	)
	default Notification notifyKnight()
	{
		return Notification.OFF;
	}
}
