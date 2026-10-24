package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Landers;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.junit.Test;

public class BeforeBoardingTest
{
	private static final Set<Reward> NONE = EnumSet.noneOf(Reward.class);

	@Test
	public void beforeThePointsAreReadItSaysToBoard()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), NONE, -1, 5, 4, false);
		String[] plan = PilotPanel.beforeBoarding(o, NONE, Collections.<Landers.Line>emptyList(), 0);
		assertEquals("At the outpost", plan[0]);
		assertNull(plan[2]);
	}

	@Test
	public void pointsThatBuyAPieceSendYouToTheShopFirst()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.of(Reward.GLOVES), 412, 5, 4, true);
		String[] plan = PilotPanel.beforeBoarding(o, NONE, Collections.<Landers.Line>emptyList(), 110);
		assertEquals("Visit the reward shop first", plan[0]);
		assertNotNull(plan[2]);
		assertTrue(plan[1], plan[1].startsWith("You can already buy the top."));
		assertTrue(plan[1], plan[1].endsWith("Your level boards the veteran lander, 5 points a win."));
	}

	@Test
	public void otherwiseItCountsTheGamesToTheNextPiece()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.of(Reward.GLOVES), 100, 5, 4, true);
		String[] plan = PilotPanel.beforeBoarding(o, NONE, Collections.<Landers.Line>emptyList(), 0);
		// The melee helm costs 200: 100 short at 5 a game.
		assertEquals("20 games to the melee helm", plan[0]);
		assertEquals("About 1.3h at your pace.", plan[1]);
	}

	@Test
	public void aCompleteGoalSaysSo()
	{
		Outlook o = Outlook.of(EnumSet.copyOf(Goal.MELEE.getRewards()), EnumSet.copyOf(Goal.MELEE.getRewards()), 100, 5, 4, true);
		assertEquals("Your goal is complete", PilotPanel.beforeBoarding(o, NONE, Collections.<Landers.Line>emptyList(), 0)[0]);
	}
}
