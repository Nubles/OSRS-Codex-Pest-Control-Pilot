package com.codex.pestcontrol.core;

/**
 * When a Pest Control game starts and ends, from whether its status bar is shown each tick. The
 * bar going does not end the game at once: its closing messages (points, a loss) arrive a few
 * ticks later, so the game settles for {@link #SETTLE_TICKS} first. If the bar comes back while it
 * settles (a dropped connection the client got back, an interface that flickered), the same game
 * carries on with the damage already dealt, rather than a fresh one starting from nothing.
 */
public final class GameWatch
{
	/** What a tick asks of the plugin. */
	public enum Act
	{
		NONE,
		/** A new game began: start counting. */
		START,
		/** The game that was settling is still on: carry on with it. */
		RESUME,
		/** The bar went: hold the game while its closing messages arrive. */
		SETTLE,
		/** The game settled: record it. */
		FINISH
	}

	/** Ticks after a game's status bar goes for its closing messages to arrive. */
	public static final int SETTLE_TICKS = 4;

	private boolean shown;
	/** Ticks the settling game has waited, counting the tick the bar went; 0 when none is settling. */
	private int settling;

	/** @param barShown the game's status bar is shown this tick */
	public Act tick(boolean barShown)
	{
		if (barShown)
		{
			if (shown)
			{
				return Act.NONE;
			}
			shown = true;
			if (settling > 0)
			{
				settling = 0;
				return Act.RESUME;
			}
			return Act.START;
		}
		if (shown)
		{
			shown = false;
			settling = 1;
			return SETTLE_TICKS <= 1 ? finish() : Act.SETTLE;
		}
		if (settling > 0 && ++settling >= SETTLE_TICKS)
		{
			return finish();
		}
		return Act.NONE;
	}

	private Act finish()
	{
		settling = 0;
		return Act.FINISH;
	}

	/** The game is over whatever the bar does next, as at logout: start afresh. */
	public void reset()
	{
		shown = false;
		settling = 0;
	}

	/** A game is being counted or settling. */
	public boolean isBusy()
	{
		return shown || settling > 0;
	}
}
