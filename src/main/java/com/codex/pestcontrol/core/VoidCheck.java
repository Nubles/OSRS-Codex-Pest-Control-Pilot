package com.codex.pestcontrol.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What the void worn now does for the weapon in hand. The set bonus needs all four slots: a helm,
 * the top (or the elite top), the robe (or the elite robe) and the gloves. The helm decides the
 * style it helps, so a mage helm worn with a bow gives nothing. Read before a game, when there is
 * still time to fix it.
 */
public final class VoidCheck
{
	public enum Kind
	{
		/** All four pieces on, the helm matching the weapon. */
		SET_ON,
		/** All four on, but the helm is for another style. */
		WRONG_HELM,
		/** Some pieces on, not all four: no bonus at all. */
		INCOMPLETE,
		/** None on, though a full set for this weapon is owned. */
		NOT_WORN
	}

	private final Kind kind;
	private final String headline;
	private final String detail;

	private VoidCheck(Kind kind, String headline, String detail)
	{
		this.kind = kind;
		this.headline = headline;
		this.detail = detail;
	}

	public Kind getKind()
	{
		return kind;
	}

	public String getHeadline()
	{
		return headline;
	}

	public String getDetail()
	{
		return detail;
	}

	/** Worth raising on the plaque: the void worn gives no bonus as it stands. */
	public boolean isWarning()
	{
		return kind == Kind.WRONG_HELM || kind == Kind.INCOMPLETE;
	}

	/** The helm that helps a style. */
	public static Reward helmFor(Style style)
	{
		switch (style)
		{
			case RANGED:
				return Reward.RANGER_HELM;
			case MAGIC:
				return Reward.MAGE_HELM;
			default:
				return Reward.MELEE_HELM;
		}
	}

	/**
	 * @param worn the rewards worn now
	 * @param style the style of the weapon in hand
	 * @param owned every reward owned, in the bank or carried
	 * @return what to say, or null when there is nothing to say (no void worn and no full set owned)
	 */
	public static VoidCheck of(Set<Reward> worn, Style style, Set<Reward> owned)
	{
		Reward helm = null;
		for (Reward h : EnumSet.of(Reward.MELEE_HELM, Reward.RANGER_HELM, Reward.MAGE_HELM))
		{
			if (worn.contains(h))
			{
				helm = h;
			}
		}
		boolean top = worn.contains(Reward.TOP) || worn.contains(Reward.ELITE_TOP);
		boolean robe = worn.contains(Reward.ROBE) || worn.contains(Reward.ELITE_ROBE);
		boolean gloves = worn.contains(Reward.GLOVES);
		boolean elite = worn.contains(Reward.ELITE_TOP) && worn.contains(Reward.ELITE_ROBE);
		Reward right = helmFor(style);
		String styleName = style.name().toLowerCase(Locale.ENGLISH);
		if (helm == null && !top && !robe && !gloves)
		{
			boolean hasSet = owned.contains(right) && (owned.contains(Reward.TOP) || owned.contains(Reward.ELITE_TOP))
				&& (owned.contains(Reward.ROBE) || owned.contains(Reward.ELITE_ROBE)) && owned.contains(Reward.GLOVES);
			return hasSet ? new VoidCheck(Kind.NOT_WORN, "Your void is not on",
				"You own a full set with the " + lower(right) + ": wear it for the " + styleName + " set bonus") : null;
		}
		if (helm == null || !top || !robe || !gloves)
		{
			List<String> missing = new ArrayList<>();
			if (helm == null)
			{
				missing.add(lower(right));
			}
			if (!top)
			{
				missing.add("top");
			}
			if (!robe)
			{
				missing.add("robe");
			}
			if (!gloves)
			{
				missing.add("gloves");
			}
			boolean ownsRest = true;
			if (helm == null && !owned.contains(right) || !top && !owned.contains(Reward.TOP) && !owned.contains(Reward.ELITE_TOP)
				|| !robe && !owned.contains(Reward.ROBE) && !owned.contains(Reward.ELITE_ROBE) || !gloves && !owned.contains(Reward.GLOVES))
			{
				ownsRest = false;
			}
			return new VoidCheck(Kind.INCOMPLETE, "Void set incomplete", "No " + join(missing) + " on, so no set bonus at all"
				+ (ownsRest ? ". You own the rest" : ""));
		}
		if (helm != right)
		{
			return new VoidCheck(Kind.WRONG_HELM, capital(lower(helm)) + " on a " + styleName + " weapon",
				owned.contains(right) ? "Swap to your " + lower(right) + " for the set bonus"
					: "Only the " + lower(right) + " helps a " + styleName + " weapon: " + right.getCost() + " points");
		}
		return new VoidCheck(Kind.SET_ON, elite ? "Elite void on" : "Void set on", capital(styleName) + " set bonus for this game");
	}

	/** "mage helm" from "Mage helm". */
	private static String lower(Reward r)
	{
		return r.getShortTitle().toLowerCase(Locale.ENGLISH);
	}

	private static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/** "top", "top or robe", "top, robe or gloves". */
	private static String join(List<String> parts)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < parts.size(); i++)
		{
			out.append(i == 0 ? "" : i == parts.size() - 1 ? " or " : ", ").append(parts.get(i));
		}
		return out.toString();
	}
}
