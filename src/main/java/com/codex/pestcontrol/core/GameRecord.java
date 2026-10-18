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
}
