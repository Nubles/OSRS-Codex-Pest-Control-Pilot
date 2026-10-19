package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/**
 * Every saved value Pest Control Pilot reads survives damage: its own {@code logbook}, Bank
 * Steward's {@code bankV1}, Gear Roadmap's {@code bank} and Navigator's {@code goalBoard}.
 * Whatever the profile holds, nothing throws, the goal and records still come out sensible, and
 * damage to one part does not cost the rest.
 */
@SuppressWarnings("unchecked")
public class DamagedDataTest
{
	private static final Gson GSON = new Gson();
	private static final long MIN = 60_000L;
	public static final long NOW = 1_760_000_000_000L;
	static final String BOARD = "{\"version\":1,\"focus\":\"quest:Song of the Elves\",\"goals\":[{\"key\":\"quest:Song of the Elves\","
		+ "\"type\":\"quest\",\"title\":\"Song of the Elves\",\"complete\":false,\"progress\":0.4,\"skills\":{\"Prayer\":70,\"Agility\":70,"
		+ "\"Magic\":70},\"quests\":[\"Mourning's End Part II\"]},{\"key\":\"diary:Ardougne Hard\",\"type\":\"diary\",\"title\":\"Ardougne Hard\","
		+ "\"complete\":false,\"skills\":{\"Magic\":66,\"Ranged\":60}},{\"key\":\"skill:Attack 90\",\"type\":\"skill\",\"title\":\"90 Attack\","
		+ "\"complete\":false,\"skills\":{\"Attack\":90}},{\"key\":\"quest:Done\",\"title\":\"Done\",\"complete\":true,\"skills\":{\"Defence\":99}}]}";

	/** An account with games behind it: points, a goal, a piece owned and the shields learned. */
	public static Logbook good(int count)
	{
		Logbook log = new Logbook();
		Random rnd = new Random(7);
		long t = NOW - count * 4 * MIN;
		for (int i = 0; i < count; i++)
		{
			t += 3 * MIN + rnd.nextInt(90) * 1000L;
			GameRecord.Result r = i % 9 == 4 ? GameRecord.Result.LOST : i % 17 == 3 ? GameRecord.Result.NO_POINTS : GameRecord.Result.WON;
			log.record(new GameRecord(t, i % 5 == 0 ? Boat.INTERMEDIATE : Boat.VETERAN, r, r == GameRecord.Result.WON ? 5 : 0,
				r == GameRecord.Result.NO_POINTS ? 34 : 60 + rnd.nextInt(150), 150 + rnd.nextInt(90)));
			log.getClock().learn(java.util.Arrays.asList(40 + rnd.nextInt(8), 70 + rnd.nextInt(8), 100, 130));
		}
		log.seePoints(412, NOW - 4 * MIN);
		log.seeOwned(EnumSet.of(Reward.GLOVES), NOW - 30 * MIN);
		log.setGoal(EnumSet.copyOf(Goal.ELITE.getRewards()));
		return log;
	}

	@Test
	public void everyDamagedLogbookReadsAndPlans()
	{
		String good = GSON.toJson(good(30));
		int tried = 0;
		for (String damaged : Damaged.of(good))
		{
			try
			{
				use(Logbook.read(GSON, damaged));
			}
			catch (RuntimeException | AssertionError | StackOverflowError e)
			{
				throw new AssertionError("logbook " + Damaged.show(damaged) + ": " + e, e);
			}
			tried++;
		}
		assertTrue(tried > 300);
	}

	/** Everything the plugin works out from a logbook, each figure checked for sense. */
	public static void use(Logbook b)
	{
		assertNotNull(b);
		assertTrue("points " + b.getPoints(), b.getPoints() >= -1 && b.getPoints() <= Logbook.POINTS_CAP);
		assertTrue(b.getPointsSeen() >= 0 && b.getOwnedSeen() >= 0);
		assertTrue("played " + b.getPlayed(), b.getPlayed() >= 0);
		assertTrue("won " + b.getWon(), b.getWon() >= 0 && b.getWon() <= b.getPlayed());
		assertTrue("earned " + b.getEarned(), b.getEarned() >= 0);
		assertTrue("seconds " + b.getSeconds(), b.getSeconds() >= 0);
		List<GameRecord> games = b.getGames();
		assertTrue(games.size() <= Logbook.KEEP);
		for (GameRecord g : games)
		{
			assertNotNull(g);
			assertTrue("game " + g.getPoints() + " " + g.getDamage() + " " + g.getSeconds() + " " + g.getEnded(),
				g.getPoints() >= 0 && g.getPoints() <= GameRecord.MAX_POINTS && g.getDamage() >= 0 && g.getSeconds() >= 0 && g.getEnded() >= 0);
			assertNotNull(g.getResult());
		}
		for (int step = 0; step < 4; step++)
		{
			int e = b.getClock().expected(step);
			assertTrue("shield gap " + e, e == -1 || e >= ShieldClock.MIN_GAP && e <= ShieldClock.MAX_GAP);
			assertTrue(b.getClock().samples(step) >= 0);
		}
		for (Boat boat : Boat.values())
		{
			double per = b.pointsPerGame(boat);
			assertTrue("per game " + per, per >= 0.5 && per <= GameRecord.MAX_POINTS);
		}
		double minutes = b.minutesPerGame();
		assertTrue("minutes " + minutes, minutes > 0 && minutes < 60);
		Set<Reward> locked = EnumSet.of(Reward.ELITE_TOP, Reward.ELITE_ROBE);
		Outlook o = Outlook.of(b.getGoal(), b.getOwned(), b.getPoints(), b.pointsPerGame(Boat.VETERAN), minutes, b.isMeasured());
		assertTrue(o.getGames() >= -1 && o.getRemaining() >= 0);
		o.buyableNow(locked);
		for (Outlook.Milestone m : o.milestones(locked))
		{
			assertTrue(m.getGames() >= -1 && m.getTotal() > 0);
		}
		Sitting s = Sitting.latest(games, NOW);
		if (s != null)
		{
			assertTrue(s.pointsPerHour() >= 0 && s.getPoints() >= 0);
		}
		for (Landers.Line l : Landers.of(games))
		{
			assertTrue(l.pointsPerHour() >= 0 && l.winRate() >= 0 && l.winRate() <= 1);
		}
		Habit h = Habit.of(games, o.isComplete(), NOW);
		assertTrue(h.afterGames(10, NOW) == -1 || h.afterGames(10, NOW) >= NOW);
		GameSummary.of(games.isEmpty() ? null : games.get(games.size() - 1), o, s);
		GameSummary.announcement(o, o, locked);
		// Another game recorded, and the logbook saved and read back, still work.
		Logbook back = Logbook.read(GSON, GSON.toJson(b));
		assertEquals(b.getGames().size(), back.getGames().size());
		assertEquals(b.getPoints(), back.getPoints());
	}

	@Test
	public void oneDamagedFieldCostsOnlyThatField()
	{
		Logbook was = good(30);
		String good = GSON.toJson(was);
		Logbook b = Logbook.read(GSON, good.replace("\"points\":412", "\"points\":\"lots\""));
		assertEquals(was.getGames().size(), b.getGames().size());
		assertEquals(was.getGoal(), b.getGoal());
		assertEquals(was.getPlayed(), b.getPlayed());
		b = Logbook.read(GSON, good.replace("\"owned\":[\"GLOVES\"]", "\"owned\":{\"GLOVES\":1}"));
		assertEquals(412, b.getPoints());
		assertEquals(was.getGames().size(), b.getGames().size());
		// One game of the wrong shape costs only that game.
		int at = good.indexOf("{\"ended\"");
		b = Logbook.read(GSON, good.substring(0, at) + "[1,2]" + good.substring(good.indexOf('}', at) + 1));
		assertEquals(was.getGames().size() - 1, b.getGames().size());
		assertEquals(412, b.getPoints());
	}

	@Test
	public void numbersOutOfRangeAreBroughtBack()
	{
		String good = GSON.toJson(good(30));
		Logbook b = Logbook.read(GSON, good.replace("\"points\":412", "\"points\":2000000000"));
		assertEquals(Logbook.POINTS_CAP, b.getPoints());
		b = Logbook.read(GSON, good.replace("\"points\":412", "\"points\":-7"));
		assertEquals(-1, b.getPoints());
		b = Logbook.read(GSON, good.replaceFirst("\"points\":5,", "\"points\":-2147483648,"));
		use(b);
		b = Logbook.read(GSON, good.replaceFirst("\"points\":5,", "\"points\":2147483647,"));
		use(b);
	}

	@Test
	public void aHugeGameHistoryIsCutToWhatIsKeptAndStaysQuick()
	{
		// Ten thousand games saved in full, as a damaged or hand-edited save could hold.
		Logbook b = good(30);
		String one = GSON.toJson(b.getGames().get(3));
		StringBuilder many = new StringBuilder("[");
		for (int i = 0; i < 10_000; i++)
		{
			many.append(i == 0 ? "" : ",").append(one);
		}
		String json = GSON.toJson(b);
		int at = json.indexOf("\"games\":[") + "\"games\":".length();
		int end = json.indexOf("]", at) + 1;
		long t0 = System.nanoTime();
		Logbook big = Logbook.read(GSON, json.substring(0, at) + many.append("]") + json.substring(end));
		long readMs = (System.nanoTime() - t0) / 1_000_000;
		assertEquals(Logbook.KEEP, big.getGames().size());
		use(big);
		// Ten thousand games played: the totals count them all, the list keeps the newest.
		Logbook played = good(10_000);
		assertEquals(Logbook.KEEP, played.getGames().size());
		assertTrue(played.getPlayed() >= 10_000 - 10_000 / 9 - 1);
		Runnable publish = () ->
		{
			// What the plugin works out on the client thread to draw the panel.
			List<GameRecord> games = played.getGames();
			Outlook o = Outlook.of(played.getGoal(), played.getOwned(), played.getPoints(), played.pointsPerGame(Boat.VETERAN),
				played.minutesPerGame(), played.isMeasured());
			o.milestones(EnumSet.noneOf(Reward.class));
			o.buyableNow(EnumSet.noneOf(Reward.class));
			Landers.of(games);
			Sitting.latest(games, NOW);
			Habit.of(games, false, NOW);
		};
		double publishMs = time(publish);
		t0 = System.nanoTime();
		String saved = GSON.toJson(played);
		double saveMs = (System.nanoTime() - t0) / 1e6;
		System.out.printf("Pest Control Pilot, 10,000 games played: read 10,000 saved in %d ms, publish %.3f ms, save %.2f ms (%d bytes)%n",
			readMs, publishMs, saveMs, saved.length());
		assertTrue("publish took " + publishMs, publishMs < 25);
		assertTrue("the save is kept small: " + saved.length(), saved.length() < 20_000);
	}

	static double time(Runnable run)
	{
		for (int i = 0; i < 20; i++)
		{
			run.run();
		}
		int runs = 50;
		long t0 = System.nanoTime();
		for (int i = 0; i < runs; i++)
		{
			run.run();
		}
		return (System.nanoTime() - t0) / 1e6 / runs;
	}

	@Test
	public void nothingSavedIsAFreshStart()
	{
		for (String s : new String[]{null, "", "null", "{}", "[]", "0", "\"logbook\""})
		{
			Logbook b = Logbook.read(GSON, s);
			use(b);
			assertEquals(-1, b.getPoints());
			assertEquals(EnumSet.copyOf(Goal.MELEE.getRewards()), b.getGoal());
		}
	}

	@Test
	public void aLargeBankIsLookedThroughQuickly()
	{
		// A full bank of 1,500 kinds, a few void pieces among them, as the plugin reads it on the
		// client thread at login and whenever Bank Steward saves it again.
		StringBuilder items = new StringBuilder();
		for (int i = 0; i < 1_500; i++)
		{
			items.append(i == 0 ? "" : ",").append('"').append(1_000 + i * 17).append("\":").append(1 + i % 400);
		}
		items.append(",\"8839\":1,\"11665\":1,\"995\":123456789");
		String json = "{\"version\":1,\"savedAt\":" + (NOW - MIN) + ",\"items\":{" + items + "},\"truncated\":false}";
		Set<Reward>[] found = new Set[1];
		Runnable read = () ->
		{
			SharedBank bank = SharedBank.read(GSON, json, null);
			Set<Reward> owned = EnumSet.noneOf(Reward.class);
			for (int id : bank.ids())
			{
				for (Reward r : Reward.values())
				{
					if (r.isOwnedBy(id))
					{
						owned.add(r);
					}
				}
			}
			found[0] = owned;
		};
		double ms = time(read);
		System.out.printf("Pest Control Pilot, 1,500-item bank: read and looked through in %.2f ms%n", ms);
		assertEquals(EnumSet.of(Reward.TOP, Reward.MELEE_HELM), found[0]);
		assertTrue("reading the bank took " + ms, ms < 50);
	}

	@Test
	public void everyDamagedSharedBankReadsSafely() throws Exception
	{
		String steward = SharedBankContractTest.fixture("bank-steward-bankV1.json");
		String roadmap = SharedBankContractTest.fixture("gear-roadmap-bank.json");
		for (String damaged : Damaged.of(steward))
		{
			bank(damaged, null);
			bank(damaged, roadmap);
		}
		for (String damaged : Damaged.of(roadmap))
		{
			bank(null, damaged);
		}
	}

	private static void bank(String steward, String roadmap)
	{
		try
		{
			SharedBank bank = SharedBank.read(GSON, steward, roadmap);
			if (bank != null)
			{
				assertTrue(bank.getSavedAt() >= 0);
				for (Map.Entry<Integer, Long> e : bank.getItems().entrySet())
				{
					// Coins are kept as saved; only item counts are held to one stack.
					assertTrue(e.getKey() > 0 && e.getValue() > 0 && (e.getKey() == 995 || e.getValue() <= Integer.MAX_VALUE));
				}
				for (int id : bank.ids())
				{
					assertTrue(bank.count(id) > 0);
					for (Reward r : Reward.values())
					{
						r.isOwnedBy(id);
					}
				}
			}
		}
		catch (RuntimeException | AssertionError | StackOverflowError e)
		{
			throw new AssertionError("steward " + Damaged.show(steward) + " roadmap " + Damaged.show(roadmap) + ": " + e, e);
		}
	}

	@Test
	public void everyDamagedGoalBoardReadsSafely()
	{
		Map<String, Integer> levels = new LinkedHashMap<>();
		for (Experience.Skill s : Experience.Skill.values())
		{
			levels.put(s.getTitle(), 60);
		}
		assertEquals(4, NavigatorGoals.needs(GSON, BOARD, levels).size());
		for (String damaged : Damaged.of(BOARD))
		{
			try
			{
				for (NavigatorGoals.Need n : NavigatorGoals.needs(GSON, damaged, levels))
				{
					assertTrue(n.getLevel() > 60 && n.getLevel() <= 99);
					assertNotNull(n.getGoal());
					assertNotNull(n.getSkill());
				}
			}
			catch (RuntimeException | AssertionError | StackOverflowError e)
			{
				throw new AssertionError("goalBoard " + Damaged.show(damaged) + ": " + e, e);
			}
		}
	}
}
