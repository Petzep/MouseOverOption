# Mouseover Options Count

Restores the "+N options" badge that the Steam / Enhanced OSRS client shows under the mouseover tooltip, indicating how many extra right-click options are available besides the one shown in the tooltip name.

RuneLite's built-in "Mouse Highlight" plugin only shows the top action's target text (e.g. "Net Fishing spot"). It drops the small counter that used to sit underneath telling you there were more options behind a right-click. This plugin adds that counter back as its own tooltip, which RuneLite automatically stacks directly below the existing hover tooltip.

## How it works

- Every frame, the overlay reads `Client.getMenuEntries()`, the same array the vanilla client and RuneLite's Mouse Highlight plugin use to build the hover tooltip and right-click menu.
- It filters out entries that don't represent a "real" extra option: `Walk here`, `Cancel`, RuneLite-injected entries, and (by default) `Examine`.
- If more than one real option remains, it subtracts one (the option already shown in the name tooltip) and adds a `Tooltip("+N options")` to the `TooltipManager`.
- Because RuneLite's `TooltipOverlay` renders every queued `Tooltip` stacked vertically near the cursor in the same frame, the badge appears right under the existing hover tooltip with no core-client changes needed.
- The badge is suppressed while an actual right-click menu is open (`Client.isMenuOpen()`).

## Configuration

| Option | Default | Description |
|---|---|---|
| Minimum extra options | 1 | Badge only appears once at least this many extra options exist. |
| Count Examine | off | Whether "Examine" counts toward the extra-options total. |
| Show on interface widgets | off | Show the badge over bank/inventory/shop widgets too, not just the game world. |
| Text color | white | Color of the "+N options" text. |

## Building

This follows the standard RuneLite external-plugin layout:

```
mouseover-options-count/
├── build.gradle
├── runelite-plugin.properties
└── src/main/java/com/mouseoveroptions/
    ├── MouseoverOptionsPlugin.java
    ├── MouseoverOptionsConfig.java
    └── MouseoverOptionsOverlay.java
```

1. Create a new repository with this structure (use the official [`runelite-example-plugin`](https://github.com/runelite/example-plugin) template as your `settings.gradle`/wrapper scaffold if you don't already have one).
2. Copy in the four files delivered here (adjust `author=` in `runelite-plugin.properties`).
3. Build and sideload it in the RuneLite plugin developer environment (IntelliJ run configuration `RuneLite` with `-ea -Dnet.runelite.developer.mode=true` and this project on the classpath) to test it in-game.
4. Once it works, publish it to the community Plugin Hub by following the [plugin-hub submission guide](https://github.com/runelite/plugin-hub#readme):
    - Push this plugin to its own public GitHub repository.
    - Fork [`runelite/plugin-hub`](https://github.com/runelite/plugin-hub), add a file under `plugins/` named after your plugin, containing `repository=<your repo URL>` and `commit=<40-char commit hash>`.
    - Open a pull request against `plugin-hub`; RuneLite's CI verifies the plugin compiles and meets hub guidelines (unique plugin name, no `net.runelite` package usage, reasonable permissions, etc.) before it's approved and distributed via the in-client Plugin Hub.

## Notes / tuning

The "extra options" heuristic is intentionally conservative (ignoring `Walk here`, `Cancel`, and `Examine` by default) to mirror what the Steam client badge counted. If you find it over- or under-counts for certain objects (e.g. multi-option fishing spots, doors, bank booths), tweak the filters in `MouseoverOptionsOverlay.isIgnored()` — that's the single place the counting logic lives.