package com.codex.pestcontrol.ui;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Numbers and times written the way the panel shows them. */
public final class Format
{
	private Format()
	{
	}

	public static String number(long n)
	{
		return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(n);
	}

	/** "45s", "3m 20s" for a game's length. */
	public static String clock(int seconds)
	{
		if (seconds < 60)
		{
			return seconds + "s";
		}
		return seconds / 60 + "m " + String.format(Locale.ENGLISH, "%02ds", seconds % 60);
	}

	/** "35m", "2.9h", "14h", "3d" for how long a goal takes, short enough for a tile. */
	public static String span(double minutes)
	{
		long m = Math.round(minutes);
		if (m < 60)
		{
			return m + "m";
		}
		double h = minutes / 60;
		if (h < 10)
		{
			return String.format(Locale.ENGLISH, "%.1fh", h);
		}
		if (h < 48)
		{
			return Math.round(h) + "h";
		}
		return Math.round(h / 24) + "d";
	}

	/** "now", "12m ago", "3h ago", "2d ago". */
	public static String ago(long then, long now)
	{
		long m = Math.max(0, now - then) / 60_000L;
		if (m < 1)
		{
			return "now";
		}
		if (m < 60)
		{
			return m + "m ago";
		}
		long h = m / 60;
		if (h < 24)
		{
			return h + "h ago";
		}
		return h / 24 + "d ago";
	}

	public static String plural(long n, String one, String many)
	{
		return number(n) + " " + (n == 1 ? one : many);
	}

	private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

	/** "16 Oct". */
	public static String shortDay(long epochMs)
	{
		return SHORT_DAY.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
	}
}
