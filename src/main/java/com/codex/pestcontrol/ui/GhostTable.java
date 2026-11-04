package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Ghost;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * The ghost split table: each segment of the game in game ticks, this game against the ghost on its
 * lander, with the gap green where it is quicker and ember where it is slower. The segment under
 * way is lit. A portal segment carries a dot in the colour of the portal that fell. The panel and
 * the in-game overlay draw the same table.
 */
public final class GhostTable extends JComponent
{
	static final int ROW = 16;
	static final int HEAD = 14;

	private final List<Ghost.Row> rows;
	private final Integer behind;
	private final boolean live;

	public GhostTable(View.GhostSplits splits)
	{
		this.rows = splits.rows;
		this.behind = Ghost.behind(splits.game, splits.ghost);
		this.live = splits.live;
		setOpaque(false);
		setPreferredSize(new Dimension(10, height(rows.size())));
	}

	/** The table's height for so many rows, with its footer. */
	public static int height(int rows)
	{
		return HEAD + rows * ROW + ROW + 2;
	}

	public int tableHeight()
	{
		return height(rows.size());
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paintTable(g, getWidth());
		g.dispose();
	}

	static Color deltaColor(int d)
	{
		return d == 0 ? Theme.TEXT_MUTED : d < 0 ? Theme.GREEN : Theme.EMBER;
	}

	static String delta(int d)
	{
		return d == 0 ? "even" : (d < 0 ? "-" : "+") + Math.abs(d);
	}

	static String ticks(int t)
	{
		return t < 0 ? "-" : t + "t";
	}

	/** Draws the table at the origin, so wide. */
	public void paintTable(Graphics2D g, int w)
	{
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int deltaRight = w - 2;
		int ghostRight = deltaRight - fm.stringWidth("+000") - 8;
		int mineRight = ghostRight - Math.max(fm.stringWidth("GHOST"), fm.stringWidth("000t")) - 8;

		int headBase = fm.getAscent();
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("SEGMENT", 0, headBase);
		right(g, fm, "YOU", mineRight, headBase);
		right(g, fm, "GHOST", ghostRight, headBase);
		right(g, fm, "+/-", deltaRight, headBase);

		int y = HEAD;
		for (Ghost.Row row : rows)
		{
			int base = y + (ROW + fm.getAscent()) / 2 - 1;
			if (row.isLive())
			{
				g.setColor(Theme.alpha(Theme.GOLD, 28));
				g.fill(new RoundRectangle2D.Float(-4, y, w + 8, ROW, 6, 6));
			}
			int x = 0;
			if (row.getSegment().kill() > 0)
			{
				Color dot = row.getPortal() == null ? Theme.STROKE : Theme.portal(row.getPortal());
				g.setColor(dot);
				g.fill(new Ellipse2D.Float(1, y + ROW / 2f - 3, 6, 6));
				x = 10;
			}
			g.setColor(row.isLive() ? Theme.GOLD : Theme.TEXT);
			g.drawString(Ui.clip(row.getSegment().getTitle(), fm, mineRight - fm.stringWidth("000t") - x - 4), x, base);
			g.setColor(row.isLive() ? Theme.GOLD_BRIGHT : Theme.TEXT);
			right(g, fm, ticks(row.getMine()), mineRight, base);
			g.setColor(Theme.TEXT_FAINT);
			right(g, fm, ticks(row.getGhost()), ghostRight, base);
			Integer d = row.getDelta();
			if (d != null)
			{
				String s = delta(d);
				Ui.shadowed(g, s, deltaRight - fm.stringWidth(s), base, deltaColor(d));
			}
			y += ROW;
		}
		g.setColor(Theme.STROKE_SOFT);
		g.fillRect(0, y + 1, w, 1);
		int base = y + 2 + (ROW + fm.getAscent()) / 2 - 1;
		g.setColor(Theme.TEXT_MUTED);
		g.drawString("Overall", 0, base);
		String right = behind == null ? "no ghost yet" : behind == 0 ? "level with ghost"
			: Math.abs(behind) + (Math.abs(behind) == 1 ? " tick " : " ticks ") + (behind < 0 ? "ahead" : "behind");
		Ui.shadowed(g, right, deltaRight - fm.stringWidth(right), base, behind == null ? Theme.TEXT_FAINT : deltaColor(behind));
	}

	private static void right(Graphics2D g, FontMetrics fm, String s, int right, int base)
	{
		g.drawString(s, right - fm.stringWidth(s), base);
	}
}
