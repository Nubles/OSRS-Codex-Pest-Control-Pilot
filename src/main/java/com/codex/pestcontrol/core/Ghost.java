package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost splits: a game cut into its natural segments, timed in game ticks, against your own best
 * game on the same lander. The segments follow the game: the wait in the lander, the landing until
 * the first shield drops, each portal falling (counted in the order they fall, from the moment
 * the one before fell or the first shield dropped), and the end after the last portal.
 *
 * <p>The ghost is the won game, boarded from the lander, that earned the most points an hour
 * counting its lander wait. Only a game seen from boarding to the end can be one.
 */
public final class Ghost
{
	/** The segments of a game, in order. */
	public enum Segment
	{
		BOAT("Boat"),
		LANDING("Landing"),
		FIRST("1st portal"),
		SECOND("2nd portal"),
		THIRD("3rd portal"),
		FOURTH("4th portal"),
		END("Game end");

		private final String title;

		Segment(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}

		/** For a portal segment, which portal in the order they fall (1 to 4); 0 otherwise. */
		public int kill()
		{
			return this.ordinal() >= FIRST.ordinal() && this.ordinal() <= FOURTH.ordinal() ? this.ordinal() - 1 : 0;
		}
	}

	/** Game ticks in an hour (a tick is 0.6 seconds). */
	static final double TICKS_PER_HOUR = 6000;

	private Ghost()
	{
	}

	/** The game so far, or whole, with its timeline: the marks are read off the battle. */
	public static GameRecord record(Battle b, long ended, Boat boat, GameRecord.Result result, int points)
	{
		int[] marks = new int[5];
		List<Integer> drops = b.getDropTicks();
		marks[0] = drops.isEmpty() || drops.get(0) == null ? -1 : drops.get(0);
		List<Portal> falls = b.getFalls();
		String[] kills = new String[falls.size()];
		for (int i = 0; i < 4; i++)
		{
			marks[i + 1] = i < falls.size() ? b.getFallTick(falls.get(i)) : -1;
		}
		for (int i = 0; i < falls.size(); i++)
		{
			kills[i] = falls.get(i).name();
		}
		return new GameRecord(ended, boat, result, points, b.getDamage(), (int) Math.round(b.getTicks() * 0.6))
			.splits(b.getWait(), b.getTicks(), marks, kills);
	}

	/** Whether a game can be a ghost: won with points, seen from boarding, every portal's fall timed. */
	public static boolean usable(GameRecord g)
	{
		if (g == null || g.getResult() != GameRecord.Result.WON || g.getPoints() <= 0 || g.getWait() < 0 || g.getTicks() <= 0
			|| g.getBoat() == null)
		{
			return false;
		}
		int last = -1;
		for (int i = 0; i < 5; i++)
		{
			int m = g.mark(i);
			if (m < 0 || m < last)
			{
				return false;
			}
			last = m;
		}
		return last <= g.getTicks();
	}

	/** Points an hour, counting the lander wait; 0 when the wait or length is not known. */
	public static double rate(GameRecord g)
	{
		if (g == null || g.getTicks() <= 0)
		{
			return 0;
		}
		double ticks = g.getTicks() + Math.max(0, g.getWait());
		return g.getPoints() * TICKS_PER_HOUR / ticks;
	}

	/** Whether the game beats the ghost and should take its place. */
	public static boolean beats(GameRecord g, GameRecord ghost)
	{
		return usable(g) && (ghost == null || rate(g) > rate(ghost));
	}

	/** The key a ghost is kept under: the lander. */
	public static String key(Boat boat)
	{
		return boat == null ? null : boat.name();
	}

	/**
	 * Ticks a segment took, or -1 when not known. For a game under way, a segment still running is
	 * not counted here; see {@link #running}.
	 */
	public static int ticks(GameRecord g, Segment s)
	{
		if (g == null)
		{
			return -1;
		}
		switch (s)
		{
			case BOAT:
				return g.getWait();
			case LANDING:
				return g.mark(0);
			case END:
			{
				// Only after the last portal: a game lost with portals standing has no end segment to race.
				int last = g.mark(4);
				return last < 0 || g.getTicks() < last ? -1 : g.getTicks() - last;
			}
			default:
			{
				int k = s.kill();
				int from = g.mark(k - 1);
				int to = g.mark(k);
				return from < 0 || to < from ? -1 : to - from;
			}
		}
	}

	/** Ticks into the game at the latest mark seen, or -1 with none. */
	static int lastMark(GameRecord g)
	{
		for (int i = 4; i >= 0; i--)
		{
			if (g.mark(i) >= 0)
			{
				return g.mark(i);
			}
		}
		return -1;
	}

	/** The segment a game under way is in: the first one whose mark has not come. */
	public static Segment running(GameRecord g)
	{
		for (int i = 0; i < 5; i++)
		{
			if (g.mark(i) < 0)
			{
				return Segment.values()[i + 1];
			}
		}
		return Segment.END;
	}

	/** One line of the split table: a segment, this game against the ghost. */
	public static final class Row
	{
		private final Segment segment;
		private final int mine;
		private final int ghost;
		private final boolean live;
		private final Portal portal;

		Row(Segment segment, int mine, int ghost, boolean live, Portal portal)
		{
			this.segment = segment;
			this.mine = mine;
			this.ghost = ghost;
			this.live = live;
			this.portal = portal;
		}

		public Segment getSegment()
		{
			return segment;
		}

		/** Ticks this game took, so far for the segment under way; -1 when not known or not reached. */
		public int getMine()
		{
			return mine;
		}

		public int getGhost()
		{
			return ghost;
		}

		public boolean isLive()
		{
			return live;
		}

		/** For a portal segment, the portal that fell in this game, or null. */
		public Portal getPortal()
		{
			return portal;
		}

		/** Ticks over (positive) or under the ghost; null when unknown, or under way and still inside the ghost's time. */
		public Integer getDelta()
		{
			if (mine < 0 || ghost < 0)
			{
				return null;
			}
			int d = mine - ghost;
			return live && d <= 0 ? null : d;
		}
	}

	/**
	 * The split table: every segment, this game against the ghost.
	 *
	 * @param live whether the game is under way, so its running segment is timed to now
	 */
	public static List<Row> rows(GameRecord g, GameRecord ghost, boolean live)
	{
		List<Row> rows = new ArrayList<>();
		if (g == null)
		{
			return rows;
		}
		Segment running = live ? running(g) : null;
		for (Segment s : Segment.values())
		{
			int mine = ticks(g, s);
			boolean isLive = s == running;
			if (isLive)
			{
				int from = s == Segment.LANDING ? 0 : s == Segment.END ? lastMark(g) : g.mark(s.kill() - 1);
				mine = from < 0 ? -1 : g.getTicks() - from;
			}
			else if (live && running != null && s.ordinal() > running.ordinal())
			{
				mine = -1;
			}
			Portal portal = s.kill() > 0 ? g.killed(s.kill()) : null;
			rows.add(new Row(s, mine, ticks(ghost, s), isLive, portal));
		}
		return rows;
	}

	/**
	 * Ticks behind (positive) or ahead of the ghost at the latest mark both games have, or null
	 * when there is none.
	 */
	public static Integer behind(GameRecord g, GameRecord ghost)
	{
		if (g == null || ghost == null)
		{
			return null;
		}
		for (int i = 4; i >= 0; i--)
		{
			if (g.mark(i) >= 0 && ghost.mark(i) >= 0)
			{
				return g.mark(i) - ghost.mark(i);
			}
		}
		return null;
	}
}
