package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides when a game is worth a desktop notification: when it begins, when a shield is seen
 * dropping, and when the Void Knight first falls low. Each fires once, so a repeated message or a
 * portal seen twice never notifies twice.
 */
public final class Alerts
{
	/** What a notification is about, so each can be switched on or off on its own. */
	public enum Kind
	{
		START,
		SHIELD,
		KNIGHT
	}

	/** One notification to send. */
	public static final class Alert
	{
		private final Kind kind;
		private final String message;

		Alert(Kind kind, String message)
		{
			this.kind = kind;
			this.message = message;
		}

		public Kind getKind()
		{
			return kind;
		}

		public String getMessage()
		{
			return message;
		}

		@Override
		public String toString()
		{
			return kind + ": " + message;
		}
	}

	private int dropsSeen;
	private boolean knightWarned;

	/** A new game began. Portals already open when it was first seen are not news. */
	public Alert start(Battle battle)
	{
		dropsSeen = battle.getDrops().size();
		knightWarned = false;
		return new Alert(Kind.START, "Pest Control: the game has begun");
	}

	/** Carrying on with a game after a dropped connection: nothing seen before it is news again. */
	public void resume(Battle battle)
	{
		dropsSeen = battle.getDrops().size();
	}

	/**
	 * What changed since the last look.
	 *
	 * @param knightWarn the knight's health at which defending him comes first; 0 never warns
	 */
	public List<Alert> check(Battle battle, int knightWarn)
	{
		List<Alert> out = new ArrayList<>();
		List<Portal> drops = battle.getDrops();
		List<Integer> ticks = battle.getDropTicks();
		for (int i = dropsSeen; i < drops.size(); i++)
		{
			// A shield found already down, or a portal seen falling with its drop missed, happened unseen.
			if (ticks.get(i) >= 0)
			{
				out.add(new Alert(Kind.SHIELD, shield(battle, drops.get(i))));
			}
		}
		dropsSeen = drops.size();
		int knight = battle.getKnightHealth();
		if (!knightWarned && knightWarn > 0 && knight > 0 && knight <= knightWarn)
		{
			knightWarned = true;
			out.add(new Alert(Kind.KNIGHT, "Pest Control: the Void Knight is down to " + knight + " health"));
		}
		return out;
	}

	static String shield(Battle battle, Portal p)
	{
		String text = "Pest Control: the " + p.getTitle().toLowerCase(java.util.Locale.ENGLISH) + " shield is down (" + p.getGate() + ")";
		List<Portal> next = battle.nextShields();
		if (next.size() == 1 && !battle.isFallen(next.get(0)))
		{
			text += "; " + next.get(0).getTitle().toLowerCase(java.util.Locale.ENGLISH) + " is next";
		}
		return text;
	}
}
