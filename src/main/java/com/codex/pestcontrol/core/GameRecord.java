package com.codex.pestcontrol.core;

/** One finished game, as kept in the logbook. Fields are plain so Gson can store it. */
public class GameRecord
{
	public enum Result
	{
		WON,
		LOST,
		/** Won, but too little damage was dealt to be given points. */
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
	}
}
