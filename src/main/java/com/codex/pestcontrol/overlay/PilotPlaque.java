package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.ui.Sigil;
import com.codex.pestcontrol.ui.Theme;
import com.codex.pestcontrol.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
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
	/** The widest a line of the detail runs before it wraps. */
	private static final int MAX_TEXT = 230;

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
		/** The layout, worked out on the first frame this is shown and kept, so a frame measures and allocates nothing. */
		private java.util.List<String> lines;
		private int width;
		private int height;
		private RoundRectangle2D shape;
		private GradientPaint fill;
		private GradientPaint edge;

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

		if (s.lines == null)
		{
			layout(s, bold, small);
		}
		java.util.List<String> detail = s.lines;
		int width = s.width;
		int height = s.height;
		g.setPaint(s.fill);
		g.fill(s.shape);
		Shape clip = g.getClip();
		g.clip(s.shape);
		g.setPaint(s.edge);
		g.fillRect(0, 0, 3, height);
		g.setClip(clip);
		g.setColor(RIM);
		g.draw(s.shape);
		g.setColor(SHINE);
		g.drawLine(8, 1, width - 8, 1);

		g.drawImage(MARK, PAD + 2, PAD + 2, null);
		int x = PAD + ICON + 10;
		int y = PAD + small.getAscent();
		g.setFont(Theme.small());
		Ui.shadowed(g, s.kicker, x, y, Theme.TEXT_MUTED);
		y += small.getDescent() + bold.getAscent() + 1;
		g.setFont(Theme.title());
		Ui.shadowed(g, s.headline, x, y, s.color);
		g.setFont(Theme.small());
		for (String line : detail)
		{
			y += small.getHeight();
			Ui.shadowed(g, line, x, y, Theme.TEXT);
		}
		if (s.foot != null)
		{
			y += small.getHeight();
			Ui.shadowed(g, s.foot, x, y, s.footColor == null ? Theme.TEXT_MUTED : s.footColor);
		}
		return new Dimension(width, height);
	}

	private static final Color RIM = Theme.alpha(Theme.GOLD, 150);
	private static final Color SHINE = Theme.alpha(Color.WHITE, 14);
	/** The mark, drawn once. */
	private static final BufferedImage MARK = Sigil.icon(ICON);

	private static void layout(Shown s, FontMetrics bold, FontMetrics small)
	{
		java.util.List<String> detail = wrap(s.detail, small, MAX_TEXT);
		int textW = Math.max(bold.stringWidth(s.headline), small.stringWidth(s.kicker));
		for (String line : detail)
		{
			textW = Math.max(textW, small.stringWidth(line));
		}
		if (s.foot != null)
		{
			textW = Math.max(textW, small.stringWidth(s.foot));
		}
		s.width = Math.max(MIN_WIDTH, PAD + ICON + 8 + textW + PAD + 2);
		int lines = 2 + detail.size() + (s.foot != null ? 1 : 0);
		s.height = PAD + small.getHeight() + bold.getHeight() + (lines - 2) * small.getHeight() + PAD - 2;
		s.shape = new RoundRectangle2D.Float(0.5f, 0.5f, s.width - 1, s.height - 1, 12, 12);
		s.fill = new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 236), 0, s.height, Theme.alpha(Theme.INK_DEEP, 236));
		s.edge = new GradientPaint(0, 0, s.color, 0, s.height, Theme.alpha(s.color, 70));
		s.lines = detail;
	}

	/** Breaks text into lines no wider than the width, at spaces. */
	static java.util.List<String> wrap(String text, FontMetrics fm, int width)
	{
		java.util.List<String> lines = new java.util.ArrayList<>();
		if (text == null || text.isEmpty())
		{
			return lines;
		}
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" "))
		{
			String next = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && fm.stringWidth(next) > width)
			{
				lines.add(line.toString());
				line = new StringBuilder(word);
			}
			else
			{
				line = new StringBuilder(next);
			}
		}
		lines.add(line.toString());
		return lines;
	}
}
