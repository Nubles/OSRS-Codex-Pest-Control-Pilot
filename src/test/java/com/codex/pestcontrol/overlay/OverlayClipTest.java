package com.codex.pestcontrol.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import com.codex.pestcontrol.core.Battle;
import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Ghost;
import com.codex.pestcontrol.core.HardcoreWorld;
import com.codex.pestcontrol.core.Portal;
import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.ui.GhostTable;
import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.View;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Every overlay hands RuneLite's graphics back as it found them: the clip above all (a cleared clip
 * breaks the overlay renderer for every plugin, every frame), and the font, colour, paint, stroke,
 * composite, transform and hints too.
 */
public class OverlayClipTest
{
	@Test
	public void thePlaqueRestoresEverything()
	{
		PilotPlaque plaque = new PilotPlaque();
		plaque.show(new PilotPlaque.Shown("WORK NEXT", "Attack Purple (west)", Theme.GOLD, "Its shield is down", "37 damage", Theme.GREEN));
		unchanged(plaque);
		plaque.show(new PilotPlaque.Shown("PEST CONTROL", "412 points", Theme.GOLD, "Void ranger set", null, null).banner(HardcoreWorld.PLAQUE));
		unchanged(plaque);
	}

	@Test
	public void theGhostTableRestoresEverything()
	{
		Battle b = new Battle();
		for (int t = 0; t < 120; t++)
		{
			b.tick();
			if (t == 25)
			{
				b.shieldDropped(Portal.BLUE);
			}
			if (t == 60)
			{
				b.setHealth(Portal.BLUE, 0);
			}
		}
		GameRecord game = Ghost.record(b, 0, Boat.NOVICE, GameRecord.Result.WON, 0);
		GhostOverlay overlay = new GhostOverlay();
		overlay.show(new GhostTable(new View.GhostSplits(game, null, true)));
		unchanged(overlay);
	}

	@Test
	public void theMarkersRestoreEverything()
	{
		NPC portal = Mockito.mock(NPC.class);
		Mockito.when(portal.getCanvasTilePoly()).thenReturn(new Polygon(new int[]{40, 60, 50}, new int[]{40, 40, 60}, 3));
		Mockito.when(portal.getCanvasTextLocation(Mockito.any(), Mockito.anyString(), Mockito.anyInt())).thenReturn(new Point(45, 30));
		Mockito.when(portal.getMinimapLocation()).thenReturn(new Point(80, 80));
		PortalMarker world = new PortalMarker(Mockito.mock(ModelOutlineRenderer.class));
		world.show(new PortalMarker.Mark(portal, Theme.GOLD, "Attack", Collections.singletonList(portal), Collections.singletonList(Color.BLUE)));
		unchanged(world);
		unchanged(new MinimapMarker(world));
	}

	@Test
	public void theShopGuideRestoresEverything()
	{
		Widget options = widget("", -1, new Rectangle(0, 0, 200, 150), false,
			widget(null, 8839, new Rectangle(10, 10, 36, 32), false),
			widget("Void knight top", -1, new Rectangle(50, 12, 100, 14), false),
			widget("Void knight robe", -1, new Rectangle(50, 52, 100, 14), false));
		Widget frame = widget("", -1, new Rectangle(5, 5, 180, 120), false);
		Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class}, (proxy, m, args) ->
			"getWidget".equals(m.getName()) && args != null && args.length == 1
				? ((Integer) args[0] == net.runelite.api.gameval.InterfaceID.PestRewardshop.OPTIONS ? options : frame) : null);
		RewardShopGuide guide = new RewardShopGuide(client);
		guide.show(new RewardShopGuide.Guide(EnumSet.of(Reward.TOP), EnumSet.of(Reward.TOP, Reward.ROBE)));
		unchanged(guide);
	}

	/** Renders the overlay into a graphics with a clip and a state set, and checks the state is the same after. */
	private static void unchanged(Overlay overlay)
	{
		BufferedImage img = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.translate(3, 4);
		Rectangle clip = new Rectangle(0, 0, 380, 280);
		g.setClip(clip);
		Font font = new Font(Font.DIALOG, Font.PLAIN, 11);
		g.setFont(font);
		g.setColor(Color.MAGENTA);
		Stroke stroke = new BasicStroke(3f);
		g.setStroke(stroke);
		Composite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
		g.setComposite(composite);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		AffineTransform transform = g.getTransform();
		Shape before = g.getClip();
		RenderingHints hints = (RenderingHints) g.getRenderingHints().clone();

		overlay.render(g);

		String name = overlay.getClass().getSimpleName();
		assertNotNull(name + " cleared the clip", g.getClip());
		assertEquals(name + " clip", before.getBounds(), g.getClip().getBounds());
		assertEquals(name + " font", font, g.getFont());
		assertEquals(name + " colour", Color.MAGENTA, g.getColor());
		assertEquals(name + " stroke", stroke, g.getStroke());
		assertEquals(name + " composite", composite, g.getComposite());
		assertEquals(name + " transform", transform, g.getTransform());
		assertEquals(name + " hints", hints, g.getRenderingHints());
		g.dispose();
	}

	/** A widget that answers only what the guide asks. */
	private static Widget widget(String text, int item, Rectangle bounds, boolean hidden, Widget... kids)
	{
		return (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class}, (proxy, m, args) ->
		{
			switch (m.getName())
			{
				case "getText":
					return text;
				case "getItemId":
					return item;
				case "getBounds":
					return bounds;
				case "isHidden":
					return hidden;
				case "getDynamicChildren":
					return kids;
				case "getStaticChildren":
				case "getNestedChildren":
					return new Widget[0];
				default:
					return null;
			}
		});
	}
}
