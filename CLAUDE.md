# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BentoBox is a Bukkit/Paper library plugin (Java 25) that provides the core platform for island-style Minecraft games (SkyBlock, AcidIsland, etc.) via an extensible addon system.

## Build Commands

```bash
./gradlew build              # Build the shaded JAR
./gradlew test               # Run all tests
./gradlew clean build        # Clean then build
./gradlew jacocoTestReport   # Generate coverage report (build/reports/jacoco/)
```

### Running a Single Test

```bash
# Run all tests in a class
./gradlew test --tests "world.bentobox.bentobox.managers.IslandsManagerTest"

# Run a specific test method
./gradlew test --tests "world.bentobox.bentobox.managers.IslandsManagerTest.testMethodName"
```

## Architecture

The main plugin class is `BentoBox.java` (extends `JavaPlugin`). Almost all subsystems are accessed via singleton managers held by the plugin instance.

### Key Packages

- **`api/`** — Public API surface for addons: events, commands, panels (GUIs), user management, flags, configuration
- **`managers/`** — Core subsystems: `IslandsManager`, `PlayersManager`, `AddonsManager`, `LocalesManager`, `FlagsManager`, `BlueprintsManager`, `BlueprintClipboardManager`, `HooksManager`, `PlaceholdersManager`, `RanksManager`, `CommandsManager`, `IslandDeletionManager`, `IslandChunkDeletionManager`, `MapManager`, `WebManager`
- **`database/`** — Database abstraction supporting MongoDB, MySQL, MariaDB, PostgreSQL (via HikariCP), and SQLite
- **`blueprints/`** — Island schematic handling and pasting
- **`listeners/`** — Bukkit event handlers (teleport, death, join/leave, panel clicks, spawn protection)
- **`commands/`** — Admin and user command implementations
- **`panels/`** — Inventory GUI panel system
- **`hooks/`** — Integrations with external plugins (Vault, PlaceholderAPI, MythicMobs, Multiverse, LuckPerms, ItemsAdder, Slimefun, Oraxen, ZNPCsPlus, FancyNpcs, BlueMap, Dynmap, LangUtils, etc.)
- **`nms/`** — NMS (Native Minecraft Server) version-specific code

### Island Data Flow

Islands are the central domain object. `IslandsManager` owns the island cache and database layer. `IslandWorldManager` holds per-world configuration. Protection logic is handled via `FlagsManager` and a rank system (`RanksManager`).

### Addon System

Addons (separate plugins) hook into BentoBox through the `api/` package. They register commands, flags, events, and panels by accessing managers through `BentoBox.getInstance()`.

### Flag System

Flags are the core protection/setting mechanism. There are three types:
- `PROTECTION` — player action blocked by rank (e.g., BLOCK_BREAK)
- `SETTING` — island-level on/off toggle (e.g., ANIMAL_SPAWNING)
- `WORLD_SETTING` — server-level toggle, admin only

To write a protection listener, extend `FlagListener`:

```java
public class MyListener extends FlagListener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSomeEvent(SomeEvent e) {
        checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.BLOCK_BREAK);
    }
}
```

`checkIsland()` handles rank comparison, event cancellation, and player notification automatically. All protection flag listeners live in `listeners/flags/protection/`.

### Key API Patterns

```java
// Island permission check
island.isAllowed(user, flag);

// Localized player messaging (never use player.sendMessage() directly)
user.sendMessage("protection.protected");

// Island lookup
Optional<Island> island = plugin.getIslands().getIslandAt(location);

// All managers accessed via singleton
plugin.getIslands()       // IslandsManager
plugin.getIWM()           // IslandWorldManager
plugin.getFlagsManager()  // FlagsManager
```

## Testing Patterns

The test suite uses JUnit 5 + Mockito + MockBukkit. **Almost every test class extends `CommonTestSetup`**, which pre-wires ~20 mocks:

- `plugin` — mocked `BentoBox` instance
- `mockPlayer`, `world`, `location`, `island` — standard game objects
- `iwm` (`IslandWorldManager`), `im` (`IslandsManager`), `lm` (`LocalesManager`), `fm` (`FlagsManager`), `hooksManager`

Use `CommonTestSetup` as the base for new tests. Call `super.setUp()` in `@BeforeEach` and `super.tearDown()` in `@AfterEach` if overriding. The `checkSpigotMessage(String)` helper asserts messages sent to the player.

Test resources and temporary database files are cleaned up automatically by the base class teardown.

## Public API Compatibility

BentoBox is a **plugin platform** — its public API is compiled against by many external addons. Binary-incompatible changes cause `NoSuchMethodError` at runtime for all addons until they recompile.

### Binary-incompatible changes (avoid without a semver-major release)
- Changing the return type of a public method (the JVM encodes return type in the method descriptor; two methods cannot share name+params with different return types)
- Removing or renaming public methods/classes
- Adding required parameters to existing public methods

### SonarCloud rules vs. API stability
Automated rules (e.g. S4738 "Replace Guava types with Java stdlib") are appropriate for internal code but **not** for public API methods whose return type is part of the binary contract. Suppress selectively with a comment:

```java
@SuppressWarnings("java:S4738") // ImmutableSet is intentional public API; changing return type is binary-incompatible
public ImmutableSet<UUID> getMemberSet() { ... }
```

Guava (`ImmutableSet`, `ImmutableList`, etc.) is reliably available at runtime via Paper's bundled JARs and is safe to use in the public API.

## MiniMessage / legacy color round-trip

`User.getTranslation()` returns a legacy `§`-coded string for backwards compatibility, even when the locale entry is MiniMessage. UI code (`PanelItem.setDescription`, etc.) then re-parses that legacy string back into a Component via `Util.parseMiniMessageOrLegacy`. This MiniMessage → Component → legacy → Component round-trip is lossy by default because of an Adventure quirk:

**Adventure's `LegacyComponentSerializer` never emits `§r` to turn off a decoration when a sibling component clears it.** Legacy color codes have no "decoration off" code — only `§r` resets — but Adventure simply omits the decoration code on the next sibling instead of resetting. When that legacy string is re-parsed under correct legacy semantics (decorations persist until `§r`), the decoration leaks into the following segment. This bit bold, italic, underlined, strikethrough, and obfuscated equally (#2917).

`Util.componentToLegacy` is therefore **not** a thin wrapper around Adventure's serializer — it's a custom Component walker (`appendComponentLegacy` / `emitStyleTransition`) that tracks the last-emitted color and decorations and inserts `§r` whenever any decoration was on and is now off, then re-applies color afterwards. **Do not replace it with `LegacyComponentSerializer.serialize()` directly** without re-introducing the leak. The round-trip is exercised by `LegacyToMiniMessageTest`.

### Multi-line strings must be parsed as a single unit

`User.convertToLegacy` parses the **whole** translated string at once — never per-line. MiniMessage tags can span newlines (e.g. a `<green>...\n...</green>` block from a multi-line YAML entry, or a multi-line value substituted into a `<green>[description]</green>` template). Splitting on `\n` before parsing orphans close tags: the line `bar</green>` has no opening, and MiniMessage renders `</green>` as **literal text** in the lore. Adventure preserves newlines through `text.content()`, so a single parse handles everything correctly.

### Locale templates: do not wrap placeholders in MiniMessage tags

A template like `<green>[description]</green>` looks harmless but is a trap. Translation placeholders are substituted **as legacy `§`-coded strings** before re-parsing, and they may contain their own colors and newlines. Wrapping them re-introduces the multi-line orphaning problem above and forces the wrapper color over content that already has its own. Leave placeholders bare (`[description]`) and let the value bring its own colors. The `protection.panel.flag-item.{description,menu,setting}-layout` keys all follow this rule across every bundled locale.

### Splitting legacy strings on a literal character collapses same-color runs

`componentToLegacy` does not re-emit a color code when an adjacent text segment has the same color — it relies on the §-code carrying over within the contiguous string. Code that takes a translated legacy string and then `.split("\\|")` (or any literal-character split) breaks this carry-over: subsequent segments lose their color prefix and render in default. If a panel uses `|`-as-line-separator on a translated value, it must propagate the active `§color`/`§format` codes across the split itself, or set lore via Adventure `Component`s instead of legacy `String`s. (See `addon-level/.../DonationPanel.java#splitWithStyleCarryover` for a working pattern.) Bukkit's deprecated `meta.setLore(List<String>)` also does not suppress Minecraft's default lore italic — `meta.lore(List<Component>)` with the `removeDefaultItalic` helper does.

## Build Notes

- The Gradle build uses the Paper `userdev` plugin and Shadow plugin to produce a fat/shaded JAR at `build/libs/BentoBox-{version}.jar`.
- `plugin.yml` and `config.yml` are filtered for the `${version}` placeholder at build time; locale files are copied without filtering.
- Java preview features are enabled for both compilation and test execution.
- Local builds produce version `{buildVersion}-LOCAL-SNAPSHOT` (current: `3.18.0-LOCAL-SNAPSHOT`); CI builds append `-b{BUILD_NUMBER}-SNAPSHOT`; `origin/master` builds produce the bare version. The authoritative version is `buildVersion` in `build.gradle.kts`.

### Minecraft 26.x / Java 25 toolchain

Supporting Minecraft 26.x forced a chain of build changes — keep these in mind before touching versions in `build.gradle.kts`:

- **Java 25.** The 26.x `paper-api` is Java 25 bytecode and its Gradle metadata requires consumers to target Java 25, so BentoBox now compiles to Java 25 (`javaVersion = "25"`, `options.release = 25`). **Addons that compile against BentoBox must also move to Java 25.**
- **paperweight `2.0.0-SNAPSHOT`.** All 26.x dev bundles are dev-bundle *data version 8*, which no released paperweight (`<= 2.0.0-beta.21`) can read. The snapshot is resolved via a `pluginManagement` block in `settings.gradle.kts` pointing at Paper's repo. The paperweight tool launcher is pinned to Java 25 (the 26.1+ paperclip patch step requires it). Revisit once a stable paperweight reads data-version-8 bundles.
- **Compile target vs. runtime support.** `paperVersion` is the latest **stable 26.1.2** dev bundle, not 26.2 — because MockBukkit has no 26.2 build and its registry mock throws on 26.2's new `minecraft:sulfur_cube_archetype` registry. Minecraft **26.2 is supported at runtime** (see `ServerCompatibility` and the Modrinth `game-versions` list); 26.2-only blocks/entities are referenced via `Enums.getIfPresent(...)` by name, never a compile-time symbol. The forward "compile against literal 26.2" work is parked in a draft PR until MockBukkit ships a 26.2 build.
- **MockBukkit coordinate.** Tests use `org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:<ver>` (from Paper's repo), which **must match `paperVersion`'s MC line** — a mismatched MockBukkit fails every test at init with `InternalDataLoadException` (it validates the live API's registries against its bundled per-version data). When bumping the MC version, bump both together.

## Dependency Source Lookup

When you need to inspect source code for a dependency (e.g., BentoBox, addons):

1. **Check local Maven repo first**: `~/.m2/repository/` — sources jars are named `*-sources.jar`
2. **Check the workspace**: Look for sibling directories or Git submodules that may contain the dependency as a local project (e.g., `../bentoBox`, `../addon-*`)
3. **Check Maven local cache for already-extracted sources** before downloading anything
4. Only download a jar or fetch from the internet if the above steps yield nothing useful

Prefer reading `.java` source files directly from a local Git clone over decompiling or extracting a jar.

In general, the latest version of BentoBox should be targeted.

## Project Layout

Related projects are checked out as siblings under `~/git/`:

**Core:**
- `bentobox/` — core BentoBox framework

**Game modes:**
- `addon-acidisland/` — AcidIsland game mode
- `addon-bskyblock/` — BSkyBlock game mode
- `Boxed/` — Boxed game mode (expandable box area)
- `CaveBlock/` — CaveBlock game mode
- `OneBlock/` — AOneBlock game mode
- `SkyGrid/` — SkyGrid game mode
- `RaftMode/` — Raft survival game mode
- `StrangerRealms/` — StrangerRealms game mode
- `Brix/` — plot game mode
- `parkour/` — Parkour game mode
- `poseidon/` — Poseidon game mode
- `gg/` — gg game mode

**Addons:**
- `addon-level/` — island level calculation
- `addon-challenges/` — challenges system
- `addon-welcomewarpsigns/` — warp signs
- `addon-limits/` — block/entity limits
- `addon-invSwitcher/` / `invSwitcher/` — inventory switcher
- `addon-biomes/` / `Biomes/` — biomes management
- `Bank/` — island bank
- `Border/` — world border for islands
- `Chat/` — island chat
- `CheckMeOut/` — island submission/voting
- `ControlPanel/` — game mode control panel
- `Converter/` — ASkyBlock to BSkyBlock converter
- `DimensionalTrees/` — dimension-specific trees
- `discordwebhook/` — Discord integration
- `Downloads/` — BentoBox downloads site
- `DragonFights/` — per-island ender dragon fights
- `ExtraMobs/` — additional mob spawning rules
- `FarmersDance/` — twerking crop growth
- `GravityFlux/` — gravity addon
- `Greenhouses-addon/` — greenhouse biomes
- `IslandFly/` — island flight permission
- `IslandRankup/` — island rankup system
- `Likes/` — island likes/dislikes
- `Limits/` — block/entity limits
- `lost-sheep/` — lost sheep adventure
- `MagicCobblestoneGenerator/` — custom cobblestone generator
- `PortalStart/` — portal-based island start
- `pp/` — pp addon
- `Regionerator/` — region management
- `Residence/` — residence addon
- `TopBlock/` — top ten for OneBlock
- `TwerkingForTrees/` — twerking tree growth
- `Upgrades/` — island upgrades (Vault)
- `Visit/` — island visiting
- `weblink/` — web link addon
- `CrowdBound/` — CrowdBound addon

**Data packs:**
- `BoxedDataPack/` — advancement datapack for Boxed

**Documentation & tools:**
- `docs/` — main documentation site
- `docs-chinese/` — Chinese documentation
- `docs-french/` — French documentation
- `BentoBoxWorld.github.io/` — GitHub Pages site
- `website/` — website
- `translation-tool/` — translation tool

Check these for source before any network fetch.

## Key Dependencies (source locations)

- `world.bentobox:bentobox` → `~/git/bentobox/src/`
