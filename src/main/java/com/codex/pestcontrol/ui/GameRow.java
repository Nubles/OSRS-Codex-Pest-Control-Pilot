package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.GameRecord;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/** One finished game: how it ended, the lander, the points it gave and how it went. */
class GameRow extends JComponent
{
	private final GameRecord game;
	private final long now;

	GameRow(GameRecord game, long now)
	{
		this.game = game;
		this.now = now;
		setOpaque(false);
		setPreferredSize(new Dimension(10, 36));
	}

	static String result(GameRecord.Result r)
	{
		switch (r)
		{
			case WON:
				return "Won";
			case LOST:
				return "Lost";
			case NO_POINTS:
				return "Won, no points";
			default:
				return "Left early";
		}
	}

	static Color color(GameRecord.Result r)
	{
		switch (r)
		{
			case WON:
				return Theme.GREEN;
			case LOST:
				return Theme.BLOOD;
			case NO_POINTS:
				return Theme.EMBER;
			default:
				return Theme.TEXT_FAINT;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 8, 8);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, h, Theme.CARD));
		g.fill(shape);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(shape);

		GameRecord.Result r = game.getResult();
		Color c = color(r);
		g.setColor(Theme.alpha(c, 60));
		g.fill(new Ellipse2D.Float(7, h / 2f - 6, 12, 12));
		g.setColor(c);
		g.fill(new Ellipse2D.Float(10, h / 2f - 3, 6, 6));

		int tx = 26;
		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Boat boat = game.getBoat();
		String title = result(r) + (boat == null ? "" : "  -  " + boat.getTitle());
		String pts = game.getPoints() > 0 ? "+" + game.getPoints() : "0";
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		int pw = bold.stringWidth(pts);
		Ui.shadowed(g, pts, w - pw - 8, 4 + bold.getAscent(), game.getPoints() > 0 ? Theme.GOLD_BRIGHT : Theme.TEXT_FAINT);
		g.setFont(Theme.body());
		Ui.shadowed(g, Ui.clip(title, body, w - tx - pw - 14), tx, 4 + body.getAscent(), Theme.TEXT);

		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String line = game.getDamage() + " dmg  -  " + Format.clock(game.getSeconds()) + "  -  " + Format.ago(game.getEnded(), now);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(line, small, w - tx - 8), tx, h - 7);
		g.dispose();
	}
}
