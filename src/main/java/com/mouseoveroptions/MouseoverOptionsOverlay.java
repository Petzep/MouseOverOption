package com.mouseoveroptions;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.EnumSet;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

/**
 * Adds a small "+N options" tooltip underneath the vanilla mouseover tooltip
 * whenever there are more right-click options available at the cursor than
 * the single option/name the tooltip already shows.
 * <p>
 * RuneLite draws every {@link Tooltip} added to the {@link TooltipManager}
 * in the same frame stacked vertically near the cursor, so adding our own
 * extra tooltip here is enough to have it appear directly below the one the
 * built-in "Mouse Highlight" plugin produces - no need to touch core code.
 */
public class MouseoverOptionsOverlay extends Overlay
{
    /**
     * Menu actions that never represent a "real" extra option and should
     * never be counted, regardless of configuration.
     */
    private static final Set<MenuAction> ALWAYS_IGNORED = EnumSet.of(
            MenuAction.WALK,
            MenuAction.CANCEL,
            MenuAction.RUNELITE,
            MenuAction.RUNELITE_OVERLAY,
            MenuAction.RUNELITE_OVERLAY_CONFIG
    );

    private final Client client;
    private final TooltipManager tooltipManager;
    private final MouseoverOptionsConfig config;

    @Inject
    private MouseoverOptionsOverlay(Client client, TooltipManager tooltipManager, MouseoverOptionsConfig config)
    {
        this.client = client;
        this.tooltipManager = tooltipManager;
        this.config = config;
        setPosition(OverlayPosition.TOOLTIP);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Don't show the badge while the real right-click menu is open.
        if (client.isMenuOpen())
        {
            return null;
        }

        MenuEntry[] menuEntries = client.getMenuEntries();
        if (menuEntries == null || menuEntries.length == 0)
        {
            return null;
        }

        int realOptions = 0;

        for (MenuEntry entry : menuEntries)
        {
            if (isIgnored(entry))
            {
                continue;
            }

            realOptions++;
        }

        // The top/primary option is already displayed by the name tooltip,
        // so only count what's left over as "extra" options.
        int extra = realOptions - 1;

        if (extra < Math.max(1, config.minimumOptions()))
        {
            return null;
        }

        String label = "+" + extra + (extra == 1 ? " option" : " options");
        String colored = ColorUtil.wrapWithColorTag(label, config.textColor());

        tooltipManager.add(new Tooltip(colored));

        // This overlay doesn't draw anything itself - the TooltipManager's
        // own overlay renders the tooltip we just queued.
        return null;
    }

    private boolean isIgnored(MenuEntry entry)
    {
        MenuAction type = entry.getType();

        if (ALWAYS_IGNORED.contains(type))
        {
            return true;
        }

        if (!config.showOnWidgets() && entry.getWidget() != null)
        {
            return true;
        }

        if (!config.includeExamine() && isExamine(type))
        {
            return true;
        }

        String option = entry.getOption();
        if (option == null || option.isEmpty())
        {
            return true;
        }

        return option.equalsIgnoreCase("Walk here") || option.equalsIgnoreCase("Cancel");
    }

    private boolean isExamine(MenuAction type)
    {
        switch (type)
        {
            case EXAMINE_OBJECT:
            case EXAMINE_NPC:
            case EXAMINE_ITEM_GROUND:
            case EXAMINE_ITEM:
                return true;
            default:
                return false;
        }
    }
}