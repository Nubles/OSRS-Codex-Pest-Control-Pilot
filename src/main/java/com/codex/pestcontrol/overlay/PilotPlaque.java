package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.ui.Sigil;
import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A gilt plaque in the game view: in a game, what to work next in the portal's colour, why,
 * and your damage toward points; at the outpost, your points toward the goal. It draws what
 * the plugin hands it.
 */
public class PilotPlaque extends Overlay
{
	private static final int PAD = 8;
	private static final int ICON = 22;
	private static final int MIN_WIDTH = 150;

	private volatile Shown shown;

	/** One plaque's worth of text. */
	public static final class Shown
	{
		final String kicker;
		final String headline;
		final Color color;
		final String detail;
		final String foot;
		final Color footColor;

		/**
		 * @param kicker the small capitals above, such as "WORK NEXT"
		 * @param foot a last line, such as the damage dealt, or null
		 */
		public Shown(String kicker, String headline, Color color, String detail, String foot, Color footColor)
		{
			this.kicker = kicker;
			this.headline = headline;
			this.color = color;
			this.detail = detail;
			this.foot = foot;
			this.footColor = footColor;
		}
	}

	public PilotPlaque()
	{
		setPosition(OverlayPosition.TOP_CENTER);
	}

	/** @param shown what to draw, or null to hide the plaque */
	public void show(Shown shown)
	{
		this.shown = shown;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Shown s = shown;
		if (s == null)
		{
			return null;
		}
		return paint(g, s);
	}

	/** Draws a plaque at the origin; previews use the same drawing. */
	public static Dimension paint(Graphics2D g, Shown s)
	{
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int textW = Math.max(bold.stringWidth(s.headline), small.stringWidth(s.kicker));
		if (s.detail != null)
		{
			textW = Math.max(textW, small.stringWidth(s.detail));
		}
		if (s.foot != null)
		{
			textW = Math.max(textW, small.stringWidth(s.foot));
		}
		int width = Math.max(MIN_WIDTH, PAD + ICON + 8 + textW + PAD + 2);
		int lines = 2 + (s.detail != null ? 1 : 0) + (s.foot != null ? 1 : 0);
		int height = PAD + small.getHeight() + bold.getHeight() + (lines - 2) * small.getHeight() + PAD - 2;

		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 12, 12);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 236), 0, height, Theme.alpha(Theme.INK_DEEP, 236)));
		g.fill(shape);
		g.setClip(shape);
		g.setPaint(new GradientPaint(0, 0, s.color, 0, height, Theme.alpha(s.color, 70)));
		g.fillRect(0, 0, 3, height);
		g.setClip(null);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(Color.WHITE, 14));
		g.drawLine(8, 1, width - 8, 1);

		Sigil.paint(g, PAD + 2, PAD + 2, ICON);
		int x = PAD + ICON + 10;
		int y = PAD + small.getAscent();
		g.setFont(Theme.small());
		Ui.shadowed(g, s.kicker, x, y, Theme.TEXT_MUTED);
		y += small.getDescent() + bold.getAscent() + 1;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.headline, x, y, s.color);
		g.setFont(Theme.small());
		if (s.detail != null)
		{
			y += small.getHeight();
			Ui.shadowed(g, s.detail, x, y, Theme.TEXT);
		}
		if (s.foot != null)
		{
			y += small.getHeight();
			Ui.shadowed(g, s.foot, x, y, s.footColor == null ? Theme.TEXT_MUTED : s.footColor);
		}
		return new Dimension(width, height);
	}
}
