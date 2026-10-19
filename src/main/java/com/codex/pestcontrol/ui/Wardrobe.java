package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Reward;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
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
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JComponent;

/**
 * The void as it would be worn: the game's own equipment screen, with each reward in its slot, the
 * three helms racked along the top. What is owned is drawn whole in a gilt slot, what the goal still
 * wants is drawn dim over a bar of the points saved toward it, and the rest is a faint outline.
 * Clicking a slot adds that reward to the goal or takes it out, as the list below does.
 */
class Wardrobe extends JComponent
{
	private static final int PAD = 12;
	private static final int SLOT_W = 38;
	private static final int SLOT_H = 34;
	private static final int GAP_X = 14;
	private static final int GAP_Y = 9;
	private static final int TOP = 30;
	private static final int ROWS = 5;
	private static final int HEIGHT = TOP + ROWS * SLOT_H + (ROWS - 1) * GAP_Y + 44;

	/** Where a slot sits on the equipment screen, by column and row, and what it holds. */
	static final class Slot
	{
		final int col;
		final int row;
		/** The reward in it, or null for a slot void has nothing for. */
		final Reward reward;

		Slot(int col, int row, Reward reward)
		{
			this.col = col;
			this.row = row;
			this.reward = reward;
		}
	}

	/** Where a reward stands, for its slot's look. */
	enum Look
	{
		OWNED,
		CAN_BUY,
		WANTED,
		LOCKED,
		NONE
	}

	private final Slot[] slots;
	private final Set<Reward> goal;
	private final Set<Reward> owned;
	private final int points;
	private final boolean pointsKnown;
	private final boolean elite;
	private final ItemIcons icons;
	/** Handed over each time an icon is asked for, so a row waiting on a picture is told once. */
	private final Runnable repaint = this::repaint;
	private final String title;

	/**
	 * @param points commendation points held, for how far each wanted piece is saved toward
	 * @param elite the hard Western Provinces diary is done, so elite pieces can be bought
	 * @param toggle adds a reward to the goal or takes it out; null for none
	 */
	Wardrobe(String title, Set<Reward> goal, Set<Reward> owned, int points, boolean pointsKnown, boolean elite, ItemIcons icons,
		Consumer<Reward> toggle)
	{
		this.title = title;
		this.goal = goal;
		this.owned = owned;
		this.points = points;
		this.pointsKnown = pointsKnown;
		this.elite = elite;
		this.icons = icons;
		this.slots = layout(goal, owned);
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
		// Tooltips are worked out per slot.
		setToolTipText("");
		if (toggle != null)
		{
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseReleased(MouseEvent e)
				{
					Slot s = at(e.getX(), e.getY());
					if (s != null && s.reward != null && contains(e.getPoint()))
					{
						toggle.accept(s.reward);
					}
				}
			});
			addMouseMotionListener(new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent e)
				{
					Slot s = at(e.getX(), e.getY());
					setCursor(Cursor.getPredefinedCursor(s != null && s.reward != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
				}
			});
		}
	}

	/**
	 * The equipment screen's slots: the helms along the top in place of the head, then neck, the
	 * weapon, body and shield row, legs, and hands, feet and ring, as the game lays them out. The
	 * body and legs hold the elite pieces when the goal wants them or they are owned.
	 */
	static Slot[] layout(Set<Reward> goal, Set<Reward> owned)
	{
		boolean eliteTop = goal.contains(Reward.ELITE_TOP) || owned.contains(Reward.ELITE_TOP);
		boolean eliteRobe = goal.contains(Reward.ELITE_ROBE) || owned.contains(Reward.ELITE_ROBE);
		return new Slot[]{
			new Slot(0, 0, Reward.MELEE_HELM), new Slot(1, 0, Reward.RANGER_HELM), new Slot(2, 0, Reward.MAGE_HELM),
			new Slot(0, 1, null), new Slot(1, 1, Reward.SEAL), new Slot(2, 1, null),
			new Slot(0, 2, Reward.MACE), new Slot(1, 2, eliteTop ? Reward.ELITE_TOP : Reward.TOP), new Slot(2, 2, null),
			new Slot(1, 3, eliteRobe ? Reward.ELITE_ROBE : Reward.ROBE),
			new Slot(0, 4, Reward.GLOVES), new Slot(1, 4, null), new Slot(2, 4, null),
		};
	}

	/** How a reward's slot looks: owned whole, wanted dim with its savings, the rest faint. */
	static Look look(Reward r, Set<Reward> goal, Set<Reward> owned, int points, boolean pointsKnown, boolean elite)
	{
		if (owned.contains(r))
		{
			return Look.OWNED;
		}
		if (!goal.contains(r))
		{
			return Look.NONE;
		}
		if (r.isElite() && !elite)
		{
			return Look.LOCKED;
		}
		return pointsKnown && points >= r.getCost() ? Look.CAN_BUY : Look.WANTED;
	}

	private int gridX()
	{
		return (getWidth() - (3 * SLOT_W + 2 * GAP_X)) / 2;
	}

	private int slotX(Slot s)
	{
		return gridX() + s.col * (SLOT_W + GAP_X);
	}

	private static int slotY(Slot s)
	{
		return TOP + s.row * (SLOT_H + GAP_Y);
	}

	private Slot at(int x, int y)
	{
		for (Slot s : slots)
		{
			int sx = slotX(s);
			int sy = slotY(s);
			if (x >= sx && x < sx + SLOT_W && y >= sy && y < sy + SLOT_H)
			{
				return s;
			}
		}
		return null;
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		Slot s = at(e.getX(), e.getY());
		if (s == null || s.reward == null)
		{
			return null;
		}
		Reward r = s.reward;
		switch (look(r, goal, owned, points, pointsKnown, elite))
		{
			case OWNED:
				return r.getTitle() + ": yours";
			case CAN_BUY:
				return r.getTitle() + ": you can buy it now (" + r.getCost() + " points). Click to take it out of your goal";
			case WANTED:
				return r.getTitle() + ": " + (pointsKnown ? Math.max(0, r.getCost() - Math.max(0, points)) + " points to go" : r.getCost() + " points")
					+ ". Click to take it out of your goal";
			case LOCKED:
				return r.getTitle() + ": needs the hard Western Provinces diary";
			default:
				return r.getTitle() + ", " + r.getCost() + " points. Click to add it to your goal";
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, HEIGHT - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.CARD_TOP, 0, HEIGHT, Theme.CARD));
		g.fill(shape);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(shape);

		g.setFont(Theme.body());
		FontMetrics body = g.getFontMetrics();
		Ui.shadowed(g, "Your void", PAD, 8 + body.getAscent(), Theme.GOLD);
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();
		int have = 0;
		int wanted = 0;
		for (Reward r : goal)
		{
			wanted++;
			have += owned.contains(r) ? 1 : 0;
		}
		String count = wanted == 0 ? "no goal" : have + " of " + wanted + " owned";
		g.setColor(have == wanted && wanted > 0 ? Theme.GREEN : Theme.TEXT_FAINT);
		g.drawString(count, w - PAD - small.stringWidth(count), 8 + body.getAscent());

		// The equipment screen's threads between the slots, as the game draws them.
		int gx = gridX();
		int cx = gx + SLOT_W + GAP_X + SLOT_W / 2;
		g.setColor(Theme.alpha(Theme.GOLD, 45));
		g.setStroke(new BasicStroke(2f));
		g.drawLine(cx, slotY(new Slot(1, 0, null)) + SLOT_H / 2, cx, slotY(new Slot(1, 4, null)) + SLOT_H / 2);
		for (int row : new int[]{1, 2, 4})
		{
			int y = TOP + row * (SLOT_H + GAP_Y) + SLOT_H / 2;
			g.drawLine(gx + SLOT_W / 2, y, gx + 2 * (SLOT_W + GAP_X) + SLOT_W / 2, y);
		}
		g.setStroke(new BasicStroke(1f));

		for (Slot s : slots)
		{
			paintSlot(g, s, slotX(s), slotY(s), small);
		}

		// Under it: where the goal stands, in one line.
		String foot = foot(title, goal, owned, points, pointsKnown, elite);
		Color footColor = wanted == 0 ? Theme.TEXT_MUTED : have == wanted ? Theme.GREEN
			: foot.startsWith("Enough") ? Theme.TEAL : foot.startsWith("Points") ? Theme.GOLD : Theme.TEXT_MUTED;
		int fy = HEIGHT - 14;
		Ui.shadowed(g, Ui.clip(foot, small, w - PAD * 2), (w - Math.min(w - PAD * 2, small.stringWidth(foot))) / 2, fy, footColor);
		g.dispose();
	}

	/**
	 * The line under the slots: what the rest of the goal still costs, or that the points are there,
	 * minding that the elite pieces cannot be bought until the diary is done.
	 */
	static String foot(String title, Set<Reward> goal, Set<Reward> owned, int points, boolean pointsKnown, boolean elite)
	{
		int left = 0;
		boolean locked = false;
		boolean all = true;
		for (Reward r : goal)
		{
			if (!owned.contains(r))
			{
				all = false;
				left += r.getCost();
				locked |= r.isElite() && !elite;
			}
		}
		if (goal.isEmpty())
		{
			return "Click a slot to set a goal";
		}
		if (all)
		{
			return "All of " + title.toLowerCase(Locale.ENGLISH) + " is yours";
		}
		if (!pointsKnown)
		{
			return Format.number(left) + " points for the rest";
		}
		int toGo = Math.max(0, left - Math.max(0, points));
		if (toGo > 0)
		{
			return Format.number(toGo) + " points to the rest";
		}
		return locked ? "Points enough; the diary is due" : "Enough points for the rest now";
	}

	private void paintSlot(Graphics2D g, Slot s, int x, int y, FontMetrics small)
	{
		Reward r = s.reward;
		Look look = r == null ? null : look(r, goal, owned, points, pointsKnown, elite);
		RoundRectangle2D well = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, SLOT_W - 1, SLOT_H - 1, 7, 7);
		g.setPaint(new GradientPaint(0, y, Theme.INK_DEEP, 0, y + SLOT_H, Theme.mix(Theme.INK_DEEP, Theme.CARD, 0.6)));
		g.fill(well);
		Color rim = look == null ? Theme.alpha(Theme.STROKE_SOFT, 120)
			: look == Look.OWNED ? Theme.GOLD
			: look == Look.CAN_BUY ? Theme.TEAL
			: look == Look.WANTED ? Theme.GOLD_DIM
			: look == Look.LOCKED ? Theme.BLOCKED : Theme.STROKE_SOFT;
		g.setColor(rim);
		g.setStroke(new BasicStroke(look == Look.OWNED || look == Look.CAN_BUY ? 1.5f : 1f));
		g.draw(well);
		g.setStroke(new BasicStroke(1f));
		if (r == null)
		{
			return;
		}
		BufferedImage icon = icons == null ? null : icons.get(r.getIcon(), repaint);
		if (icon != null)
		{
			Composite was = g.getComposite();
			float alpha = look == Look.OWNED || look == Look.CAN_BUY ? 1f : look == Look.NONE ? 0.22f : 0.5f;
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			g.drawImage(icon, x + (SLOT_W - icon.getWidth()) / 2, y + (SLOT_H - icon.getHeight()) / 2, null);
			g.setComposite(was);
		}
		if (look == Look.OWNED)
		{
			// A small gilt tick in the corner.
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(Theme.INK_DEEP);
			g.drawPolyline(new int[]{x + SLOT_W - 11, x + SLOT_W - 8, x + SLOT_W - 3}, new int[]{y + SLOT_H - 8, y + SLOT_H - 5, y + SLOT_H - 11}, 3);
			g.setColor(Theme.GREEN);
			g.setStroke(new BasicStroke(1.2f));
			g.drawPolyline(new int[]{x + SLOT_W - 11, x + SLOT_W - 8, x + SLOT_W - 3}, new int[]{y + SLOT_H - 9, y + SLOT_H - 6, y + SLOT_H - 12}, 3);
			g.setStroke(new BasicStroke(1f));
		}
		else if (look == Look.WANTED || look == Look.CAN_BUY)
		{
			// The points saved toward it, as a bar along the slot's foot.
			double share = pointsKnown ? Math.max(0, Math.min(1, points / (double) r.getCost())) : 0;
			int bw = SLOT_W - 8;
			g.setColor(Theme.alpha(Theme.INK_DEEP, 220));
			g.fillRect(x + 4, y + SLOT_H - 5, bw, 2);
			g.setColor(look == Look.CAN_BUY ? Theme.TEAL : Theme.progressColor(share));
			g.fillRect(x + 4, y + SLOT_H - 5, (int) Math.round(bw * share), 2);
		}
		else if (look == Look.LOCKED)
		{
			String lock = "diary";
			g.setColor(Theme.BLOCKED);
			g.drawString(lock, x + (SLOT_W - small.stringWidth(lock)) / 2, y + SLOT_H - 3);
		}
	}
}
