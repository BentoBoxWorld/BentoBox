package world.bentobox.bentobox.util.teleport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there.
 * @author tastybento
 *
 */
public class SafeSpotTeleport {

    private static final int MAX_CHUNKS = 6;
    private static final long SPEED = 1;
    private static final int MAX_RADIUS = 50;
    private static final int MAX_HEIGHT = 235;
    private boolean notChecking;
    private BukkitTask task;

    // Parameters
    private final Entity entity;
    private final Location location;
    private boolean portal;
    private final int homeNumber;

    // Locations
    private Location bestSpot;

    private BentoBox plugin;
    private List<Pair<Integer, Integer>> chunksToScan;

    /**
     * Teleports and entity to a safe spot on island
     * @param plugin - plugin object
     * @param entity - entity to teleport
     * @param location - the location initial desired location to go to
     * @param failureMessage - locale key for the failure message
     * @param portal - true if this is a portal teleport
     * @param homeNumber - home number to go to
     */
    public SafeSpotTeleport(BentoBox plugin, final Entity entity, final Location location, final String failureMessage, boolean portal, int homeNumber) {
        this.plugin = plugin;
        this.entity = entity;
        this.location = location;
        this.portal = portal;
        this.homeNumber = homeNumber;

        // If there is no portal scan required, try the desired location immediately
        if (plugin.getIslands().isSafeLocation(location)) {
            if (portal) {
                // If the desired location is safe, then that's where you'll go if there's no portal
                bestSpot = location;
            } else {
                // If this is not a portal teleport, then go to the safe location immediately
                entity.teleport(location);
                return;
            }
        }

        // Get chunks to scan
        chunksToScan = getChunksToScan();

        // Start checking
        notChecking = true;

        // Start a recurring task until done or cancelled
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> gatherChunks(failureMessage), 0L, SPEED);
    }

    private void gatherChunks(String failureMessage) {
        if (!notChecking) {
            return;
        }
        notChecking = false;
        List<ChunkSnapshot> chunkSnapshot = new ArrayList<>();
        Iterator<Pair<Integer, Integer>> it = chunksToScan.iterator();
        if (!it.hasNext()) {
            // Nothing left
            tidyUp(entity, failureMessage);
            return;
        }
        // Add chunk snapshots to the list
        while (it.hasNext() && chunkSnapshot.size() < MAX_CHUNKS) {
            Pair<Integer, Integer> pair = it.next();
            if (location.getWorld() != null) {
                boolean isLoaded = location.getWorld().getChunkAt(pair.x, pair.z).isLoaded();
                chunkSnapshot.add(location.getWorld().getChunkAt(pair.x, pair.z).getChunkSnapshot());
                if (!isLoaded) {
                    location.getWorld().getChunkAt(pair.x, pair.z).unload();
                }
            }
            it.remove();
        }
        // Move to next step
        checkChunks(chunkSnapshot);
    }

    private void tidyUp(Entity entity, String failureMessage) {
        // Nothing left to check and still not canceled
        task.cancel();
        // Check portal
        if (portal && bestSpot != null) {
            // Portals found, teleport to the best spot we found
            teleportEntity(bestSpot);
        } else if (entity instanceof Player) {
            // Failed, no safe spot
            if (!failureMessage.isEmpty()) {
                User.getInstance(entity).notify(failureMessage);
            }
            if (!plugin.getIWM().inWorld(entity.getLocation())) {
                // Last resort
                if (Bukkit.getServer().isPrimaryThread()) {
                    ((Player)entity).performCommand("spawn");
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> ((Player)entity).performCommand("spawn"));
                }
            } else {
                // Create a spot for the player to be
                if (location.getWorld().getEnvironment().equals(Environment.NETHER)) {
                    makeAndTelport(Material.NETHERRACK);
                } else if (location.getWorld().getEnvironment().equals(Environment.THE_END)) {
                    makeAndTelport(Material.END_STONE);
                } else {
                    makeAndTelport(Material.COBBLESTONE);
                }
            }
        }
    }

    private void makeAndTelport(Material m) {
        location.getBlock().getRelative(BlockFace.DOWN).setType(m, false);
        location.getBlock().setType(Material.AIR, false);
        location.getBlock().getRelative(BlockFace.UP).setType(Material.AIR, false);
        location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(m, false);
        entity.teleport(location.clone().add(new Vector(0.5D, 0D, 0.5D)));
    }

    /**
     * Gets a set of chunk coords that will be scanned.
     * @return - list of chunk coords to be scanned
     */
    private List<Pair<Integer, Integer>> getChunksToScan() {
        List<Pair<Integer, Integer>> result = new ArrayList<>();
        int maxRadius = plugin.getIslands().getIslandAt(location).map(Island::getProtectionRange).orElse(plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        maxRadius = Math.min(MAX_RADIUS, maxRadius);
        int x = location.getBlockX();
        int z = location.getBlockZ();
        // Create ever increasing squares around the target location
        int radius = 0;
        do {
            for (int i = x - radius; i <= x + radius; i+=16) {
                for (int j = z - radius; j <= z + radius; j+=16) {
                    addChunk(result, new Pair<>(i,j), new Pair<>(i >> 4, j >> 4));
                }
            }
            radius++;
        } while (radius < maxRadius);
        return result;
    }

    private void addChunk(List<Pair<Integer, Integer>> result, Pair<Integer, Integer> blockCoord, Pair<Integer, Integer> chunkCoord) {
        if (!result.contains(chunkCoord) && plugin.getIslands().getIslandAt(location).map(is -> is.inIslandSpace(blockCoord)).orElse(true)) {
            result.add(chunkCoord);
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
            notChecking = true;
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
                    if (checkBlock(chunk, x,y,z)) {
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
        if (!portal && entity instanceof Player && homeNumber > 0) {
            // Set home if so marked
            plugin.getPlayers().setHomeLocation(User.getInstance(entity), loc, homeNumber);
        }
        Vector velocity = entity.getVelocity();
        // Return to main thread and teleport the player
        Bukkit.getScheduler().runTask(plugin, () -> PaperLib.teleportAsync(entity, loc).thenAccept(b -> entity.setVelocity(velocity)));
    }

    /**
     * Returns true if the location is a safe one.
     * @param chunk - chunk snapshot
     * @param x - x coordinate
     * @param y - y coordinate
     * @param z - z coordinate
     * @return true if this is a safe spot, false if this is a portal scan
     */
    private boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z) {
        World world = location.getWorld();
        Material type = chunk.getBlockType(x, y, z);
        if (!type.equals(Material.AIR)) { // AIR
            Material space1 = chunk.getBlockType(x, Math.min(y + 1, SafeSpotTeleport.MAX_HEIGHT), z);
            Material space2 = chunk.getBlockType(x, Math.min(y + 2, SafeSpotTeleport.MAX_HEIGHT), z);
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
         * @deprecated As of 1.6.0, for removal. No longer in use as the player's gamemode is no longer changed upon teleporting.
         */
        @Deprecated
        public Builder overrideGamemode(boolean overrideGamemode) {
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
            return new SafeSpotTeleport(plugin, entity, location, failureMessage, portal, homeNumber);
        }
    }
}