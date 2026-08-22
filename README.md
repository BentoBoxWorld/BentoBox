# BentoBox

[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)
[![Build Status](https://ci.codemc.io/job/BentoBoxWorld/job/BentoBox/badge/icon)](https://ci.codemc.io/job/BentoBoxWorld/job/BentoBox/)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=security_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=bugs)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)

# SkyBlock, OneBlock, AcidIsland, and more - all in one plugin

[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)

BentoBox powers island-style game modes for Paper servers. Pick the game modes you want, drop them in, and you're running. No forks, no outdated code — one actively maintained platform that stays current with every Minecraft release.

**Game modes available:**

- **BSkyBlock** — classic SkyBlock, successor to the original ASkyBlock
- **AOneBlock** — the popular OneBlock experience
- **AcidIsland** — survive in a sea of acid
- **Boxed** — expand your world by completing advancements
- **CaveBlock** — underground survival
- **SkyGrid** — scattered blocks, maximum adventure
- **Poseidon** — underwater island challenge
- And more community-created game modes

**Why server admins choose BentoBox:**

- Run multiple game modes on one server with shared features (challenges, warps, levels, leaderboards)
- 20+ addons let you customize exactly the experience you want
- Actively maintained and always up to date with the latest Minecraft version
- Free and open source — used on 1,100+ servers worldwide
- Rich API for developers who want to build custom addons

[Full Documentation](https://docs.bentobox.world)

# Installation

1. Place the BentoBox jar in your plugins folder
2. Start the server
3. Download the game mode and feature addons you want from [this site](https://hangar.papermc.io/BentoboxWorld/) or [download.bentobox.world](https://download.bentobox.world) and place them in the `plugins/BentoBox/addons` folder
4. Restart the server — you're good to go

## Addons
These are some popular Gamemodes:
* [**AcidIsland**](https://github.com/BentoBoxWorld/AcidIsland): You are marooned in a sea of acid!
* [**AOneBlock**](https://github.com/BentoBoxWorld/AOneBlock): Start to play with only 1 magical block.
* [**Boxed**](https://github.com/BentoBoxWorld/Boxed): A game mode where you are boxed into a tiny space that only expands by completing advancements.
* [**BSkyBlock**](https://github.com/BentoBoxWorld/BSkyBlock): The successor to the popular ASkyBlock.
* [**CaveBlock**](https://github.com/BentoBoxWorld/CaveBlock): Try to live underground!
* [**SkyGrid**](https://github.com/BentoBoxWorld/SkyGrid): Survive in world made up of scattered blocks - what an adventure!

All official Addons are listed here:
* [**Addons**](https://github.com/BentoBoxWorld/BentoBox/blob/develop/ADDON.md)

There are also plenty of other official or community-made Addons you can try and use for your server!

## Documentation

* Start reading: [https://docs.bentobox.world](https://docs.bentobox.world)
* For developers: [Javadocs](https://ci.codemc.io/job/BentoBoxWorld/job/BentoBox/ws/target/apidocs/index.html)

## Bugs or Issues
[File bugs on GitHub](https://github.com/BentoBoxWorld/BentoBox/issues). Confused? Ask on Discord. Note: we are **not** a company, so please be kind with your requests.

### Developers
* [Jenkins](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/) (**untested and mostly unstable builds**)
* [Javadocs](https://bentoboxworld.github.io/BentoBox/)

## What about contributing?

Join the BentoBox [community](https://github.com/BentoBoxWorld/BentoBox/graphs/contributors).
You don't need to know any programming language to start helping us.

You can contribute by:

* Donating or sponsoring the developers
* Coding new addons
* Adopting an Addon and maintaining it
* Translating text for BentoBox and Addons — native speakers only, please (see the [AI policy](https://github.com/BentoBoxWorld/.github/blob/master/AI_POLICY.md#translations))
* Submitting good bug reports or helpful feature requests
* Fixing bugs and submitting Pull Requests for the fixes

If you contribute code it **must be in agreement** with:
* our [license](https://github.com/BentoBoxWorld/BentoBox/blob/develop/LICENSE)
* our [code of conduct](https://github.com/BentoBoxWorld/.github/blob/master/CODE_OF_CONDUCT.md)
* our [contribution guidelines](https://github.com/BentoBoxWorld/.github/blob/master/CONTRIBUTING.md)
* our [AI policy](https://github.com/BentoBoxWorld/.github/blob/master/AI_POLICY.md)

### Report bugs and suggest features
Bugs and feature requests must be filed on our [issue tracker](https://github.com/BentoBoxWorld/BentoBox/issues).

### Pull requests
We consider Pull Requests from non-collaborators that contain actual code improvements or bug fixes.
Do not submit PRs that only address code formatting because they will not be accepted.
AI-assisted PRs are welcome — there is no restriction on using AI tools, but you must be able to
stand behind your code, at least until it is accepted. See the [AI policy](https://github.com/BentoBoxWorld/.github/blob/master/AI_POLICY.md).

## Policies

* [Privacy Policy](https://github.com/BentoBoxWorld/.github/blob/master/PRIVACY.md) — what anonymous usage data BentoBox submits to bStats, what is never collected, and how to opt out
* [AI Policy](https://github.com/BentoBoxWorld/.github/blob/master/AI_POLICY.md) — how the project uses AI and the rules for AI-assisted contributions
* [Contributing Guide](https://github.com/BentoBoxWorld/.github/blob/master/CONTRIBUTING.md)
* [Code of Conduct](https://github.com/BentoBoxWorld/.github/blob/master/CODE_OF_CONDUCT.md)

## API

BentoBox uses Maven, and its Maven repository is kindly provided by [CodeMC](https://codemc.org).

### Maven
```xml
<repositories>
  <repository>
    <id>codemc-snapshots</id>
    <url>https://repo.codemc.org/repository/maven-snapshots</url>
  </repository>
  <repository>
    <id>codemc-repo</id>
    <url>https://repo.codemc.org/repository/maven-public/</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>world.bentobox</groupId>
    <artifactId>bentobox</artifactId>
    <version>PUT-VERSION-HERE</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

### Gradle
```groovy
repositories {
  maven { url "https://repo.codemc.org/repository/maven-public/" }
}

dependencies {
  compileOnly 'world.bentobox:bentobox:PUT-VERSION-HERE-SNAPSHOT'
}
```
**Note:** Due to a Gradle issue with versions for Maven, you need to use -SNAPSHOT at the end.

## Building from Source

BentoBox builds with Gradle. You do **not** need to install Gradle — the repository ships
the Gradle wrapper, which downloads the exact version the project is built with.

### Requirements

* **JDK 25** — BentoBox compiles to Java 25 bytecode, because the Paper API it builds against does.
* **Git** and an internet connection for the first build.

### Quick start

```bash
git clone https://github.com/BentoBoxWorld/BentoBox.git
cd BentoBox
./gradlew clean build          # use gradlew.bat on Windows
```

The shaded (fat) jar is written to `build/libs/`, named `BentoBox-<version>.jar` — for example
`build/libs/BentoBox-3.22.1-SNAPSHOT-LOCAL.jar`. Drop that straight into your test server's
`plugins` folder.

`build` runs the tests and produces the shaded jar. If you only want the jar, run
`./gradlew clean shadowJar` instead.

> **The first build is slow.** The Paperweight plugin downloads and remaps a Paper development
> bundle before anything compiles. This is a one-off cost — later builds reuse the cached bundle.

### Other useful tasks

| Command | What it does |
| --- | --- |
| `./gradlew test` | Run the JUnit 5 test suite |
| `./gradlew test --tests "world.bentobox.bentobox.managers.IslandsManagerTest"` | Run a single test class |
| `./gradlew test jacocoTestReport` | Test with a coverage report in `build/reports/jacoco/` |
| `./gradlew javadocJar` | Build `BentoBox-<version>-javadoc.jar` |
| `./gradlew sourcesJar` | Build `BentoBox-<version>-sources.jar` |
| `./gradlew publishToMavenLocal` | Install to `~/.m2` so a local addon can build against it |

Tasks can be combined on one command line, e.g. `./gradlew clean build javadocJar sourcesJar`.

### If Gradle can't find a Java 25 toolchain

Compilation always happens on Java 25 regardless of which JVM runs Gradle itself. If Gradle
reports that no matching toolchain is available, point it at your JDK 25 installation:

```bash
./gradlew build -Porg.gradle.java.installations.paths=/path/to/jdk-25
```

This is what CI does — it runs the Gradle daemon on Java 21 and compiles on a separate
Java 25 install discovered via `-Porg.gradle.java.installations.fromEnv=JAVA_HOME_25_X64`.

### Version numbers

The version is derived from `buildVersion` in `build.gradle.kts`:

* local builds → `3.22.1-SNAPSHOT-LOCAL`
* CI builds → `3.22.1-SNAPSHOT` (with the build number recorded separately)
* release builds from `origin/master` → `3.22.1`
