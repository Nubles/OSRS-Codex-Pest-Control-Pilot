package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.DamagedAccess;
import com.codex.pestcontrol.core.DamagedDataTest;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Landers;
import com.codex.pestcontrol.core.Logbook;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.core.Sitting;
import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Test;

/** A damaged logbook still draws: every tab of the panel and the void card, without an exception. */
public class DamagedPanelTest
{
	private static final Gson GSON = new Gson();
	private static final long NOW = DamagedDataTest.NOW;

	/** The view the plugin builds from a logbook, as when the panel is published. */
	static View view(Logbook b, boolean elite)
	{
		Outlook o = Outlook.of(b.getGoal(), b.getOwned(), b.getPoints(), b.pointsPerGame(Boat.VETERAN), b.minutesPerGame(), b.isMeasured());
		List<GameRecord> games = new ArrayList<>(b.getGames());
		View v = new View(View.State.OUTPOST, "Zezima", null, games.isEmpty() ? null : games.get(games.size() - 1), o, b.getGoal(),
			b.getOwned(), Goal.matching(b.getGoal()), Boat.VETERAN, b.getOwnedSeen() > 0, Collections.emptyList(), elite, b.getPlayed(),
			b.getWon(), b.getEarned(), b.getSeconds(), games, NOW);
		v.landers = Landers.of(games);
		v.combat = 100;
		v.sitting = Sitting.latest(games, NOW);
		return v;
	}

	@Test
	public void everyDamagedLogbookDraws() throws Exception
	{
		String good = GSON.toJson(DamagedDataTest.good(30));
		List<String> all = DamagedAccess.of(good);
		int drawn = 0;
		for (int i = 0; i < all.size(); i += 4)
		{
			String damaged = all.get(i);
			View v = view(Logbook.read(GSON, damaged), i % 8 == 0);
			try
			{
				draw(v);
				VoidSheet.render(null, "Zezima", v.outlook, v.goal, v.owned, v.outlook.buyableNow(Collections.<Reward>emptySet()), v.elite, 0, NOW, null);
			}
			catch (RuntimeException | AssertionError e)
			{
				throw new AssertionError("logbook " + DamagedAccess.show(damaged) + ": " + e, e);
			}
			drawn++;
		}
		assertTrue(drawn > 50);
	}

	@Test
	public void aBrandNewAndAMaxedAccountDraw() throws Exception
	{
		Logbook fresh = new Logbook();
		draw(view(fresh, false));
		draw(View.loggedOut());
		// Every reward owned, the cap of points held and ten thousand games behind it.
		Logbook maxed = DamagedDataTest.good(10_000);
		maxed.setGoal(java.util.EnumSet.allOf(Reward.class));
		maxed.seeOwned(java.util.EnumSet.allOf(Reward.class), NOW);
		maxed.seePoints(Logbook.POINTS_CAP, NOW);
		View v = view(maxed, true);
		assertTrue(v.outlook.isComplete());
		draw(v);
		VoidSheet.render(null, null, v.outlook, v.goal, v.owned, v.outlook.buyableNow(null), true, 0, NOW, null);
	}

	static void draw(View view) throws Exception
	{
		Throwable[] failed = new Throwable[1];
		SwingUtilities.invokeAndWait(() ->
		{
			try
			{
				PilotPanel p = new PilotPanel((id, then) -> null, null);
				p.update(view);
				for (int tab = 0; tab < 3; tab++)
				{
					p.showTab(tab);
					p.setSize(225, 1400);
					layout(p);
					BufferedImage img = new BufferedImage(225, 1400, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = img.createGraphics();
					p.printAll(g);
					g.dispose();
				}
			}
			catch (Throwable t)
			{
				failed[0] = t;
			}
		});
		if (failed[0] != null)
		{
			throw new RuntimeException(failed[0]);
		}
	}

	private static void layout(Component c)
	{
		c.doLayout();
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				layout(k);
			}
		}
	}
}
