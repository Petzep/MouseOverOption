package com.mouseoveroptions;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
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
    private final Client client;
    private final TooltipManager tooltipManager;
    private final MouseoverOptionsConfig config;

    // Mirrors the vanilla client's own exclusion list for the
    // "<action> / N more options" counter.
    private static final Set<String> ALWAYS_IGNORED_OPTIONS = new HashSet<>(Arrays.asList(
            "walk here",
            "cancel",
            "continue",
            "move"
    ));

    /**
     * Menu actions that never represent a "real" extra option and should
     * never be counted, regardless of configuration.
     */
    private static final Set<MenuAction> ALWAYS_IGNORED_ACTIONS = EnumSet.of(
            MenuAction.WALK,
            MenuAction.CANCEL,
            MenuAction.RUNELITE,
            MenuAction.RUNELITE_OVERLAY,
            MenuAction.RUNELITE_OVERLAY_CONFIG
    );

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

        MenuEntry[] menuEntries = client.getMenu().getMenuEntries();
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

        // realOptions already excludes "Walk here"/"Cancel" etc, so the
        // primary action (shown by the name tooltip) is the only thing left
        // to subtract - this now matches the native "N more options" count.
        int extra = realOptions - 1;

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "All options: " + menuEntries.length + " filtered: " + realOptions, null);

        if (extra < Math.max(1, config.minimumOptions()))
        {
            return null;
        }

        // Add tooltip
        String label = "+" + extra + (extra == 1 ? " option" : " options");
        tooltipManager.add(new Tooltip(new ScaledOptionsBadge(label, config.textColor(), 0.8f)));

        return null;
    }

    private boolean isIgnored(MenuEntry entry)
    {
        if (ALWAYS_IGNORED_ACTIONS.contains(entry.getType()))
        {
            return true;
        }

        String option = entry.getOption();
        if (option == null || option.isEmpty() || ALWAYS_IGNORED_OPTIONS.contains(option.toLowerCase()))
        {
            return true;
        }

        // These two are now opt-in deviations from the native count, not
        // defaults - flip the config defaults to false if you want strict
        // parity with "Show mouseover text".
        if (!config.showOnWidgets() && entry.getWidget() != null)
        {
            return true;
        }
        if (!config.includeExamine() && isExamine(entry.getType()))
        {
            return true;
        }

        return false;
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

    private static class ScaledOptionsBadge implements LayoutableRenderableEntity
    {
        private static final int PADDING = 0;

        private final String text;
        private final Color color;
        private final float fontScale; // e.g. 0.8f ≈ enhanced client's smaller badge
        private Point location = new Point(0, 0);
        private Rectangle bounds = new Rectangle();

        private ScaledOptionsBadge(String text, Color color, float fontScale)
        {
            this.text = text;
            this.color = color;
            this.fontScale = fontScale;
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
            Font base = graphics.getFont();
            Font scaled = base.deriveFont(base.getSize2D() * fontScale);
            graphics.setFont(scaled);

            FontMetrics metrics = graphics.getFontMetrics();
            int width = metrics.stringWidth(text) + PADDING * 2;
            int height = metrics.getHeight() + PADDING * 2;

            // Right-align the badge under the cursor position TooltipOverlay gave us.
            int x = location.x;
            int y = location.y;

            graphics.setColor(new Color(0, 0, 0, 200));
            graphics.fillRect(x, y, width, height);
            graphics.setColor(color);
            graphics.drawString(text, x + PADDING, y + height - metrics.getDescent() - PADDING / 2);

            bounds = new Rectangle(x, y, width, height);
            return new Dimension(width, height);
        }

        @Override
        public Rectangle getBounds()
        {
            return bounds;
        }

        @Override
        public void setPreferredLocation(Point position)
        {
            this.location = position;
        }

        @Override
        public void setPreferredSize(Dimension dimension)
        {
            // size is derived from the scaled font, not overridable here
        }
    }
}