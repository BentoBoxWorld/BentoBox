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

#### Off-island, a PROTECTION flag is a world on/off switch

A protection flag has a rank only *on an island*. Off-island — the wilderness, or
any location with no island — `FlagListener.checkIsland` falls through to
`flag.isSetForWorld(world)`, a plain boolean held in `WorldSettings.getWorldFlags()`.
That is the value an admin edits on the **World Protections** tab
(`WorldDefaultSettingsTab` / `WorldToggleClick`), and it is what any UI must show
when there is no island — see `Flag.createProtectionFlag` and
`WorldProtectionInfoTab`, the read-only player-facing view of it.

Do **not** reach for `IWM.getDefaultIslandFlags()` here. Despite the name, that map
is the rank each *new island* is created with — the admin panel's **Island Defaults**
tab (`IslandDefaultSettingsTab`) — and has no bearing on what is allowed outside an
island. It is also sparse: it only contains flags listed under `default-island-flags`
in the game mode config, so a lookup miss is normal and must not render as "no rules".

The three concepts are distinct and easy to conflate:

| Question | Source | Admin tab |
| --- | --- | --- |
| What rank may do this on *this* island? | `island.getFlag(flag)` | Protection (per island) |
| What may anyone do *off*-island in this world? | `flag.isSetForWorld(world)` | World Protections |
| What rank will a *new* island start with? | `IWM.getDefaultIslandFlags(world)` | Island Defaults |

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
- Locale translations are produced with Claude, not GitLocalize. When a key is added to `en-US.yml`, translate it into every other `src/main/resources/locales/*.yml` file in the same PR, preserving each file's existing style (e.g. the MiniMessage-tagged names in `zh-CN.yml` / `zh-HK.yml`).
- Java preview features are enabled for both compilation and test execution.
- The authoritative version is `buildVersion` in `build.gradle.kts` (current: `3.22.1`). Two related but different strings come out of it:
  - **Gradle artifact version** (`project.version`, and so the jar name): `{buildVersion}-SNAPSHOT-LOCAL` locally, `{buildVersion}-SNAPSHOT` on CI (when `BUILD_NUMBER` is set), and the bare `{buildVersion}` when `GIT_BRANCH=origin/master`. So a local build yields `build/libs/BentoBox-3.22.1-SNAPSHOT-LOCAL.jar`.
  - **`plugin.yml` version**, the one `/bentobox version` reports: the template is `${project.version}${build.number}`, so CI appends the build number — `3.22.1-SNAPSHOT-b1234`. Locally this doubles the marker (`3.22.1-SNAPSHOT-LOCAL-LOCAL`) because `project.version` already ends in `-LOCAL`; cosmetic, but expected, so don't "fix" it by guessing.

### Minecraft 26.x / Java 25 toolchain

See `.claude/rules/build-toolchain.md` — loaded automatically when working with the Gradle build files.

## Dependency Source Lookup

When you need to inspect source code for a dependency (e.g., BentoBox, addons):

1. **Check local Maven repo first**: `~/.m2/repository/` — sources jars are named `*-sources.jar`
2. **Check the workspace**: Look for sibling directories or Git submodules that may contain the dependency as a local project (e.g., `../bentoBox`, `../addon-*`)
3. **Check Maven local cache for already-extracted sources** before downloading anything
4. Only download a jar or fetch from the internet if the above steps yield nothing useful

Prefer reading `.java` source files directly from a local Git clone over decompiling or extracting a jar.

In general, the latest version of BentoBox should be targeted.

## Project Layout

Related BentoBox projects — the core, game modes, addons, docs and tooling — are checked out as
siblings under `~/git/`. Run `ls ~/git/` for the current list; check there for source before any
network fetch.
