package com.codex.pestcontrol.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A calm warning about a life that cannot be got back, such as on a Hardcore World: a single
 * heart beside small capitals and a line or two, on a surface washed with deep red. Quiet enough to
 * sit on screen all the time; it never flashes.
 */
public class LifeWarning extends JPanel
{
	/**
	 * @param kicker the small capitals, such as "HARDCORE WORLD"
	 * @param text the warning
	 * @param foot a fainter line under it, or null
	 * @param width the width the text wraps at
	 */
	public LifeWarning(String kicker, String text, String foot, int width)
	{
		super(new BorderLayout(8, 0));
		setOpaque(false);
		setBorder(new EmptyBorder(7, 9, 7, 9));
		add(new Heart(), BorderLayout.WEST);
		StackPanel words = new StackPanel(2, 0);
		words.add(Ui.label(kicker, Theme.small(), Theme.EMBER));
		JLabel line = Ui.wrapped(text, Theme.small(), Theme.TEXT, width - 40);
		line.getAccessibleContext().setAccessibleName(kicker + ": " + text);
		words.add(line);
		if (foot != null)
		{
			words.add(Ui.wrapped(foot, Theme.small(), Theme.TEXT_FAINT, width - 40));
		}
		add(words, BorderLayout.CENTER);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.CARD_TOP, Theme.BLOOD_DEEP, 0.45), w, h, Theme.mix(Theme.CARD, Theme.BLOOD_DEEP, 0.12)));
		g.fill(shape);
		g.setClip(shape);
		g.setPaint(new GradientPaint(0, 0, Theme.EMBER, 0, h, Theme.alpha(Theme.BLOOD, 120)));
		g.fillRect(0, 0, 3, h);
		g.setClip(null);
		g.setColor(Theme.alpha(Theme.EMBER, 110));
		g.draw(shape);
		g.setColor(Theme.alpha(Color.WHITE, 12));
		g.drawLine(Theme.RADIUS / 2, 1, w - Theme.RADIUS / 2, 1);
		g.dispose();
	}

	/** One heart, for one life. */
	static final class Heart extends JComponent
	{
		private static final int SIZE = 18;

		Heart()
		{
			setOpaque(false);
			setPreferredSize(new Dimension(SIZE, SIZE));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			paint(g, 0, 2, SIZE);
			g.dispose();
		}

		/** Draws the heart in a size by size box. */
		static void paint(Graphics2D g, float x, float y, float size)
		{
			float s = size / 16f;
			Path2D heart = new Path2D.Float();
			heart.moveTo(x + 8 * s, y + 14.5f * s);
			heart.curveTo(x + 2 * s, y + 10 * s, x + 0.5f * s, y + 7 * s, x + 1 * s, y + 4.5f * s);
			heart.curveTo(x + 1.6f * s, y + 1.6f * s, x + 5.6f * s, y + 0.6f * s, x + 8 * s, y + 4 * s);
			heart.curveTo(x + 10.4f * s, y + 0.6f * s, x + 14.4f * s, y + 1.6f * s, x + 15 * s, y + 4.5f * s);
			heart.curveTo(x + 15.5f * s, y + 7 * s, x + 14 * s, y + 10 * s, x + 8 * s, y + 14.5f * s);
			heart.closePath();
			g.setPaint(new GradientPaint(x, y, Theme.mix(Theme.BLOOD, Theme.EMBER, 0.45), x, y + size, Theme.BLOOD_DEEP));
			g.fill(heart);
			g.setStroke(new BasicStroke(1f));
			g.setColor(Theme.alpha(Theme.GOLD, 150));
			g.draw(heart);
			g.setColor(Theme.alpha(Color.WHITE, 90));
			g.fill(new java.awt.geom.Ellipse2D.Float(x + 3.6f * s, y + 3.2f * s, 2.4f * s, 2f * s));
		}
	}
}
