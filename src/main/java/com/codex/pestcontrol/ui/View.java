package com.codex.pestcontrol.ui;

import com.codex.pestcontrol.core.Advice;
import com.codex.pestcontrol.core.Boat;
import com.codex.pestcontrol.core.GameRecord;
import com.codex.pestcontrol.core.Goal;
import com.codex.pestcontrol.core.Outlook;
import com.codex.pestcontrol.core.Portal;
import com.codex.pestcontrol.core.Reward;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Everything the panel shows, worked out on the client thread and handed over whole. */
public final class View
{
	/** Where the player is, as far as the panel cares. */
	public enum State
	{
		LOGGED_OUT,
		AWAY,
		OUTPOST,
		LANDER,
		BATTLE
	}

	/** One portal as the status widget and the portals themselves show it. */
	public static final class PortalState
	{
		final Portal portal;
		final boolean shielded;
		final boolean fallen;
		final int health;

		public PortalState(Portal portal, boolean shielded, boolean fallen, int health)
		{
			this.portal = portal;
			this.shielded = shielded;
			this.fallen = fallen;
			this.health = health;
		}
	}

	/** The game in progress. */
	public static final class Fight
	{
		final List<PortalState> portals;
		final Advice advice;
		final int knightHealth;
		final int damage;
		final List<Portal> order;
		final int rotations;
		final int seconds;

		/**
		 * @param order the shield order as far as it is known, dropped ones first
		 * @param rotations how many rotations are still possible
		 */
		public Fight(List<PortalState> portals, Advice advice, int knightHealth, int damage, List<Portal> order, int rotations, int seconds)
		{
			this.portals = portals;
			this.advice = advice;
			this.knightHealth = knightHealth;
			this.damage = damage;
			this.order = order;
			this.rotations = rotations;
			this.seconds = seconds;
		}

		PortalState portal(Portal p)
		{
			return portals.get(p.ordinal());
		}
	}

	final State state;
	final String player;
	final Fight fight;
	final GameRecord last;
	final Outlook outlook;
	final Set<Reward> goal;
	final Set<Reward> owned;
	final Goal preset;
	final Boat boat;
	final boolean ownedKnown;
	final List<String> missing;
	final boolean elite;
	final int played;
	final int won;
	final int earned;
	final long seconds;
	final List<GameRecord> games;
	final long now;

	/**
	 * @param fight the game in progress, or null
	 * @param last the newest finished game, or null
	 * @param missing combat levels void gear needs that the account lacks, such as "42 Magic"
	 * @param elite whether elite void can be bought (the hard Western Provinces diary is done)
	 */
	public View(State state, String player, Fight fight, GameRecord last, Outlook outlook, Set<Reward> goal, Set<Reward> owned,
		Goal preset, Boat boat, boolean ownedKnown, List<String> missing, boolean elite,
		int played, int won, int earned, long seconds, List<GameRecord> games, long now)
	{
		this.state = state;
		this.player = player;
		this.fight = fight;
		this.last = last;
		this.outlook = outlook;
		this.goal = goal == null ? EnumSet.noneOf(Reward.class) : goal;
		this.owned = owned == null ? EnumSet.noneOf(Reward.class) : owned;
		this.preset = preset;
		this.boat = boat;
		this.ownedKnown = ownedKnown;
		this.missing = missing == null ? Collections.emptyList() : missing;
		this.elite = elite;
		this.played = played;
		this.won = won;
		this.earned = earned;
		this.seconds = seconds;
		this.games = games == null ? Collections.emptyList() : games;
		this.now = now;
	}

	public static View loggedOut()
	{
		return new View(State.LOGGED_OUT, null, null, null, null, null, null, null, null, false, null, false, 0, 0, 0, 0, null, System.currentTimeMillis());
	}
}
