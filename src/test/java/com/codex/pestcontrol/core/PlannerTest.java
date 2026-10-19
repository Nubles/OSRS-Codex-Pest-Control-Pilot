package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

public class PlannerTest
{
	private static final long MIN = 60_000L;

	@Test
	public void piecesAreBoughtCheapestFirstWithLockedOnesLast()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.ELITE.getRewards()), EnumSet.noneOf(Reward.class), 100, 4, 6, true);
		List<Outlook.Milestone> m = o.milestones(EnumSet.of(Reward.ELITE_TOP, Reward.ELITE_ROBE));
		assertEquals(Reward.GLOVES, m.get(0).getReward());
		assertEquals(Reward.RANGER_HELM, m.get(1).getReward());
		assertEquals(Reward.ELITE_TOP, m.get(2).getReward());
		// Gloves 150 with 100 held: 50 short at 4 a game is 13 games.
		assertEquals(13, m.get(0).getGames());
		assertEquals(13 * 6.0, m.get(0).getMinutes(), 1e-9);
		assertEquals(750, m.get(3).getTotal());
		assertEquals((int) Math.ceil(650 / 4.0), m.get(3).getGames());
	}

	@Test
	public void piecesAlreadyAffordableAreNow()
	{
		Outlook o = Outlook.of(EnumSet.of(Reward.SEAL, Reward.GLOVES), EnumSet.noneOf(Reward.class), 200, 5, 5, true);
		List<Outlook.Milestone> m = o.milestones(null);
		assertEquals(0, m.get(0).getGames());
		assertEquals(0, m.get(1).getGames());
	}

	private static GameRecord game(long minute, Boat boat, GameRecord.Result r, int points, int damage)
	{
		return new GameRecord(minute * MIN, boat, r, points, damage, 240);
	}

	@Test
	public void eachLanderIsRatedFromItsOwnGames()
	{
		List<GameRecord> games = new ArrayList<>();
		// Four veteran games six minutes apart, one lost; three intermediate games four minutes apart.
		games.add(game(0, Boat.VETERAN, GameRecord.Result.WON, 5, 120));
		games.add(game(6, Boat.VETERAN, GameRecord.Result.LOST, 0, 90));
		games.add(game(12, Boat.VETERAN, GameRecord.Result.WON, 5, 110));
		games.add(game(18, Boat.VETERAN, GameRecord.Result.WON, 5, 130));
		games.add(game(100, Boat.INTERMEDIATE, GameRecord.Result.WON, 4, 140));
		games.add(game(104, Boat.INTERMEDIATE, GameRecord.Result.WON, 4, 150));
		games.add(game(108, Boat.INTERMEDIATE, GameRecord.Result.WON, 4, 160));
		List<Landers.Line> lines = Landers.of(games);
		assertEquals(2, lines.size());
		Landers.Line inter = lines.get(0);
		Landers.Line vet = lines.get(1);
		assertEquals(Boat.INTERMEDIATE, inter.getBoat());
		assertEquals(3.75, vet.getPointsPerGame(), 1e-9);
		assertEquals(6, vet.getMinutesPerGame(), 1e-9);
		assertEquals(37.5, vet.pointsPerHour(), 1e-9);
		assertEquals(60, inter.pointsPerHour(), 1e-9);
		assertEquals(0.75, vet.winRate(), 1e-9);
		assertEquals(Boat.INTERMEDIATE, Landers.best(lines, 110).getBoat());
		// A level 80 cannot board the veteran lander, and two games are not enough to rate one.
		assertEquals(Boat.INTERMEDIATE, Landers.best(lines, 80).getBoat());
		assertNull(Landers.best(Landers.of(games.subList(4, 6)), 110));
	}

	@Test
	public void gamesLeftEarlyBreakTheChainOfGaps()
	{
		List<GameRecord> games = new ArrayList<>();
		games.add(game(0, Boat.NOVICE, GameRecord.Result.WON, 3, 80));
		games.add(game(3, null, GameRecord.Result.LEFT, 0, 10));
		games.add(game(9, Boat.NOVICE, GameRecord.Result.WON, 3, 80));
		games.add(game(14, Boat.NOVICE, GameRecord.Result.WON, 3, 80));
		Landers.Line l = Landers.of(games).get(0);
		assertEquals(3, l.getGames());
		// Only one back-to-back gap (5 minutes): not enough, so game length plus the lander wait.
		assertEquals((240 + 60) / 60.0, l.getMinutesPerGame(), 1e-9);
	}
}
