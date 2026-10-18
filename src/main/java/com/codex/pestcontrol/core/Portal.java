package com.codex.pestcontrol.core;

import java.util.Locale;

/**
 * The four portals around the outpost, in the order the game numbers them: the status widgets,
 * the portal NPCs and the shield messages all count purple, blue, yellow, red.
 */
public enum Portal
{
	PURPLE("Purple", "west", "west gate", 0xB070E0),
	BLUE("Blue", "east", "east gate", 0x5AA0F0),
	YELLOW("Yellow", "south-east", "south gate", 0xE8C84A),
	RED("Red", "south-west", "south gate", 0xE05A4A);

	private final String title;
	private final String side;
	private final String gate;
	private final int rgb;

	Portal(String title, String side, String gate, int rgb)
	{
		this.title = title;
		this.side = side;
		this.gate = gate;
		this.rgb = rgb;
	}

	public String getTitle()
	{
		return title;
	}

	/** Where it stands from the Void Knight, such as "south-east". */
	public String getSide()
	{
		return side;
	}

	/** The outpost gate to leave by to reach it. */
	public String getGate()
	{
		return gate;
	}

	public int getRgb()
	{
		return rgb;
	}

	/** The portal a shield message names, such as "purple", or null. */
	public static Portal named(String colour)
	{
		if (colour == null)
		{
			return null;
		}
		String c = colour.trim().toUpperCase(Locale.ENGLISH);
		for (Portal p : values())
		{
			if (p.name().equals(c))
			{
				return p;
			}
		}
		return null;
	}
}
