package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Battle;
import com.codex.pestcontrol.core.GameRecord;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JComponent;

/**
 * Your damage in each recent game as a row of bars, oldest on the left, coloured by how the game
 * ended, against the dashed line at the 50 damage a game needs for points. A bar short of the
 * line is a game that gave nothing for your time.
 */
class DamageChart extends JComponent
{
	private static final int HEIGHT = 64;

	private final List<GameRecord> games;

	DamageChart(List<GameRecord> games)
	{
		this.games = games;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		int w = getWidth();
		int h = getHeight();
		int most = Battle.DAMAGE_FOR_POINTS * 2;
		for (GameRecord game : games)
		{
			most = Math.max(most, game.getDamage());
		}
		int floor = h - 1;
		int top = fm.getAscent() + 2;
		int span = floor - top;
		g.setColor(Theme.alpha(Theme.GOLD, 40));
		g.fillRect(0, floor, w, 1);

		int n = Math.max(1, games.size());
		int gap = 2;
		int slot = Math.max(3, Math.min(12, (w + gap) / n - gap));
		int x = 0;
		for (GameRecord game : games)
		{
			int bh = Math.max(2, (int) Math.round(span * Math.min(game.getDamage(), most) / (double) most));
			Color c = game.getDamage() < Battle.DAMAGE_FOR_POINTS ? Theme.EMBER : GameRow.color(game.getResult());
			g.setPaint(new GradientPaint(0, floor - bh, c, 0, floor, Theme.mix(c, Theme.INK, 0.6)));
			g.fillRoundRect(x, floor - bh, slot, bh, 3, 3);
			x += slot + gap;
		}

		int line = floor - (int) Math.round(span * Battle.DAMAGE_FOR_POINTS / (double) most);
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{3, 3}, 0));
		g.setColor(Theme.alpha(Theme.TEXT, 150));
		g.drawLine(0, line, w, line);
		String label = Battle.DAMAGE_FOR_POINTS + " for points";
		Ui.shadowed(g, label, w - fm.stringWidth(label), line - 3, Theme.TEXT_MUTED);
		String peak = String.valueOf(most);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(peak, 0, fm.getAscent());
		g.dispose();
	}
}
