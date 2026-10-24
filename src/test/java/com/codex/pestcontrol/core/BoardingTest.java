package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BoardingTest
{
	@Test
	public void readsThePlayersReadyLine()
	{
		assertEquals(3, Boarding.parse("Players Ready: 3"));
		assertEquals(12, Boarding.parse("<col=ffffff>Players Ready: 12</col>"));
		assertEquals(-1, Boarding.parse("Players Ready:"));
		assertEquals(-1, Boarding.parse(null));
	}

	@Test
	public void aLanderShortOfPlayersForHalfAMinuteIsQuietOffThePestControlWorld()
	{
		Boarding b = new Boarding();
		for (int i = 0; i < Boarding.QUIET_TICKS - 1; i++)
		{
			b.tick("Players Ready: 2");
		}
		assertFalse("not yet: landers fill in seconds on a busy world", b.isQuiet(301));
		b.tick("Players Ready: 2");
		assertTrue(b.isQuiet(301));
		assertFalse(b.isQuiet(Boarding.PEST_CONTROL_WORLD));
		assertEquals("Only 2 aboard and a game needs 5; most players use world 344", b.tip());
		b.tick("Players Ready: 5");
		assertFalse("enough for a game", b.isQuiet(301));
	}

	@Test
	public void leavingStartsTheWaitAgain()
	{
		Boarding b = new Boarding();
		for (int i = 0; i < Boarding.QUIET_TICKS; i++)
		{
			b.tick("Players Ready: 1");
		}
		b.leave();
		assertEquals(-1, b.getReady());
		b.tick("Players Ready: 1");
		assertFalse(b.isQuiet(301));
	}

	@Test
	public void anUnreadLineIsNeverQuiet()
	{
		Boarding b = new Boarding();
		for (int i = 0; i < 100; i++)
		{
			b.tick(null);
		}
		assertFalse(b.isQuiet(301));
	}
}
