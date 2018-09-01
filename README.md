BentoBox
========

[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/bentobox)](https://ci.codemc.org/job/BentoBoxWorld/job/bentobox/)
![Lines Of Code](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=ncloc)
![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=sqale_rating)
![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=reliability_rating)
![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=security_rating)
![Bugs](https://sonarcloud.io/api/project_badges/measure?project=world.bentobox%3Abentobox%3Adevelop&metric=bugs)


BentoBox is an expandable Minecraft Bukkit plugin for island-type games like SkyBlock or AcidIsland.
Admins can assemble the game or games how they like with a variety of add-ons. These include:

* BSkyBlock - the successor to the popular ASkyBlock. Don't fall!
* AcidIsland - you are marooned in a sea of acid!
* Level - an add-on to calculate your island level and show a top ten
* Challenges - an add-on that gives you challenges to accomplish
* Welcome WarpSigns - an add-on that enables players to plant a warp sign

Bentobox represents a turning point on ASkyBlock's history : Tastybento and Poslovitch thought up and designed this complete rewrite in order to provide a whole new way to play Skyblock and other island-style games.

Disclaimer
==========
BentoBox and all subsequent addons made by the BentoBoxWorld team are being developped **as a hobby** without receiving any kind of remuneration. You will **never** have to pay to download BentoBox or any addon created by the BentoBoxWorld team or to get access to the source code.

Hereby, **you** and **only you** are wrong when you :
* insist that we should fix a bug within an irrational period of time;
* behave rudely;
* are asking for support whereas solutions have already been given or are easily accessible;

Bugs and Feature requests
=========================
File bug and feature requests here: https://github.com/BentoBoxWorld/bentobox/issues. Make sure that your issue is following the guidelines, otherwise it will be declined.

Note for developers
===================
We consider Pull Requests from non-collaborators that contain actual code improvements or bug fixes. Do not submit PRs that only address code formatting because they will not be accepted.

Official Builds
===============
https://github.com/BentoBoxWorld/bentobox/releases

Development Builds
==================
**These development builds are not eligible to any kind of support. Use them at your own discretion or only under our approval.** *Most of them are __untested__. In the worst cases, they can even __corrupt your worlds and/or your databases__.*

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
