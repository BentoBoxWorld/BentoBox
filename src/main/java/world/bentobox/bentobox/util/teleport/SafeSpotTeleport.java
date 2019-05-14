package world.bentobox.bentobox.util.teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there.
 * @author tastybento
 *
 */
public class SafeSpotTeleport {

    private static final int MAX_CHUNKS = 200;
    private static final long SPEED = 1;
    private static final int MAX_RADIUS = 200;
    private static final int MAX_HEIGHT = 235;
    private boolean checking;
    private BukkitTask task;

    // Parameters
    private final Entity entity;
    private final Location location;
    private boolean portal;
    private final int homeNumber;
    private final boolean overrideGamemode;

    // Locations
    private Location bestSpot;

    private BentoBox plugin;
    private List<Pair<Integer, Integer>> chunksToScan;

    /**
     * Teleports and entity to a safe spot on island
     * @param plugin - plugin object
     * @param entity - entity to teleport
     * @param location - the location
     * @param failureMessage - locale key for the failure message
     * @param portal - true if this is a portal teleport
     * @param homeNumber - home number to go to
     */
    public SafeSpotTeleport(BentoBox plugin, final Entity entity, final Location location, final String failureMessage, boolean portal, int homeNumber, boolean overrideGamemode) {
        this.plugin = plugin;
        this.entity = entity;
        this.location = location;
        this.portal = portal;
        this.homeNumber = homeNumber;
        this.overrideGamemode = overrideGamemode;

        // Put player into spectator mode
        if (overrideGamemode && entity instanceof Player && ((Player)entity).getGameMode().equals(GameMode.SURVIVAL)) {
            ((Player)entity).setGameMode(GameMode.SPECTATOR);
        }

        // If there is no portal scan required, try the desired location immediately
        if (plugin.getIslands().isSafeLocation(location)) {
            if (portal) {
                // If the desired location is safe, then that's where you'll go if there's no portal
                bestSpot = location;
            } else {
                // If this is not a portal teleport, then go to the safe location immediately
                entity.teleport(location);
                // Exit spectator mode if in it
                if (entity instanceof Player) {
                    Player player = (Player)entity;
                    if (overrideGamemode && player.getGameMode().equals(GameMode.SPECTATOR)) {
                        player.setGameMode(plugin.getIWM().getDefaultGameMode(player.getWorld()));
                    }
                }
                return;
            }
        }

        // Get chunks to scan
        chunksToScan = getChunksToScan();

        // Start checking
        checking = true;

        // Start a recurring task until done or cancelled
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<ChunkSnapshot> chunkSnapshot = new ArrayList<>();
            if (checking) {
                Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
                if (!it.hasNext()) {
                    // Nothing left
                    tidyUp(entity, failureMessage);
                    return;
                }
                // Add chunk snapshots to the list
                while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
                    Pair<Integer, Integer> pair = it.next();
                    chunkSnapshot.add(location.getWorld().getChunkAt(pair.x, pair.z).getChunkSnapshot());
                    it.remove();
                }
                // Move to next step
                checking = false;
                checkChunks(chunkSnapshot);
            }
        }, 0L, SPEED);
    }

    private void tidyUp(Entity entity, String failureMessage) {
        // Nothing left to check and still not canceled
        task.cancel();
        // Check portal
        if (portal && bestSpot != null) {
            // Portals found, teleport to the best spot we found
            teleportEntity(bestSpot);
            if (overrideGamemode && entity instanceof Player && ((Player)entity).getGameMode().equals(GameMode.SPECTATOR)) {
                ((Player)entity).setGameMode(plugin.getIWM().getDefaultGameMode(bestSpot.getWorld()));
            }
        } else if (entity instanceof Player) {
            // Failed, no safe spot
            if (!failureMessage.isEmpty()) {
                User.getInstance(entity).notify(failureMessage);
            }
            if (overrideGamemode && ((Player)entity).getGameMode().equals(GameMode.SPECTATOR)) {
                if (plugin.getIWM().inWorld(entity.getLocation())) {
                    ((Player)entity).setGameMode(plugin.getIWM().getDefaultGameMode(entity.getWorld()));
                } else {
                    // Last resort
                    ((Player)entity).setGameMode(GameMode.SURVIVAL);
                    if (Bukkit.getServer().isPrimaryThread()) {
                        ((Player)entity).performCommand("spawn");
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> ((Player)entity).performCommand("spawn"));
                    }
                }
            }
        }

    }

    /**
     * Gets a set of chunk coords that will be scanned.
     * @return - list of chunk coords to be scanned
     */
    private List<Pair<Integer, Integer>> getChunksToScan() {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        // Get island if available
        Optional<Island> island = plugin.getIslands().getIslandAt(location);
        if (!island.isPresent()) {
            return new ArrayList<>();
        }
        int maxRadius = island.map(Island::getProtectionRange).orElse(plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        maxRadius = maxRadius > MAX_RADIUS ? MAX_RADIUS : maxRadius;

        int x = location.getBlockX();
        int z = location.getBlockZ();
        // Create ever increasing squares around the target location
        int radius = 0;
        do {
            for (int i = x - radius; i <= x + radius; i+=16) {
                for (int j = z - radius; j <= z + radius; j+=16) {
                    addChunk(result, island, new Pair<>(i,j), new Pair<>(i >> 4, j >> 4));
                }
            }
            radius++;
        } while (radius < maxRadius);
        return result;
    }

    private void addChunk(List<Pair<Integer, Integer>> result, Optional<Island> island, Pair<Integer, Integer> blockCoord, Pair<Integer, Integer> chunkCoord) {
        if (!result.contains(chunkCoord)) {
            // Add the chunk coord
            if (!island.isPresent()) {
                // If there is no island, just add it
                result.add(chunkCoord);
            } else {
                // If there is an island, only add it if the coord is in island space
                island.ifPresent(is -> {
                    if (is.inIslandSpace(blockCoord)) {
                        result.add(chunkCoord);
                    }
                });
            }
        }
    }

    /**
     * Loops through the chunks and if a safe spot is found, fires off the teleportation
     * @param chunkSnapshot - list of chunk snapshots to check
     */
    private void checkChunks(final List<ChunkSnapshot> chunkSnapshot) {
        // Run async task to scan chunks
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (ChunkSnapshot chunk: chunkSnapshot) {
                if (scanChunk(chunk)) {
                    task.cancel();
                    return;
                }
            }
            // Nothing happened, change state
            checking = true;
        });
    }


    /**
     * @param chunk - chunk snapshot
     * @return true if a safe spot was found
     */
    private boolean scanChunk(ChunkSnapshot chunk) {
        // Run through the chunk
        for (int x = 0; x< 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Work down from the entry point up
                for (int y = Math.min(chunk.getHighestBlockYAt(x, z), MAX_HEIGHT); y >= 0; y--) {
                    if (checkBlock(chunk, x,y,z, MAX_HEIGHT)) {
                        return true;
                    }
                } // end y
            } //end z
        } // end x
        return false;
    }

    /**
     * Teleports entity to the safe spot
     */
    private void teleportEntity(final Location loc) {
        task.cancel();
        // Return to main thread and teleport the player
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!portal && entity instanceof Player && homeNumber > 0) {
                // Set home if so marked
                plugin.getPlayers().setHomeLocation(User.getInstance(entity), loc, homeNumber);
            }
            Vector velocity = entity.getVelocity();
            entity.teleport(loc);
            // Exit spectator mode if in it
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (overrideGamemode && player.getGameMode().equals(GameMode.SPECTATOR)) {
                    player.setGameMode(plugin.getIWM().getDefaultGameMode(loc.getWorld()));
                }
            } else {
                entity.setVelocity(velocity);
            }
        });

    }

    /**
     * Returns true if the location is a safe one.
     * @param chunk - chunk snapshot
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @param worldHeight - height of world
     * @return true if this is a safe spot, false if this is a portal scan
     */
    private boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z, int worldHeight) {
        World world = location.getWorld();
        Material type = chunk.getBlockType(x, y, z);
        if (!type.equals(Material.AIR)) { // AIR
            Material space1 = chunk.getBlockType(x, Math.min(y + 1, worldHeight), z);
            Material space2 = chunk.getBlockType(x, Math.min(y + 2, worldHeight), z);
            if ((space1.equals(Material.AIR) && space2.equals(Material.AIR)) || (space1.equals(Material.NETHER_PORTAL) && space2.equals(Material.NETHER_PORTAL))
                    && (!type.toString().contains("FENCE") && !type.toString().contains("DOOR") && !type.toString().contains("GATE") && !type.toString().contains("PLATE")
                    && !type.toString().contains("SIGN"))) {
                switch (type) {
                // Unsafe
                case ANVIL:
                case BARRIER:
                case CACTUS:
                case END_PORTAL:
                case FIRE:
                case FLOWER_POT:
                case LADDER:
                case LAVA:
                case LEVER:
                case TALL_GRASS:
                case PISTON_HEAD:
                case MOVING_PISTON:
                case STONE_BUTTON:
                case TORCH:
                case TRIPWIRE:
                case WATER:
                case COBWEB:
                    //Block is dangerous
                    break;
                case NETHER_PORTAL:
                    if (portal) {
                        // A portal has been found, switch to non-portal mode now
                        portal = false;
                    }
                    break;
                default:
                    return safe(chunk, x, y, z, world);
                }
            }
        }
        return false;
    }

    private boolean safe(ChunkSnapshot chunk, int x, int y, int z, World world) {
        Vector newSpot = new Vector((chunk.getX() << 4) + x + 0.5D, y + 1.0D, (chunk.getZ() << 4) + z + 0.5D);
        if (portal) {
            if (bestSpot == null) {
                // Stash the best spot
                bestSpot = newSpot.toLocation(world);
            }
            return false;
        } else {
            teleportEntity(newSpot.toLocation(world));
            return true;
        }
    }

    public static class Builder {
        private BentoBox plugin;
        private Entity entity;
        private int homeNumber = 0;
        private boolean portal = false;
        private String failureMessage = "";
        private Location location;
        private boolean overrideGamemode = true;

        public Builder(BentoBox plugin) {
            this.plugin = plugin;
        }

        /**
         * Set who or what is going to teleport
         * @param entity entity to teleport
         * @return Builder
         */
        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Set the island to teleport to
         * @param island island destination
         * @return Builder
         */
        public Builder island(Island island) {
            this.location = island.getCenter();
            return this;
        }

        /**
         * Set the home number to this number
         * @param homeNumber home number
         * @return Builder
         */
        public Builder homeNumber(int homeNumber) {
            this.homeNumber = homeNumber;
            return this;
        }

        /**
         * This is a portal teleportation
         * @return Builder
         */
        public Builder portal() {
            this.portal = true;
            return this;
        }

        /**
         * Set the failure message if this teleport cannot happen
         * @param failureMessage failure message to report to user
         * @return Builder
         */
        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        /**
         * Set the desired location
         * @param location the location
         * @return Builder
         */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Sets whether the player's gamemode should be overridden. Default is <tt>true</tt>
         * @param overrideGamemode whether the player's gamemode should be overridden.
         * @return Builder
         */
        public Builder overrideGamemode(boolean overrideGamemode) {
            this.overrideGamemode = overrideGamemode;
            return this;
        }

        /**
         * Try to teleport the player
         * @return SafeSpotTeleport
         */
        @Nullable
        public SafeSpotTeleport build() {
            // Error checking
            if (entity == null) {
                plugin.logError("Attempt to safe teleport a null entity!");
                return null;
            }
            if (location == null) {
                plugin.logError("Attempt to safe teleport to a null location!");
                return null;
            }
            if (failureMessage.isEmpty() && entity instanceof Player) {
                failureMessage = "general.errors.no-safe-location-found";
            }
            return new SafeSpotTeleport(plugin, entity, location, failureMessage, portal, homeNumber, overrideGamemode);
        }
    }
}