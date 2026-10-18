package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import org.junit.Test;

public class BattleTest
{
	@Test
	public void waitsWhileThreePortalsCouldDropFirst()
	{
		Battle b = new Battle();
		Advice a = b.advise(null, null, 0);
		assertEquals(Advice.Kind.WAIT, a.getKind());
		assertEquals(Arrays.asList(Portal.PURPLE, Portal.BLUE, Portal.YELLOW), a.getCandidates());
		assertEquals("Purple, Blue or Yellow drops next", a.getDetail());
	}

	@Test
	public void attacksTheOpenPortal()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.BLUE);
		b.setHealth(Portal.BLUE, 200);
		Advice a = b.advise(null, null, 0);
		assertEquals(Advice.Kind.ATTACK, a.getKind());
		assertEquals(Portal.BLUE, a.getPortal());
		assertEquals("Attack Blue (east)", a.getHeadline());
		assertEquals("Shield down, 200 health left", a.getDetail());
	}

	@Test
	public void readsTheRotationAhead()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.BLUE);
		assertEquals(Arrays.asList(Portal.RED, Portal.PURPLE), b.nextShields());
		b.shieldDropped(Portal.PURPLE);
		assertEquals(Rotation.BPRY, b.getRotation());
		b.fall(Portal.BLUE);
		b.fall(Portal.PURPLE);
		Advice a = b.advise(null, null, 0);
		assertEquals(Advice.Kind.NEXT, a.getKind());
		assertEquals(Portal.RED, a.getPortal());
		assertEquals("Its shield drops next: leave by the south gate", a.getDetail());
		assertEquals(Arrays.asList(Portal.BLUE, Portal.PURPLE, Portal.RED, Portal.YELLOW), b.expectedOrder());
	}

	@Test
	public void fallingDropsTheShieldToo()
	{
		Battle b = new Battle();
		b.setHealth(Portal.YELLOW, 0);
		assertTrue(b.isFallen(Portal.YELLOW));
		assertFalse(b.isShielded(Portal.YELLOW));
		assertEquals(2, b.getPossible().size());
	}

	@Test
	public void unexplainedDropKeepsWhatIsKnown()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.RED);
		assertEquals(6, b.getPossible().size());
		assertFalse(b.nextShields().contains(Portal.RED));
	}

	@Test
	public void weakestFirstUnlessAnotherIsMuchNearer()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.PURPLE);
		b.shieldDropped(Portal.BLUE);
		b.setHealth(Portal.PURPLE, 50);
		b.setHealth(Portal.BLUE, 150);
		Map<Portal, Integer> far = new EnumMap<>(Portal.class);
		far.put(Portal.PURPLE, 20);
		far.put(Portal.BLUE, 10);
		assertEquals(Portal.PURPLE, b.advise(far, null, 0).getPortal());
		far.put(Portal.PURPLE, 40);
		assertEquals(Portal.BLUE, b.advise(far, null, 0).getPortal());
	}

	@Test
	public void spinnersComeFirst()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.YELLOW);
		Map<Portal, Integer> spin = new EnumMap<>(Portal.class);
		spin.put(Portal.YELLOW, 2);
		assertEquals("2 spinners are healing it: kill them first", b.advise(null, spin, 0).getDetail());
	}

	@Test
	public void guardsALowKnight()
	{
		Battle b = new Battle();
		b.setKnightHealth(40);
		assertEquals(Advice.Kind.GUARD, b.advise(null, null, 50).getKind());
		b.shieldDropped(Portal.PURPLE);
		Advice a = b.advise(null, null, 50);
		assertEquals(Advice.Kind.ATTACK, a.getKind());
		assertTrue(a.getDetail().startsWith("Knight at 40 health."));
	}

	@Test
	public void doneWhenAllFall()
	{
		Battle b = new Battle();
		for (Portal p : Portal.values())
		{
			b.fall(p);
		}
		assertEquals(Advice.Kind.DONE, b.advise(null, null, 0).getKind());
		assertNull(b.advise(null, null, 0).getPortal());
	}

	@Test
	public void damageForPoints()
	{
		Battle b = new Battle();
		b.addDamage(30);
		b.addDamage(-4);
		assertFalse(b.hasPoints());
		b.addDamage(20);
		assertTrue(b.hasPoints());
	}
}
