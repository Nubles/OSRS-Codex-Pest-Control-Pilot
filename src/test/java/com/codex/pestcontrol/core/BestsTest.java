package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public class BestsTest
{
	private static GameRecord game(GameRecord.Result r, int damage, int seconds, long at)
	{
		return new GameRecord(at, Boat.VETERAN, r, r == GameRecord.Result.WON ? 5 : 0, damage, seconds);
	}

	@Test
	public void theFirstGamesAreBestsWithoutBeingNews()
	{
		Bests b = new Bests();
		for (int i = 0; i < Bests.NEWS_AFTER; i++)
		{
			assertTrue(b.add(game(GameRecord.Result.WON, 100 + i * 10, 200 - i, i)).isEmpty());
		}
		assertEquals(140, b.getDamage());
		assertEquals(196, b.getFastest());
		assertEquals(5, b.getBestStreak());
	}

	@Test
	public void aGameThatBeatsTheBestsSaysSoOnce()
	{
		Bests b = new Bests();
		for (int i = 0; i < 6; i++)
		{
			b.add(game(GameRecord.Result.WON, 100, 200, i));
		}
		b.add(game(GameRecord.Result.LOST, 50, 300, 7));
		for (int i = 0; i < 6; i++)
		{
			assertTrue(b.add(game(GameRecord.Result.WON, 90, 210, 8 + i)).isEmpty());
		}
		Set<Bests.Kind> beat = b.add(game(GameRecord.Result.WON, 180, 150, 20));
		assertEquals(EnumSet.allOf(Bests.Kind.class), beat);
		assertEquals("A new best: 180 damage, a win in 2m 30s, 7 wins in a row", b.news(beat));
		assertTrue("a longer run is not news again", b.add(game(GameRecord.Result.WON, 90, 210, 21)).isEmpty());
		assertEquals(8, b.getBestStreak());
		assertNull(b.news(EnumSet.noneOf(Bests.Kind.class)));
	}

	@Test
	public void aLossEndsTheRunAndAGameLeftCountsForNothing()
	{
		Bests b = new Bests();
		b.add(game(GameRecord.Result.WON, 80, 200, 1));
		b.add(game(GameRecord.Result.LEFT, 500, 30, 2));
		b.add(game(GameRecord.Result.WON, 80, 200, 3));
		assertEquals(2, b.getStreak());
		assertEquals(80, b.getDamage());
		b.add(game(GameRecord.Result.LOST, 70, 200, 4));
		assertEquals(0, b.getStreak());
		assertEquals(2, b.getBestStreak());
	}

	@Test
	public void aWinSeenOnlyAtItsEndIsNotTheFastest()
	{
		Bests b = new Bests();
		b.add(game(GameRecord.Result.WON, 80, 40, 1));
		assertEquals(0, b.getFastest());
		b.add(game(GameRecord.Result.NO_POINTS, 20, 90, 2));
		assertEquals("a win with no points is not counted as fastest", 0, b.getFastest());
	}

	@Test
	public void anOldSaveWorksItsBestsOutFromItsGamesAndTheyAreSaved()
	{
		Gson gson = new Gson();
		Logbook old = DamagedDataTest.good(30);
		String json = gson.toJson(old).replaceAll(",?\"bests\":\\{[^}]*\\}", "");
		assertTrue(!json.contains("bests"));
		Logbook read = Logbook.read(gson, json);
		Bests b = read.getBests();
		assertTrue(b.getDamage() > 0);
		assertTrue(b.getBestStreak() > 0);
		Logbook again = Logbook.read(gson, gson.toJson(read));
		assertEquals(b.getDamage(), again.getBests().getDamage());
		assertEquals(b.getFastest(), again.getBests().getFastest());
	}

	@Test
	public void aGamePickedUpPartWayIsNeverTheFastestWin()
	{
		Bests b = new Bests();
		b.add(game(GameRecord.Result.WON, 80, 200, 1));
		b.add(game(GameRecord.Result.WON, 80, 90, 2), false);
		assertEquals(200, b.getFastest());
		assertEquals(2, b.getBestStreak());
	}

	@Test
	public void bestsADamagedSaveLostComeBackFromTheGames()
	{
		Gson gson = new Gson();
		Logbook old = DamagedDataTest.good(30);
		int damage = old.getBests().getDamage();
		int fastest = old.getBests().getFastest();
		String json = gson.toJson(old).replaceAll("\"damage\":[0-9]+,\"damageAt\"", "\"damage\":\"x\",\"damageAt\"")
			.replaceAll("\"fastest\":[0-9]+", "\"fastest\":-4");
		Bests read = Logbook.read(gson, json).getBests();
		assertEquals(damage, read.getDamage());
		assertEquals(fastest, read.getFastest());
	}
}
