package us.tastybento.bskyblock.database.managers.island;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.island.builders.IslandBuilder;
import us.tastybento.bskyblock.island.builders.IslandBuilder.IslandType;

/**
 * Create and paste a new island
 * @author ben
 *
 */
public class NewIsland {
    private static final boolean DEBUG = false;
    private final BSkyBlock plugin = BSkyBlock.getPlugin();
    private Island island;
    private final Player player;

    private NewIsland(Island oldIsland, Player player) {
        super();
        this.player = player;
        newIsland();
        if (oldIsland != null) {
            // Delete the old island
            plugin.getIslands().deleteIsland(oldIsland, true);
        }
    }

    /**
     * @return the island that was created
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Start building a new island
     * @return New island builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a new island for a player using a schematic
     * @author ben
     *
     */
    public static class Builder {
        private Island oldIsland;
        private Player player;

        public Builder oldIsland(Island oldIsland) {
            this.oldIsland = oldIsland;
            return this;
        }


        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Island build() throws IOException {
            if (player != null) {
                NewIsland newIsland = new NewIsland(oldIsland, player);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a schematic and a player");
        }
    }

    /**
     * Makes an island.
     */
    public void newIsland() {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: new island");
        //long time = System.nanoTime();
        final UUID playerUUID = player.getUniqueId();
        boolean firstTime = false;
        if (!plugin.getPlayers().hasIsland(playerUUID)) {
            firstTime = true;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: finding island location");
        Location next = getNextIsland(player.getUniqueId());
        if (DEBUG)
            plugin.getLogger().info("DEBUG: found " + next);

        // Add to the grid
        island = plugin.getIslands().createIsland(next, playerUUID);
        // Save the player so that if the server is reset weird things won't happen
        plugin.getPlayers().save(true);
        plugin.getIslands().save(true);

        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(playerUUID);

        // Set the biome
        //BiomesPanel.setIslandBiome(next, schematic.getBiome());
        // Set home loction
        plugin.getPlayers().setHomeLocation(playerUUID, next, 1);

        // Create island
        new IslandBuilder(island)
        .setPlayer(player)
        .setChestItems(Settings.chestItems)
        .setType(IslandType.ISLAND)
        .build();
        if (Settings.netherGenerate && Settings.netherIslands && IslandWorld.getNetherWorld() != null) {
            new IslandBuilder(island)
            .setPlayer(player)
            .setChestItems(Settings.chestItems)
            .setType(IslandType.NETHER)
            .build();
        }
        if (Settings.endGenerate && Settings.endIslands && IslandWorld.getEndWorld() != null) {
            new IslandBuilder(island)
            .setPlayer(player)
            .setChestItems(Settings.chestItems)
            .setType(IslandType.END)
            .build();
        }
    }

    /**
     * Get the location of next free island spot
     * @param playerUUID
     * @return Location of island spot
     */
    private Location getNextIsland(UUID playerUUID) {
        Location last = plugin.getIslands().getLast();

        if (DEBUG)
            plugin.getLogger().info("DEBUG: last = " + last);
        // Find the next free spot

        if (last == null) {
            last = new Location(IslandWorld.getIslandWorld(), Settings.islandXOffset + Settings.islandStartX, Settings.islandHeight, Settings.islandZOffset + Settings.islandStartZ);
        }
        Location next = last.clone();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: last 2 = " + last);
        while (plugin.getIslands().isIsland(next)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: getting next loc");
            next = nextGridLocation(next);
        };
        // Make the last next, last
        last = next.clone();
        if (DEBUG)
            plugin.getLogger().info("DEBUG: last 3 = " + last);
        return next;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     *
     * @param lastIsland
     * @return Location of next free island
     */
    private Location nextGridLocation(final Location lastIsland) {
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        Location nextPos = lastIsland;
        if (x < z) {
            if (-1 * x < z) {
                nextPos.setX(nextPos.getX() + Settings.islandDistance*2);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() + Settings.islandDistance*2);
            return nextPos;
        }
        if (x > z) {
            if (-1 * x >= z) {
                nextPos.setX(nextPos.getX() - Settings.islandDistance*2);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() - Settings.islandDistance*2);
            return nextPos;
        }
        if (x <= 0) {
            nextPos.setZ(nextPos.getZ() + Settings.islandDistance*2);
            return nextPos;
        }
        nextPos.setZ(nextPos.getZ() - Settings.islandDistance*2);
        return nextPos;
    }
}
