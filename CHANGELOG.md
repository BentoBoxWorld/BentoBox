# BentoBox Changelog

## [3.12.0] — 2026-03-28

### New Release Highlights

* 🗺️ ⚙️ **Web-map integrations** — BentoBox now ships with built-in **BlueMap** and **Dynmap** hooks that display island markers and area overlays on your web map automatically. A public Map API lets addon authors target any web-map plugin.
* ⚙️ 🔡 **SPAWN_PROTECTION world flag** — Prevents players from dying to the void at spawn. Configurable per game-mode via the world settings panel.
* ⚙️ 🔡 **WIND_CHARGE protection flag** — Controls whether visitors can fire wind charges on islands. Defaults to blocked for visitors.
* ⚙️ 🔡 **Expel fallback command** — A new `expelCommand` config key (default: `spawn`) specifies the command run when an expelled player has no island or spawn point to fall back to.
* ⚙️ 🔡 **Blueprint paste commands** — Blueprint bundles can define a list of commands that run when the blueprint is pasted, enabling automation on island creation.
* 🔡 **Blueprint GUI pagination** — The blueprint management panel now paginates so large blueprint libraries are no longer cramped onto a single screen.
* ⚙️ 🔡 **Admin max team-size command** — A new `/bbox admin setmaxteamsize <player> <size>` command lets you override the maximum team size on a per-island basis without editing config files.
* 🔡 **Clickable home list** — The `/is homes` list is now clickable; selecting a home name in chat runs the go-home command automatically.
* ⚙️ 🔡 **Force-field fling** — Players are now flung away from island force-fields when they walk into them, rather than just being teleported (improves feel on flying game modes).
* ⚙️ **Obsidian-scooping abuse fix** — Two new config knobs (`obsidianScoopingCooldown` and `obsidianScoopingRadius`) prevent rapid lava/obsidian duplication exploits.

### Compatibility

✔️ Paper Minecraft 1.21.5 – 1.21.1  
✔️ Java 21

### Upgrading

1. **As always, take backups just in case.** (Make a copy of everything!)
2. Stop the server.
3. Replace the BentoBox jar with this one.
4. Restart the server.
5. You should be good to go!

> ⚙️ **Config migration note:** Three new keys are added to `config.yml` (`expelCommand`, `obsidianScoopingRadius`, `obsidianScoopingCooldown`). BentoBox will add them automatically on first start.

### Legend

- 🔡 locale files may need to be regenerated or updated.
- ⚙️ config options have been removed, renamed, or added.
- 🔺 special attention needed.

---

## New Features

### ⚙️ BlueMap & Dynmap Web-Map Integration
[[PR #2861](https://github.com/BentoBoxWorld/BentoBox/pull/2861)] [[PR #2883](https://github.com/BentoBoxWorld/BentoBox/pull/2883)] [[PR #2884](https://github.com/BentoBoxWorld/BentoBox/pull/2884)] [[PR #2885](https://github.com/BentoBoxWorld/BentoBox/pull/2885)]

BentoBox now ships with hooks for [BlueMap](https://bluemap.bluecolored.de/) and [Dynmap](https://www.spigotmc.org/resources/dynmap.274/). When either plugin is installed, island markers and (for BlueMap) coloured area overlays are added to the live web map automatically, without any configuration required.

A new generic **Map API** (`world.bentobox.bentobox.api.map`) lets addon authors register their own web-map implementations, so any mapping plugin can be supported in the future.

Additional Map API polish after initial merge:
- Map hooks now register before addons enable, so addons can create markers during `onEnable()`. Island population is deferred to `BentoBoxReadyEvent` when islands are fully loaded.
- `addPointMarker()` now accepts an `iconName` parameter; Dynmap maps this to its icon registry (with fallback to `"default"`) and all 85 built-in Dynmap icon names are documented in `MapManager` Javadoc.
- Dynmap point marker labels support HTML markup (e.g. coloured owner names) via `isMarkupLabel=true`.

### ⚙️ 🔡 SPAWN_PROTECTION World Setting Flag
[[PR #2865](https://github.com/BentoBoxWorld/BentoBox/pull/2865)]

A new `SPAWN_PROTECTION` world setting flag prevents players at spawn from falling into the void. When enabled, the spawn area is treated as protected ground and void-death is suppressed. Useful for game modes where the spawn platform is exposed.

### ⚙️ 🔡 WIND_CHARGE Protection Flag
[[PR #2855](https://github.com/BentoBoxWorld/BentoBox/pull/2855)]

A new `WIND_CHARGE` island protection flag controls who can use wind charge items on an island. By default visitors are blocked, preventing griefing through knockback.

### ⚙️ 🔡 Expel Fallback Command
[[PR #2846](https://github.com/BentoBoxWorld/BentoBox/pull/2846)]

A new `expelCommand` setting in `config.yml` (default: `spawn`) specifies the console command run when an expelled player has no home island and no known spawn point. Previously such players would be stuck.

### ⚙️ Blueprint Paste Commands
[[PR #2852](https://github.com/BentoBoxWorld/BentoBox/pull/2852)]

Blueprint bundles now support an optional `commands` list. Each command is run (as the server console) when the bundle's blueprint is pasted, making it easy to trigger automation or economy actions on island creation.

### 🔡 Blueprint GUI Pagination
[[PR #2867](https://github.com/BentoBoxWorld/BentoBox/pull/2867)]

The blueprint management GUI now paginates. Servers with large numbers of blueprints will see next/previous page navigation buttons instead of a single overflowing panel.

### ⚙️ 🔡 Admin Max Team-Size Command
[[PR #2851](https://github.com/BentoBoxWorld/BentoBox/pull/2851)] [[PR #2854](https://github.com/BentoBoxWorld/BentoBox/pull/2854)]

A new `/bbox admin setmaxteamsize <player> <size>` command lets server admins override the maximum team size for a specific island at runtime, without touching config files.

### 🔡 Clickable Home List in Chat
[[PR #2879](https://github.com/BentoBoxWorld/BentoBox/pull/2879)]

The `/is homes` listing in chat is now clickable. Clicking a home name runs the go-home command for that home automatically, saving players from typing.

### 🔡 Force-Field Fling
[[PR #2122](https://github.com/BentoBoxWorld/BentoBox/pull/2122)] [[PR #2880](https://github.com/BentoBoxWorld/BentoBox/pull/2880)]

Players who walk into an island force-field (locked island boundary) are now physically flung back rather than teleported, giving a more natural feel, especially on flying game modes.

### 🔡 Bypass Lock Notification
[[PR #2869](https://github.com/BentoBoxWorld/BentoBox/pull/2869)]

Admins and players with the bypass permission now receive a notification message when they enter a locked island, so it is clear that the lock has been overridden.

### Geo-Limit Projectiles
[[PR #2863](https://github.com/BentoBoxWorld/BentoBox/pull/2863)]

Projectiles (arrows, tridents, etc.) are now included in the geo-limit settings panel, giving island owners granular control over which projectile types visitors are allowed to fire.

### Paginated Help Command
[[PR #2859](https://github.com/BentoBoxWorld/BentoBox/pull/2859)]

`/is help` now supports a page number argument (`/is help 2`) so large command lists do not flood chat.

### /island lock Command
[[PR #2858](https://github.com/BentoBoxWorld/BentoBox/pull/2858)]

A new `/island lock` shortcut command lets island owners toggle the island lock without opening the settings panel.

---

## Bug Fixes

### Lava Bucket / Obsidian Duplication
[[PR #2842](https://github.com/BentoBoxWorld/BentoBox/pull/2842)] [[PR #2856](https://github.com/BentoBoxWorld/BentoBox/pull/2856)] [[PR #2860](https://github.com/BentoBoxWorld/BentoBox/pull/2860)]

Fixed an exploit where players could rapidly scoop buckets of lava from obsidian to duplicate it. A per-player cooldown (`obsidianScoopingCooldown`, default: 1 minute) and a proximity radius check (`obsidianScoopingRadius`, default: 5 blocks) now prevent abuse. Both values are configurable in `config.yml`.

### Player XP Not Resetting
[[PR #2866](https://github.com/BentoBoxWorld/BentoBox/pull/2866)]

Fixed a bug where player XP was not being reset when joining a team or creating a new island via the no-teleport code path.

### Sugar Cane, Cocoa Beans & Nether Wart Protection Flags
[[PR #2870](https://github.com/BentoBoxWorld/BentoBox/pull/2870)]

Sugar cane was not registered under the `HARVEST` flag and was not protected against premature breaking. Sugar cane, cocoa beans, and nether wart were also missing from `CROP_PLANTING` protection. They now all respect the correct flag settings.

### Purge Unowned Islands NPE
[[PR #2843](https://github.com/BentoBoxWorld/BentoBox/pull/2843)]

Fixed a `NullPointerException` in the purge-unowned-islands command that occurred when an island's world was `null`.

### End/Nether Explosion Crash
[[PR #2844](https://github.com/BentoBoxWorld/BentoBox/pull/2844)]

Fixed a `NullPointerException` in `StandardSpawnProtectionListener` that occurred when an explosion happened in a standard End or Nether world that was not managed by BentoBox.

### Island Settings Comparator
[[PR #2864](https://github.com/BentoBoxWorld/BentoBox/pull/2864)]

Fixed an inconsistent comparator in the island settings panel that could throw a `IllegalArgumentException` under certain flag ordering conditions.

### Color Codes in Multi-Line Translated Strings
[[PR #2877](https://github.com/BentoBoxWorld/BentoBox/pull/2877)]

Fixed color/formatting codes being stripped from the second and subsequent lines of multi-line locale strings.

### Players Falling into the Void on New Island Creation
[[PR #2890](https://github.com/BentoBoxWorld/BentoBox/pull/2890)]

Fixed a bug where players could fall into the void immediately after a new island was created if the teleport destination hadn't solidified yet. `homeTeleportAsync` now performs a safe-spot check before teleporting and retries with exception handling if the location lookup fails, preventing void deaths on fresh islands.

---

## Configuration Changes

Three new settings in `config.yml`:

| Key | Default | Description |
|-----|---------|-------------|
| `expelCommand` | `spawn` | Console command run when an expelled player has no destination |
| `obsidianScoopingRadius` | `5` | Block radius (0–15) checked for nearby obsidian during bucket use |
| `obsidianScoopingCooldown` | `1` | Minutes before a player can scoop obsidian again (minimum: 1) |

---

## Internal / Developer Changes

* Added a generic **Map API** (`api/map`) so addon authors can integrate with any web-map plugin.
* Extensive SonarCloud quality pass: sealed classes, `ChatColor` removal, lambda/`@Override` style, variable-shadowing fixes, cognitive-complexity reductions (see [PR #2875](https://github.com/BentoBoxWorld/BentoBox/pull/2875)).
* Removed unnecessary `public` modifiers from JUnit 5 test classes and methods ([PR #2849](https://github.com/BentoBoxWorld/BentoBox/pull/2849)).
* Added `CLAUDE.md` project guidance file for AI-assisted development ([PR #2848](https://github.com/BentoBoxWorld/BentoBox/pull/2848)).
* Resolved all previously failing and skipped tests; test suite is now fully green ([PR #2872](https://github.com/BentoBoxWorld/BentoBox/pull/2872)).
* Added public API method for reading Why-debug flag messages from `FlagListener` ([PR #2857](https://github.com/BentoBoxWorld/BentoBox/pull/2857)).

---

## What's Changed

* Fix lava bucket duplication exploit with obsidian scooping cooldown by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2842
* Fix NPE in purge unowned command when island world is null by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2843
* Fix NPE in StandardSpawnProtectionListener for end/nether explosions by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2844
* Fix error message when setting home by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2845
* ⚙️ 🔡 Add expelCommand config for expelled player fallback by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2846
* Add Copilot instructions setup by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2848
* chore: remove public modifiers from JUnit 5 test methods by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2849
* ⚙️ Run commands when blueprint bundle is pasted by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2852
* Fix black glass description by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2853
* ⚙️ 🔡 Add AdminTeamSetMaxSizeCommand by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2851
* ⚙️ 🔡 Show team size in admin info and add coop/trust placeholders by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2854
* ⚙️ 🔡 Add WIND_CHARGE island protection flag by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2855
* ⚙️ Add configurable obsidian scooping radius by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2856
* Add public API for Why debug reporting in FlagListener by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2857
* 🔡 Add /island lock command by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2858
* 🔡 Add pagination to DefaultHelpCommand by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2859
* ⚙️ Add configurable obsidian scooping cooldown duration by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2860
* ⚙️ Add BlueMap hook for island markers by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2861
* Add projectile support to geo-limit-settings by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2863
* Fix island settings comparator by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2864
* ⚙️ 🔡 Add SPAWN_PROTECTION world setting flag by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2865
* Fix player XP not resetting on team join or island creation by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2866
* 🔡 Add pagination to blueprint management GUI by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2867
* Fix BSkyBlock end world protection by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2868
* 🔡 Add bypass lock notification message by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2869
* Fix sugar cane / cocoa beans / nether wart protection flags by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2870
* Fix all failing and skipped tests by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2872
* Fix BlueMap hook registration error by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2874
* Fix SonarCloud issues (low / medium / high) by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2875
* Fix color codes in multi-line translated strings by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2877
* 🔡 Make home list clickable in chat by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2879
* Merge force-field fling feature by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2880
* Fix translation hover text by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2882
* ⚙️ Add Dynmap hook for island border display by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2883
* Add area markers and public API to BlueMapHook by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2884
* ⚙️ Add generic Map API for web-map addon integration by @tastybento in https://github.com/BentoBoxWorld/BentoBox/pull/2885
* Fix players falling into void on new island creation by @Copilot in https://github.com/BentoBoxWorld/BentoBox/pull/2890
* Release 3.12.0 by @tastybento

**Full Changelog**: https://github.com/BentoBoxWorld/BentoBox/compare/3.11.2...3.12.0
