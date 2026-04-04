# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BentoBox is a Bukkit/Paper library plugin (Java 21) that provides the core platform for island-style Minecraft games (SkyBlock, AcidIsland, etc.) via an extensible addon system.

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

## Build Notes

- The Gradle build uses the Paper `userdev` plugin and Shadow plugin to produce a fat/shaded JAR at `build/libs/BentoBox-{version}.jar`.
- `plugin.yml` and `config.yml` are filtered for the `${version}` placeholder at build time; locale files are copied without filtering.
- Java preview features are enabled for both compilation and test execution.
- Local builds produce version `3.13.0-LOCAL-SNAPSHOT`; CI builds append `-b{BUILD_NUMBER}-SNAPSHOT`; `origin/master` builds produce the bare version.
