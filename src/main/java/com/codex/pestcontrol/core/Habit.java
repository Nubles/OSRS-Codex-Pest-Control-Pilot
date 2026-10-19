package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.List;

/**
 * How much the player plays Pest Control, read from their own sittings over the last four weeks,
 * so a number of games can be turned into a date: "the set is yours about 12 Nov". A first busy
 * weekend is spread over a week, so it does not read as playing every day, unless the logbook no
 * longer holds the older games, in which case only the time its games span is counted.
 */
public final class Habit
{
	static final long DAY_MS = 24 * 3_600_000L;
	static final long WINDOW_MS = 28 * DAY_MS;
	static final long MIN_SPAN_MS = 7 * DAY_MS;
	/** Fewer sittings than this say nothing about how often the player comes back. */
	static final int MIN_SITTINGS = 2;
	/** Past this a date says nothing useful. */
	static final long MAX_DAYS = 3 * 365;

	private final int sittings;
	private final int games;
	private final long playedMs;
	private final long spanMs;

	private Habit(int sittings, int games, long playedMs, long spanMs)
	{
		this.sittings = sittings;
		this.games = games;
		this.playedMs = playedMs;
		this.spanMs = spanMs;
	}

	/**
	 * @param games the games kept, oldest first
	 * @param complete whether the logbook still holds every game ever played, so the oldest kept is the first
	 */
	public static Habit of(List<GameRecord> games, boolean complete, long now)
	{
		List<List<GameRecord>> sittings = new ArrayList<>();
		List<GameRecord> group = null;
		for (GameRecord g : games)
		{
			long start = g.getEnded() - g.getSeconds() * 1000L;
			if (start < now - WINDOW_MS || g.getEnded() > now)
			{
				continue;
			}
			if (group == null || start - group.get(group.size() - 1).getEnded() > Logbook.BREAK_MS)
			{
				group = new ArrayList<>();
				sittings.add(group);
			}
			group.add(g);
		}
		int n = 0;
		long played = 0;
		long first = now;
		for (List<GameRecord> s : sittings)
		{
			GameRecord a = s.get(0);
			long start = a.getEnded() - a.getSeconds() * 1000L;
			played += Math.max(0, s.get(s.size() - 1).getEnded() - start);
			n += s.size();
			first = Math.min(first, start);
		}
		long span = Math.max(DAY_MS, now - first);
		if (complete)
		{
			span = Math.max(MIN_SPAN_MS, span);
		}
		return new Habit(sittings.size(), n, played, span);
	}

	/** Enough sittings to tell a pace from. */
	public boolean isKnown()
	{
		return sittings >= MIN_SITTINGS && playedMs > 0 && games > 0;
	}

	/** Hours played a week. */
	public double hoursPerWeek()
	{
		return playedMs / 3_600_000.0 * (7.0 * DAY_MS / spanMs);
	}

	/** Games played an hour, the lander time between them included. */
	public double gamesPerHour()
	{
		return playedMs <= 0 ? 0 : games * 3_600_000.0 / playedMs;
	}

	/** @return when that many more games are played at this pace, or -1 when it cannot be told or is years away */
	public long afterGames(long more, long now)
	{
		if (!isKnown() || more < 0)
		{
			return -1;
		}
		double days = more / gamesPerHour() / hoursPerWeek() * 7;
		return days > MAX_DAYS ? -1 : now + Math.round(days * DAY_MS);
	}
}
