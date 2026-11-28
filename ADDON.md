# BentoBox Addons

BentoBox is a platform: on its own it does nothing. **Game mode addons** create the world your players play in, and **feature addons** add extras on top of any game mode. Install at least one game mode, then add whichever features you want.

Addons go in `plugins/BentoBox/addons/`, not in `plugins/`. Restart the server after adding one and it will create its config files.

The easiest way to get a matching set of jars is [download.bentobox.world](https://download.bentobox.world), which bundles BentoBox with the addons you pick. Each addon is also on [Hangar](https://hangar.papermc.io/BentoboxWorld/), on Modrinth, and in the Releases tab of its GitHub repository. Full documentation for each one is at [docs.bentobox.world](https://docs.bentobox.world).

## Game modes

| Game mode | Description |
|---|---|
| [**AcidIsland**](https://github.com/BentoBoxWorld/AcidIsland) | Survive on an island surrounded by a sea of acid. The rain is acid too. |
| [**AOneBlock**](https://github.com/BentoBoxWorld/AOneBlock) | Start on a single magic block that regenerates as you mine it, moving through phases. |
| [**Boxed**](https://github.com/BentoBoxWorld/Boxed) | Semi-vanilla survival in a tiny box that only expands as you complete advancements. |
| [**BSkyBlock**](https://github.com/BentoBoxWorld/BSkyBlock) | Classic SkyBlock on a small island in the sky. The successor to ASkyBlock. |
| [**CaveBlock**](https://github.com/BentoBoxWorld/CaveBlock) | Survive underground in a world of solid stone. |
| [**ChunkBlock**](https://github.com/BentoBoxWorld/ChunkBlock) | One magic block in one chunk. Raise your island level to unlock the chunks around you. Requires Level. |
| [**Parkour**](https://github.com/BentoBoxWorld/Parkour) | Build parkour courses with checkpoints and best-time leaderboards, and challenge others to run them. |
| [**Poseidon**](https://github.com/BentoBoxWorld/poseidon) | Shipwrecked underwater survival. |
| [**SkyGrid**](https://github.com/BentoBoxWorld/SkyGrid) | Survive in a world of single blocks scattered over the void. |
| [**StrangerRealms**](https://github.com/BentoBoxWorld/StrangerRealms) | The Nether is replaced by the Upside Down, a dark copy of the Overworld. Claimed land is protected in both. |
| [**TradeWinds**](https://github.com/BentoBoxWorld/TradeWinds) | Sea trading, smuggling and piracy across an endless ocean of NPC ports. Requires Vault. |

## Feature addons

| Addon | Description |
|---|---|
| [**Bank**](https://github.com/BentoBoxWorld/Bank) | A shared island bank account with interest, statements and a richest-islands ranking. Requires Vault. |
| [**Biomes**](https://github.com/BentoBoxWorld/Biomes) | Lets players change the biome on their island. |
| [**Border**](https://github.com/BentoBoxWorld/Border) | Shows a border around each island, using the vanilla world border or barrier blocks and particles. |
| [**CauldronWitchery**](https://github.com/BentoBoxWorld/CauldronWitchery) | Summon mobs with a cauldron and some magic. |
| [**Challenges**](https://github.com/BentoBoxWorld/Challenges) | Challenges for players to complete, with rewards and unlockable levels. |
| [**Chat**](https://github.com/BentoBoxWorld/Chat) | Island chat and team chat channels, which admins can spy on. |
| [**CheckMeOut**](https://github.com/BentoBoxWorld/CheckMeOut) | Players submit their island for admin review, for example in build competitions. |
| [**ControlPanel**](https://github.com/BentoBoxWorld/ControlPanel) | A configurable GUI control panel players use to manage their island. |
| [**DeathChest**](https://github.com/BentoBoxWorld/DeathChest) | Saves a player's items into a chest when they die. A void or acid death puts the chest on their own island. |
| [**DimensionalTrees**](https://github.com/BentoBoxWorld/DimensionalTrees) | Trees grown in the Nether or the End turn into dimension-specific materials. |
| [**ExtraMobs**](https://github.com/BentoBoxWorld/ExtraMobs) | Adjusts mob spawning so Blazes, Wither Skeletons, Shulkers and more spawn naturally. |
| [**FarmersDance**](https://github.com/BentoBoxWorld/FarmersDance) | Crouch repeatedly next to crops, cactus, pumpkins and other plants to make them grow faster. |
| [**Greenhouses**](https://github.com/BentoBoxWorld/Greenhouses) | Build glass greenhouses that create a chosen biome inside. |
| [**InvSwitcher**](https://github.com/BentoBoxWorld/InvSwitcher) | Separate inventory, ender chest, health, food, experience and advancements per world. Recommended when running more than one game mode. |
| [**IslandFly**](https://github.com/BentoBoxWorld/IslandFly) | Lets players fly on their island. |
| [**LagBudget**](https://github.com/BentoBoxWorld/LagBudget) | Gives every island a lag budget and quietly drops new spawns on islands that go over it. |
| [**Level**](https://github.com/BentoBoxWorld/Level) | Calculates an island level from the blocks on it, with a Top Ten leaderboard. |
| [**Likes**](https://github.com/BentoBoxWorld/Likes) | Players like or dislike the islands they visit, ranking the islands. |
| [**Limits**](https://github.com/BentoBoxWorld/Limits) | Limits how many of each block and entity players can have on their island. |
| [**MagicCobblestoneGenerator**](https://github.com/BentoBoxWorld/MagicCobblestoneGenerator) | Cobblestone generators produce other blocks, with tiers that unlock as islands progress. |
| [**TopBlock**](https://github.com/BentoBoxWorld/TopBlock) | A Top Ten for AOneBlock and ChunkBlock, ranked by magic blocks mined. |
| [**TwerkingForTrees**](https://github.com/BentoBoxWorld/TwerkingForTrees) | Twerk next to saplings to make trees grow faster. |
| [**Upgrades**](https://github.com/BentoBoxWorld/Upgrades) | Players buy island upgrades such as protection range and higher limits. Requires Vault. |
| [**Visit**](https://github.com/BentoBoxWorld/Visit) | Visit any island that allows visitors from a menu, no signs needed. |
| [**VoidPortals**](https://github.com/BentoBoxWorld/VoidPortals) | Fall into the void to travel to the Nether or the End. |
| [**Warps**](https://github.com/BentoBoxWorld/Warps) | Players place a warp sign so others can visit their island from a warp menu. |

## Writing your own

Addons are built against the BentoBox API. See the [developer documentation](https://docs.bentobox.world) and the [Javadocs](https://bentoboxworld.github.io/BentoBox/) to get started, and look at any of the repositories above for working examples.
