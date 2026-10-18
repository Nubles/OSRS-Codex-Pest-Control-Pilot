package com.codex.pestcontrol.core;

import static com.codex.pestcontrol.core.Portal.BLUE;
import static com.codex.pestcontrol.core.Portal.PURPLE;
import static com.codex.pestcontrol.core.Portal.RED;
import static com.codex.pestcontrol.core.Portal.YELLOW;

/**
 * The orders in which portal shields drop. Every game follows one of these six, so once a
 * shield or two has dropped the rest of the game can be read ahead.
 */
public enum Rotation
{
	PBYR(PURPLE, BLUE, YELLOW, RED),
	PYBR(PURPLE, YELLOW, BLUE, RED),
	BRYP(BLUE, RED, YELLOW, PURPLE),
	BPRY(BLUE, PURPLE, RED, YELLOW),
	YRPB(YELLOW, RED, PURPLE, BLUE),
	YPRB(YELLOW, PURPLE, RED, BLUE);

	private final Portal[] order;

	Rotation(Portal... order)
	{
		this.order = order;
	}

	/** The portal whose shield drops at a step, counting from zero, or null past the last. */
	public Portal at(int step)
	{
		return step < 0 || step >= order.length ? null : order[step];
	}

	public Portal[] order()
	{
		return order.clone();
	}
}
