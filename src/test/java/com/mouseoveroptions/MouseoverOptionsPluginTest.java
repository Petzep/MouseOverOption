package com.mouseoveroptions;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MouseoverOptionsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MouseoverOptionsPlugin.class);
		RuneLite.main(args);
	}
}