package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
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
		// Unread points are not zero points, so the games are only counted once they are read.
		this.games = points < 0 && !needed.isEmpty() ? -1 : remaining == 0 ? 0 : (int) Math.ceil(remaining / perGame);
		this.minutesPerGame = minutesPerGame;
		this.minutes = games < 0 ? -1 : games * minutesPerGame;
		List<Reward> best = bestSet(needed, Math.max(0, points));
		this.nextBuy = best.isEmpty() ? null : best.get(0);
	}

	/**
	 * The pieces that together spend the most of the points held, dearest first. Ties go to fewer
	 * pieces, then to the pieces earlier in the reward list. A goal has at most ten pieces, so every
	 * set is tried.
	 */
	static List<Reward> bestSet(List<Reward> pieces, int held)
	{
		List<Reward> sorted = new ArrayList<>(pieces);
		// Stable: pieces of the same cost keep the reward order.
		sorted.sort((a, b) -> Integer.compare(b.getCost(), a.getCost()));
		int n = Math.min(sorted.size(), 16);
		int bestMask = 0;
		int bestSum = 0;
		int bestCount = 0;
		for (int mask = 1; mask < 1 << n; mask++)
		{
			int sum = 0;
			for (int i = 0; i < n && sum <= held; i++)
			{
				if ((mask & 1 << i) != 0)
				{
					sum += sorted.get(i).getCost();
				}
			}
			if (sum > held || sum < bestSum)
			{
				continue;
			}
			int count = Integer.bitCount(mask);
			if (sum > bestSum || count < bestCount || count == bestCount && earlier(mask, bestMask))
			{
				bestMask = mask;
				bestSum = sum;
				bestCount = count;
			}
		}
		List<Reward> out = new ArrayList<>();
		for (int i = 0; i < n; i++)
		{
			if ((bestMask & 1 << i) != 0)
			{
				out.add(sorted.get(i));
			}
		}
		return out;
	}

	/** Whether set a holds the first piece, in order, that the two sets do not share. */
	private static boolean earlier(int a, int b)
	{
		int diff = a ^ b;
		return diff != 0 && (a & Integer.lowestOneBit(diff)) != 0;
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

	/** Whether the games left can be counted: not before the points held are read. */
	public boolean isGamesKnown()
	{
		return games >= 0;
	}

	public int getRemaining()
	{
		return remaining;
	}

	/** Games until the points cover the whole goal: 0 when they do, -1 before the points are read. */
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

	/** The dearest piece of the set the points buy together, locked or not, or null. */
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

		/** Games until it can be bought, 0 when it can be already, -1 before the points are read. */
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
	 * The pieces still needed in the order to buy them. First the pieces the points held buy
	 * together, choosing the set that spends the most of them, so the Void Knights hold as few
	 * points as they can: 400 points toward a top and two helms buy both helms, not the top and
	 * 150 left over. The rest follow cheapest first, as each is yours soonest. Pieces in the given
	 * set (elite pieces still locked behind the diary) go last. Before the points are read no
	 * piece is timed: each one's games are -1.
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
		int held = Math.max(0, points);
		List<Reward> order = new ArrayList<>(bestSet(open, held));
		open.removeAll(order);
		// Nothing left fits in what the set leaves, or the set would have taken it.
		order.addAll(open);
		order.addAll(later);
		List<Milestone> out = new ArrayList<>();
		int total = 0;
		for (Reward r : order)
		{
			total += r.getCost();
			int shortfall = Math.max(0, total - held);
			int g = !isPointsKnown() ? -1 : shortfall == 0 ? 0 : (int) Math.ceil(shortfall / perGame);
			out.add(new Milestone(r, total, g, g < 0 ? -1 : g * minutesPerGame));
		}
		return out;
	}

	/**
	 * The pieces the points held buy now, together: the set that spends the most of them, which
	 * the buy order starts with. With 412 points toward a top, a robe and a helm, that is the top
	 * alone, not each of the three that 412 would cover on its own; with 400 toward a top and two
	 * helms, it is the two helms. Pieces in {@code locked} (elite ones behind the diary) are never
	 * among them; unknown points buy nothing.
	 */
	public Set<Reward> buyableNow(Set<Reward> locked)
	{
		Set<Reward> out = EnumSet.noneOf(Reward.class);
		if (!isPointsKnown())
		{
			return out;
		}
		for (Milestone m : milestones(locked))
		{
			if (m.getGames() > 0)
			{
				break;
			}
			if (locked == null || !locked.contains(m.getReward()))
			{
				out.add(m.getReward());
			}
		}
		return out;
	}
}
