package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * How each lander has gone for you, from your own games: how often you win there, the points a
 * game really gives once losses are counted, how long one game takes from the end of the last,
 * and so the points an hour. A veteran game pays more but a quick intermediate world can beat it.
 */
public final class Landers
{
	/** Games on a lander before its figures are trusted for a recommendation. */
	public static final int TRUSTED = 3;

	/** One lander's figures. */
	public static final class Line
	{
		private final Boat boat;
		private final int games;
		private final int won;
		private final double pointsPerGame;
		private final double minutesPerGame;
		private final double averageDamage;

		Line(Boat boat, int games, int won, double pointsPerGame, double minutesPerGame, double averageDamage)
		{
			this.boat = boat;
			this.games = games;
			this.won = won;
			this.pointsPerGame = pointsPerGame;
			this.minutesPerGame = minutesPerGame;
			this.averageDamage = averageDamage;
		}

		public Boat getBoat()
		{
			return boat;
		}

		public int getGames()
		{
			return games;
		}

		public int getWon()
		{
			return won;
		}

		public double winRate()
		{
			return games == 0 ? 0 : won / (double) games;
		}

		public double getPointsPerGame()
		{
			return pointsPerGame;
		}

		/** Minutes from the end of one game to the end of the next, lander wait included. */
		public double getMinutesPerGame()
		{
			return minutesPerGame;
		}

		public double getAverageDamage()
		{
			return averageDamage;
		}

		public double pointsPerHour()
		{
			return minutesPerGame <= 0 ? 0 : pointsPerGame * 60 / minutesPerGame;
		}

		public boolean isTrusted()
		{
			return games >= TRUSTED;
		}
	}

	private Landers()
	{
	}

	/**
	 * Each lander's figures from the games kept, in lander order, leaving out landers never played.
	 * Games left early are not counted.
	 */
	public static List<Line> of(List<GameRecord> games)
	{
		Map<Boat, int[]> counts = new EnumMap<>(Boat.class);
		Map<Boat, double[]> sums = new EnumMap<>(Boat.class);
		GameRecord previous = null;
		for (GameRecord g : games)
		{
			if (g.getResult() == GameRecord.Result.LEFT)
			{
				previous = null;
				continue;
			}
			Boat b = g.getBoat();
			if (b == null)
			{
				previous = g;
				continue;
			}
			int[] c = counts.computeIfAbsent(b, k -> new int[3]);
			double[] s = sums.computeIfAbsent(b, k -> new double[4]);
			c[0]++;
			if (g.getResult() != GameRecord.Result.LOST)
			{
				c[1]++;
			}
			s[0] += g.getPoints();
			s[1] += g.getSeconds();
			s[3] += g.getDamage();
			// Back to back on the same lander: the gap between endings is a whole game with its wait.
			if (previous != null && previous.getBoat() == b)
			{
				long gap = g.getEnded() - previous.getEnded();
				if (gap > 0 && gap < Logbook.BREAK_MS)
				{
					s[2] += gap / 60_000.0;
					c[2]++;
				}
			}
			previous = g;
		}
		List<Line> out = new ArrayList<>();
		for (Boat b : Boat.values())
		{
			int[] c = counts.get(b);
			if (c == null)
			{
				continue;
			}
			double[] s = sums.get(b);
			double minutes = c[2] >= 2 ? s[2] / c[2] : (s[1] / c[0] + Logbook.LANDER_SECONDS) / 60.0;
			out.add(new Line(b, c[0], c[1], s[0] / c[0], minutes, s[3] / c[0]));
		}
		return out;
	}

	/** The trusted lander with the most points an hour that the combat level can board, or null. */
	public static Line best(List<Line> lines, int combat)
	{
		Line best = null;
		for (Line l : lines)
		{
			if (l.isTrusted() && combat >= l.getBoat().getCombat() && (best == null || l.pointsPerHour() > best.pointsPerHour()))
			{
				best = l;
			}
		}
		return best;
	}

	/** The line for a lander, or null when it has not been played. */
	public static Line of(List<Line> lines, Boat boat)
	{
		for (Line l : lines)
		{
			if (l.getBoat() == boat)
			{
				return l;
			}
		}
		return null;
	}
}
