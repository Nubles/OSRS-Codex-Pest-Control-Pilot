package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Advice;
import com.codex.pestcontrol.core.Portal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * The island drawn as a small chart: the outpost walls and gates round the Void Knight, and the
 * four portals where they stand. Open portals glow, shielded ones sit behind a pale ring,
 * fallen ones go grey, and the one to work next wears a gold halo with the way there traced
 * from the knight through the right gate.
 */
public class OutpostMap extends JComponent
{
	private static final int HEIGHT = 178;
	private static final int R = 15;

	private final Timer pulse = new Timer(60, e -> repaint());
	private View.Fight fight;

	public OutpostMap()
	{
		setOpaque(false);
		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
	}

	public void setFight(View.Fight fight)
	{
		this.fight = fight;
		boolean animate = fight != null && fight.advice != null && fight.advice.getPortal() != null;
		if (animate && !pulse.isRunning())
		{
			pulse.start();
		}
		else if (!animate)
		{
			pulse.stop();
		}
		repaint();
	}

	@Override
	public void removeNotify()
	{
		pulse.stop();
		super.removeNotify();
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		setFight(fight);
	}

	private static Point2D.Float spot(Portal p, int w, int h)
	{
		switch (p)
		{
			case PURPLE:
				return new Point2D.Float(w * 0.13f, h * 0.36f);
			case BLUE:
				return new Point2D.Float(w * 0.87f, h * 0.36f);
			case YELLOW:
				return new Point2D.Float(w * 0.72f, h * 0.80f);
			default:
				return new Point2D.Float(w * 0.28f, h * 0.80f);
		}
	}

	/** The gate a portal is reached through, on the outpost wall. */
	private static Point2D.Float gate(Portal p, float left, float top, float right, float bottom)
	{
		float midY = (top + bottom) / 2;
		switch (p)
		{
			case PURPLE:
				return new Point2D.Float(left, midY);
			case BLUE:
				return new Point2D.Float(right, midY);
			default:
				return new Point2D.Float((left + right) / 2, bottom);
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		int w = getWidth();
		int h = getHeight();

		// The chart: a dark well with a faint sea-green wash, the island a warmer patch.
		RoundRectangle2D well = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, Theme.RADIUS, Theme.RADIUS);
		g.setPaint(new GradientPaint(0, 0, Theme.mix(Theme.INK_DEEP, Theme.TEAL, 0.06), 0, h, Theme.INK_DEEP));
		g.fill(well);
		g.setClip(well);
		g.setPaint(new RadialGradientPaint(w / 2f, h * 0.5f, w * 0.62f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(Theme.mix(Theme.CARD_TOP, Theme.GOLD_DIM, 0.25), 200), Theme.alpha(Theme.INK_DEEP, 0)}));
		g.fill(new Ellipse2D.Float(-w * 0.1f, h * 0.02f, w * 1.2f, h * 0.98f));
		g.setClip(null);
		g.setColor(Theme.STROKE_SOFT);
		g.draw(well);

		// The outpost walls with their three gates left open.
		float left = w * 0.33f;
		float right = w * 0.67f;
		float top = h * 0.14f;
		float bottom = h * 0.56f;
		float gap = 7;
		float midX = (left + right) / 2;
		float midY = (top + bottom) / 2;
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.mix(Theme.STROKE, Theme.GOLD_DIM, 0.35));
		Path2D wall = new Path2D.Float();
		wall.moveTo(left, midY - gap);
		wall.lineTo(left, top);
		wall.lineTo(right, top);
		wall.lineTo(right, midY - gap);
		wall.moveTo(right, midY + gap);
		wall.lineTo(right, bottom);
		wall.lineTo(midX + gap, bottom);
		wall.moveTo(midX - gap, bottom);
		wall.lineTo(left, bottom);
		wall.lineTo(left, midY + gap);
		g.draw(wall);

		View.Fight f = fight;
		Portal target = f == null || f.advice == null ? null : f.advice.getPortal();
		List<Portal> maybe = f == null || f.advice == null ? null : f.advice.getCandidates();
		boolean waiting = f != null && f.advice != null && f.advice.getKind() == Advice.Kind.WAIT;

		// The way there: from the knight, out through the gate, to the portal.
		Point2D.Float knight = new Point2D.Float(midX, midY);
		if (target != null)
		{
			Point2D.Float door = gate(target, left, top, right, bottom);
			Point2D.Float to = spot(target, w, h);
			Path2D path = new Path2D.Float();
			path.moveTo(knight.x, knight.y);
			path.lineTo(door.x, door.y);
			path.lineTo(to.x, to.y);
			// The dashes march outward: the phase runs down through one dash period.
			float phase = 10 - (System.currentTimeMillis() % 1200) / 1200f * 10;
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4, 6}, phase));
			g.setColor(Theme.alpha(Theme.GOLD, 190));
			g.draw(path);
		}

		// The Void Knight.
		g.setStroke(new BasicStroke(1f));
		paintKnight(g, knight.x, knight.y, f == null ? -1 : f.knightHealth);

		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		double glow = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 260.0);
		for (Portal p : Portal.values())
		{
			View.PortalState s = f == null ? new View.PortalState(p, true, false, -1) : f.portal(p);
			Point2D.Float c = spot(p, w, h);
			boolean isTarget = p == target;
			boolean isMaybe = waiting && maybe != null && maybe.contains(p);
			paintPortal(g, s, c.x, c.y, isTarget, isMaybe, glow);

			String below = s.fallen ? "Fallen" : s.health >= 0 ? String.valueOf(s.health) : s.shielded ? "Shielded" : "Open";
			Color belowColor = s.fallen ? Theme.TEXT_FAINT : s.shielded ? Theme.TEXT_MUTED : Theme.TEXT;
			Ui.shadowed(g, below, Math.round(c.x - fm.stringWidth(below) / 2f), Math.round(c.y + R + 4 + fm.getAscent()), belowColor);

			int order = f == null ? -1 : f.order.indexOf(p);
			if (order >= 0 && !s.fallen)
			{
				paintBadge(g, String.valueOf(order + 1), c.x + R * 0.72f, c.y - R * 0.72f, fm);
			}
			if (isTarget || isMaybe)
			{
				String tag = isTarget ? (f.advice.getKind() == Advice.Kind.ATTACK ? "ATTACK" : "NEXT") : "MAYBE";
				Color tagColor = isTarget ? Theme.GOLD_BRIGHT : Theme.TEXT_MUTED;
				Ui.shadowed(g, tag, Math.round(c.x - fm.stringWidth(tag) / 2f), Math.round(c.y - R - 5), tagColor);
			}
		}

		// The compass rose's north mark, so west and east read the right way round.
		g.setColor(Theme.TEXT_FAINT);
		g.drawString("N", w - 14, 14);
		g.fill(new Path2D.Float(new java.awt.Polygon(new int[]{w - 11, w - 14, w - 8}, new int[]{17, 22, 22}, 3)));
		g.dispose();
	}

	private static void paintKnight(Graphics2D g, float x, float y, int health)
	{
		float r = 8;
		g.setPaint(new RadialGradientPaint(x, y - 2, r * 1.6f, new float[]{0f, 1f},
			new Color[]{Theme.alpha(Theme.GOLD_BRIGHT, 90), Theme.alpha(Theme.GOLD, 0)}));
		g.fill(new Ellipse2D.Float(x - r * 1.6f, y - r * 1.6f, r * 3.2f, r * 3.2f));
		Sigil.paint(g, x - r, y - r, r * 2);
		if (health >= 0)
		{
			g.setFont(Theme.small());
			FontMetrics fm = g.getFontMetrics();
			String text = String.valueOf(health);
			Color color = health <= 60 ? Theme.BLOOD : health <= 120 ? Theme.EMBER : Theme.TEXT;
			Ui.shadowed(g, text, Math.round(x - fm.stringWidth(text) / 2f), Math.round(y + r + 2 + fm.getAscent()), color);
		}
	}

	private static void paintPortal(Graphics2D g, View.PortalState s, float x, float y, boolean target, boolean maybe, double glow)
	{
		Color colour = Theme.portal(s.portal);
		if (s.fallen)
		{
			colour = Theme.mix(colour, Theme.INK, 0.72);
		}
		else if (s.shielded)
		{
			colour = Theme.mix(colour, Theme.INK, 0.35);
		}

		if (target)
		{
			float halo = R + 7 + (float) glow * 2;
			g.setPaint(new RadialGradientPaint(x, y, halo, new float[]{0.55f, 1f},
				new Color[]{Theme.alpha(Theme.GOLD_BRIGHT, (int) (70 + 60 * glow)), Theme.alpha(Theme.GOLD, 0)}));
			g.fill(new Ellipse2D.Float(x - halo, y - halo, halo * 2, halo * 2));
		}
		else if (!s.fallen && !s.shielded)
		{
			float halo = R + 6;
			g.setPaint(new RadialGradientPaint(x, y, halo, new float[]{0.5f, 1f},
				new Color[]{Theme.alpha(colour, 110), Theme.alpha(colour, 0)}));
			g.fill(new Ellipse2D.Float(x - halo, y - halo, halo * 2, halo * 2));
		}

		// The rift itself: bright at its heart, the portal's colour at its rim.
		g.setPaint(new RadialGradientPaint(x - R * 0.2f, y - R * 0.25f, R, new float[]{0f, 0.6f, 1f},
			new Color[]{Theme.mix(colour, Color.WHITE, s.fallen ? 0.05 : 0.45), colour, Theme.mix(colour, Theme.INK_DEEP, 0.55)}));
		g.fill(new Ellipse2D.Float(x - R, y - R, R * 2, R * 2));
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(Theme.mix(colour, Theme.INK_DEEP, 0.3));
		g.draw(new Ellipse2D.Float(x - R, y - R, R * 2, R * 2));

		if (!s.fallen)
		{
			// A swirl inside, so it reads as a portal and not a gem.
			g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(Theme.alpha(Color.WHITE, s.shielded ? 60 : 130));
			Path2D swirl = new Path2D.Float();
			swirl.moveTo(x + R * 0.55f, y - R * 0.1f);
			swirl.curveTo(x + R * 0.5f, y - R * 0.6f, x - R * 0.35f, y - R * 0.6f, x - R * 0.45f, y);
			swirl.curveTo(x - R * 0.5f, y + R * 0.45f, x + R * 0.15f, y + R * 0.5f, x + R * 0.2f, y + R * 0.1f);
			g.draw(swirl);
		}

		if (s.shielded && !s.fallen)
		{
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{3, 3}, 0));
			g.setColor(Theme.alpha(Theme.TEXT, maybe ? 200 : 120));
			g.draw(new Ellipse2D.Float(x - R - 3, y - R - 3, (R + 3) * 2, (R + 3) * 2));
		}
		if (s.fallen)
		{
			g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(Theme.alpha(Theme.TEXT_FAINT, 200));
			float d = R * 0.45f;
			g.draw(new java.awt.geom.Line2D.Float(x - d, y - d, x + d, y + d));
			g.draw(new java.awt.geom.Line2D.Float(x - d, y + d, x + d, y - d));
		}
		if (target)
		{
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(Theme.GOLD_BRIGHT);
			g.draw(new Ellipse2D.Float(x - R - 3.5f, y - R - 3.5f, (R + 3.5f) * 2, (R + 3.5f) * 2));
		}
		g.setStroke(new BasicStroke(1f));
	}

	private static void paintBadge(Graphics2D g, String text, float x, float y, FontMetrics fm)
	{
		float d = 11;
		g.setColor(Theme.INK_DEEP);
		g.fill(new Ellipse2D.Float(x - d / 2, y - d / 2, d, d));
		g.setColor(Theme.alpha(Theme.GOLD, 200));
		g.draw(new Ellipse2D.Float(x - d / 2, y - d / 2, d, d));
		g.setColor(Theme.GOLD_BRIGHT);
		g.drawString(text, Math.round(x - fm.stringWidth(text) / 2f), Math.round(y + fm.getAscent() / 2f - 1));
	}
}
