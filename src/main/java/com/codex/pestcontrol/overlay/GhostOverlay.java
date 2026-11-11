package com.codex.pestcontrol.overlay;

import com.codex.pestcontrol.ui.GhostTable;
import com.codex.pestcontrol.ui.Theme;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The ghost splits in the game view during a game, for players who keep the panel shut: each
 * segment in game ticks against the ghost on the lander. Off unless turned on in the settings. It
 * draws what the plugin hands it, and nothing outside a game.
 */
public class GhostOverlay extends Overlay
{
	private static final int PAD = 7;
	private static final int WIDTH = 180;

	private volatile GhostTable shown;

	public GhostOverlay()
	{
		setPosition(OverlayPosition.TOP_LEFT);
	}

	/** @param table the table to draw, or null to hide it */
	public void show(GhostTable table)
	{
		shown = table;
	}

	public GhostTable getShown()
	{
		return shown;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Drawn on a copy, so the clip, font, colour and hints RuneLite hands every overlay come back as they were.
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			return draw(g);
		}
		finally
		{
			g.dispose();
		}
	}

	private Dimension draw(Graphics2D g)
	{
		GhostTable s = shown;
		if (s == null)
		{
			return null;
		}
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int title = fm.getHeight() + 2;
		int w = WIDTH + PAD * 2;
		int h = PAD + title + s.tableHeight() + PAD - 2;
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 232), 0, h, Theme.alpha(Theme.INK_DEEP, 232)));
		g.fill(shape);
		g.setColor(Theme.alpha(Theme.GOLD_DIM, 170));
		g.draw(shape);
		g.setColor(Theme.GOLD);
		g.drawString("GHOST SPLITS", PAD, PAD + fm.getAscent());
		Graphics2D t = (Graphics2D) g.create();
		t.translate(PAD, PAD + title);
		s.paintTable(t, WIDTH);
		t.dispose();
		return new Dimension(w, h);
	}
}
