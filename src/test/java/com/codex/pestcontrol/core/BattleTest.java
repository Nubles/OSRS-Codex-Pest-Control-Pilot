package com.codex.pestcontrol.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import org.junit.Test;

public class BattleTest
{
	@Test
	public void waitsWhileThreePortalsCouldDropFirst()
	{
		Battle b = new Battle();
		Advice a = b.advise(null);
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
		Advice a = b.advise(null);
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
		Advice a = b.advise(null);
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
		Field far = new Field().distance(Portal.PURPLE, 20).distance(Portal.BLUE, 10);
		assertEquals(Portal.PURPLE, b.advise(far).getPortal());
		far.distance(Portal.PURPLE, 40);
		assertEquals(Portal.BLUE, b.advise(far).getPortal());
	}

	@Test
	public void spinnersComeFirst()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.YELLOW);
		Field spin = new Field().spinners(Portal.YELLOW, 2);
		assertEquals("2 spinners are healing it: kill them first", b.advise(spin).getDetail());
		spin.style(Style.RANGED);
		assertEquals("2 spinners are healing it: shoot them from range, then the portal", b.advise(spin).getDetail());
	}

	@Test
	public void guardsALowKnight()
	{
		Battle b = new Battle();
		b.setKnightHealth(40);
		assertEquals(Advice.Kind.GUARD, b.advise(new Field().knightWarn(50)).getKind());
		b.shieldDropped(Portal.PURPLE);
		Advice a = b.advise(new Field().knightWarn(50));
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
		assertEquals(Advice.Kind.DONE, b.advise(null).getKind());
		assertNull(b.advise(null).getPortal());
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

	@Test
	public void quieterPortalUntilYouHaveYourPoints()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.PURPLE);
		b.shieldDropped(Portal.BLUE);
		b.setHealth(Portal.PURPLE, 60);
		b.setHealth(Portal.BLUE, 180);
		Field f = new Field().crowd(Portal.PURPLE, 14).crowd(Portal.BLUE, 3);
		Advice a = b.advise(f);
		assertEquals(Portal.BLUE, a.getPortal());
		assertEquals("Only 3 players there: easier damage for your points", a.getDetail());
		b.addDamage(60);
		assertEquals(Portal.PURPLE, b.advise(f).getPortal());
		assertEquals("Shield down, 60 health left, 14 players", b.advise(f).getDetail());
	}

	@Test
	public void brawlersOnlyBlockMelee()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.RED);
		Field f = new Field().brawlers(Portal.RED, 1);
		assertEquals("A brawler blocks the way: clear it first", b.advise(f).getDetail());
		f.style(Style.MAGIC);
		assertTrue(b.advise(f).getDetail().startsWith("Shield down"));
	}

	@Test
	public void countsDownToTheNextShield()
	{
		ShieldClock clock = new ShieldClock();
		clock.learn(Arrays.asList(50, 100, 150, 200));
		assertEquals(-1, clock.expected(0));
		clock.learn(Arrays.asList(60, 110, -1, 220));
		assertEquals(55, clock.expected(0));
		assertEquals(50, clock.expected(1));
		assertEquals(1, clock.samples(2));
		assertEquals(1, clock.samples(3));

		Battle b = new Battle();
		for (int i = 0; i < 25; i++)
		{
			b.tick();
		}
		assertEquals(18, b.secondsToNextDrop(clock));
		Advice a = b.advise(new Field().nextDrop(b.secondsToNextDrop(clock)).activity(10));
		assertEquals("Purple, Blue or Yellow drops in about 18s. Repair a barricade meanwhile to raise your activity", a.getDetail());
		b.foundOpen(Portal.BLUE);
		assertEquals(-1, b.secondsToNextDrop(clock));
		assertEquals(Arrays.asList(-1), b.getDropTicks());
	}

	@Test
	public void rangedHangsBack()
	{
		Battle b = new Battle();
		b.shieldDropped(Portal.YELLOW);
		b.fall(Portal.YELLOW);
		b.shieldDropped(Portal.RED);
		b.fall(Portal.RED);
		Advice a = b.advise(new Field().style(Style.RANGED).nextDrop(2));
		assertEquals("Its shield drops any moment: leave by the west gate. Hang back and hit it from range", a.getDetail());
	}

	@Test
	public void weaponsTellTheStyle()
	{
		assertEquals(Style.RANGED, Style.ofWeapon("Rune crossbow"));
		assertEquals(Style.RANGED, Style.ofWeapon("Toxic blowpipe"));
		assertEquals(Style.MAGIC, Style.ofWeapon("Trident of the swamp"));
		assertEquals(Style.MELEE, Style.ofWeapon("Abyssal whip"));
		assertEquals(Style.MELEE, Style.ofWeapon(null));
	}
}
