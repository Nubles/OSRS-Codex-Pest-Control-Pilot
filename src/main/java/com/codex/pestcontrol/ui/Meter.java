package com.codex.pestcontrol.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** A gilt progress bar with its label above: "412 / 850 points" and how far that is. */
class Meter extends JComponent
{
	private final Animator fill = new Animator(this);
	private String left = "";
	private String right = "";
	private Color color = Theme.GOLD;

	Meter()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, 30));
	}

	void set(double progress, String left, String right, Color color)
	{
		this.left = left;
		this.right = right;
		this.color = color;
		fill.set(Math.max(0, Math.min(1, progress)), true);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int base = fm.getAscent();
		Ui.shadowed(g, Ui.clip(left, fm, w - fm.stringWidth(right) - 8), 0, base, Theme.TEXT);
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(right, w - fm.stringWidth(right), base);

		int y = base + 6;
		int h = 10;
		RoundRectangle2D track = new RoundRectangle2D.Float(0.5f, y + 0.5f, w - 1, h - 1, h, h);
		g.setColor(Theme.INK_DEEP);
		g.fill(track);
		int fw = (int) Math.round((w - 2) * fill.get());
		if (fw > 0)
		{
			g.setClip(track);
			g.setPaint(new GradientPaint(0, y, Theme.mix(color, Color.WHITE, 0.3), 0, y + h, Theme.mix(color, Theme.INK_DEEP, 0.3)));
			g.fill(new RoundRectangle2D.Float(1, y + 1, fw, h - 2, h - 2, h - 2));
			g.setColor(Theme.alpha(Color.WHITE, 60));
			g.drawLine(3, y + 2, Math.max(3, fw - 2), y + 2);
			g.setClip(null);
		}
		g.setColor(Theme.STROKE_SOFT);
		g.draw(track);
		// Quarter marks, engraved into the track.
		g.setColor(Theme.alpha(Theme.INK_DEEP, 160));
		for (int i = 1; i < 4; i++)
		{
			int x = w * i / 4;
			g.drawLine(x, y + 2, x, y + h - 3);
		}
		g.dispose();
	}
}
