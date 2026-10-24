package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	/** Until you have your points, an open portal with this many fewer players is worth going to. */
	private static final int QUIETER_BY = 6;
	/** Below this activity, waiting time is better spent on a barricade. */
	private static final int LOW_ACTIVITY = 30;
	/** The team's pace at a portal is measured over this many ticks, about ten seconds. */
	static final int PACE_WINDOW = 17;
	/** Ticks of readings needed before a pace is trusted. */
	static final int PACE_MIN = 6;
	/** A portal falling this many seconds later than the weakest holds the game up enough to go there instead. */
	private static final int LATER_BY = 12;
	/** Seconds shown for a portal the team is not wearing down at all. */
	public static final int STALLED = Integer.MAX_VALUE;

	private final boolean[] shielded = {true, true, true, true};
	private final boolean[] fallen = new boolean[4];
	private final int[] health = {-1, -1, -1, -1};
	private final List<Portal> drops = new ArrayList<>();
	private final List<Integer> dropTicks = new ArrayList<>();
	private List<Rotation> possible = new ArrayList<>(Arrays.asList(Rotation.values()));
	private int knightHealth = -1;
	private int damage;
	private int ticks;
	/** Each portal's health readings over the pace window: {tick, health}, oldest first. */
	private final List<List<int[]>> readings = new ArrayList<>();
	/** Your own hits on each portal over the pace window: {tick, damage}. */
	private final List<List<int[]>> ownHits = new ArrayList<>();
	/** The portal last sent to once points were earned, held unless another clearly holds the game up more. */
	private Portal holding;

	/** Whether the game was boarded from the lander, so seen from its start. */
	private boolean whole;

	public boolean isWhole()
	{
		return whole;
	}

	public void setWhole(boolean whole)
	{
		this.whole = whole;
	}

	public Battle()
	{
		for (int i = 0; i < 4; i++)
		{
			readings.add(new ArrayList<>());
			ownHits.add(new ArrayList<>());
		}
	}

	/** A portal's shield has dropped just now, by its message or by the portal changing. Repeats are ignored. */
	public void shieldDropped(Portal portal)
	{
		drop(portal, ticks);
	}

	/** A portal found already open, so the moment its shield dropped is not known. */
	public void foundOpen(Portal portal)
	{
		drop(portal, -1);
	}

	private void drop(Portal portal, int at)
	{
		if (portal == null || !shielded[portal.ordinal()])
		{
			return;
		}
		shielded[portal.ordinal()] = false;
		// Readings from behind the shield would make a portal the team has only just reached look stalled.
		readings.get(portal.ordinal()).clear();
		ownHits.get(portal.ordinal()).clear();
		int step = drops.size();
		drops.add(portal);
		dropTicks.add(at);
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
		List<int[]> r = readings.get(portal.ordinal());
		if (!r.isEmpty() && r.get(r.size() - 1)[0] == ticks)
		{
			r.get(r.size() - 1)[1] = hp;
		}
		else
		{
			r.add(new int[]{ticks, hp});
		}
		trim(r);
		if (hp == 0)
		{
			fall(portal);
		}
	}

	public void fall(Portal portal)
	{
		fallen[portal.ordinal()] = true;
		// A portal can only be killed with its shield down; when its drop was missed, its moment is unknown.
		foundOpen(portal);
	}

	public void setKnightHealth(int hp)
	{
		knightHealth = hp;
	}

	public void addDamage(int amount)
	{
		addDamage(amount, null);
	}

	/** One of your hitsplats; on a portal it is kept apart so the team's own pace there can be told from yours. */
	public void addDamage(int amount, Portal on)
	{
		if (amount <= 0)
		{
			return;
		}
		damage += amount;
		if (on != null)
		{
			List<int[]> hits = ownHits.get(on.ordinal());
			hits.add(new int[]{ticks, amount});
			trim(hits);
		}
	}

	private void trim(List<int[]> samples)
	{
		while (!samples.isEmpty() && samples.get(0)[0] < ticks - PACE_WINDOW)
		{
			samples.remove(0);
		}
	}

	/**
	 * Seconds until the open portal falls at the pace the rest of the team is wearing it down,
	 * leaving your own hits out; {@link #STALLED} when they are not wearing it down at all, or
	 * -1 when it is not open or has not been watched long enough to tell.
	 */
	public int secondsToFall(Portal portal)
	{
		int i = portal.ordinal();
		if (shielded[i] || fallen[i])
		{
			return -1;
		}
		List<int[]> r = readings.get(i);
		trim(r);
		if (r.size() < 2)
		{
			return -1;
		}
		int[] first = r.get(0);
		int[] last = r.get(r.size() - 1);
		int span = last[0] - first[0];
		if (span < PACE_MIN || last[1] <= 0)
		{
			return -1;
		}
		// A hit stamped with a tick lands after that tick's reading, so it shows from the next one on.
		int mine = 0;
		for (int[] hit : ownHits.get(i))
		{
			if (hit[0] >= first[0] && hit[0] < last[0])
			{
				mine += hit[1];
			}
		}
		double perTick = (first[1] - last[1] - mine) / (double) span;
		if (perTick <= 0.05)
		{
			return STALLED;
		}
		return (int) Math.ceil(last[1] / perTick * 0.6);
	}

	/**
	 * Whether you are the only one wearing the open portal down: your hits have landed on it lately
	 * but, leaving them out, its health is not falling. Its pace then reads as stalled, which is the
	 * team's pace, not yours.
	 */
	public boolean isYoursAlone(Portal portal)
	{
		List<int[]> hits = ownHits.get(portal.ordinal());
		trim(hits);
		return !hits.isEmpty() && secondsToFall(portal) == STALLED;
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

	/** The tick each shield dropped on, in drop order, -1 where it was not seen. */
	public List<Integer> getDropTicks()
	{
		return Collections.unmodifiableList(dropTicks);
	}

	/** Seconds until the next shield is due by the clock learned so far, or -1 when not known. */
	public int secondsToNextDrop(ShieldClock clock)
	{
		int step = drops.size();
		if (step >= 4 || clock == null)
		{
			return -1;
		}
		int gap = clock.expected(step);
		if (gap < 0)
		{
			return -1;
		}
		int since;
		if (step == 0)
		{
			since = ticks;
		}
		else
		{
			int last = dropTicks.get(step - 1);
			if (last < 0)
			{
				return -1;
			}
			since = ticks - last;
		}
		return (int) Math.max(0, Math.round((gap - since) * 0.6));
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

	/** What to do now, from the status bar and what can be seen around the island. */
	public Advice advise(Field field)
	{
		Field f = field == null ? new Field() : field;
		boolean knightLow = knightHealth >= 0 && f.knightWarn > 0 && knightHealth <= f.knightWarn;

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
			Portal weakest = weakest(open);
			Portal target = pick(open, weakest, f);
			boolean quieter = target != weakest && quieter(target, weakest, f);
			Portal slowest = hasPoints() ? holdingUp(open, target) : null;
			holding = slowest;
			if (slowest != null)
			{
				target = slowest;
				quieter = false;
			}
			String detail = slowest != null ? slowestDetail(slowest, f) : attackDetail(target, quieter, f);
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

		String when = f.nextDrop < 0 ? "next" : f.nextDrop <= 3 ? "any moment" : "in about " + f.nextDrop + "s";
		boolean idle = f.activity >= 0 && f.activity < LOW_ACTIVITY;
		List<Portal> next = nextShields();
		if (next.size() == 1)
		{
			Portal p = next.get(0);
			String detail = "Its shield drops " + when + ": leave by the " + p.getGate();
			if (f.style.isDistant())
			{
				detail += ". Hang back and hit it from range";
			}
			return new Advice(Advice.Kind.NEXT, p, next, "Head to " + p.getTitle() + " (" + p.getSide() + ")", detail);
		}
		if (next.isEmpty())
		{
			next = standing;
		}
		String detail = list(next) + " drops " + when;
		if (idle)
		{
			detail += ". Repair a barricade meanwhile to raise your activity";
		}
		return new Advice(Advice.Kind.WAIT, null, next, "Hold by the knight", detail);
	}

	/**
	 * Once your points are earned the game ends when the last portal falls, so the open portal
	 * the team is wearing down slowest is where your hits shorten it most. Returns it when it falls
	 * clearly later than the portal otherwise picked, else null. The last pick is held while it is
	 * still close to the slowest, so the advice does not flicker between two portals.
	 */
	private Portal holdingUp(List<Portal> open, Portal picked)
	{
		if (open.size() < 2)
		{
			return null;
		}
		Portal slowest = null;
		int slowestEta = -1;
		for (Portal p : open)
		{
			int eta = secondsToFall(p);
			if (eta < 0)
			{
				// Not every open portal is measured yet: no fair comparison.
				return null;
			}
			if (eta > slowestEta || eta == slowestEta && rank(health[p.ordinal()]) > rank(health[slowest.ordinal()]))
			{
				slowest = p;
				slowestEta = eta;
			}
		}
		if (holding != null && open.contains(holding) && holding != slowest)
		{
			int held = secondsToFall(holding);
			if (held == STALLED || slowestEta != STALLED && slowestEta - held < LATER_BY)
			{
				return holding;
			}
		}
		if (slowest == picked)
		{
			return slowestEta == STALLED || slowestEta >= LATER_BY ? slowest : null;
		}
		int pickedEta = secondsToFall(picked);
		boolean later = slowestEta == STALLED ? pickedEta != STALLED : slowestEta - pickedEta >= LATER_BY && slowestEta >= pickedEta * 3 / 2;
		return later ? slowest : null;
	}

	private String slowestDetail(Portal p, Field f)
	{
		int spinning = f.spinners.getOrDefault(p, 0);
		String lead;
		int eta = secondsToFall(p);
		if (eta == STALLED)
		{
			Integer crowd = f.crowdAt(p);
			if (isYoursAlone(p))
			{
				lead = "Only you are wearing it down: it holds the game up";
			}
			else
			{
				lead = crowd != null && crowd == 0 ? "Nobody is there: it holds the game up" : "The team is not wearing it down: it holds the game up";
			}
		}
		else
		{
			lead = "Falls last at the team's pace, in about " + eta + "s";
		}
		if (spinning > 0)
		{
			return lead + ". Kill the " + (spinning == 1 ? "spinner" : spinning + " spinners") + " healing it first";
		}
		return lead + ". Your points are earned, so your hits end the game sooner here";
	}

	/** What to say about the portal to attack: what stands in the way first, then why this one. */
	private String attackDetail(Portal target, boolean quieter, Field f)
	{
		int spinning = f.spinners.getOrDefault(target, 0);
		if (spinning > 0)
		{
			if (f.style.isDistant())
			{
				return spinning == 1 ? "A spinner is healing it: shoot it from range, then the portal"
					: spinning + " spinners are healing it: shoot them from range, then the portal";
			}
			return spinning == 1 ? "A spinner is healing it: kill it first" : spinning + " spinners are healing it: kill them first";
		}
		int brawling = f.brawlers.getOrDefault(target, 0);
		if (brawling > 0 && !f.style.isDistant())
		{
			return brawling == 1 ? "A brawler blocks the way: clear it first" : brawling + " brawlers block the way: clear them first";
		}
		Integer crowd = f.crowdAt(target);
		if (quieter && crowd != null)
		{
			return crowd == 0 ? "Nobody is there yet: easy damage for your points" : "Only " + players(crowd) + " there: easier damage for your points";
		}
		StringBuilder sb = new StringBuilder("Shield down");
		if (health[target.ordinal()] > 0)
		{
			sb.append(", ").append(health[target.ordinal()]).append(" health left");
		}
		else
		{
			sb.append(": leave by the ").append(target.getGate());
		}
		if (crowd != null && crowd > 0)
		{
			sb.append(", ").append(players(crowd));
		}
		return sb.toString();
	}

	private Portal weakest(List<Portal> open)
	{
		Portal weakest = open.get(0);
		for (Portal p : open)
		{
			if (rank(health[p.ordinal()]) < rank(health[weakest.ordinal()]))
			{
				weakest = p;
			}
		}
		return weakest;
	}

	/**
	 * The weakest open portal, unless you still need damage for points and another has far
	 * fewer players to share it with, or another is a good deal nearer.
	 */
	private Portal pick(List<Portal> open, Portal weakest, Field f)
	{
		if (!hasPoints())
		{
			Portal quietest = weakest;
			for (Portal p : open)
			{
				Integer c = f.crowd.get(p);
				Integer best = f.crowd.get(quietest);
				if (c != null && best != null && c < best)
				{
					quietest = p;
				}
			}
			if (quietest != weakest && quieter(quietest, weakest, f))
			{
				return quietest;
			}
		}
		Portal nearest = weakest;
		for (Portal p : open)
		{
			Integer d = f.distance.get(p);
			Integer best = f.distance.get(nearest);
			if (d != null && (best == null || d < best))
			{
				nearest = p;
			}
		}
		Integer dw = f.distance.get(weakest);
		Integer dn = f.distance.get(nearest);
		if (nearest != weakest && dn != null && (dw == null || dw - dn >= NEARER_BY))
		{
			return nearest;
		}
		return weakest;
	}

	private static boolean quieter(Portal a, Portal than, Field f)
	{
		Integer ca = f.crowd.get(a);
		Integer cb = f.crowd.get(than);
		return ca != null && cb != null && cb - ca >= QUIETER_BY;
	}

	/** "1 player", "12 players". */
	static String players(int n)
	{
		return n + (n == 1 ? " player" : " players");
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
