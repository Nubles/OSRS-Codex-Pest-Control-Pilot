package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.Ui;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

/**
 * Marks the portal to work next in the world: an outline in its colour, a gilt ring on the
 * ground and a word over it. Portals that might be next get a faint outline only.
 */
public class PortalMarker extends Overlay
{
	private final ModelOutlineRenderer outlines;
	private volatile Mark mark;

	/** What to mark. */
	public static final class Mark
	{
		final NPC target;
		final Color color;
		final String label;
		final List<NPC> maybe;
		final List<Color> maybeColors;

		public Mark(NPC target, Color color, String label, List<NPC> maybe, List<Color> maybeColors)
		{
			this.target = target;
			this.color = color;
			this.label = label;
			this.maybe = maybe == null ? Collections.emptyList() : maybe;
			this.maybeColors = maybeColors == null ? Collections.emptyList() : maybeColors;
		}
	}

	@Inject
	PortalMarker(ModelOutlineRenderer outlines)
	{
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** @param mark what to mark, or null to mark nothing */
	public void show(Mark mark)
	{
		this.mark = mark;
	}

	Mark current()
	{
		return mark;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Mark m = mark;
		if (m == null)
		{
			return null;
		}
		for (int i = 0; i < m.maybe.size(); i++)
		{
			NPC npc = m.maybe.get(i);
			outlines.drawOutline(npc, 2, Theme.alpha(m.maybeColors.get(i), 110), 2);
		}
		NPC npc = m.target;
		if (npc == null)
		{
			return null;
		}
		outlines.drawOutline(npc, 3, m.color, 3);
		Polygon tile = npc.getCanvasTilePoly();
		if (tile != null)
		{
			g.setStroke(new BasicStroke(2f));
			g.setColor(Theme.alpha(Theme.GOLD_BRIGHT, 200));
			g.draw(tile);
			g.setColor(Theme.alpha(m.color, 40));
			g.fill(tile);
		}
		if (m.label != null)
		{
			g.setFont(Theme.title());
			Point at = npc.getCanvasTextLocation(g, m.label, npc.getLogicalHeight() + 40);
			if (at != null)
			{
				Ui.shadowed(g, m.label, at.getX(), at.getY(), Theme.GOLD_BRIGHT);
			}
		}
		return null;
	}
}
