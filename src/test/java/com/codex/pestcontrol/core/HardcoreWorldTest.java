package com.codex.pestcontrol.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import net.runelite.api.WorldType;
import org.junit.Test;

/** A Hardcore World is known by RuneLite's own world type flag, matched by name. */
public class HardcoreWorldTest
{
	/** A stand-in for a client that knows the flag under its name, whatever else it knows. */
	private enum Flag
	{
		MEMBERS,
		HARDCORE_MAIN
	}

	@Test
	public void theHardcoreFlagMakesAHardcoreWorld()
	{
		assertTrue(HardcoreWorld.is(EnumSet.of(WorldType.MEMBERS, WorldType.HARDCORE_MAIN)));
		assertTrue(HardcoreWorld.is(EnumSet.of(WorldType.HARDCORE_MAIN)));
		assertTrue("matched by name, not by RuneLite's enum", HardcoreWorld.is(Collections.singletonList(Flag.HARDCORE_MAIN)));
	}

	@Test
	public void otherWorldsAreNot()
	{
		assertFalse(HardcoreWorld.is(EnumSet.noneOf(WorldType.class)));
		assertFalse(HardcoreWorld.is(EnumSet.of(WorldType.MEMBERS)));
		// Dangerous worlds of other kinds are not Hardcore Worlds.
		assertFalse(HardcoreWorld.is(EnumSet.of(WorldType.MEMBERS, WorldType.PVP, WorldType.HIGH_RISK)));
		assertFalse(HardcoreWorld.is(EnumSet.of(WorldType.SEASONAL, WorldType.DEADMAN)));
		assertFalse(HardcoreWorld.is(Collections.singletonList(Flag.MEMBERS)));
	}

	@Test
	public void unreadTypesAreNot()
	{
		assertFalse(HardcoreWorld.is(null));
		assertFalse(HardcoreWorld.is(Arrays.asList((WorldType) null)));
	}

	@Test
	public void theLinesFitTheirPlaces()
	{
		// The plaque's line sits on one row; the panel's ribbon on two at most.
		assertTrue(HardcoreWorld.PLAQUE.length() <= 44);
		assertTrue(HardcoreWorld.LINE.length() <= 44);
		assertTrue(HardcoreWorld.DETAIL.contains("official death"));
	}
}
