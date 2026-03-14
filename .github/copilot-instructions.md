# Copilot Instructions for BentoBox

## Project Overview

BentoBox is a powerful Bukkit library plugin for Minecraft servers that provides the core engine for island-style games (SkyBlock, AcidIsland, SkyGrid, etc.). Games and features are added via a modular **Addon system**, enabling admins to mix and match game modes. BentoBox exposes a rich public API for addon developers covering island protection, GUI panels, team management, and more.

## Build System

The project uses **Gradle** with the Kotlin DSL (`build.gradle.kts`).

### Key Commands

```bash
# Build the project (produces shaded JAR in build/libs/)
./gradlew build

# Run unit tests
./gradlew test

# Generate JaCoCo code coverage report (HTML + XML in build/reports/jacoco/)
./gradlew jacocoTestReport

# Build + test + coverage + SonarQube analysis (what CI runs)
./gradlew build test jacocoTestReport sonar --info

# Produce the final shaded plugin JAR
./gradlew shadowJar

# Generate Javadocs
./gradlew javadoc
```

### Build Output

- Shaded (production) JAR: `build/libs/bentobox-<version>.jar`
- Plain JAR: `build/libs/bentobox-<version>-original.jar`
- Test coverage report: `build/reports/jacoco/test/html/index.html`
- Javadocs: `build/docs/javadoc/`

## Tech Stack

| Area | Technology |
|------|------------|
| Language | Java 21 (with `--enable-preview`) |
| Plugin Framework | PaperMC / Bukkit API (1.21) |
| Build Tool | Gradle (Kotlin DSL) |
| Testing | JUnit 5 (Jupiter), Mockito 5, MockBukkit |
| Coverage | JaCoCo |
| Code Quality | SonarQube (runs on CI for PRs to `develop`) |
| Null Safety | Eclipse JDT `@NonNull` / `@Nullable` annotations |

## Project Structure

```
src/
├── main/java/world/bentobox/bentobox/
│   ├── api/           # Public API for addon developers (events, flags, commands, panels, etc.)
│   ├── blueprints/    # Island blueprint/schematic system
│   ├── commands/      # Player and admin commands
│   ├── database/      # Database abstraction (MySQL, MariaDB, PostgreSQL, MongoDB)
│   ├── hooks/         # Integrations with third-party plugins (Vault, PlaceholderAPI, etc.)
│   ├── listeners/     # Bukkit event listeners
│   ├── managers/      # Core managers (addons, islands, players, blueprints, etc.)
│   ├── nms/           # Version-specific NMS (net.minecraft.server) code
│   ├── panels/        # Built-in inventory GUI panels
│   └── util/          # Utility classes
├── main/resources/
│   ├── config.yml     # Default plugin configuration
│   ├── locales/       # Default en-US locale and other translations
│   └── panels/        # Panel layout YAML files
└── test/java/world/bentobox/bentobox/
    └── (mirrors main package structure)
```

## Coding Conventions

- **Java version**: Java 21; preview features are enabled.
- **Packages**: all lowercase, rooted at `world.bentobox.bentobox`.
- **Classes**: PascalCase. Test classes must end with `Test` (e.g., `IslandManagerTest`).
- **Methods / fields**: camelCase.
- **Constants**: `UPPER_SNAKE_CASE`.
- **Encoding**: UTF-8 everywhere.
- **Null safety**: Annotate method parameters and return types with `@NonNull` or `@Nullable` from `org.eclipse.jdt.annotation`.
- **No formatting-only PRs**: Do not submit PRs that only reformat code; they will be rejected.
- **Javadoc**: Public API methods and classes must have Javadoc. `Xdoclint:none` suppresses strict doc-lint warnings, but well-documented code is expected.

## Testing Guidelines

- Use **JUnit 5** (`@Test`, `@BeforeEach`, `@AfterEach`, `@ExtendWith`).
- Use **Mockito** (`@Mock`, `@InjectMocks`, `MockitoExtension`) for mocking.
- Use **MockBukkit** to mock Bukkit/Paper server objects when necessary.
- Extend `CommonTestSetup` where a common Bukkit environment is needed.
- Test classes live in the matching package under `src/test/java/`.
- Run `./gradlew test` to execute all tests.
- Run `./gradlew jacocoTestReport` to generate the coverage report and verify coverage.
- The CI pipeline runs both on every PR to `develop`.

### Example Test Skeleton

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyClassTest {

    @Mock
    private SomeDependency dependency;

    private MyClass myClass;

    @BeforeEach
    void setUp() {
        myClass = new MyClass(dependency);
    }

    @Test
    void testSomeBehavior() {
        when(dependency.someMethod()).thenReturn("expected");
        String result = myClass.doSomething();
        assertEquals("expected", result);
    }
}
```

## Architecture Notes

- **Addon system**: Addons implement `GameModeAddon` or `Addon` and are loaded at startup. Each game-mode addon registers its own worlds, commands, and flags.
- **Island protection**: The `Flags` system combined with `IslandProtectionManager` controls what players can do on/near islands.
- **Database layer**: `AbstractDatabaseHandler<T>` abstracts YAML, JSON, MySQL, PostgreSQL, and MongoDB storage. New data classes must extend `DataObject`.
- **Event-driven**: All major actions emit Bukkit events in the `world.bentobox.bentobox.api.events` hierarchy; addons should listen to these instead of low-level Bukkit events where possible.
- **Panel system**: Inventory GUI panels are driven by YAML layouts (`src/main/resources/panels/`) combined with `PanelBuilder` / `PanelItem` classes in `world.bentobox.bentobox.api.panels`.
- **NMS compatibility**: Version-specific code lives in `world.bentobox.bentobox.nms` and is kept minimal; favour Paper API over NMS when possible.

## CI / CD

- **Workflow**: `.github/workflows/build.yml` runs on pushes to `develop` and on all pull requests.
- **Java**: JDK 21 (Zulu distribution) is used in CI.
- **Quality gate**: SonarQube analysis must pass. The `SONAR_TOKEN` secret is required.
- **Artifact**: The shaded JAR is the deployable artifact; it is published to the CodeMC Maven repository on successful builds.

## Pull Request Guidelines

- Target the **`develop`** branch for all contributions.
- All tests must pass (`./gradlew test`).
- Keep PRs focused — one feature or bug fix per PR.
- Add or update unit tests for every changed behaviour.
- Follow the existing code style; do not reformat unrelated code.
- Ensure SonarQube quality gate passes (checked automatically on PRs).
- Reference the related GitHub issue in the PR description.
