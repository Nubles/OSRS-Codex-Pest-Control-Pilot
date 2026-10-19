package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

public class GameWatchTest
{
	@Test
	public void aGameStartsSettlesAndFinishes()
	{
		GameWatch w = new GameWatch();
		assertEquals(GameWatch.Act.NONE, w.tick(false));
		assertEquals(GameWatch.Act.START, w.tick(true));
		assertEquals(GameWatch.Act.NONE, w.tick(true));
		assertEquals(GameWatch.Act.SETTLE, w.tick(false));
		assertEquals(GameWatch.Act.NONE, w.tick(false));
		assertEquals(GameWatch.Act.NONE, w.tick(false));
		// The fourth tick without the bar, counting the one it went on, as the closing messages are in.
		assertEquals(GameWatch.Act.FINISH, w.tick(false));
		assertEquals(GameWatch.Act.NONE, w.tick(false));
		assertFalse(w.isBusy());
	}

	@Test
	public void theBarComingBackWhileSettlingCarriesTheSameGameOn()
	{
		GameWatch w = new GameWatch();
		w.tick(true);
		// A dropped connection the client got back: the bar is missing for a couple of ticks.
		assertEquals(GameWatch.Act.SETTLE, w.tick(false));
		assertEquals(GameWatch.Act.NONE, w.tick(false));
		assertEquals("not a fresh game with the damage gone", GameWatch.Act.RESUME, w.tick(true));
		assertEquals(GameWatch.Act.SETTLE, w.tick(false));
	}

	@Test
	public void afterAResetTheNextBarIsANewGame()
	{
		GameWatch w = new GameWatch();
		w.tick(true);
		w.tick(false);
		w.reset();
		assertEquals(GameWatch.Act.START, w.tick(true));
	}
}
