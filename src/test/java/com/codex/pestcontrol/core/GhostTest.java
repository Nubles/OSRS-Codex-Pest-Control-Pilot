package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.List;
import org.junit.Test;

public class GhostTest
{
	/**
	 * A game played tick by tick: shields drop 25 ticks in and every 50 after (the wiki's 15 and 30
	 * seconds), in the order purple, blue, yellow, red; each portal falls at the tick given.
	 */
	private static Battle battle(int wait, int... kills)
	{
		Battle b = new Battle();
		b.setWhole(true);
		b.setWait(wait);
		Portal[] order = {Portal.PURPLE, Portal.BLUE, Portal.YELLOW, Portal.RED};
		int end = kills.length == 0 ? 0 : kills[kills.length - 1];
		for (int t = 1; t <= end; t++)
		{
			b.tick();
			for (int i = 0; i < 4; i++)
			{
				if (t == 25 + 50 * i)
				{
					b.shieldDropped(order[i]);
				}
				if (i < kills.length && t == kills[i])
				{
					b.setHealth(order[i], 0);
				}
			}
		}
		return b;
	}

	private static GameRecord won(int wait, int... kills)
	{
		return Ghost.record(battle(wait, kills), 1000, Boat.NOVICE, GameRecord.Result.WON, 3);
	}

	@Test
	public void aGameIsCutIntoTheWaitTheLandingEachPortalAndTheEnd()
	{
		GameRecord g = won(40, 60, 110, 150, 200);
		assertEquals(40, Ghost.ticks(g, Ghost.Segment.BOAT));
		assertEquals(25, Ghost.ticks(g, Ghost.Segment.LANDING));
		assertEquals(35, Ghost.ticks(g, Ghost.Segment.FIRST));
		assertEquals(50, Ghost.ticks(g, Ghost.Segment.SECOND));
		assertEquals(40, Ghost.ticks(g, Ghost.Segment.THIRD));
		assertEquals(50, Ghost.ticks(g, Ghost.Segment.FOURTH));
		assertEquals(0, Ghost.ticks(g, Ghost.Segment.END));
		assertEquals(200, g.getTicks());
		assertEquals(120, g.getSeconds());
		assertEquals(Portal.BLUE, g.killed(2));
		assertTrue(Ghost.usable(g));
		// 3 points in 240 ticks: 75 an hour.
		assertEquals(75, Ghost.rate(g), 1e-9);
	}

	@Test
	public void theSplitTableTimesTheSegmentUnderWayAndHoldsItsGapUntilItOpens()
	{
		GameRecord ghost = won(30, 60, 110, 150, 200);
		Battle b = battle(30, 60);
		for (int t = 0; t < 30; t++)
		{
			b.tick();
		}
		// 90 ticks in: purple fell at 60, blue dropped at 75 and is being worked.
		GameRecord now = Ghost.record(b, 0, Boat.NOVICE, GameRecord.Result.WON, 0);
		List<Ghost.Row> rows = Ghost.rows(now, ghost, true);
		assertEquals(Ghost.Segment.SECOND, Ghost.running(now));
		Ghost.Row blue = rows.get(Ghost.Segment.SECOND.ordinal());
		assertTrue(blue.isLive());
		assertEquals(30, blue.getMine());
		assertNull(blue.getDelta());
		assertEquals(-1, rows.get(Ghost.Segment.THIRD.ordinal()).getMine());
		assertEquals(-1, rows.get(Ghost.Segment.END.ordinal()).getMine());
		assertEquals(Integer.valueOf(0), rows.get(Ghost.Segment.FIRST.ordinal()).getDelta());
		for (int t = 0; t < 25; t++)
		{
			b.tick();
		}
		// 55 ticks on blue, five over the ghost's 50.
		Ghost.Row later = Ghost.rows(Ghost.record(b, 0, Boat.NOVICE, GameRecord.Result.WON, 0), ghost, true).get(Ghost.Segment.SECOND.ordinal());
		assertEquals(Integer.valueOf(5), later.getDelta());
		assertEquals(Integer.valueOf(0), Ghost.behind(now, ghost));
	}

	@Test
	public void theLetterIsPointsAnHourAgainstTheGhostCountingTheWait()
	{
		GameRecord ghost = won(30, 60, 110, 150, 200);
		assertEquals(Grade.Letter.S, Grade.of(ghost, ghost).getLetter());
		// 230 ticks against 260: 88.5% of the ghost's rate.
		Grade b = Grade.of(won(30, 60, 160, 190, 230), ghost);
		assertEquals(Grade.Letter.B, b.getLetter());
		assertEquals("2nd portal down (Blue): 100 ticks, ghost took 50", b.getSinks().get(0).sentence());
		assertEquals(1, b.getSinks().size());
		// A longer wait in the lander costs the rate too, and is named.
		Grade a = Grade.of(won(50, 60, 110, 150, 200), ghost);
		assertEquals(Grade.Letter.A, a.getLetter());
		assertEquals("Boat: 50 ticks waiting in the lander, ghost took 30", a.getSinks().get(0).sentence());
		Grade c = Grade.of(won(150, 60, 110, 150, 200), ghost);
		assertEquals(Grade.Letter.C, c.getLetter());
		Grade s = Grade.of(won(20, 55, 100, 140, 190), ghost);
		assertEquals(Grade.Letter.S, s.getLetter());
		assertTrue(s.isNewGhost());
		assertTrue(s.getSinks().isEmpty());
	}

	@Test
	public void aGameWithNoPointsIsACAndOneLeftEarlyIsNotGraded()
	{
		GameRecord ghost = won(30, 60, 110, 150, 200);
		GameRecord lost = Ghost.record(battle(30, 60, 110), 0, Boat.NOVICE, GameRecord.Result.LOST, 0);
		Grade g = Grade.of(lost, ghost);
		assertEquals(Grade.Letter.C, g.getLetter());
		assertFalse(g.isNewGhost());
		// The end is timed only after the last portal: a lost game with two standing has none.
		assertEquals(-1, Ghost.ticks(lost, Ghost.Segment.END));
		assertEquals(-1, Ghost.ticks(lost, Ghost.Segment.THIRD));
		assertNull(Grade.of(Ghost.record(battle(30, 60), 0, Boat.NOVICE, GameRecord.Result.LEFT, 0), ghost));
		GameRecord noZeal = Ghost.record(battle(30, 60, 110, 150, 200), 0, Boat.NOVICE, GameRecord.Result.NO_POINTS, 0);
		assertEquals(Grade.Letter.C, Grade.of(noZeal, ghost).getLetter());
		assertFalse(Ghost.usable(noZeal));
	}

	@Test
	public void theFirstWinOnALanderSetsTheGhost()
	{
		Grade g = Grade.of(won(30, 60, 110, 150, 200), null);
		assertNull(g.getLetter());
		assertTrue(g.isNewGhost());
		Grade lost = Grade.of(Ghost.record(battle(30, 60), 0, Boat.NOVICE, GameRecord.Result.LOST, 0), null);
		assertNull(lost.getLetter());
		assertFalse(lost.isNewGhost());
	}

	@Test
	public void onlyAWholeTimedWinCanBeAGhost()
	{
		// Not seen boarding: no wait.
		assertFalse(Ghost.usable(Ghost.record(battle(-1, 60, 110, 150, 200), 0, Boat.NOVICE, GameRecord.Result.WON, 3)));
		// A portal found already open: its drop is not timed.
		Battle b = battle(30);
		b.foundOpen(Portal.PURPLE);
		assertFalse(Ghost.usable(Ghost.record(b, 0, Boat.NOVICE, GameRecord.Result.WON, 3)));
		// No lander known.
		assertFalse(Ghost.usable(Ghost.record(battle(30, 60, 110, 150, 200), 0, null, GameRecord.Result.WON, 3)));
		// Saved before ghost splits.
		assertFalse(Ghost.usable(new GameRecord(0, Boat.NOVICE, GameRecord.Result.WON, 3, 100, 120)));
	}

	@Test
	public void ghostsAreKeptByLanderAndOnlyAQuickerWinReplacesOne()
	{
		Logbook book = new Logbook();
		GameRecord ghost = won(30, 60, 110, 150, 200);
		assertTrue(book.offerGhost(ghost));
		assertFalse(book.offerGhost(won(30, 60, 160, 190, 230)));
		assertSame(ghost, book.ghost(Boat.NOVICE));
		assertNull(book.ghost(Boat.VETERAN));
		GameRecord vet = Ghost.record(battle(30, 60, 110, 150, 200), 0, Boat.VETERAN, GameRecord.Result.WON, 5);
		assertTrue(book.offerGhost(vet));
		assertSame(vet, book.ghost(Boat.VETERAN));
		GameRecord quicker = won(20, 60, 110, 150, 200);
		assertTrue(book.offerGhost(quicker));
		assertSame(quicker, book.ghost(Boat.NOVICE));
	}

	@Test
	public void ghostsSurviveASaveAndOutliveTheGamesKept()
	{
		Gson gson = new Gson();
		Logbook book = new Logbook();
		GameRecord ghost = won(30, 60, 110, 150, 200);
		book.offerGhost(ghost);
		for (int i = 0; i < Logbook.KEEP + 5; i++)
		{
			book.record(new GameRecord(i, Boat.NOVICE, GameRecord.Result.WON, 3, 80, 300));
		}
		Logbook back = Logbook.read(gson, gson.toJson(book));
		GameRecord kept = back.ghost(Boat.NOVICE);
		assertNotNull(kept);
		assertEquals(Logbook.KEEP, back.getGames().size());
		for (Ghost.Segment s : Ghost.Segment.values())
		{
			assertEquals(s.name(), Ghost.ticks(ghost, s), Ghost.ticks(kept, s));
		}
		assertEquals(Portal.RED, kept.killed(4));
	}

	@Test
	public void aDamagedGhostIsLostAloneAndTheRestOfTheSaveIsKept()
	{
		Gson gson = new Gson();
		Logbook book = new Logbook();
		book.offerGhost(won(30, 60, 110, 150, 200));
		book.offerGhost(Ghost.record(battle(30, 60, 110, 150, 200), 0, Boat.VETERAN, GameRecord.Result.WON, 5));
		book.seePoints(321, 5);
		JsonObject json = gson.fromJson(gson.toJson(book), JsonObject.class);
		json.getAsJsonObject("ghosts").add("NOVICE", new JsonPrimitive(7));
		Logbook back = Logbook.read(gson, gson.toJson(json));
		assertNull(back.ghost(Boat.NOVICE));
		assertNotNull(back.ghost(Boat.VETERAN));
		assertEquals(321, back.getPoints());
		// A ghost filed under the wrong lander is not raced on either.
		JsonObject moved = gson.fromJson(gson.toJson(book), JsonObject.class);
		moved.getAsJsonObject("ghosts").add("INTERMEDIATE", moved.getAsJsonObject("ghosts").get("VETERAN"));
		moved.getAsJsonObject("ghosts").remove("VETERAN");
		assertEquals(1, Logbook.read(gson, gson.toJson(moved)).ghostCount());
	}
}
