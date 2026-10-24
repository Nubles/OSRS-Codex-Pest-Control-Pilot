package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.core.Reward;
import com.codex.pestcontrol.ui.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Lights your goal in the Void Knights' reward shop: teal round the pieces your points buy now,
 * a quiet gold line round the rest of the goal. Lines it cannot read are left alone.
 */
public class RewardShopGuide extends Overlay
{
	/** Deeper than any line of the shop nests its words. */
	private static final int MAX_DEPTH = 4;

	private final Client client;
	private volatile Guide guide;

	/** What to light. */
	public static final class Guide
	{
		final Set<Reward> buy;
		final Set<Reward> goal;

		/**
		 * @param buy the goal's pieces the points buy now
		 * @param goal the goal's pieces still to buy
		 */
		public Guide(Set<Reward> buy, Set<Reward> goal)
		{
			this.buy = buy == null || buy.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(buy);
			this.goal = goal == null || goal.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(goal);
		}
	}

	@Inject
	RewardShopGuide(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** @param guide what to light, or null to light nothing */
	public void show(Guide guide)
	{
		this.guide = guide;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Guide gd = guide;
		if (gd == null || gd.goal.isEmpty())
		{
			return null;
		}
		Widget options = client.getWidget(InterfaceID.PestRewardshop.OPTIONS);
		if (options == null || options.isHidden())
		{
			return null;
		}
		Map<Reward, Rectangle> lines = lines(options);
		if (lines.isEmpty())
		{
			return null;
		}
		Widget frame = client.getWidget(InterfaceID.PestRewardshop.OPTIONS_CONTAINER);
		Shape clip = g.getClip();
		if (frame != null && !frame.isHidden())
		{
			g.clip(frame.getBounds());
		}
		Theme.smooth(g);
		for (Map.Entry<Reward, Rectangle> e : lines.entrySet())
		{
			Reward r = e.getKey();
			if (!gd.goal.contains(r))
			{
				continue;
			}
			paint(g, e.getValue(), gd.buy.contains(r));
		}
		g.setClip(clip);
		return null;
	}

	/** Each reward's line in the shop: its words and its picture together. */
	static Map<Reward, Rectangle> lines(Widget options)
	{
		Map<Reward, Rectangle> lines = new EnumMap<>(Reward.class);
		find(options, lines, 0);
		return lines;
	}

	private static void find(Widget w, Map<Reward, Rectangle> lines, int depth)
	{
		if (w == null || w.isHidden() || depth > MAX_DEPTH)
		{
			return;
		}
		Reward r = Reward.inShop(w.getText(), w.getItemId());
		Rectangle b = w.getBounds();
		if (r != null && b != null && b.width > 0 && b.height > 0)
		{
			Rectangle had = lines.get(r);
			lines.put(r, had == null ? new Rectangle(b) : had.union(b));
		}
		for (Widget[] kids : new Widget[][]{w.getDynamicChildren(), w.getStaticChildren(), w.getNestedChildren()})
		{
			if (kids != null)
			{
				for (Widget k : kids)
				{
					find(k, lines, depth + 1);
				}
			}
		}
	}

	/** Draws one lit line of the shop; previews use the same drawing. */
	public static void paint(Graphics2D g, Rectangle line, boolean buy)
	{
		Color c = buy ? Theme.TEAL : Theme.GOLD;
		Rectangle r = new Rectangle(line.x - 2, line.y - 1, line.width + 4, line.height + 2);
		g.setColor(Theme.alpha(c, buy ? 46 : 22));
		g.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
		g.setStroke(buy ? new BasicStroke(1.6f) : new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3f, 3f}, 0f));
		g.setColor(Theme.alpha(c, buy ? 230 : 170));
		g.drawRoundRect(r.x, r.y, r.width, r.height, 6, 6);

		String tag = buy ? "BUY" : "GOAL";
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth(tag) + 8;
		int h = fm.getAscent() + 2;
		int x = r.x + r.width - w - 2;
		int y = r.y + 2;
		g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
		g.fillRoundRect(x, y, w, h, 5, 5);
		g.setColor(c);
		g.drawRoundRect(x, y, w, h, 5, 5);
		g.drawString(tag, x + 4, y + fm.getAscent());
	}
}
