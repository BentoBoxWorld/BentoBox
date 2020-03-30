# BentoBox

[![Discord](https://img.shields.io/discord/272499714048524288.svg?logo=discord)](https://discord.bentobox.world)
[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/BentoBox)](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=ncloc)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=security_rating)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=BentoBoxWorld_BentoBox&metric=bugs)](https://sonarcloud.io/dashboard?id=BentoBoxWorld_BentoBox)

## About BentoBox

### Description

BentoBox is a powerful Bukkit library plugin that provides core features for island-style games like SkyBlock, AcidIsland, SkyGrid and others. 
These games are added to it via its **unique Addon system**. Further, non-game addons can provide features across games, such as challenges or warps. This enables admins to mix and match games and features to customize their server. It also enables the same code to be run 
across games, reducing bugs and speeding updates across all games. For coders, 
BentoBox has a **powerful API** allows for quick and easy development of these addons and simplifies complex aspects such as island protection, GUIs, and team management.

BentoBox is **[free](https://www.gnu.org/philosophy/free-sw.en.html) and open-source software** so join us to make this platform grow, become even more powerful and popular! Admins can pay to support BentoBox and Addons via donations and sponsorship.   

Start now to create the server you've dreamed of!

## Addons
These are some popular Gamemodes:
* [**AcidIsland**](https://github.com/BentoBoxWorld/AcidIsland): You are marooned in a sea of acid!
* [**BSkyBlock**](https://github.com/BentoBoxWorld/BSkyBlock): The successor to the popular ASkyBlock.
* [**CaveBlock**](https://github.com/BentoBoxWorld/CaveBlock): Try to live underground!
* [**SkyGrid**](https://github.com/BentoBoxWorld/SkyGrid): Survive in world made up of scattered blocks - what an adventure!

All official Addons are listed here:
* [**Addons**](https://github.com/BentoBoxWorld/BentoBox/blob/develop/ADDON.md)

There are also plenty of other official or community-made Addons you can try and use for your server!

## Documentation

* Start reading: [https://docs.bentobox.world](https://docs.bentobox.world)
* For developers: [Javadocs](https://bentoboxworld.github.io/BentoBox/)

## Downloads

### Webtool
A [webtool](https://download.bentobox.world/) is currently being developed to allow you to easily setup BentoBox and Addons on your server.

### Direct links
* [Download](https://github.com/BentoBoxWorld/BentoBox/releases)

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
* Translating text for BentoBox and Addons (We use GitLocalize to make this easier)
* Submitting good bug reports or helpful feature requests
* Fixing bugs and submitting Pull Requests for the fixes

If you contribute code it **must be in agreement** with:
* our license
* our [code of conduct](https://github.com/BentoBoxWorld/BentoBox/tree/master/.github/CODE_OF_CONDUCT.md)
* our [contribution guidelines](https://github.com/BentoBoxWorld/BentoBox/tree/master/.github/CONTRIBUTING.md)

### Report bugs and suggest features
Bugs and feature requests must be filed on our [issue tracker](https://github.com/BentoBoxWorld/BentoBox/issues).

### Pull requests
We consider Pull Requests from non-collaborators that contain actual code improvements or bug fixes.
Do not submit PRs that only address code formatting because they will not be accepted.

## API

BentoBox uses Maven, and its Maven repository is kindly provided by [CodeMC](https://codemc.org).

### Maven
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

### Gradle
```groovy
repositories {
  maven { url "https://repo.codemc.org/repository/maven-public/" }
}

dependencies {
  compileOnly 'world.bentobox:bentobox:PUT-VERSION-HERE'
}
```

### History

[tastybento](https://github.com/tastybento) created ASkyBlock and AcidIsland that shared the same codebase. These plugins became very popular but became hard to maintain.
[Poslovitch](https://github.com/Poslovitch) was running a Skyblock server before starting to contribute regularly to ASkyBlock's codebase. He proposed the idea of completely rewriting ASkyBlock
to make it easier to maintain and richer in features. In May 2017, this became the *BSkyBlock* project. As development progressed it became clear that a lot of the new core features could be used by other
island-style games and so that core functionality was split off and renamed *BentoBox* and the addon system was created. The addons for BSkyBlock and AcidIsland became very simple to develop and much smaller. 
The community started to grow and we added new game modes like SkyGrid and CaveBlock by BONNe. BONNe also took over maintenance of Challenges and Biomes and contributed to other addons.  

In December 2019, Poslovitch launched the BentoBox collection on SpigotMC and the story continues! 
