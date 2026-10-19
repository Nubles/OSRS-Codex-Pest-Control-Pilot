package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SittingTest
{
	private static final long MIN = 60_000L;

	/** A ten minute game ending at the given minute. */
	private static GameRecord game(long endMinute, GameRecord.Result result, int points)
	{
		return new GameRecord(endMinute * MIN, Boat.VETERAN, result, points, result == GameRecord.Result.NO_POINTS ? 20 : 200, 600);
	}

	@Test
	public void aBreakEndsTheSittingAndLanderTimeCounts()
	{
		// An old game, then three games each ten minutes long with two minutes in the lander between.
		List<GameRecord> games = Arrays.asList(game(0, GameRecord.Result.WON, 5),
			game(100, GameRecord.Result.WON, 5), game(112, GameRecord.Result.NO_POINTS, 0), game(124, GameRecord.Result.WON, 5));
		Sitting s = Sitting.latest(games, 125 * MIN);
		assertEquals(3, s.getGames().size());
		assertTrue(s.isCurrent());
		assertEquals(3, s.getWon());
		assertEquals(1, s.getUnpaid());
		assertEquals(10, s.getPoints());
		// From minute 90 to 124: 34 minutes for 10 points.
		assertEquals(34 * MIN, s.spanMs());
		assertEquals(Math.round(10 * 60 / 34.0), s.pointsPerHour());
	}

	@Test
	public void anOldOrShortSitting()
	{
		Sitting old = Sitting.latest(Collections.singletonList(game(0, GameRecord.Result.WON, 5)), 600 * MIN);
		assertFalse(old.isCurrent());
		assertEquals("one game has no lander time to rate", 0, old.pointsPerHour());
		assertTrue(Sitting.latest(Collections.emptyList(), 0).isEmpty());
		// A game starting exactly fifteen minutes after the last ended still belongs.
		assertEquals(2, Sitting.latest(Arrays.asList(game(0, GameRecord.Result.WON, 5), game(25, GameRecord.Result.WON, 5)), 30 * MIN)
			.getGames().size());
		assertEquals(1, Sitting.latest(Arrays.asList(game(0, GameRecord.Result.WON, 5), game(26, GameRecord.Result.WON, 5)), 30 * MIN)
			.getGames().size());
	}
}
