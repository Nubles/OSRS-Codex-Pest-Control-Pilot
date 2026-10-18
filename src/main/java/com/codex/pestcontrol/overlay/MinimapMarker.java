package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** A gilt ring on the minimap round the portal to work next, so it can be found from afar. */
public class MinimapMarker extends Overlay
{
	private final PortalMarker world;

	@Inject
	MinimapMarker(PortalMarker world)
	{
		this.world = world;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		PortalMarker.Mark m = world.current();
		NPC npc = m == null ? null : m.target;
		if (npc == null)
		{
			return null;
		}
		Point at = npc.getMinimapLocation();
		if (at == null)
		{
			return null;
		}
		Theme.smooth(g);
		float r = 6;
		g.setColor(Theme.alpha(m.color, 200));
		g.fill(new Ellipse2D.Float(at.getX() - r / 2, at.getY() - r / 2, r, r));
		g.setStroke(new BasicStroke(1.6f));
		g.setColor(Theme.GOLD_BRIGHT);
		g.draw(new Ellipse2D.Float(at.getX() - r, at.getY() - r, r * 2, r * 2));
		return null;
	}
}
