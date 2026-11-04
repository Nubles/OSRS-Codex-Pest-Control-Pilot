package com.codex.pestcontrol.core;

/** One finished game, as kept in the logbook. Fields are plain so Gson can store it. */
public class GameRecord
{
	public enum Result
	{
		WON,
		LOST,
		/** Won, but no points: the activity bar ran out during the game. */
		NO_POINTS,
		/** Left or logged out before the end. */
		LEFT
	}

	/** The most points one game gives: a won veteran game, with room to spare. */
	public static final int MAX_POINTS = 100;
	/** No game lasts longer than its twenty minutes. */
	static final int MAX_SECONDS = 20 * 60;

	private long ended;
	private String boat;
	private String result;
	private int points;
	private int damage;
	private int seconds;
	/** Ticks waited in the lander before the game; -1 when not seen boarding, or saved before ghost splits. */
	private int wait = -1;
	/** The game's length in ticks; 0 in saves made before ghost splits. */
	private int ticks;
	/**
	 * Ticks into the game of the first shield drop and of each portal falling, in the order they fell;
	 * -1 where not seen. Null in saves made before ghost splits.
	 */
	private int[] marks;
	/** The portals in the order they fell, by name. */
	private String[] kills;

	public GameRecord()
	{
	}

	public GameRecord(long ended, Boat boat, Result result, int points, int damage, int seconds)
	{
		this.ended = ended;
		this.boat = boat == null ? null : boat.name();
		this.result = result.name();
		this.points = points;
		this.damage = damage;
		this.seconds = seconds;
	}

	/** Adds the game's timeline, which the ghost splits are drawn from. */
	public GameRecord splits(int wait, int ticks, int[] marks, String[] kills)
	{
		this.wait = wait;
		this.ticks = ticks;
		this.marks = marks == null ? null : marks.clone();
		this.kills = kills == null ? null : kills.clone();
		return this;
	}

	/** Ticks waited in the lander, or -1. */
	public int getWait()
	{
		return wait;
	}

	/** The game's length in ticks, or 0 when not kept. */
	public int getTicks()
	{
		return ticks;
	}

	/** Ticks into the game of a mark (0 the first shield drop, 1 to 4 each portal falling), or -1. */
	public int mark(int i)
	{
		return marks == null || i < 0 || i >= marks.length ? -1 : marks[i];
	}

	/** The portal that fell k-th (from 1), or null. */
	public Portal killed(int k)
	{
		return kills == null || k < 1 || k > kills.length ? null : Portal.named(kills[k - 1]);
	}

	public long getEnded()
	{
		return ended;
	}

	public Boat getBoat()
	{
		return Boat.named(boat);
	}

	public Result getResult()
	{
		try
		{
			return result == null ? Result.LEFT : Result.valueOf(result);
		}
		catch (IllegalArgumentException e)
		{
			return Result.LEFT;
		}
	}

	public int getPoints()
	{
		return points;
	}

	public int getDamage()
	{
		return damage;
	}

	public int getSeconds()
	{
		return seconds;
	}

	/** Brings figures a damaged save holds out of range back into it. */
	void repair()
	{
		ended = Math.max(0, ended);
		points = Math.max(0, Math.min(points, MAX_POINTS));
		damage = Math.max(0, damage);
		seconds = Math.max(0, Math.min(seconds, MAX_SECONDS));
		wait = Math.max(-1, wait);
		ticks = Math.max(0, Math.min(ticks, MAX_SECONDS * 5 / 3));
		if (marks != null)
		{
			for (int i = 0; i < marks.length; i++)
			{
				marks[i] = Math.max(-1, Math.min(marks[i], ticks));
			}
		}
	}
}
