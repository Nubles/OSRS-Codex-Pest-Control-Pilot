package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.awt.event.MouseEvent;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.Test;

/** A wanted piece's tooltip counts the points the buy order spends before it, not just the pieces bought now. */
public class WardrobeOrderTest
{
	private static String tip(Wardrobe w, String title)
	{
		JPanel source = new JPanel();
		for (int y = 0; y < w.getHeight(); y += 2)
		{
			for (int x = 0; x < w.getWidth(); x += 2)
			{
				String t = w.getToolTipText(new MouseEvent(source, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false));
				if (t != null && t.startsWith(title + ":"))
				{
					return t;
				}
			}
		}
		throw new AssertionError("no slot for " + title);
	}

	@Test
	public void aPieceLaterInTheOrderNeedsWhatThePiecesBeforeItCost()
	{
		// 412 points toward a top (250), a melee helm (200) and a robe (250): the top now, the helm
		// at 450, the robe at 700. The robe is 288 points away, not the 88 that the 162 left after
		// the top would leave it on its own.
		Set<Reward> goal = EnumSet.of(Reward.TOP, Reward.ROBE, Reward.MELEE_HELM);
		Set<Reward> owned = EnumSet.noneOf(Reward.class);
		Outlook o = Outlook.of(goal, owned, 412, 4, 1, true);
		Wardrobe w = new Wardrobe("Void", goal, owned, o.buyableNow(null), 412, true, true, null, null);
		w.setSize(196, w.getPreferredSize().height);
		assertEquals(EnumSet.of(Reward.TOP), o.buyableNow(null));
		assertEquals(true, tip(w, "Void melee helm").contains(": 38 points to go"));
		assertEquals(true, tip(w, "Void knight robe").contains(": 288 points to go"));
		// Saved toward each: the helm has the 162 left after the top, the robe nothing yet.
		assertEquals(162 / 200.0, Wardrobe.saved(Reward.MELEE_HELM, goal, owned, 412, true), 1e-9);
		assertEquals(0, Wardrobe.saved(Reward.ROBE, goal, owned, 412, true), 1e-9);
	}

	@Test
	public void lockedElitePiecesComeLastInTheCount()
	{
		// Without the diary the elite top waits behind the open pieces whatever its cost.
		Set<Reward> goal = EnumSet.of(Reward.ELITE_TOP, Reward.GLOVES);
		Set<Reward> owned = EnumSet.noneOf(Reward.class);
		assertEquals(150 + 200 - 300, Wardrobe.toGo(Reward.ELITE_TOP, goal, owned, 300, false));
		assertEquals(0, Wardrobe.toGo(Reward.GLOVES, goal, owned, 300, false));
	}
}
