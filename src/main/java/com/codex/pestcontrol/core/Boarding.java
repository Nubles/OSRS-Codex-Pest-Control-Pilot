package com.codex.pestcontrol.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The lander you sit in: how many players are ready, and whether it is filling too slowly for a
 * game to start, in which case the Pest Control world is the place to be.
 */
public final class Boarding
{
	/** A lander leaves with no fewer than this many players aboard. */
	public static final int MIN_PLAYERS = 5;
	/** The world most Pest Control players use. */
	public static final int PEST_CONTROL_WORLD = 344;
	/** Ticks a lander must stay short of players before it is called quiet: about half a minute. */
	static final int QUIET_TICKS = 50;

	private static final Pattern NUMBER = Pattern.compile("([0-9]{1,4})");

	private int ticks;
	private int ready = -1;

	/**
	 * One tick aboard.
	 *
	 * @param readyText the lander's players-ready line, such as "Players Ready: 3", or null when unread
	 */
	public void tick(String readyText)
	{
		ticks++;
		int n = parse(readyText);
		if (n >= 0)
		{
			ready = n;
		}
	}

	/** Off the lander: start again next time. */
	public void leave()
	{
		ticks = 0;
		ready = -1;
	}

	/** Players ready, or -1 when not read. */
	public int getReady()
	{
		return ready;
	}

	/** The lander has sat short of a game for a while, on a world other than the Pest Control world. */
	public boolean isQuiet(int world)
	{
		return ready >= 0 && ready < MIN_PLAYERS && ticks >= QUIET_TICKS && world != PEST_CONTROL_WORLD;
	}

	/** What to say about a quiet lander. */
	public String tip()
	{
		return "Only " + ready + " aboard and a game needs " + MIN_PLAYERS + "; most players use world " + PEST_CONTROL_WORLD;
	}

	static int parse(String text)
	{
		if (text == null)
		{
			return -1;
		}
		Matcher m = NUMBER.matcher(text.replaceAll("<[^>]*>", ""));
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}
}
