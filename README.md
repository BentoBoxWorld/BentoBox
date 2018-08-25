BentoBox
========

[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/bentobox)](https://ci.codemc.org/job/BentoBoxWorld/job/bentobox/)
![Lines Of Code](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=ncloc)
![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=sqale_rating)
![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=reliability_rating)
![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=security_rating)
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=bugs)


BentoBox is an expandable Minecraft Bukkit plugin for island-type games like ASkyBlock or AcidIsland.
Admins can assemble the game or games how they like with a variety of add-ons. These include:

* BSkyBlock - the successor to the popular ASkyBlock. Don't fall!
* AcidIsland - you are marooned in a sea of acid!
* Level - an add-on to calculate your island level and show a top ten
* Challenges - an add-on that gives you challenges to accomplish
* Welcome WarpSigns - an add-on that enables players to plant a warp sign

Bentobox represents a turning point on ASkyBlock's history : Tastybento and Poslovitch thought up and designed this complete rewrite in order to provide a whole new way to play Skyblock and other island-style games.

**Discover BSkyBlock todfay, its gameplay overhaul, and enjoy the Skyblock revival!**

Bugs and Feature requests
=========================
File bug and feature requests here: https://github.com/BentoBoxWorld/bentobox/issues

Note for developers
===================
We consider Pull Requests from non-collaborators that contain actual code improvements or bug fixes. Do not submit PRs that only address code formatting because they will not be accepted.

Development Builds
==================
Jenkins: https://ci.codemc.org/job/BentoBoxWorld/job/bentobox/

API
===
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

You can find the javadoc here: https://ci.codemc.org/job/BentoBoxWorld/job/bentobox/javadoc/
