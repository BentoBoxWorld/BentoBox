# BentoBox

[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/BentoBox)](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/)
![Lines Of Code](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=ncloc)
![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=sqale_rating)
![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=reliability_rating)
![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=security_rating)
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=bugs)

BentoBox is an expandable Minecraft Spigot plugin for island-type games like SkyBlock or AcidIsland.

It is designed to take over ASkyBlock and AcidIsland for 1.13+ Minecraft versions.

## Addons
Admins can assemble the game or games how they like with a variety of Addons.
These include some popular Gamemodes:
* [**BSkyBlock**](https://github.com/BentoBoxWorld/BSkyBlock): the successor to the popular ASkyBlock.
* [**AcidIsland**](https://github.com/BentoBoxWorld/AcidIsland): you are marooned in a sea of acid!

And these include Addons that are bringing incredible new features, such as:
* [**Level**](https://github.com/BentoBoxWorld/Level): calculate your island level and show a top ten.
* [**Challenges**](https://github.com/BentoBoxWorld/addon-challenges): gives you challenges to accomplish.
* [**WarpSigns**](https://github.com/BentoBoxWorld/addon-welcomewarpsigns): enables players to plant a warp sign and visit other player's islands.

And there are plenty of other official or community-made Addons you can try and use for your server!

## Downloads

### Webtool
A [webtool](https://bentobox-tool.herokuapp.com/) is currently being developed to allow you to easily setup BentoBox and Addons on your server.

### Direct links
* [Installation guide](https://github.com/BentoBoxWorld/bentobox/wiki/Install-Bentobox)
* [Download](https://github.com/BentoBoxWorld/BentoBox/releases)

### Developers
* [Jenkins](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/)
* [Javadocs](https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/javadoc/)

## What about contributing?

BentoBox heavily relies on the community that gets involved in its development.
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
Maven dependency:
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

You can find the javadoc here: https://ci.codemc.org/job/BentoBoxWorld/job/BentoBox/javadoc/
