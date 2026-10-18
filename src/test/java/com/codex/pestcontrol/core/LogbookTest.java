package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.EnumSet;
import org.junit.Test;

public class LogbookTest
{
	private static final long MIN = 60_000L;

	@Test
	public void assumesTheBoatUntilThreeGames()
	{
		Logbook log = new Logbook();
		assertEquals(5, log.pointsPerGame(Boat.VETERAN), 0);
		log.record(new GameRecord(10 * MIN, Boat.VETERAN, GameRecord.Result.WON, 5, 120, 150));
		log.record(new GameRecord(14 * MIN, Boat.VETERAN, GameRecord.Result.LOST, 0, 80, 200));
		assertFalse(log.isMeasured());
		log.record(new GameRecord(18 * MIN, Boat.VETERAN, GameRecord.Result.WON, 5, 90, 160));
		assertTrue(log.isMeasured());
		assertEquals(10 / 3.0, log.pointsPerGame(Boat.VETERAN), 1e-9);
		assertEquals(4, log.minutesPerGame(), 1e-9);
		assertEquals(3, log.getPlayed());
		assertEquals(2, log.getWon());
		assertEquals(10, log.getEarned());
	}

	@Test
	public void breaksAndLeftGamesDoNotCount()
	{
		Logbook log = new Logbook();
		log.record(new GameRecord(0, Boat.NOVICE, GameRecord.Result.WON, 3, 60, 180));
		log.record(new GameRecord(60 * MIN, Boat.NOVICE, GameRecord.Result.WON, 3, 60, 180));
		log.record(new GameRecord(61 * MIN, Boat.NOVICE, GameRecord.Result.LEFT, 0, 10, 30));
		assertEquals(2, log.getPlayed());
		// Only one break-length gap, so the lander time is assumed.
		assertEquals(4, log.minutesPerGame(), 1e-9);
	}

	@Test
	public void keepsTheNewestGames()
	{
		Logbook log = new Logbook();
		for (int i = 0; i < Logbook.KEEP + 5; i++)
		{
			log.record(new GameRecord(i * MIN, Boat.NOVICE, GameRecord.Result.WON, 3, 60, 100));
		}
		assertEquals(Logbook.KEEP, log.getGames().size());
		assertEquals(Logbook.KEEP + 5, log.getPlayed());
	}

	@Test
	public void goalToggles()
	{
		Logbook log = new Logbook();
		assertEquals(EnumSet.copyOf(Goal.MELEE.getRewards()), log.getGoal());
		log.toggle(Reward.MACE);
		assertTrue(log.getGoal().contains(Reward.MACE));
		log.toggle(Reward.MACE);
		assertFalse(log.getGoal().contains(Reward.MACE));
		assertTrue(log.seeOwned(EnumSet.of(Reward.TOP), 5));
		assertFalse(log.seeOwned(EnumSet.of(Reward.TOP), 6));
	}
}
