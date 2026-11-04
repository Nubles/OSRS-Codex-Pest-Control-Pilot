package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A finished game's grade against the ghost on its lander: S, A, B or C from its points an hour
 * (counting the lander wait) against the ghost's, and the one to three segments that lost the most
 * time, in plain words ("2nd portal down (Blue): 120 ticks, ghost took 60"). A lost game, or one
 * the activity bar ran out in, earns no points and is a C.
 */
public final class Grade
{
	public enum Letter
	{
		S, A, B, C
	}

	/** Shares of the ghost's rate each letter needs: S within 2% of it (or past it), A 90%, B 75%. */
	static final double S_SHARE = 0.98;
	static final double A_SHARE = 0.90;
	static final double B_SHARE = 0.75;
	/** Segments named at most. */
	public static final int SINKS = 3;
	/** Ticks over the ghost a segment must lose before it is named: three seconds. */
	public static final int SINK_MIN = 5;

	/** One segment where the game lost time to the ghost. */
	public static final class Sink
	{
		private final Ghost.Segment segment;
		private final int mine;
		private final int ghost;
		private final Portal portal;

		Sink(Ghost.Segment segment, int mine, int ghost, Portal portal)
		{
			this.segment = segment;
			this.mine = mine;
			this.ghost = ghost;
			this.portal = portal;
		}

		public Ghost.Segment getSegment()
		{
			return segment;
		}

		public int getMine()
		{
			return mine;
		}

		public int getGhost()
		{
			return ghost;
		}

		public int getLost()
		{
			return mine - ghost;
		}

		/** The sink in plain words: "2nd portal down (Blue): 120 ticks, ghost took 60". */
		public String sentence()
		{
			String took = mine + " ticks";
			switch (segment)
			{
				case BOAT:
					return "Boat: " + took + " waiting in the lander, ghost took " + ghost;
				case LANDING:
					return "Landing: " + took + " to the first shield, ghost took " + ghost;
				case END:
					return "Game end: " + took + " after the last portal, ghost took " + ghost;
				default:
					return segment.getTitle() + " down" + (portal == null ? "" : " (" + portal.getTitle() + ")") + ": " + took + ", ghost took " + ghost;
			}
		}
	}

	private final Letter letter;
	private final Boat boat;
	private final double rate;
	private final double ghostRate;
	private final int points;
	private final List<Sink> sinks;
	private final boolean newGhost;
	private final GameRecord.Result result;

	private Grade(Letter letter, Boat boat, double rate, double ghostRate, int points, List<Sink> sinks, boolean newGhost, GameRecord.Result result)
	{
		this.letter = letter;
		this.boat = boat;
		this.rate = rate;
		this.ghostRate = ghostRate;
		this.points = points;
		this.sinks = sinks;
		this.newGhost = newGhost;
		this.result = result;
	}

	/**
	 * @param game the finished game, with its timeline
	 * @param ghost the ghost on its lander as it was before this game, or null
	 * @return the grade, or null for a game left early, which is not graded
	 */
	public static Grade of(GameRecord game, GameRecord ghost)
	{
		if (game == null || game.getResult() == GameRecord.Result.LEFT)
		{
			return null;
		}
		double rate = game.getResult() == GameRecord.Result.WON ? Ghost.rate(game) : 0;
		boolean replaces = Ghost.beats(game, ghost);
		if (ghost == null)
		{
			// The first game on a lander has nothing to be measured against: a win sets the ghost,
			// anything else waits for one.
			return new Grade(null, game.getBoat(), rate, 0, game.getPoints(), Collections.emptyList(), replaces, game.getResult());
		}
		double ghostRate = Ghost.rate(ghost);
		double share = ghostRate <= 0 ? 0 : rate / ghostRate;
		Letter letter = rate <= 0 ? Letter.C : share >= S_SHARE ? Letter.S : share >= A_SHARE ? Letter.A : share >= B_SHARE ? Letter.B : Letter.C;
		return new Grade(letter, game.getBoat(), rate, ghostRate, game.getPoints(), sinks(game, ghost), replaces, game.getResult());
	}

	/** The segments that lost the most time to the ghost, worst first. */
	static List<Sink> sinks(GameRecord game, GameRecord ghost)
	{
		List<Sink> all = new ArrayList<>();
		for (Ghost.Segment s : Ghost.Segment.values())
		{
			int mine = Ghost.ticks(game, s);
			int theirs = Ghost.ticks(ghost, s);
			if (mine >= 0 && theirs >= 0 && mine - theirs >= SINK_MIN)
			{
				all.add(new Sink(s, mine, theirs, s.kill() > 0 ? game.killed(s.kill()) : null));
			}
		}
		all.sort((x, y) -> x.getLost() != y.getLost() ? Integer.compare(y.getLost(), x.getLost()) : Integer.compare(x.segment.ordinal(), y.segment.ordinal()));
		return all.size() > SINKS ? new ArrayList<>(all.subList(0, SINKS)) : all;
	}

	/** The letter, or null for the first graded game on a lander. */
	public Letter getLetter()
	{
		return letter;
	}

	public Boat getBoat()
	{
		return boat;
	}

	/** Points an hour, counting the lander wait. */
	public double getRate()
	{
		return rate;
	}

	public double getGhostRate()
	{
		return ghostRate;
	}

	public int getPoints()
	{
		return points;
	}

	public List<Sink> getSinks()
	{
		return Collections.unmodifiableList(sinks);
	}

	public boolean isNewGhost()
	{
		return newGhost;
	}

	public GameRecord.Result getResult()
	{
		return result;
	}

	/** The game's rate as a share of the ghost's, or 0 with no ghost. */
	public double share()
	{
		return ghostRate <= 0 ? 0 : rate / ghostRate;
	}

	/** The lander's name for the card, such as "novice". */
	public String boatName()
	{
		return boat == null ? "lander" : boat.getTitle().toLowerCase(Locale.ENGLISH);
	}
}
