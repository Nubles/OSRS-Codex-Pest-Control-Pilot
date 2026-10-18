package com.codex.pestcontrol.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * What can be seen around the island at one moment, beyond the status bar: how far each portal
 * is, the pests and players gathered at it, your combat style and activity, and when the next
 * shield is due. Anything not known is simply left out.
 */
public final class Field
{
	final Map<Portal, Integer> distance = new EnumMap<>(Portal.class);
	final Map<Portal, Integer> spinners = new EnumMap<>(Portal.class);
	final Map<Portal, Integer> brawlers = new EnumMap<>(Portal.class);
	final Map<Portal, Integer> crowd = new EnumMap<>(Portal.class);
	Style style = Style.MELEE;
	int knightWarn;
	int activity = -1;
	int nextDrop = -1;

	public Field distance(Portal p, int tiles)
	{
		distance.put(p, tiles);
		return this;
	}

	public Field spinners(Portal p, int count)
	{
		spinners.put(p, count);
		return this;
	}

	public Field brawlers(Portal p, int count)
	{
		brawlers.put(p, count);
		return this;
	}

	/** Other players near the portal. */
	public Field crowd(Portal p, int players)
	{
		crowd.put(p, players);
		return this;
	}

	public Field style(Style s)
	{
		style = s == null ? Style.MELEE : s;
		return this;
	}

	/** The knight's health at or below which defending him comes first; 0 never. */
	public Field knightWarn(int hp)
	{
		knightWarn = hp;
		return this;
	}

	/** Your activity bar, 0 to 100, or -1 when not read. */
	public Field activity(int percent)
	{
		activity = percent;
		return this;
	}

	/** Seconds until the next shield is due, or -1 when not yet learned. */
	public Field nextDrop(int seconds)
	{
		nextDrop = seconds;
		return this;
	}

	public Integer crowdAt(Portal p)
	{
		return crowd.get(p);
	}
}
