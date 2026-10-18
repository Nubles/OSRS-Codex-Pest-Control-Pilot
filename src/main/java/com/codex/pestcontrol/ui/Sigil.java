package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Portal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Pilot's mark: a gilt Void Knight's shield with the four portals set around a glowing
 * rift at its heart, each quarter in its portal's colour.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		Path2D shield = new Path2D.Float();
		shield.moveTo(x + 1.6f * s, y + 1.4f * s);
		shield.lineTo(x + 14.4f * s, y + 1.4f * s);
		shield.lineTo(x + 14.4f * s, y + 7.4f * s);
		shield.curveTo(x + 14.4f * s, y + 11.4f * s, x + 11.2f * s, y + 13.6f * s, x + 8f * s, y + 15.2f * s);
		shield.curveTo(x + 4.8f * s, y + 13.6f * s, x + 1.6f * s, y + 11.4f * s, x + 1.6f * s, y + 7.4f * s);
		shield.closePath();

		g.setPaint(new GradientPaint(x, y, Theme.mix(Theme.INK, Theme.VIOLET, 0.28), x, y + size, Theme.INK_DEEP));
		g.fill(shield);
		g.setStroke(new BasicStroke(Math.max(1f, 1.1f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(x, y, Theme.GOLD_BRIGHT, x, y + size, Theme.GOLD_DIM));
		g.draw(shield);

		// The rift: a teal glow with the four portals' colours wheeling round it.
		float cx = x + 8f * s;
		float cy = y + 7.6f * s;
		float r = 4.1f * s;
		g.setPaint(new RadialGradientPaint(cx, cy, r, new float[]{0f, 0.55f, 1f},
			new Color[]{Theme.mix(Theme.TEAL, Color.WHITE, 0.55), Theme.alpha(Theme.TEAL, 170), Theme.alpha(Theme.TEAL, 0)}));
		g.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
		float ring = 3.2f * s;
		g.setStroke(new BasicStroke(Math.max(1f, 1.15f * s), BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
		// West, south-west, south-east and east, as the portals stand round the knight.
		double[] starts = {135, 225, 270, 315};
		double[] extents = {80, 40, 40, 80};
		Portal[] order = {Portal.PURPLE, Portal.RED, Portal.YELLOW, Portal.BLUE};
		for (int i = 0; i < order.length; i++)
		{
			g.setColor(new Color(order[i].getRgb()));
			g.draw(new Arc2D.Float(cx - ring, cy - ring, ring * 2, ring * 2, (float) starts[i] + 4, (float) extents[i] - 8, Arc2D.OPEN));
		}
		g.setColor(Theme.alpha(Color.WHITE, 220));
		float dot = Math.max(1.2f, 1.3f * s);
		g.fill(new Ellipse2D.Float(cx - dot / 2, cy - dot / 2, dot, dot));
	}
}
