package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.Test;

public class GameSummaryTest
{
	private static final long MIN = 60_000L;

	@Test
	public void aWonGameWithTheGoalAndTheSitting()
	{
		GameRecord g1 = new GameRecord(10 * MIN, Boat.VETERAN, GameRecord.Result.WON, 5, 120, 300);
		GameRecord g2 = new GameRecord(17 * MIN, Boat.VETERAN, GameRecord.Result.WON, 5, 210, 300);
		Outlook goal = Outlook.of(EnumSet.of(Reward.TOP), EnumSet.noneOf(Reward.class), 200, 5, 7, true);
		String text = GameSummary.of(g2, goal, Sitting.latest(Arrays.asList(g1, g2), 17 * MIN));
		// From minute 5 to 17: 10 points in 12 minutes is 50 an hour.
		assertEquals("Won, +5 points, 210 damage. 200 of 250 points for your goal, about 10 games to go."
			+ " This sitting: 2 games, 10 points, 50 an hour.", text);
	}

	@Test
	public void aGameWonShortOfTheDamageSaysHowShort()
	{
		GameRecord g = new GameRecord(0, Boat.VETERAN, GameRecord.Result.NO_POINTS, 0, 34, 300);
		assertEquals("Won, but no points: 34 of the 50 damage needed.", GameSummary.of(g, null, null));
	}

	@Test
	public void noPointsWithTheDamageInDoesNotClaimTooLittle()
	{
		// The game said no points though the damage counted here was enough (a hit not seen as yours):
		// "60 of the 50 needed" would make no sense.
		GameRecord g = new GameRecord(0, Boat.VETERAN, GameRecord.Result.NO_POINTS, 0, 60, 300);
		String text = GameSummary.of(g, null, null);
		assertTrue(text, !text.contains("60 of the 50"));
	}

	@Test
	public void leftGamesSayNothingAndTheCapIsWarned()
	{
		assertNull(GameSummary.of(new GameRecord(0, Boat.VETERAN, GameRecord.Result.LEFT, 0, 10, 60), null, null));
		Outlook full = Outlook.of(EnumSet.noneOf(Reward.class), EnumSet.noneOf(Reward.class), 3960, 5, 7, true);
		String text = GameSummary.of(new GameRecord(0, Boat.VETERAN, GameRecord.Result.LOST, 0, 80, 300), full, null);
		assertEquals("Lost, 80 damage. You hold 3,960 of the 4,000 points the knights will keep: spend some.", text);
	}

	@Test
	public void theChatNamesWhatThePointsBuyAndNeverALockedPiece()
	{
		java.util.Set<Reward> goal = EnumSet.copyOf(Goal.ELITE.getRewards());
		java.util.Set<Reward> owned = EnumSet.of(Reward.GLOVES, Reward.RANGER_HELM);
		java.util.Set<Reward> locked = EnumSet.of(Reward.ELITE_TOP, Reward.ELITE_ROBE);
		Outlook before = Outlook.of(goal, owned, 190, 5, 7, true);
		Outlook after = Outlook.of(goal, owned, 410, 5, 7, true);
		// Only the elite top and robe are left and the diary locks both: no "visit the shop".
		assertNull(GameSummary.announcement(before, after, locked));
		assertEquals("your 410 points now cover everything left in your goal. Visit the Void Knights' reward shop.",
			GameSummary.announcement(before, after, EnumSet.noneOf(Reward.class)));
		// With an open piece beside a locked one, the line names the open piece.
		java.util.Set<Reward> mixed = EnumSet.of(Reward.ELITE_TOP, Reward.TOP);
		assertEquals("your 260 points now cover the void knight top.", GameSummary.announcement(
			Outlook.of(mixed, EnumSet.noneOf(Reward.class), 100, 5, 7, true), Outlook.of(mixed, EnumSet.noneOf(Reward.class), 260, 5, 7, true),
			EnumSet.of(Reward.ELITE_TOP)));
	}
}
