package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TeamPaceTest
{
	/** Both side portals open; purple's health falls by the given amount a tick, blue's by another. */
	private static Battle twoOpen(int purpleStart, int purplePerTick, int blueStart, int bluePerTick, int ticks)
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.PURPLE);
		b.shieldDropped(Portal.BLUE);
		for (int t = 0; t < ticks; t++)
		{
			b.tick();
			b.setHealth(Portal.PURPLE, purpleStart - purplePerTick * t);
			b.setHealth(Portal.BLUE, blueStart - bluePerTick * t);
		}
		return b;
	}

	@Test
	public void thePaceIsMeasuredFromTheHealthFalling()
	{
		Battle b = twoOpen(200, 4, 200, 0, 12);
		// 156 left at 4 a tick is 39 ticks, about 24 seconds.
		assertEquals((int) Math.ceil(156 / 4.0 * 0.6), b.secondsToFall(Portal.PURPLE));
		assertEquals(Battle.STALLED, b.secondsToFall(Portal.BLUE));
		assertEquals(-1, b.secondsToFall(Portal.RED));
	}

	@Test
	public void tooFewReadingsAreNotAPace()
	{
		Battle b = twoOpen(200, 4, 200, 0, 3);
		assertEquals(-1, b.secondsToFall(Portal.PURPLE));
	}

	@Test
	public void yourOwnHitsAreLeftOutOfTheTeamsPace()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.PURPLE);
		int hp = 200;
		for (int t = 0; t < 12; t++)
		{
			b.tick();
			b.setHealth(Portal.PURPLE, hp);
			// You alone hit it for 5 a tick; nobody else is there.
			b.addDamage(5, Portal.PURPLE);
			hp -= 5;
		}
		assertEquals(Battle.STALLED, b.secondsToFall(Portal.PURPLE));
	}

	@Test
	public void withPointsEarnedItSendsYouToThePortalHoldingTheGameUp()
	{
		Battle b = twoOpen(100, 5, 220, 0, 12);
		b.addDamage(60);
		Advice a = b.advise(null);
		assertEquals(Advice.Kind.ATTACK, a.getKind());
		assertEquals(Portal.BLUE, a.getPortal());
		assertTrue(a.getDetail(), a.getDetail().contains("holds the game up"));
	}

	@Test
	public void beforePointsTheWeakestStillComesFirst()
	{
		Battle b = twoOpen(100, 5, 220, 0, 12);
		assertEquals(Portal.PURPLE, b.advise(null).getPortal());
	}

	@Test
	public void twoPortalsFallingAtTheSamePaceDoNotPullYouAcross()
	{
		Battle b = twoOpen(100, 5, 110, 5, 12);
		b.addDamage(60);
		Advice a = b.advise(null);
		assertEquals(Portal.PURPLE, a.getPortal());
		assertTrue(a.getDetail(), a.getDetail().startsWith("Shield down"));
	}

	@Test
	public void theChoiceHoldsWhileTheGapIsSmall()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.PURPLE);
		b.shieldDropped(Portal.BLUE);
		b.addDamage(60);
		int p = 120;
		int q = 200;
		for (int t = 0; t < 12; t++)
		{
			b.tick();
			b.setHealth(Portal.PURPLE, p -= 6);
			b.setHealth(Portal.BLUE, q);
		}
		assertEquals(Portal.BLUE, b.advise(null).getPortal());
		// The team arrives at blue and wears it down; purple is now a little slower but not by much.
		for (int t = 0; t < 18; t++)
		{
			b.tick();
			b.setHealth(Portal.PURPLE, p -= 2);
			b.setHealth(Portal.BLUE, q -= 3);
		}
		assertEquals(Portal.BLUE, b.advise(null).getPortal());
	}
}
