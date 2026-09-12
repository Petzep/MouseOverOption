package com.mouseoveroptions;

import javax.inject.Inject;
import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

/**
 * Adds back the "+N options" badge that the Steam / Enhanced OSRS client shows
 * underneath the mouseover (hover) tooltip, indicating how many extra
 * right-click options are available besides the one shown in the tooltip.
 *
 * RuneLite's vanilla "Mouse Highlight" plugin only shows the name/target of the
 * top menu entry (e.g. "Net Fishing spot") but drops the little counter badge
 * that used to sit underneath it. This plugin restores that counter as its own
 * lightweight tooltip, stacked below the existing one by RuneLite's tooltip
 * renderer.
 */
@PluginDescriptor(
		name = "Mouseover Options Count",
		description = "Shows a '+N options' badge under the hover tooltip, like the old Steam/Enhanced client, "
				+ "indicating how many extra right-click options are available",
		tags = {"tooltip", "menu", "options", "mouseover", "hover", "steam", "enhanced"}
)
public class MouseoverOptionsPlugin extends Plugin
{
	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseoverOptionsOverlay overlay;

	@Provides
	MouseoverOptionsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MouseoverOptionsConfig.class);
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
	}
}