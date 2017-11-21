package us.tastybento.bskyblock.database.managers.island;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.schematics.Schematic;
import us.tastybento.bskyblock.schematics.Schematic.PasteReason;

/**
 * Create and paste a new island
 * @author ben
 *
 */
public class NewIsland {
    private static final boolean DEBUG = false;
    private final BSkyBlock plugin = BSkyBlock.getPlugin();
    private Island island;
    private final Island oldIsland;
    private final Schematic schematic;
    private final Player player;

    private NewIsland(Island oldIsland, Schematic schematic, Player player) {
        super();
        this.oldIsland = oldIsland;
        this.schematic = schematic;
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
        private Schematic schematic;
        private Player player;

        public Builder oldIsland(Island oldIsland) {
            this.oldIsland = oldIsland;
            return this;
        }

        public Builder schematic(Schematic schematic) {
            this.schematic = schematic;
            return this;
        }

        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Island build() throws IOException {
            if (schematic != null && player != null) {
                NewIsland newIsland = new NewIsland(oldIsland,schematic, player);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a schematic and a player");
        }
    }

    /**
     * Makes an island using schematic. No permission checks are made. They have to be decided
     * before this method is called.
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
        Island myIsland = plugin.getIslands().createIsland(next, playerUUID);
        myIsland.setLevelHandicap(schematic.getLevelHandicap());
        // Save the player so that if the server is reset weird things won't happen
        plugin.getPlayers().save(true);
        plugin.getIslands().save(true);

        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(playerUUID);

        // Set the biome
        //BiomesPanel.setIslandBiome(next, schematic.getBiome());
        // Teleport to the new home
        if (schematic.isPlayerSpawn()) {
            // Set home and teleport
            plugin.getPlayers().setHomeLocation(playerUUID, schematic.getPlayerSpawn(next), 1);
        }

        // Create island based on schematic
        if (schematic != null) {
            //plugin.getLogger().info("DEBUG: pasting schematic " + schematic.getName() + " " + schematic.getPerm());
            //plugin.getLogger().info("DEBUG: nether world is " + BSkyBlock.getNetherWorld());
            // Paste the starting island. If it is a HELL biome, then we start in the Nether
            if (Settings.netherGenerate && schematic.isInNether() && Settings.netherIslands && IslandWorld.getNetherWorld() != null) {
                // Nether start
                // Paste the overworld if it exists
                if (!schematic.getPartnerName().isEmpty()) {
                    // A partner schematic is available
                    pastePartner(plugin.getSchematics().getSchematic(schematic.getPartnerName()),next, player);
                }
                // Switch home location to the Nether
                next = next.toVector().toLocation(IslandWorld.getNetherWorld());
                // Set the player's island location to this new spot
                //plugin.getPlayers().setIslandLocation(playerUUID, next);
                schematic.pasteSchematic(next, player, true, firstTime ? PasteReason.NEW_ISLAND: PasteReason.RESET, oldIsland);
            } else {
                // Over world start
                //plugin.getLogger().info("DEBUG: pasting");
                //long timer = System.nanoTime();
                // Paste the island and teleport the player home
                schematic.pasteSchematic(next, player, true, firstTime ? PasteReason.NEW_ISLAND: PasteReason.RESET, oldIsland);
                //double diff = (System.nanoTime() - timer)/1000000;
                //plugin.getLogger().info("DEBUG: nano time = " + diff + " ms");
                //plugin.getLogger().info("DEBUG: pasted overworld");
                if (Settings.netherGenerate && Settings.netherIslands && IslandWorld.getNetherWorld() != null) {
                    // Paste the other world schematic
                    final Location netherLoc = next.toVector().toLocation(IslandWorld.getNetherWorld());
                    if (schematic.getPartnerName().isEmpty()) {
                        // This will paste the over world schematic again
                        //plugin.getLogger().info("DEBUG: pasting nether");
                        pastePartner(schematic, netherLoc, player);
                        //plugin.getLogger().info("DEBUG: pasted nether");
                    } else {
                        if (plugin.getSchematics().getAll().containsKey(schematic.getPartnerName())) {
                            //plugin.getLogger().info("DEBUG: pasting partner");
                            // A partner schematic is available
                            pastePartner(plugin.getSchematics().getAll().get(schematic.getPartnerName()),netherLoc, player);
                        } else {
                            plugin.getLogger().severe("Partner schematic heading '" + schematic.getPartnerName() + "' does not exist");
                        }
                    }
                }
            }
        }
    }

    /**
     * Does a delayed pasting of the partner island
     * @param schematic
     * @param player
     */
    private void pastePartner(final Schematic schematic, final Location loc, final Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                schematic.pasteSchematic(loc, player, false, PasteReason.PARTNER, null);

            }}, 60L);

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
