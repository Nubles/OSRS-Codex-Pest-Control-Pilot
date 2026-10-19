package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Outlook;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 * One stop on the way to the goal, drawn as a node on a gilt line: the piece, the points it takes
 * with everything before it, and how many games until it can be bought.
 */
class MilestoneRow extends JComponent
{
	private static final int HEIGHT = 30;

	private final Outlook.Milestone milestone;
	private final boolean first;
	private final boolean last;
	private final boolean locked;
	private final ItemIcons icons;
	/** Handed over each time an icon is asked for, so a row waiting on a picture is told once. */
	private final Runnable repaint = this::repaint;

	MilestoneRow(Outlook.Milestone milestone, boolean first, boolean last, boolean locked, ItemIcons icons)
	{
		this.milestone = milestone;
		this.first = first;
		this.last = last;
		this.locked = locked;
		this.icons = icons;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		int cx = 8;
		int cy = h / 2;
		boolean ready = milestone.getGames() == 0;
		Color node = locked ? Theme.BLOCKED : ready ? Theme.TEAL : Theme.GOLD;

		g.setStroke(new BasicStroke(2f));
		g.setColor(Theme.alpha(Theme.GOLD, 90));
		if (!first)
		{
			g.drawLine(cx, 0, cx, cy - 5);
		}
		if (!last)
		{
			g.drawLine(cx, cy + 5, cx, h);
		}
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(cx - 5, cy - 5, 10, 10));
		g.setColor(node);
		g.draw(new Ellipse2D.Float(cx - 5, cy - 5, 10, 10));
		if (ready || last)
		{
			g.fill(new Ellipse2D.Float(cx - 2.5f, cy - 2.5f, 5, 5));
		}

		int tx = 22;
		BufferedImage icon = icons == null ? null : icons.get(milestone.getReward().getIcon(), repaint);
		if (icon != null)
		{
			int s = 24;
			g.drawImage(icon, tx, cy - s / 2, s, s, null);
			tx += s + 4;
		}

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		boolean unknown = milestone.getGames() < 0;
		String when = locked ? "diary" : ready ? "now" : unknown ? "?" : Format.plural(milestone.getGames(), "game", "games");
		String time = locked || ready || unknown ? null : Format.span(milestone.getMinutes());
		int rw = Math.max(small.stringWidth(when), time == null ? 0 : small.stringWidth(time));
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		String name = milestone.getReward().getShortTitle();
		Ui.shadowed(g, Ui.clip(name, body, w - tx - rw - 8), tx, cy - 1, last ? Theme.GOLD_BRIGHT : Theme.TEXT);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(Format.number(milestone.getTotal()) + " points", small, w - tx - rw - 8), tx, cy + small.getAscent() + 1);
		Ui.shadowed(g, when, w - small.stringWidth(when), time == null ? cy + small.getAscent() / 2 : cy - 1, node);
		if (time != null)
		{
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(time, w - small.stringWidth(time), cy + small.getAscent() + 1);
		}
		g.dispose();
	}
}
