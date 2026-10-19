package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One sitting at the outpost: games played back to back, with no break longer than
 * {@link Logbook#BREAK_MS} between one ending and the next starting. Its rate counts the time in
 * the lander between games, which the record's "in games" rate leaves out, so it is what an hour
 * at Pest Control really pays.
 */
public final class Sitting
{
	private final List<GameRecord> games;
	private final boolean current;
	private final int won;
	private final int points;
	private final int unpaid;

	private Sitting(List<GameRecord> games, boolean current)
	{
		this.games = Collections.unmodifiableList(games);
		this.current = current;
		int w = 0;
		int p = 0;
		int u = 0;
		for (GameRecord g : games)
		{
			w += g.getResult() == GameRecord.Result.WON || g.getResult() == GameRecord.Result.NO_POINTS ? 1 : 0;
			p += g.getPoints();
			u += g.getResult() == GameRecord.Result.NO_POINTS ? 1 : 0;
		}
		won = w;
		points = p;
		unpaid = u;
	}

	/**
	 * The newest sitting in the games.
	 *
	 * @param games the games kept, oldest first
	 * @param now the time now, to tell a sitting still going from one that has ended
	 */
	public static Sitting latest(List<GameRecord> games, long now)
	{
		List<GameRecord> picked = new ArrayList<>();
		if (games != null)
		{
			for (int i = games.size() - 1; i >= 0; i--)
			{
				GameRecord g = games.get(i);
				if (!picked.isEmpty() && start(picked.get(0)) - g.getEnded() > Logbook.BREAK_MS)
				{
					break;
				}
				picked.add(0, g);
			}
		}
		boolean current = !picked.isEmpty() && now - picked.get(picked.size() - 1).getEnded() <= Logbook.BREAK_MS;
		return new Sitting(picked, current);
	}

	private static long start(GameRecord g)
	{
		return g.getEnded() - g.getSeconds() * 1000L;
	}

	public boolean isEmpty()
	{
		return games.isEmpty();
	}

	/** Whether the last game ended recently enough that the sitting is still going. */
	public boolean isCurrent()
	{
		return current;
	}

	public List<GameRecord> getGames()
	{
		return games;
	}

	/** Games won, with or without the damage for points. */
	public int getWon()
	{
		return won;
	}

	public int getPoints()
	{
		return points;
	}

	/** Games won without the 50 damage for points. */
	public int getUnpaid()
	{
		return unpaid;
	}

	public long getStartedAt()
	{
		return games.isEmpty() ? 0 : start(games.get(0));
	}

	/** From the first game starting to the last ending, the lander time between them included. */
	public long spanMs()
	{
		return games.isEmpty() ? 0 : Math.max(0, games.get(games.size() - 1).getEnded() - getStartedAt());
	}

	/**
	 * Points an hour over the sitting, or 0 before there are two games (one game has no lander time
	 * in it to count) or ten minutes, when the figure means little.
	 */
	public int pointsPerHour()
	{
		long span = spanMs();
		return games.size() < 2 || span < 10 * 60_000L ? 0 : (int) Math.round(points * 3_600_000.0 / span);
	}
}
