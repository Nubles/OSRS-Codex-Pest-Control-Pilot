package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class VoidSheetTest
{
	private static final Set<Reward> MELEE = EnumSet.copyOf(Goal.MELEE.getRewards());

	@Test
	public void theColumnShowsPointsGamesPaceAndPieces()
	{
		Set<Reward> owned = EnumSet.of(Reward.GLOVES);
		Outlook o = Outlook.of(MELEE, owned, 412, 4.6, 3.5, true);
		List<VoidSheet.Fact> facts = VoidSheet.facts(o, MELEE, owned, true, 0);
		assertEquals(4, facts.size());
		assertEquals("Points", facts.get(0).label);
		assertEquals("412", facts.get(0).value);
		assertEquals("Games left", facts.get(1).label);
		assertTrue(o.getGames() > 0);
		assertEquals("4.6", facts.get(2).value);
		assertEquals("from your games", facts.get(2).note);
		assertEquals("1 of " + MELEE.size(), facts.get(3).value);
	}

	@Test
	public void enoughPointsSaysNoGamesAreLeft()
	{
		Outlook o = Outlook.of(MELEE, EnumSet.noneOf(Reward.class), 2000, 4, 3.5, false);
		List<VoidSheet.Fact> facts = VoidSheet.facts(o, MELEE, EnumSet.noneOf(Reward.class), true, 0);
		assertEquals("None", facts.get(1).value);
		assertEquals("points enough now", facts.get(1).note);
		assertEquals("4", facts.get(2).value);
		assertEquals("a typical game", facts.get(2).note);
	}

	@Test
	public void theNameIsLeftOffWhenAsked()
	{
		assertEquals("ZEZIMA'S VOID", VoidSheet.kicker("Zezima"));
		assertEquals("PEST CONTROL", VoidSheet.kicker(null));
	}

	@Test
	public void theCardWaitsForEveryPieceItShows()
	{
		List<Integer> ids = VoidSheet.pictures(MELEE, EnumSet.noneOf(Reward.class));
		for (Reward r : MELEE)
		{
			assertTrue(r + " is waited for", ids.contains(r.getIcon()));
		}
	}

	@Test
	public void piecesLockedBehindTheDiaryAreNotPointsEnoughNow()
	{
		// Everything but the elite top and robe is owned, and 500 points cover their 400, but
		// without the hard diary neither can be bought.
		Set<Reward> goal = EnumSet.copyOf(Goal.ELITE.getRewards());
		Set<Reward> owned = EnumSet.of(Reward.GLOVES, Reward.RANGER_HELM);
		Outlook o = Outlook.of(goal, owned, 500, 4, 3.5, true);
		VoidSheet.Fact games = VoidSheet.facts(o, goal, owned, false, 0).get(1);
		assertEquals("Games left", games.label);
		assertTrue(games.note, !games.note.contains("points enough now"));
		assertTrue(games.note, games.note.contains("diary"));
		// With the diary done the same points are enough now.
		assertEquals("points enough now", VoidSheet.facts(o, goal, owned, true, 0).get(1).note);
	}

	@Test
	public void unreadPointsLeaveTheGamesUnknown()
	{
		Outlook o = Outlook.of(MELEE, EnumSet.noneOf(Reward.class), -1, 4, 3.5, false);
		VoidSheet.Fact games = VoidSheet.facts(o, MELEE, EnumSet.noneOf(Reward.class), true, 0).get(1);
		assertEquals("?", games.value);
		assertTrue(games.note, games.note.contains("points"));
	}

	@Test
	public void thePanelNamesWhatThePointsBuyAndWhy()
	{
		assertEquals("You can already buy the top.", PilotPanel.canBuy(EnumSet.of(Reward.TOP)));
		assertEquals("You can already buy the melee helm and ranger helm: together they spend the most of your points.",
			PilotPanel.canBuy(EnumSet.of(Reward.RANGER_HELM, Reward.MELEE_HELM)));
	}
}
