package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Reward;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * One reward from the Void Knights: its picture in a bank slot, its name and price, and a
 * chip saying where it stands. Clicking adds it to the goal or takes it out.
 */
class RewardRow extends JComponent
{
	/** Where a reward stands against the goal. */
	enum Standing
	{
		OWNED("OWNED", Theme.GREEN),
		CAN_BUY("CAN BUY", Theme.TEAL),
		IN_GOAL("IN GOAL", Theme.GOLD),
		LOCKED("LOCKED", Theme.BLOCKED),
		NONE(null, Theme.TEXT_FAINT);

		final String chip;
		final Color color;

		Standing(String chip, Color color)
		{
			this.chip = chip;
			this.color = color;
		}
	}

	private static final int HEIGHT = 42;

	private final Reward reward;
	private final Standing standing;
	private final String note;
	private final ItemIcons icons;
	/** Handed over each time an icon is asked for, so a row waiting on a picture is told once. */
	private final Runnable repaint = this::repaint;
	private boolean hover;

	/** @param note a quiet line under the price, such as what it needs, or null */
	RewardRow(Reward reward, Standing standing, String note, ItemIcons icons, Runnable onClick)
	{
		this.reward = reward;
		this.standing = standing;
		this.note = note;
		this.icons = icons;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setToolTipText(standing == Standing.NONE ? "Add to your goal" : standing == Standing.OWNED ? "Already yours" : "Click to take it out of your goal");
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				hover = true;
				repaint();
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				hover = false;
				repaint();
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()))
				{
					onClick.run();
				}
			}
		});
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		boolean inGoal = standing != Standing.NONE;
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		Color top = hover ? Theme.CARD_HOVER : inGoal ? Theme.CARD_TOP : Theme.mix(Theme.CARD, Theme.INK, 0.5);
		g.setPaint(new GradientPaint(0, 0, top, 0, h, inGoal ? Theme.CARD : Theme.INK));
		g.fill(shape);
		if (inGoal)
		{
			g.setClip(shape);
			g.setPaint(new GradientPaint(0, 0, standing.color, 0, h, Theme.alpha(standing.color, 90)));
			g.fillRect(0, 0, 3, h);
			g.setClip(null);
		}
		g.setColor(hover ? Theme.alpha(Theme.GOLD, 90) : Theme.STROKE_SOFT);
		g.draw(shape);

		BufferedImage icon = icons.get(reward.getIcon(), repaint);
		Well.paint(g, 6, (h - Well.SIZE) / 2, icon, inGoal ? Theme.alpha(standing.color, 90) : null, !inGoal && !hover);

		int tx = 6 + Well.SIZE + 4 + 8;
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Color nameColor = inGoal ? Theme.TEXT : Theme.TEXT_MUTED;
		Ui.shadowed(g, Ui.clip(reward.getTitle(), body, w - tx - 8), tx, 6 + body.getAscent(), nameColor);

		// The chip sits on the price line, so the name keeps the full width.
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int chipW = 0;
		int baseline = h - 9;
		if (standing.chip != null)
		{
			chipW = small.stringWidth(standing.chip) + 10;
			int cx = w - chipW - 7;
			int cy = baseline - small.getAscent() - 2;
			RoundRectangle2D chip = new RoundRectangle2D.Float(cx + 0.5f, cy + 0.5f, chipW - 1, 13, 8, 8);
			g.setColor(Theme.alpha(standing.color, 34));
			g.fill(chip);
			g.setColor(Theme.alpha(standing.color, 150));
			g.draw(chip);
			g.setColor(standing.color);
			g.drawString(standing.chip, cx + 5, baseline - 1);
		}
		else if (hover)
		{
			String add = "+ ADD";
			chipW = small.stringWidth(add) + 6;
			g.setColor(Theme.GOLD);
			g.drawString(add, w - chipW - 4, baseline);
		}
		String price = standing == Standing.OWNED ? "Yours" : Format.number(reward.getCost()) + " pts";
		String line = note == null ? price : price + ", " + note;
		g.setColor(note != null && standing == Standing.LOCKED ? Theme.BLOCKED : inGoal ? Theme.GOLD_DIM : Theme.TEXT_FAINT);
		g.drawString(Ui.clip(line, small, w - tx - chipW - 14), tx, baseline);
		g.dispose();
	}
}
