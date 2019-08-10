# BentoBox

[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)
[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/BentoBox)](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox&metric=ncloc)](https://sonarcloud.io/dashboard?id=world.bentobox%3Abentobox)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=world.bentobox%3Abentobox)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=world.bentobox%3Abentobox)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox&metric=security_rating)](https://sonarcloud.io/dashboard?id=world.bentobox%3Abentobox)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox&metric=bugs)](https://sonarcloud.io/dashboard?id=world.bentobox%3Abentobox)

BentoBox is an expandable Minecraft **1.13+ Spigot plugin** for island-type games like SkyBlock or AcidIsland.

## About BentoBox

### History

ASkyBlock and AcidIsland were originally created by [tastybento](https://github.com/tastybento).
These two plugins were sharing the same codebase, which grew fastly but ultimately became hard to maintain.
[Poslovitch](https://github.com/Poslovitch) was running a Skyblock server before starting to contribute regularly on ASkyBlock's codebase.

Then came the idea of *completely rewriting ASkyBlock*.
A turning point that would be called *BSkyBlock*.
In May 2017, tastybento agreed to the idea, and Poslovitch and him spent more time developing the code of the plugin which would become *BentoBox*.
During summer 2018, ASkyBlock's support got dropped and development was focused on the now called plugin *BentoBox*, which would then take over ASkyBlock and AcidIsland for 1.13+ servers.

### Description

BentoBox introduces a **unique Addon system** and a **powerful API** which allows for a technically **unlimited customization** of the gamemodes.
Therefore, BentoBox **does nothing on its own**: addons and gamemodes are bringing the features.

BentoBox being totally **free and open-sourced**, we are confident in seeing this platform grow and become even more powerful in the future.

Start now to create the server you've dreamed of!

## Addons
These include some popular Gamemodes:
* [**BSkyBlock**](https://github.com/BentoBoxWorld/BSkyBlock): the successor to the popular ASkyBlock.
* [**AcidIsland**](https://github.com/BentoBoxWorld/AcidIsland): you are marooned in a sea of acid!
* [**CaveBlock**](https://github.com/BentoBoxWorld/CaveBlock): try to live underground!
* [**SkyGrid**](https://github.com/BentoBoxWorld/SkyGrid): survive in world made up of scattered blocks - what an adventure!

And these include some of the official Addons that are bringing incredible new features, such as:
* [**Level**](https://github.com/BentoBoxWorld/Level): calculate your island level and show a top ten.
* [**Challenges**](https://github.com/BentoBoxWorld/Challenges): gives your players challenges to accomplish.
* [**WarpSigns**](https://github.com/BentoBoxWorld/addon-welcomewarpsigns): enables players to plant a warp sign and visit other player's islands.
* [**Biomes**](https://github.com/BentoBoxWorld/Biomes): enables players to change biomes on islands.
* [**invSwitcher**](https://github.com/BentoBoxWorld/addon-invSwitcher): allows you to split inventories between the different gamemode worlds.
* [**VoidPortals**](https://github.com/BentoBoxWorld/VoidPortals): teleports to the dimensions by falling into the Void:
* [**MagicCobblestoneGenerator**](https://github.com/BentoBoxWorld/MagicCobblestoneGenerator): generates any blocks in cobblestone generators.
* [**IslandFly**](https://github.com/BentoBoxWorld/IslandFly): allows your players to fly on their islands.
* [**Limits**](https://github.com/BentoBoxWorld/addon-limits): limits blocks and entities that your players can place on their islands.
* [**Greenhouses**](https://github.com/BentoBoxWorld/greenhouses): enables players to make mini biome glass houses on their island.

And there are plenty of other official or community-made Addons you can try and use for your server!

## Documentation

* [Installation guide](https://github.com/BentoBoxWorld/bentobox/wiki/Install-Bentobox)
* [Javadocs](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/javadoc/)
* [Wiki](https://github.com/BentoBoxWorld/BentoBox/wiki)

## Downloads

### Webtool
A [webtool](https://bentobox-tool.herokuapp.com/) is currently being developed to allow you to easily setup BentoBox and Addons on your server.

### Direct links
* [Download](https://github.com/BentoBoxWorld/BentoBox/releases)

### Developers
* [Jenkins](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/) (**untested and mostly unstable builds**)

## What about contributing?

BentoBox heavily relies on [the community that gets involved in its development](https://github.com/BentoBoxWorld/BentoBox/graphs/contributors).
You don't need to know any programming language to start helping us.

However, your contribution **must be in agreement** with:
* our [code of conduct](https://github.com/BentoBoxWorld/BentoBox/tree/master/.github/CODE_OF_CONDUCT.md)
* our [contribution guidelines](https://github.com/BentoBoxWorld/BentoBox/tree/master/.github/CONTRIBUTING.md)

### Report bugs and suggest features
Bugs and feature requests must be filed on our [issue tracker](https://github.com/BentoBoxWorld/BentoBox/issues).

### Pull requests
We consider Pull Requests from non-collaborators that contain actual code improvements or bug fixes.
Do not submit PRs that only address code formatting because they will not be accepted.

## API

BentoBox uses Maven, and its Maven repository is kindly provided by [CodeMC](https://codemc.org).

### Maven dependency
```xml
<repositories>
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
