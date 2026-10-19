package com.codex.pestcontrol.core;

import java.util.List;

/** The damaged forms, for tests outside this package. */
public final class DamagedAccess
{
	private DamagedAccess()
	{
	}

	public static List<String> of(String good)
	{
		return Damaged.of(good);
	}

	public static String show(String s)
	{
		return Damaged.show(s);
	}
}
