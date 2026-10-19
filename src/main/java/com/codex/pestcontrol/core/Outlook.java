package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** How far the goal is: the points still needed, the games that takes and how long. */
public final class Outlook
{
	private final List<Reward> needed;
	private final List<Reward> owned;
	private final int cost;
	private final int points;
	private final int remaining;
	private final int games;
	private final double minutes;
	private final double perGame;
	private final boolean measured;
	private final Reward nextBuy;
	private final double minutesPerGame;

	private Outlook(List<Reward> needed, List<Reward> owned, int cost, int points, double perGame, double minutesPerGame, boolean measured)
	{
		this.needed = needed;
		this.owned = owned;
		this.cost = cost;
		this.points = points;
		this.perGame = perGame;
		this.measured = measured;
		this.remaining = Math.max(0, cost - Math.max(0, points));
		this.games = remaining == 0 ? 0 : (int) Math.ceil(remaining / perGame);
		this.minutesPerGame = minutesPerGame;
		this.minutes = games * minutesPerGame;
		Reward cheapest = null;
		for (Reward r : needed)
		{
			if (r.getCost() <= points && (cheapest == null || r.getCost() > cheapest.getCost()))
			{
				cheapest = r;
			}
		}
		this.nextBuy = cheapest;
	}

	/**
	 * @param points points held, or -1 when not yet seen
	 */
	public static Outlook of(Set<Reward> goal, Set<Reward> owned, int points, double perGame, double minutesPerGame, boolean measured)
	{
		List<Reward> needed = new ArrayList<>();
		List<Reward> have = new ArrayList<>();
		int cost = 0;
		for (Reward r : Reward.values())
		{
			if (!goal.contains(r))
			{
				continue;
			}
			if (owned.contains(r))
			{
				have.add(r);
			}
			else
			{
				needed.add(r);
				cost += r.getCost();
			}
		}
		return new Outlook(needed, have, cost, points, Math.max(0.5, perGame), minutesPerGame, measured);
	}

	public List<Reward> getNeeded()
	{
		return Collections.unmodifiableList(needed);
	}

	public List<Reward> getOwned()
	{
		return Collections.unmodifiableList(owned);
	}

	/** Points the pieces still needed cost. */
	public int getCost()
	{
		return cost;
	}

	public int getPoints()
	{
		return points;
	}

	public boolean isPointsKnown()
	{
		return points >= 0;
	}

	public int getRemaining()
	{
		return remaining;
	}

	public int getGames()
	{
		return games;
	}

	public double getMinutes()
	{
		return minutes;
	}

	public double getPerGame()
	{
		return perGame;
	}

	public boolean isMeasured()
	{
		return measured;
	}

	/** How much of the cost is already held, from 0 to 1. */
	public double progress()
	{
		if (cost == 0)
		{
			return 1;
		}
		return Math.min(1, Math.max(0, points) / (double) cost);
	}

	public boolean isComplete()
	{
		return needed.isEmpty();
	}

	public boolean isAffordable()
	{
		return !needed.isEmpty() && remaining == 0;
	}

	/** The dearest needed piece the points already cover, or null. */
	public Reward getNextBuy()
	{
		return nextBuy;
	}

	/** Whether the cost is more than the Void Knights will hold at once. */
	public boolean isOverCap()
	{
		return cost > Logbook.POINTS_CAP;
	}

	/** One piece on the way to the goal: the points it takes counting the pieces before it, and when. */
	public static final class Milestone
	{
		private final Reward reward;
		private final int total;
		private final int games;
		private final double minutes;

		Milestone(Reward reward, int total, int games, double minutes)
		{
			this.reward = reward;
			this.total = total;
			this.games = games;
			this.minutes = minutes;
		}

		public Reward getReward()
		{
			return reward;
		}

		/** Points spent once this piece and every one before it are bought. */
		public int getTotal()
		{
			return total;
		}

		/** Games until it can be bought, 0 when it can be already. */
		public int getGames()
		{
			return games;
		}

		public double getMinutes()
		{
			return minutes;
		}
	}

	/**
	 * The pieces still needed in the order to buy them. Whenever the points held cover a piece, the
	 * dearest one they cover comes next (the same piece the outlook names as buyable now), so the
	 * Void Knights hold as few points as they can; otherwise the cheapest, as it is yours soonest.
	 * Pieces in the given set (elite pieces still locked behind the diary) go last.
	 */
	public List<Milestone> milestones(Set<Reward> last)
	{
		List<Reward> open = new ArrayList<>();
		List<Reward> later = new ArrayList<>();
		for (Reward r : needed)
		{
			(last != null && last.contains(r) ? later : open).add(r);
		}
		// Stable sorts keep the reward order between pieces of the same cost.
		open.sort((a, b) -> Integer.compare(a.getCost(), b.getCost()));
		later.sort((a, b) -> Integer.compare(a.getCost(), b.getCost()));
		List<Reward> order = new ArrayList<>();
		int held = Math.max(0, points);
		int spent = 0;
		while (!open.isEmpty())
		{
			Reward next = open.get(0);
			for (Reward r : open)
			{
				if (r.getCost() <= held - spent && r.getCost() > next.getCost())
				{
					next = r;
				}
			}
			open.remove(next);
			order.add(next);
			spent += next.getCost();
		}
		order.addAll(later);
		List<Milestone> out = new ArrayList<>();
		int total = 0;
		for (Reward r : order)
		{
			total += r.getCost();
			int shortfall = Math.max(0, total - held);
			int g = shortfall == 0 ? 0 : (int) Math.ceil(shortfall / perGame);
			out.add(new Milestone(r, total, g, g * minutesPerGame));
		}
		return out;
	}
}
