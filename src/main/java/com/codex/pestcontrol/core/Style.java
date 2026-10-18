package com.codex.pestcontrol.core;

import java.util.Locale;

/** How you fight, read from the weapon you hold: ranged and magic can work from behind cover. */
public enum Style
{
	MELEE,
	RANGED,
	MAGIC;

	private static final String[] RANGED_WORDS = {"bow", "dart", "knife", "thrownaxe", "chinchompa", "blowpipe", "ballista", "javelin", "eclipse atlatl"};
	private static final String[] MAGIC_WORDS = {"staff", "wand", "trident", "sceptre", "sanguinesti", "tumeken", "nightmare staff", "harmonised", "kodai"};

	/** The style a weapon's name suggests; melee for anything else or nothing held. */
	public static Style ofWeapon(String name)
	{
		if (name == null)
		{
			return MELEE;
		}
		String n = name.toLowerCase(Locale.ENGLISH);
		for (String w : RANGED_WORDS)
		{
			if (n.contains(w))
			{
				return RANGED;
			}
		}
		for (String w : MAGIC_WORDS)
		{
			if (n.contains(w))
			{
				return MAGIC;
			}
		}
		return MELEE;
	}

	public boolean isDistant()
	{
		return this != MELEE;
	}
}
