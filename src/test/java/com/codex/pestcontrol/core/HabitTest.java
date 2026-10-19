package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class HabitTest
{
	private static final long MIN = 60_000L;
	private static final long DAY = 24 * 60 * MIN;

	/** Six games of nine minutes, each a minute in the lander after the last, from the given day. */
	private static void sitting(List<GameRecord> games, long day)
	{
		for (int i = 0; i < 6; i++)
		{
			long end = day * DAY + (i * 10 + 9) * MIN;
			games.add(new GameRecord(end, Boat.VETERAN, GameRecord.Result.WON, 5, 120, 9 * 60));
		}
	}

	@Test
	public void twoSittingsAWeekPutTwentyFourGamesTwoWeeksAway()
	{
		List<GameRecord> games = new ArrayList<>();
		for (long day : new long[]{100, 103, 107, 110})
		{
			sitting(games, day);
		}
		long now = 114 * DAY;
		Habit h = Habit.of(games, true, now);
		assertTrue(h.isKnown());
		// Each sitting runs 59 minutes for six games; two a week.
		assertEquals(2 * 59 / 60.0, h.hoursPerWeek(), 1e-9);
		// Twenty-four games is four sittings: two weeks.
		assertEquals(now + 14 * DAY, h.afterGames(24, now), 60_000);
	}

	@Test
	public void aFirstBusyWeekendIsSpreadOverAWeekUnlessOlderGamesWereDropped()
	{
		List<GameRecord> games = new ArrayList<>();
		sitting(games, 100);
		sitting(games, 101);
		long now = 102 * DAY;
		assertEquals(2 * 59 / 60.0, Habit.of(games, true, now).hoursPerWeek(), 1e-9);
		// With older games dropped from the logbook, the two days kept are all that is known.
		assertEquals(2 * 59 / 60.0 * 3.5, Habit.of(games, false, now).hoursPerWeek(), 1e-9);
	}

	@Test
	public void oneSittingSaysNothing()
	{
		List<GameRecord> games = new ArrayList<>();
		sitting(games, 100);
		Habit h = Habit.of(games, true, 101 * DAY);
		assertFalse(h.isKnown());
		assertEquals(-1, h.afterGames(5, 101 * DAY));
	}
}
