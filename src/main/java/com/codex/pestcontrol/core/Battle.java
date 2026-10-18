package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One Pest Control game as the Pilot follows it: which shields are down, which portals have
 * fallen, the rotations still possible, the knight's health and the damage you have dealt.
 * It works out what to do next; the plugin only feeds it what the game shows.
 */
public class Battle
{
	/** Damage a player has to deal in a game to be given points for it. */
	public static final int DAMAGE_FOR_POINTS = 50;
	/** A portal this many tiles nearer than the weakest open one is worth going to first. */
	private static final int NEARER_BY = 15;

	private final boolean[] shielded = {true, true, true, true};
	private final boolean[] fallen = new boolean[4];
	private final int[] health = {-1, -1, -1, -1};
	private final List<Portal> drops = new ArrayList<>();
	private List<Rotation> possible = new ArrayList<>(Arrays.asList(Rotation.values()));
	private int knightHealth = -1;
	private int damage;
	private int ticks;

	/** A portal's shield has dropped, by its message or by the portal changing. Repeats are ignored. */
	public void shieldDropped(Portal portal)
	{
		if (portal == null || !shielded[portal.ordinal()])
		{
			return;
		}
		shielded[portal.ordinal()] = false;
		int step = drops.size();
		drops.add(portal);
		List<Rotation> left = new ArrayList<>();
		for (Rotation r : possible)
		{
			if (r.at(step) == portal)
			{
				left.add(r);
			}
		}
		// A drop no rotation explains means a missed message; keep reading ahead from what is known.
		if (!left.isEmpty())
		{
			possible = left;
		}
	}

	/** The portal's health as the status widget shows it; zero means it has fallen. */
	public void setHealth(Portal portal, int hp)
	{
		health[portal.ordinal()] = hp;
		if (hp == 0)
		{
			fall(portal);
		}
	}

	public void fall(Portal portal)
	{
		fallen[portal.ordinal()] = true;
		// A portal can only be killed with its shield down.
		shieldDropped(portal);
	}

	public void setKnightHealth(int hp)
	{
		knightHealth = hp;
	}

	public void addDamage(int amount)
	{
		if (amount > 0)
		{
			damage += amount;
		}
	}

	public void tick()
	{
		ticks++;
	}

	public boolean isShielded(Portal portal)
	{
		return shielded[portal.ordinal()];
	}

	public boolean isFallen(Portal portal)
	{
		return fallen[portal.ordinal()];
	}

	/** The portal's health, or -1 before the status widget has been read. */
	public int getHealth(Portal portal)
	{
		return health[portal.ordinal()];
	}

	public int getKnightHealth()
	{
		return knightHealth;
	}

	public int getDamage()
	{
		return damage;
	}

	public int getTicks()
	{
		return ticks;
	}

	public List<Portal> getDrops()
	{
		return Collections.unmodifiableList(drops);
	}

	public List<Rotation> getPossible()
	{
		return Collections.unmodifiableList(possible);
	}

	/** The order of the game once it is known, else null. */
	public Rotation getRotation()
	{
		return possible.size() == 1 ? possible.get(0) : null;
	}

	/** Portals whose shield could drop next, standing ones only. */
	public List<Portal> nextShields()
	{
		List<Portal> next = new ArrayList<>();
		for (Rotation r : possible)
		{
			for (int step = drops.size(); step < 4; step++)
			{
				Portal p = r.at(step);
				if (p != null && shielded[p.ordinal()])
				{
					if (!next.contains(p))
					{
						next.add(p);
					}
					break;
				}
			}
		}
		return next;
	}

	/** The portals in the order their shields drop, as far as it is known: dropped ones first. */
	public List<Portal> expectedOrder()
	{
		List<Portal> order = new ArrayList<>(drops);
		Rotation r = getRotation();
		if (r != null)
		{
			for (Portal p : r.order())
			{
				if (!order.contains(p))
				{
					order.add(p);
				}
			}
		}
		return order;
	}

	public boolean hasPoints()
	{
		return damage >= DAMAGE_FOR_POINTS;
	}

	/**
	 * What to do now.
	 *
	 * @param distance tiles from you to each portal you can see, missing when not known
	 * @param spinners spinners near each portal, healing it
	 * @param knightWarn the knight's health at or below which defending him comes first
	 */
	public Advice advise(Map<Portal, Integer> distance, Map<Portal, Integer> spinners, int knightWarn)
	{
		Map<Portal, Integer> far = distance == null ? new EnumMap<>(Portal.class) : distance;
		Map<Portal, Integer> heal = spinners == null ? new EnumMap<>(Portal.class) : spinners;
		boolean knightLow = knightHealth >= 0 && knightWarn > 0 && knightHealth <= knightWarn;

		List<Portal> open = new ArrayList<>();
		List<Portal> standing = new ArrayList<>();
		for (Portal p : Portal.values())
		{
			if (!fallen[p.ordinal()])
			{
				standing.add(p);
				if (!shielded[p.ordinal()])
				{
					open.add(p);
				}
			}
		}
		if (standing.isEmpty())
		{
			return new Advice(Advice.Kind.DONE, null, null, "Every portal has fallen", "The game ends as the last pests fall");
		}

		if (!open.isEmpty())
		{
			Portal target = pick(open, far);
			String detail;
			int spinning = heal.getOrDefault(target, 0);
			if (spinning > 0)
			{
				detail = spinning == 1 ? "A spinner is healing it: kill it first" : spinning + " spinners are healing it: kill them first";
			}
			else if (health[target.ordinal()] > 0)
			{
				detail = "Shield down, " + health[target.ordinal()] + " health left";
			}
			else
			{
				detail = "Shield down: leave by the " + target.getGate();
			}
			if (knightLow)
			{
				detail = "Knight at " + knightHealth + " health. " + detail;
			}
			return new Advice(Advice.Kind.ATTACK, target, open, "Attack " + target.getTitle() + " (" + target.getSide() + ")", detail);
		}

		if (knightLow)
		{
			return new Advice(Advice.Kind.GUARD, null, null, "Defend the Void Knight",
				"He is at " + knightHealth + " health and no shield is down: clear the pests around him");
		}

		List<Portal> next = nextShields();
		if (next.size() == 1)
		{
			Portal p = next.get(0);
			return new Advice(Advice.Kind.NEXT, p, next, "Head to " + p.getTitle() + " (" + p.getSide() + ")",
				"Its shield drops next: leave by the " + p.getGate());
		}
		if (next.isEmpty())
		{
			next = standing;
		}
		return new Advice(Advice.Kind.WAIT, null, next, "Hold by the knight", list(next) + " drops next");
	}

	/** The weakest open portal, unless another is a good deal nearer. */
	private Portal pick(List<Portal> open, Map<Portal, Integer> far)
	{
		Portal weakest = open.get(0);
		for (Portal p : open)
		{
			if (rank(health[p.ordinal()]) < rank(health[weakest.ordinal()]))
			{
				weakest = p;
			}
		}
		Portal nearest = weakest;
		for (Portal p : open)
		{
			Integer d = far.get(p);
			Integer best = far.get(nearest);
			if (d != null && (best == null || d < best))
			{
				nearest = p;
			}
		}
		Integer dw = far.get(weakest);
		Integer dn = far.get(nearest);
		if (nearest != weakest && dn != null && (dw == null || dw - dn >= NEARER_BY))
		{
			return nearest;
		}
		return weakest;
	}

	private static int rank(int hp)
	{
		return hp < 0 ? Integer.MAX_VALUE : hp;
	}

	/** "Purple", "Purple or Blue", "Purple, Blue or Yellow". */
	static String list(List<Portal> portals)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < portals.size(); i++)
		{
			if (i > 0)
			{
				sb.append(i == portals.size() - 1 ? " or " : ", ");
			}
			sb.append(portals.get(i).getTitle());
		}
		return sb.toString();
	}
}
