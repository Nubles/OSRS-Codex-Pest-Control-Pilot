package com.codex.pestcontrol.core;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The line written in the chat box after a game: what it gave, how far the goal is, and the
 * sitting so far with its points an hour, lander time counted. Warns when the points near the cap
 * the Void Knights will hold.
 */
public final class GameSummary
{
	/** The warning comes this close to the cap. */
	static final int CAP_WARN = 50;
	/** Damage a game needs before it gives points. */
	static final int ZEAL = 50;

	private GameSummary()
	{
	}

	/**
	 * @param after the goal as it stands after the game, or null
	 * @param sitting the sitting the game ends, or null
	 * @return the line, or null for a game left before the end
	 */
	public static String of(GameRecord game, Outlook after, Sitting sitting)
	{
		if (game == null || game.getResult() == GameRecord.Result.LEFT)
		{
			return null;
		}
		StringBuilder text = new StringBuilder();
		switch (game.getResult())
		{
			case WON:
				text.append("Won, +").append(game.getPoints()).append(game.getPoints() == 1 ? " point" : " points");
				break;
			case NO_POINTS:
				// The damage counted here can miss what the game counts; only say how short when it was.
				text.append(game.getDamage() < ZEAL ? "Won, but no points: " + game.getDamage() + " of the 50 damage needed"
					: "Won, but the game gave no points for too little damage");
				break;
			default:
				text.append("Lost");
				break;
		}
		if (game.getResult() != GameRecord.Result.NO_POINTS)
		{
			text.append(", ").append(game.getDamage()).append(" damage");
		}
		if (after != null && after.isPointsKnown())
		{
			if (!after.isComplete() && !after.isAffordable())
			{
				text.append(". ").append(number(after.getPoints())).append(" of ").append(number(after.getCost()))
					.append(" points for your goal, about ").append(number(after.getGames())).append(after.getGames() == 1 ? " game" : " games")
					.append(" to go");
			}
			if (after.getPoints() >= Logbook.POINTS_CAP - CAP_WARN)
			{
				text.append(". You hold ").append(number(after.getPoints())).append(" of the ")
					.append(number(Logbook.POINTS_CAP)).append(" points the knights will keep: spend some");
			}
		}
		if (sitting != null && sitting.getGames().size() >= 2)
		{
			text.append(". This sitting: ").append(number(sitting.getGames().size())).append(" games, ")
				.append(number(sitting.getPoints())).append(" points");
			if (sitting.pointsPerHour() > 0)
			{
				text.append(", ").append(sitting.pointsPerHour()).append(" an hour");
			}
		}
		return text.append(".").toString();
	}

	private static String number(long n)
	{
		return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(n);
	}
}
