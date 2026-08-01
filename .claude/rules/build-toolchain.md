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
- **Compile target vs. runtime support.** `paperVersion` is the latest **stable 26.1.2** dev bundle, not 26.2 — because MockBukkit has no 26.2 build and its registry mock throws on 26.2's new `minecraft:sulfur_cube_archetype` registry. Minecraft **26.2 is supported at runtime** (see `ServerCompatibility` and the Modrinth `game-versions` list); 26.2-only blocks/entities are referenced via `Enums.getIfPresent(...)` by name, never a compile-time symbol. The forward "compile against literal 26.2" work is parked in a draft PR until MockBukkit ships a 26.2 build.
- **MockBukkit coordinate.** Tests use `org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:<ver>` (from Paper's repo), which **must match `paperVersion`'s MC line** — a mismatched MockBukkit fails every test at init with `InternalDataLoadException` (it validates the live API's registries against its bundled per-version data). When bumping the MC version, bump both together.
