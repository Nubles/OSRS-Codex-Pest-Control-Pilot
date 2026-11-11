package com.codex.pestcontrol.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.codex.pestcontrol.core.HardcoreWorld;
import com.codex.pestcontrol.core.Logbook;
import com.codex.pestcontrol.overlay.PilotPlaque;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.Test;

/** On a Hardcore World the panel warns on every page and the plaque carries the line over its own. */
public class HardcoreWarningTest
{
	@Test
	public void thePanelWarnsOnEveryPageOnlyOnAHardcoreWorld() throws Exception
	{
		View hardcore = DamagedPanelTest.view(new Logbook(), false);
		hardcore.hardcoreWorld = true;
		View ordinary = DamagedPanelTest.view(new Logbook(), false);
		SwingUtilities.invokeAndWait(() ->
		{
			PilotPanel p = new PilotPanel((id, then) -> null, null);
			p.update(hardcore);
			for (int tab = 0; tab < 3; tab++)
			{
				p.showTab(tab);
				assertTrue("tab " + tab, shows(p, HardcoreWorld.LINE));
			}
			// The Battle page outside a game says what an official death means.
			p.showTab(0);
			assertTrue(shows(p, "official death"));
			p.update(ordinary);
			p.showTab(0);
			assertFalse(shows(p, HardcoreWorld.LINE));
			assertFalse(shows(p, "official death"));
			// Logged out, the world is not known.
			View out = View.loggedOut();
			out.hardcoreWorld = true;
			p.update(out);
			assertFalse(shows(p, HardcoreWorld.LINE));
		});
	}

	@Test
	public void thePlaqueMakesRoomForTheLine()
	{
		Graphics2D g = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
		Dimension plain = PilotPlaque.paint(g, new PilotPlaque.Shown("PEST CONTROL", "120 points", Theme.GOLD, "Void melee set: 880 to go", null, null));
		PilotPlaque.Shown warned = new PilotPlaque.Shown("PEST CONTROL", "120 points", Theme.GOLD, "Void melee set: 880 to go", null, null)
			.banner(HardcoreWorld.PLAQUE);
		Dimension both = PilotPlaque.paint(g, warned);
		g.setFont(Theme.small());
		assertTrue(both.height > plain.height);
		assertTrue(both.width >= g.getFontMetrics().stringWidth(HardcoreWorld.PLAQUE));
		assertEquals(HardcoreWorld.PLAQUE, warned.getBanner());
		g.dispose();
	}

	/** Whether any label showing in the panel, laid out, holds the text. */
	private static boolean shows(PilotPanel p, String text)
	{
		p.setSize(225, 1400);
		layout(p);
		List<JLabel> labels = new ArrayList<>();
		collect(p, labels);
		for (JLabel l : labels)
		{
			if (l.getText() != null && l.getText().contains(text))
			{
				return true;
			}
		}
		return false;
	}

	/** Labels in components that are visible, so a hidden ribbon or a page not in view is skipped. */
	private static void collect(Component c, List<JLabel> out)
	{
		if (!c.isVisible())
		{
			return;
		}
		if (c instanceof JLabel)
		{
			out.add((JLabel) c);
		}
		if (c instanceof Container)
		{
			for (Component k : ((Container) c).getComponents())
			{
				collect(k, out);
			}
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
