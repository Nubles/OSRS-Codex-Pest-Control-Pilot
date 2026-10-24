package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import org.junit.Test;

public class AlertsTest
{
	@Test
	public void portalsOpenWhenTheGameIsFirstSeenAreNotNews()
	{
		Battle b = new Battle();
		b.foundOpen(Portal.PURPLE);
		Alerts a = new Alerts();
		assertEquals(Alerts.Kind.START, a.start(b).getKind());
		assertTrue(a.check(b, 60).isEmpty());
	}

	@Test
	public void eachShieldSeenDroppingNotifiesOnceAndNamesWhatIsNext()
	{
		Battle b = new Battle();
		Alerts a = new Alerts();
		a.start(b);
		b.tick();
		b.shieldDropped(Portal.PURPLE);
		b.shieldDropped(Portal.PURPLE);
		List<Alerts.Alert> out = a.check(b, 60);
		assertEquals(1, out.size());
		assertEquals("Pest Control: the purple shield is down (west gate)", out.get(0).getMessage());
		assertTrue(a.check(b, 60).isEmpty());
		b.shieldDropped(Portal.BLUE);
		// Purple then blue leaves one order: yellow, then red.
		out = a.check(b, 60);
		assertEquals("Pest Control: the blue shield is down (east gate); yellow is next", out.get(0).getMessage());
	}

	@Test
	public void aPortalSeenFallingWithItsDropMissedIsNotAShieldAlert()
	{
		Battle b = new Battle();
		Alerts a = new Alerts();
		a.start(b);
		b.setHealth(Portal.RED, 0);
		assertTrue(a.check(b, 60).isEmpty());
	}

	@Test
	public void theKnightWarnsOnceAGameAndNotWhenSwitchedOff()
	{
		Battle b = new Battle();
		Alerts a = new Alerts();
		a.start(b);
		b.setKnightHealth(200);
		assertTrue(a.check(b, 60).isEmpty());
		b.setKnightHealth(55);
		List<Alerts.Alert> out = a.check(b, 60);
		assertEquals(Alerts.Kind.KNIGHT, out.get(0).getKind());
		assertEquals("Pest Control: the Void Knight is down to 55 health", out.get(0).getMessage());
		b.setKnightHealth(40);
		assertTrue(a.check(b, 60).isEmpty());

		Battle next = new Battle();
		a.start(next);
		next.setKnightHealth(10);
		assertTrue(a.check(next, 0).isEmpty());
	}

	@Test
	public void aReconnectRepeatsNothing()
	{
		Battle b = new Battle();
		Alerts a = new Alerts();
		a.start(b);
		b.tick();
		b.shieldDropped(Portal.YELLOW);
		a.resume(b);
		assertTrue(a.check(b, 60).isEmpty());
	}
}
