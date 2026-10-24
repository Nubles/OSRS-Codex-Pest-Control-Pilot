package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the Pilot keeps for one account: the points last seen, the goal, which rewards
 * the account already owns, and its games. Saved as one Gson blob per RuneScape profile.
 */
public class Logbook
{
	/** Games kept one by one; older ones live on only in the totals. */
	static final int KEEP = 60;
	/**
	 * A save at least this long is far bigger than any the pilot writes (60 games come to about
	 * 6,000 characters) and takes long enough to read that it is read off the client thread.
	 */
	public static final int LARGE = 100_000;
	/** Games the points-per-game average is taken over. */
	static final int RECENT = 20;
	/** A gap longer than this between two games is a break, not lander time. */
	static final long BREAK_MS = 15 * 60_000L;
	/** Lander wait assumed before any has been measured, in seconds. */
	static final int LANDER_SECONDS = 60;
	/** The most points the Void Knights will hold for you. */
	public static final int POINTS_CAP = 4000;

	private int points = -1;
	private long pointsSeen;
	private List<String> goal;
	private List<String> owned;
	private long ownedSeen;
	private List<GameRecord> games;
	private int played;
	private int won;
	private int earned;
	private long seconds;
	private ShieldClock clock;
	private Bests bests;

	public Logbook()
	{
		goal = names(Goal.MELEE.getRewards());
		owned = new ArrayList<>();
		games = new ArrayList<>();
		clock = new ShieldClock();
		bests = new Bests();
	}

	/** How long shields take to drop, learned from your games. */
	public ShieldClock getClock()
	{
		if (clock == null)
		{
			clock = new ShieldClock();
		}
		return clock;
	}

	/**
	 * Reads the logbook saved for an account, or a fresh one when there is none. A damaged save
	 * keeps all it can: a field or game that cannot be read is lost alone.
	 */
	public static Logbook read(com.google.gson.Gson gson, String saved)
	{
		Logbook read = Salvage.read(gson, saved, Logbook.class);
		return read == null ? new Logbook() : read.repair();
	}

	/** Fills in anything an older or damaged save left out, and brings back figures out of range. */
	public Logbook repair()
	{
		if (goal == null)
		{
			goal = names(Goal.MELEE.getRewards());
		}
		goal.removeIf(n -> n == null);
		if (owned == null)
		{
			owned = new ArrayList<>();
		}
		owned.removeIf(n -> n == null);
		if (games == null)
		{
			games = new ArrayList<>();
		}
		games.removeIf(g -> g == null);
		games.forEach(GameRecord::repair);
		while (games.size() > KEEP)
		{
			games.remove(0);
		}
		if (clock == null)
		{
			clock = new ShieldClock();
		}
		clock.repair();
		// A save from before bests were kept starts from the games it holds.
		bests = bests == null || bests.isEmpty() ? Bests.of(games) : bests.repair(games);
		// The knights hold at most the cap; below zero is "not read yet".
		points = points < 0 ? -1 : Math.min(points, POINTS_CAP);
		pointsSeen = Math.max(0, pointsSeen);
		ownedSeen = Math.max(0, ownedSeen);
		played = Math.max(0, played);
		won = Math.max(0, Math.min(won, played));
		earned = Math.max(0, earned);
		seconds = Math.max(0, seconds);
		return this;
	}

	/** @return whether the number changed */
	public boolean seePoints(int value, long now)
	{
		if (value < 0)
		{
			return false;
		}
		boolean changed = value != points;
		points = value;
		pointsSeen = now;
		return changed;
	}

	/** Points last seen, or -1 before the first lander or reward shop. */
	public int getPoints()
	{
		return points;
	}

	public long getPointsSeen()
	{
		return pointsSeen;
	}

	public Set<Reward> getGoal()
	{
		return set(goal);
	}

	public void setGoal(Set<Reward> rewards)
	{
		goal = names(rewards);
	}

	public void toggle(Reward reward)
	{
		Set<Reward> g = getGoal();
		if (!g.remove(reward))
		{
			g.add(reward);
		}
		setGoal(g);
	}

	public Set<Reward> getOwned()
	{
		return set(owned);
	}

	/** @return whether what is owned changed */
	public boolean seeOwned(Set<Reward> rewards, long now)
	{
		Set<Reward> before = getOwned();
		owned = names(rewards);
		ownedSeen = now;
		return !before.equals(rewards);
	}

	/** When the bank was last looked through for rewards, or 0 if never. */
	public long getOwnedSeen()
	{
		return ownedSeen;
	}

	/** Your best games; see {@link Bests}. */
	public Bests getBests()
	{
		if (bests == null)
		{
			bests = Bests.of(games);
		}
		return bests;
	}

	/** @return the bests the game beat */
	public Set<Bests.Kind> record(GameRecord game)
	{
		return record(game, true);
	}

	/**
	 * @param whole whether the game was seen from its start
	 * @return the bests the game beat
	 */
	public Set<Bests.Kind> record(GameRecord game, boolean whole)
	{
		Set<Bests.Kind> beat = getBests().add(game, whole);
		games.add(game);
		while (games.size() > KEEP)
		{
			games.remove(0);
		}
		if (game.getResult() == GameRecord.Result.LEFT)
		{
			return beat;
		}
		played++;
		if (game.getResult() != GameRecord.Result.LOST)
		{
			won++;
		}
		earned += game.getPoints();
		seconds += game.getSeconds();
		return beat;
	}

	public List<GameRecord> getGames()
	{
		return Collections.unmodifiableList(games);
	}

	/** Finished games, not counting ones left early. */
	public int getPlayed()
	{
		return played;
	}

	public int getWon()
	{
		return won;
	}

	public int getEarned()
	{
		return earned;
	}

	public long getSeconds()
	{
		return seconds;
	}

	/** The finished games among the most recent ones, newest last. */
	List<GameRecord> recentFinished()
	{
		List<GameRecord> out = new ArrayList<>();
		for (int i = games.size() - 1; i >= 0 && out.size() < RECENT; i--)
		{
			GameRecord g = games.get(i);
			if (g.getResult() != GameRecord.Result.LEFT)
			{
				out.add(0, g);
			}
		}
		return out;
	}

	/**
	 * Points a game is worth to you, lost games included: measured once three games are in,
	 * else what a won game on the given lander gives.
	 */
	public double pointsPerGame(Boat fallback)
	{
		List<GameRecord> recent = recentFinished();
		if (recent.size() < 3)
		{
			return fallback.getPoints();
		}
		double sum = 0;
		for (GameRecord g : recent)
		{
			sum += g.getPoints();
		}
		return Math.max(0.5, sum / recent.size());
	}

	/** Whether the points per game are measured rather than assumed. */
	public boolean isMeasured()
	{
		return recentFinished().size() >= 3;
	}

	/**
	 * Minutes from one game to the next, lander wait included: measured from games played back
	 * to back, else the average game plus a minute in the lander, else five minutes.
	 */
	public double minutesPerGame()
	{
		List<GameRecord> recent = recentFinished();
		double gaps = 0;
		int n = 0;
		for (int i = 1; i < recent.size(); i++)
		{
			long gap = recent.get(i).getEnded() - recent.get(i - 1).getEnded();
			if (gap > 0 && gap < BREAK_MS)
			{
				gaps += gap;
				n++;
			}
		}
		if (n >= 2)
		{
			return gaps / n / 60_000.0;
		}
		if (!recent.isEmpty())
		{
			double s = 0;
			for (GameRecord g : recent)
			{
				s += g.getSeconds();
			}
			return (s / recent.size() + LANDER_SECONDS) / 60.0;
		}
		return 5;
	}

	private static List<String> names(Iterable<Reward> rewards)
	{
		List<String> out = new ArrayList<>();
		for (Reward r : rewards)
		{
			out.add(r.name());
		}
		return out;
	}

	private static Set<Reward> set(List<String> names)
	{
		Set<Reward> out = EnumSet.noneOf(Reward.class);
		if (names != null)
		{
			for (String n : names)
			{
				Reward r = Reward.named(n);
				if (r != null)
				{
					out.add(r);
				}
			}
		}
		return out;
	}
}
