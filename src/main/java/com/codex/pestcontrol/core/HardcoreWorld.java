package com.codex.pestcontrol.core;

/**
 * Hardcore Worlds, the members-only mode where a character has one life. The OSRS Wiki (Hardcore
 * Worlds) lists Pest Control among the activities that "are UNSAFE and dying in them will result
 * in an official death". After an official death the player chooses to reset the character or
 * transfer it off Hardcore Worlds, and the items that would go to a gravestone are deleted either
 * way.
 *
 * <p>The world is read from the client's own world type flags. RuneLite calls the Hardcore World
 * flag {@code HARDCORE_MAIN} ({@code net.runelite.api.WorldType}, bit 11; its hiscores are
 * "Hardcore Main"). It is matched by name, so a client that does not know the flag reads false
 * rather than failing.
 */
public final class HardcoreWorld
{
	/** RuneLite's name for the world type flag Hardcore Worlds carry. */
	static final String TYPE = "HARDCORE_MAIN";

	/** The small capitals over the warning. */
	public static final String KICKER = "HARDCORE WORLD";

	/** The warning in one short line, for the plaque and the panel's ribbon. */
	public static final String LINE = "A death in Pest Control is not safe here";

	/** The warning across the top of the plaque. */
	public static final String PLAQUE = "HARDCORE WORLD: a death here is not safe";

	/** The warning in full, with what an official death means, for the panel. */
	public static final String DETAIL = "Pest Control is on the Hardcore Worlds list of unsafe activities: dying in a game is an "
		+ "official death. You then reset the character or transfer it off Hardcore Worlds, and anything bound for a "
		+ "gravestone is deleted either way.";

	/** Where the rules come from, under the warning. */
	public static final String SOURCE = "From the Hardcore Worlds rules on the OSRS Wiki.";

	private HardcoreWorld()
	{
	}

	/**
	 * Whether the world is a Hardcore World.
	 *
	 * @param worldTypes the client's world types, or null
	 */
	public static boolean is(Iterable<? extends Enum<?>> worldTypes)
	{
		if (worldTypes == null)
		{
			return false;
		}
		for (Enum<?> type : worldTypes)
		{
			if (type != null && TYPE.equals(type.name()))
			{
				return true;
			}
		}
		return false;
	}
}
