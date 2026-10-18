package com.codex.pestcontrol;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CodexPestControlPluginRunner
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CodexPestControlPlugin.class);
		RuneLite.main(args);
	}
}
