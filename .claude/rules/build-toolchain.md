---
description: Minecraft 26.x / Java 25 build toolchain constraints — read before changing versions in the Gradle build
paths:
  - build.gradle.kts
  - settings.gradle.kts
---

# Minecraft 26.x / Java 25 toolchain

Supporting Minecraft 26.x forced a chain of build changes — keep these in mind before touching versions in `build.gradle.kts`:

- **Java 25.** The 26.x `paper-api` is Java 25 bytecode and its Gradle metadata requires consumers to target Java 25, so BentoBox now compiles to Java 25 (`javaVersion = "25"`, `options.release = 25`). **Addons that compile against BentoBox must also move to Java 25.**
- **paperweight `2.0.0-SNAPSHOT`.** All 26.x dev bundles are dev-bundle *data version 8*, which no released paperweight (`<= 2.0.0-beta.21`) can read. The snapshot is resolved via a `pluginManagement` block in `settings.gradle.kts` pointing at Paper's repo. The paperweight tool launcher is pinned to Java 25 (the 26.1+ paperclip patch step requires it). Revisit once a stable paperweight reads data-version-8 bundles.
  - **The snapshot dictates the Gradle wrapper version.** Paper's repo keeps only the *latest* timestamped snapshot, and each one is built with whatever Gradle paperweight currently uses, stamped as the `org.gradle.plugin.api-version` attribute. Gradle refuses a plugin whose api-version is newer than itself, so when paperweight bumps Gradle, every build (Jenkins, GitHub Actions, local once the 24h snapshot cache expires) fails at configuration time with `No matching variant of io.papermc.paperweight:paperweight-userdev:2.0.0-SNAPSHOT ... 'org.gradle.plugin.api-version'`. Fix: `./gradlew wrapper --gradle-version <at least the version in the error>` and commit the wrapper files. Pinning an older timestamped snapshot is not an option because it is gone from the repo. Happened 2026-09-25 (snapshot -220 built with Gradle 9.7.1, wrapper was 9.0.0 → moved to 9.8.0).
- **Compile target vs. test runtime are split.** `paperVersion` (dev bundle, `compileOnly`, `testCompileOnly`) is the newest Paper line so new-version symbols (`EntityType.CUSHION`, `EntityBreakByEntityEvent`, `STRAW_BED`...) are visible at compile time. `testPaperVersion` (`testRuntimeOnly`, forced on `testRuntimeClasspath`) is the line MockBukkit actually supports. The dev bundle's server jar only feeds `compileOnly`, which is what makes the split possible. Consequences: (1) main code must still guard newer-than-`testPaperVersion` API (`Enums.getIfPresent`, or a listener class registered only when `Class.forName` succeeds) because the plugin also runs on older servers; (2) a test that loads a class missing from the runtime API throws `NoClassDefFoundError`, so guard such tests with `@EnabledIf`. Collapse the two back into one when MockBukkit catches up.
- **MockBukkit coordinate.** Tests use `org.mockbukkit.mockbukkit:mockbukkit-v26.2:<ver>` (from Paper's repo), which **must match `testPaperVersion`'s MC line** — a mismatched MockBukkit fails every test at init with `InternalDataLoadException` (it validates the live API's registries against its bundled per-version data). When MockBukkit ships a new line, bump `testPaperVersion` and the MockBukkit coordinate together.
