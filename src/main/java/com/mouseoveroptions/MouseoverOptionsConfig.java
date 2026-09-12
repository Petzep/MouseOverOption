package com.mouseoveroptions;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mouseoveroptions")
public interface MouseoverOptionsConfig extends Config
{
    @ConfigItem(
            keyName = "minimumOptions",
            name = "Minimum extra options",
            description = "The badge is only shown once at least this many extra options are available.",
            position = 1
    )
    default int minimumOptions()
    {
        return 1;
    }

    @ConfigItem(
            keyName = "includeExamine",
            name = "Count Examine",
            description = "Whether the 'Examine' entry counts towards the extra options total.",
            position = 2
    )
    default boolean includeExamine()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showOnWidgets",
            name = "Show on interface widgets",
            description = "Show the badge while hovering interface widgets (bank, inventory, shops, etc), "
                    + "not just objects/NPCs/ground items in the game world.",
            position = 3
    )
    default boolean showOnWidgets()
    {
        return true;
    }

    @ConfigItem(
            keyName = "textColor",
            name = "Text color",
            description = "Color used for the '+N options' text.",
            position = 4
    )
    default Color textColor()
    {
        return Color.WHITE;
    }
}