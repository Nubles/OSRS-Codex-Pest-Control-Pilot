package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Reward;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The void card: the goal as it would be worn, the Goal page's equipment screen, beside the points
 * held, the games left and when, in the panel's ink and gold. Made to be pasted into a chat to show
 * a friend how close the set is, so it carries its own heading and needs nothing around it.
 */
public final class VoidSheet
{
	static final int WIDTH = 330;
	private static final int PAD = 14;
	private static final int HEADER = 66;
	/** The equipment screen's width on the card; the figures take the rest. */
	private static final int WARDROBE_W = 196;

	private VoidSheet()
	{
	}

	/** One figure in the column beside the equipment screen. */
	static final class Fact
	{
		final String label;
		final String value;
		final String note;
		final Color color;

		Fact(String label, String value, String note, Color color)
		{
			this.label = label;
			this.value = value;
			this.note = note;
			this.color = color;
		}
	}

	/**
	 * The figures beside the void, most telling first: the points against the goal, the games
	 * left and when, the points a game, and the pieces owned. A goal already bought, or one with
	 * no points read yet, still fills the column.
	 *
	 * @param elite whether the hard diary is done, so elite pieces can be bought
	 * @param by when the goal is likely bought at the player's pace, or 0 when not known
	 */
	static List<Fact> facts(Outlook o, Set<Reward> goal, Set<Reward> owned, boolean elite, long by)
	{
		List<Fact> out = new ArrayList<>();
		String held = o.isPointsKnown() ? Format.number(o.getPoints()) : "?";
		// Points that cover elite pieces the hard diary still locks do not buy them yet.
		boolean locked = false;
		for (Reward r : o.getNeeded())
		{
			locked |= r.isElite() && !elite;
		}
		out.add(new Fact("Points", held, "of " + Format.number(o.getCost()) + " needed", o.isAffordable() && !locked ? Theme.TEAL : Theme.GOLD));
		if (o.isComplete())
		{
			out.add(new Fact("Games left", "None", "the set is yours", Theme.GREEN));
		}
		else if (!o.isGamesKnown())
		{
			out.add(new Fact("Games left", "?", "points not read yet", Theme.TEXT_MUTED));
		}
		else if (o.getGames() == 0)
		{
			out.add(locked ? new Fact("Games left", "None", "diary first", Theme.EMBER)
				: new Fact("Games left", "None", "points enough now", Theme.TEAL));
		}
		else
		{
			out.add(new Fact("Games left", Format.number(o.getGames()), by > 0 ? "about " + Format.shortDay(by) : "about " + Format.span(o.getMinutes()),
				Theme.GOLD));
		}
		String per = String.format(Locale.ENGLISH, o.getPerGame() % 1 == 0 ? "%.0f" : "%.1f", o.getPerGame());
		out.add(new Fact("Per game", per, o.isMeasured() ? "from your games" : "a typical game", o.isMeasured() ? Theme.TEAL : Theme.TEXT_MUTED));
		int have = 0;
		for (Reward r : goal)
		{
			have += owned.contains(r) ? 1 : 0;
		}
		out.add(new Fact("Owned", have + " of " + goal.size(), goal.size() == 1 ? "piece" : "pieces", have == goal.size() ? Theme.GREEN : Theme.GOLD));
		return out;
	}

	/** The card's heading: whose void it is, or the plain kicker when names are left off. */
	static String kicker(String player)
	{
		return player == null ? "PEST CONTROL" : player.toUpperCase(Locale.ENGLISH) + "'S VOID";
	}

	/** The item pictures the card shows, for waiting on before it is drawn. */
	static List<Integer> pictures(Set<Reward> goal, Set<Reward> owned)
	{
		List<Integer> ids = new ArrayList<>();
		for (Wardrobe.Slot s : Wardrobe.layout(goal, owned))
		{
			if (s.reward != null)
			{
				ids.add(s.reward.getIcon());
			}
		}
		return ids;
	}

	/**
	 * Draws the card from the pictures as they are; the panel waits for them
	 * ({@link ItemIcons#whenLoaded}) before it asks.
	 *
	 * @param title the goal's name, such as "Void melee set", or null for pieces picked one by one
	 * @param player the name for the heading, or null to leave it off
	 */
	public static BufferedImage render(String title, String player, Outlook o, Set<Reward> goal, Set<Reward> owned, Set<Reward> buyable,
		boolean elite, long by, long now, ItemIcons icons)
	{
		List<Fact> facts = facts(o, goal, owned, elite, by);
		Wardrobe wardrobe = new Wardrobe(title == null ? "your goal" : title, goal, owned, buyable, Math.max(0, o.getPoints()),
			o.isPointsKnown(), elite, icons, null);
		int wardrobeH = wardrobe.getPreferredSize().height;
		int bodyY = HEADER + 12;
		int footY = bodyY + wardrobeH + 12;
		int height = footY + 26;
		BufferedImage image = new BufferedImage(WIDTH, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);

		// The parchment-dark ground with a gilt frame.
		RoundRectangle2D frame = new RoundRectangle2D.Float(0.5f, 0.5f, WIDTH - 1, height - 1, 14, 14);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, height, Theme.INK_DEEP));
		g.fill(frame);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK, Theme.GOLD_DIM, 0.3), 0, HEADER, Theme.alpha(Theme.INK, 0)));
		g.fill(new RoundRectangle2D.Float(1, 1, WIDTH - 2, HEADER, 13, 13));
		g.setStroke(new BasicStroke(1.5f));
		g.setPaint(new GradientPaint(0, 0, Theme.GOLD, 0, height, Theme.GOLD_DIM));
		g.draw(new RoundRectangle2D.Float(1, 1, WIDTH - 2.5f, height - 2.5f, 13, 13));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 40));
		g.draw(new RoundRectangle2D.Float(4.5f, 4.5f, WIDTH - 9, height - 9, 10, 10));

		header(g, player, title == null ? "MY VOID" : title.toUpperCase(Locale.ENGLISH));
		rule(g, HEADER + 2);

		// The equipment screen, as the Goal page draws it.
		wardrobe.setSize(WARDROBE_W, wardrobeH);
		Graphics2D wg = (Graphics2D) g.create(PAD, bodyY, WARDROBE_W, wardrobeH);
		wardrobe.paint(wg);
		wg.dispose();

		// The figures beside it.
		int fx = PAD + WARDROBE_W + 12;
		int fw = WIDTH - fx - PAD;
		int each = wardrobeH / facts.size();
		int fy = bodyY + 2;
		for (Fact f : facts)
		{
			fact(g, f, fx, fy, fw, each);
			fy += each;
		}

		// The foot: where it came from and when.
		rule(g, footY);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		String left = o.isMeasured() ? "At your own pace" : "At a typical pace";
		String right = "Codex Pest Control Pilot  -  " + Format.shortDay(now);
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(left, small, WIDTH - PAD * 2 - small.stringWidth(right) - 10), PAD, footY + 16);
		g.setColor(Theme.GOLD_DIM);
		g.drawString(right, WIDTH - PAD - small.stringWidth(right), footY + 16);
		g.dispose();
		return image;
	}

	private static void header(Graphics2D g, String player, String title)
	{
		int md = 46;
		int mx = PAD;
		int my = (HEADER - md) / 2 + 3;
		g.setPaint(new RadialGradientPaint(mx + md / 2f, my + md / 3f, md * 0.7f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.35), Theme.INK_DEEP}));
		g.fill(new Ellipse2D.Float(mx, my, md, md));
		g.setStroke(new BasicStroke(2f));
		g.setPaint(new GradientPaint(mx, my, Theme.GOLD_BRIGHT, mx, my + md, Theme.GOLD_DIM));
		g.draw(new Ellipse2D.Float(mx + 1, my + 1, md - 2, md - 2));
		g.setStroke(new BasicStroke(1f));
		g.setColor(Theme.alpha(Theme.GOLD, 70));
		g.draw(new Ellipse2D.Float(mx + 5, my + 5, md - 10, md - 10));
		Sigil.paint(g, mx + 10, my + 10, md - 20);

		int tx = mx + md + 12;
		int room = WIDTH - tx - PAD;
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(kicker(player), small, room), tx, my + 12);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		if (bold.stringWidth(title) * 2 + 2 <= room)
		{
			// The crest's doubled figure lettering, the way the game draws big text.
			Crest.paintLarge(g, title, tx, my + 18, Theme.GOLD);
		}
		else
		{
			Ui.shadowed(g, Ui.clip(title, bold, room), tx, my + 34, Theme.GOLD_BRIGHT);
		}
	}

	private static void fact(Graphics2D g, Fact f, int x, int y, int w, int h)
	{
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		g.setColor(Theme.TEXT_MUTED);
		g.drawString(Ui.clip(f.label.toUpperCase(Locale.ENGLISH), small, w), x, y + small.getAscent());
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		Ui.shadowed(g, Ui.clip(f.value, bold, w), x, y + small.getHeight() + bold.getAscent() + 1, f.color);
		g.setFont(Theme.small());
		g.setColor(Theme.TEXT_FAINT);
		g.drawString(Ui.clip(f.note, small, w), x, y + small.getHeight() + bold.getHeight() + small.getAscent() + 1);
		// A hairline under each figure, fading out, to keep the column in step with the slots.
		int ly = y + h - 8;
		g.setPaint(new GradientPaint(x, 0, Theme.alpha(Theme.GOLD, 70), x + w, 0, Theme.alpha(Theme.GOLD, 0)));
		g.drawLine(x, ly, x + w, ly);
	}

	private static void rule(Graphics2D g, int y)
	{
		g.setPaint(new GradientPaint(PAD, 0, Theme.alpha(Theme.GOLD, 0), WIDTH / 2f, 0, Theme.alpha(Theme.GOLD, 140), true));
		g.drawLine(PAD, y, WIDTH - PAD, y);
		Path2D diamond = new Path2D.Float();
		diamond.moveTo(WIDTH / 2f, y - 3);
		diamond.lineTo(WIDTH / 2f + 3, y);
		diamond.lineTo(WIDTH / 2f, y + 3);
		diamond.lineTo(WIDTH / 2f - 3, y);
		diamond.closePath();
		g.setColor(Theme.GOLD);
		g.fill(diamond);
	}

	/** The card as something the system clipboard can hold. */
	public static Transferable transferable(Image image)
	{
		return new Transferable()
		{
			@Override
			public DataFlavor[] getTransferDataFlavors()
			{
				return new DataFlavor[]{DataFlavor.imageFlavor};
			}

			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
				return DataFlavor.imageFlavor.equals(flavor);
			}

			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
			{
				if (!isDataFlavorSupported(flavor))
				{
					throw new UnsupportedFlavorException(flavor);
				}
				return image;
			}
		};
	}
}
