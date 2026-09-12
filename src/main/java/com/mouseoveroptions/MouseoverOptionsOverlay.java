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
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.Text;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.ui.overlay.components.ComponentConstants;

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

        // Skip if no primary action is shown
        // Last item is the top priority
        MenuEntry primaryEntry = menuEntries[menuEntries.length - 1];
        if (primaryEntry.isDeprioritized())
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

        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "All options: " + menuEntries.length + " filtered: " + realOptions, null);

        if (realOptions < Math.max(1, config.minimumOptions()))
        {
            return null;
        }

        String optionText = primaryEntry.getTarget().isEmpty() ? "" : primaryEntry.getOption();
        String targetText = primaryEntry.getTarget() == null ? "" : primaryEntry.getTarget();

        // Join strings with space if both are non-empty
        String primaryText = !optionText.isEmpty() && !targetText.isEmpty()
                ? optionText + " " + targetText
                : optionText + targetText;

        String sanitizedText = Text.removeTags(primaryText);
        int size = sanitizedText.length();
        //client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Primary text: [" + Text.escapeJagex(primaryText) + "], Sanitized text: [" + sanitizedText + "]  size: [" + size + "]", null);
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Sanitized text: [" + sanitizedText + "]  size: [" + size + "]", null);

        // Add tooltip
        String label = "+" + realOptions + (realOptions == 1 ? " option" : " options");
        tooltipManager.add(new Tooltip(new ScaledOptionsBadge(primaryText, label, config.textColor(), 0.75f)));

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
            case EXAMINE_WORLD_ENTITY:
                return true;
            default:
                return false;
        }
    }

    private static class ScaledOptionsBadge implements LayoutableRenderableEntity
    {
        private static final int PADDING = 0;

        private final String primaryText;
        private final String text;
        private final Color textColor;
        private final float fontScale;
        private Point location = new Point(0, 0);
        private Rectangle bounds = new Rectangle();

        private ScaledOptionsBadge(String primaryText, String text, Color textColor, float fontScale)
        {
            this.primaryText = Text.removeTags(primaryText);
            this.text = text;
            this.textColor = textColor;
            this.fontScale = fontScale;
        }

        @Override
        public Dimension render(Graphics2D graphics)
        {
            Font base = graphics.getFont();
            Font scaled = base.deriveFont(base.getSize2D() * fontScale);
            graphics.setFont(scaled);
            FontMetrics metrics = graphics.getFontMetrics();

            // Calculated width of the primary menu item
            Font primaryFont = FontManager.getRunescapeSmallFont();
            int primaryWidth = graphics.getFontMetrics(primaryFont).stringWidth(primaryText) + 2 * ComponentConstants.STANDARD_BORDER - 1;

            int width = metrics.stringWidth(text) + PADDING * 2;
            int height = metrics.getHeight() + PADDING * 2;

            // Right-align the badge under the cursor position TooltipOverlay gave us.
            int x = location.x + Math.max(primaryWidth - width, 0);
            int y = location.y - (ComponentConstants.STANDARD_BORDER / 2);

            graphics.setColor(Color.BLACK);
            graphics.drawRect(x,y, width, height);
            graphics.setColor(ComponentConstants.STANDARD_BACKGROUND_COLOR);
            graphics.fillRect(x, y, width, height);
            graphics.setColor(textColor);
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