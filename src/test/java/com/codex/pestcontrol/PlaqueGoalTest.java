package com.codex.pestcontrol;

import static org.junit.Assert.assertEquals;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.util.EnumSet;
import org.junit.Test;

/** The plaque's goal line agrees with the panel and the void card about the diary. */
public class PlaqueGoalTest
{
	@Test
	public void pointsForDiaryLockedPiecesDoNotSendThePlayerToTheShop()
	{
		// Only the elite top is left and the points cover it, but the hard diary is not done.
		Outlook o = Outlook.of(EnumSet.of(Reward.TOP, Reward.ELITE_TOP), EnumSet.of(Reward.TOP), 300, 5, 3, false);
		assertEquals("Points enough; the diary is due", CodexPestControlPlugin.goalDetail(o, false));
		assertEquals("Enough for all of it: visit the reward shop", CodexPestControlPlugin.goalDetail(o, true));
	}

	@Test
	public void otherwiseTheLineIsAsBefore()
	{
		Outlook done = Outlook.of(EnumSet.of(Reward.TOP), EnumSet.of(Reward.TOP), 10, 5, 3, false);
		assertEquals("Every piece is yours", CodexPestControlPlugin.goalDetail(done, false));
		Outlook shy = Outlook.of(EnumSet.of(Reward.TOP), EnumSet.noneOf(Reward.class), 240, 5, 3, false);
		assertEquals("10 to go, about 2 games", CodexPestControlPlugin.goalDetail(shy, false));
	}

	@Test
	public void pointsThatAlreadyBuyAPieceSaySoFirst()
	{
		Outlook part = Outlook.of(EnumSet.of(Reward.TOP, Reward.ROBE, Reward.MELEE_HELM), EnumSet.noneOf(Reward.class), 412, 5, 3, false);
		assertEquals("buy the top now; 288 to go, about 58 games", CodexPestControlPlugin.goalDetail(part, false));
		Outlook two = Outlook.of(EnumSet.of(Reward.MELEE_HELM, Reward.MAGE_HELM, Reward.TOP), EnumSet.noneOf(Reward.class), 400, 5, 3, false);
		assertEquals("buy 2 pieces now; 250 to go, about 50 games", CodexPestControlPlugin.goalDetail(two, false));
	}
}
