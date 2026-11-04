package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Grade;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Locale;
import javax.swing.JComponent;

/**
 * The head of the grade card: the letter struck on a gilt medallion, and beside it the game's
 * points an hour, counting the lander wait, against the ghost's.
 */
class GradeMedal extends JComponent
{
	static final int HEIGHT = 62;
	private static final int MEDAL = 50;

	private final Grade grade;

	GradeMedal(Grade grade)
	{
		this.grade = grade;
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
	}

	/** The letter's colour: S bright gold, A teal, B dull gold, C ember; the first ghost teal. */
	static Color tint(Grade.Letter letter)
	{
		if (letter == null)
		{
			return Theme.TEAL;
		}
		switch (letter)
		{
			case S:
				return Theme.GOLD_BRIGHT;
			case A:
				return Theme.TEAL;
			case B:
				return Theme.GOLD;
			default:
				return Theme.EMBER;
		}
	}

	static String rate(double r)
	{
		return Math.round(r) + " pts/hr";
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		Color tint = tint(grade.getLetter());
		int cy = HEIGHT / 2;

		Ellipse2D disc = new Ellipse2D.Float(1.5f, cy - MEDAL / 2f, MEDAL, MEDAL);
		g.setPaint(new GradientPaint(0, cy - MEDAL / 2f, Theme.CARD_TOP, 0, cy + MEDAL / 2f, Theme.INK_DEEP));
		g.fill(disc);
		g.setStroke(new BasicStroke(2f));
		g.setPaint(new GradientPaint(0, cy - MEDAL / 2f, Theme.mix(tint, Color.WHITE, 0.25), 0, cy + MEDAL / 2f, Theme.alpha(tint, 120)));
		g.draw(disc);
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(tint, 60));
		g.draw(new Ellipse2D.Float(6.5f, cy - MEDAL / 2f + 5, MEDAL - 10, MEDAL - 10));
		if (grade.getLetter() == null)
		{
			paintPixels(g, "1st", 1.5f + MEDAL / 2f, cy, 2, tint);
		}
		else
		{
			paintPixels(g, grade.getLetter().name(), 1.5f + MEDAL / 2f, cy, 3, tint);
		}

		int x = MEDAL + 10;
		int w = getWidth() - x;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();

		int y = 5 + small.getAscent();
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(grade.boatName().toUpperCase(Locale.ENGLISH) + " LANDER", small, w), x, y);

		y += bold.getHeight();
		g.setFont(Theme.title());
		String headline = grade.getResult() == GameRecord.Result.WON ? rate(grade.getRate())
			: grade.getResult() == GameRecord.Result.LOST ? "Lost" : "No points";
		Ui.shadowed(g, Ui.clip(headline, bold, w), x, y, tint);

		g.setFont(Theme.small());
		y += small.getHeight() + 2;
		String vs;
		if (grade.getLetter() == null)
		{
			vs = grade.isNewGhost() ? "now your ghost" : "no ghost yet";
		}
		else
		{
			vs = "ghost " + rate(grade.getGhostRate()) + (grade.getRate() > 0 ? ", " + Math.round(grade.share() * 100) + "%" : "");
		}
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(vs, small, w), x, y);
		y += small.getHeight() + 1;
		if (grade.isNewGhost() && grade.getLetter() != null)
		{
			String chip = "NEW GHOST";
			int cw = small.stringWidth(chip) + 8;
			g.setColor(Theme.alpha(Theme.TEAL, 40));
			g.fillRoundRect(x, y - small.getAscent() - 1, cw, small.getHeight() + 1, 6, 6);
			g.setColor(Theme.alpha(Theme.TEAL, 150));
			g.drawRoundRect(x, y - small.getAscent() - 1, cw, small.getHeight() + 1, 6, 6);
			Ui.shadowed(g, chip, x + 4, y, Theme.TEAL);
		}
		else
		{
			g.setColor(Theme.TEXT_FAINT);
			g.drawString(Ui.clip(Format.plural(grade.getPoints(), "point", "points") + " this game", small, w), x, y);
		}
		g.dispose();
	}

	/** Text centred on a point at a whole multiple of the game's bold font, every pixel enlarged. */
	static void paintPixels(Graphics2D g, String text, float cx, float cy, int scale, Color tint)
	{
		Font font = Theme.title();
		FontMetrics fm = g.getFontMetrics(font);
		int w = Math.max(1, fm.stringWidth(text) + 1);
		int h = fm.getAscent() + 2;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D sg = img.createGraphics();
		sg.setFont(font);
		sg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		int base = fm.getAscent();
		sg.setColor(Color.BLACK);
		sg.drawString(text, 1, base + 1);
		sg.setPaint(new GradientPaint(0, 2, Theme.mix(tint, Color.WHITE, 0.4), 0, base, tint));
		sg.drawString(text, 0, base);
		sg.dispose();
		int top = 0;
		search:
		for (int yy = 0; yy < h; yy++)
		{
			for (int xx = 0; xx < w; xx++)
			{
				if ((img.getRGB(xx, yy) >>> 24) != 0)
				{
					top = yy;
					break search;
				}
			}
		}
		Graphics2D lg = (Graphics2D) g.create();
		lg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		int dw = w * scale;
		int dh = h * scale;
		lg.drawImage(img, Math.round(cx - dw / 2f), Math.round(cy - (top + h) * scale / 2f), dw, dh, null);
		lg.dispose();
	}
}
