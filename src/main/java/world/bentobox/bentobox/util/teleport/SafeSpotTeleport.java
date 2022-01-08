package world.bentobox.bentobox.util.teleport;

import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there.
 *
 * @author tastybento
 */
public class SafeSpotTeleport {

    private static final int MAX_CHUNKS = 6;
    private static final long SPEED = 1;
    private static final int MAX_RADIUS = 50;
    // Parameters
    private final Entity entity;
    private final Location location;
    private final int homeNumber;
    private final BentoBox plugin;
    private final Runnable runnable;
    private final Runnable failRunnable;
    private final CompletableFuture<Boolean> result;
    private final String homeName;
    private final int maxHeight;
    private final World world;
    private final AtomicBoolean checking = new AtomicBoolean();
    private BukkitTask task;
    private boolean portal;
    // Locations
    private Location bestSpot;
    private Iterator<Pair<Integer, Integer>> chunksToScanIterator;
    private int checkedChunks = 0;

    /**
     * Teleports and entity to a safe spot on island
     *
     * @param builder - safe spot teleport builder
     */
    SafeSpotTeleport(Builder builder) {
        this.plugin = builder.getPlugin();
        this.entity = builder.getEntity();
        this.location = builder.getLocation();
        this.portal = builder.isPortal();
        this.homeNumber = builder.getHomeNumber();
        this.homeName = builder.getHomeName();
        this.runnable = builder.getRunnable();
        this.failRunnable = builder.getFailRunnable();
        this.result = builder.getResult();
        this.world = Objects.requireNonNull(location.getWorld());
        this.maxHeight = world.getMaxHeight() - 20;
        // Try to go
        Util.getChunkAtAsync(location).thenRun(() -> tryToGo(builder.getFailureMessage()));
    }

    void tryToGo(String failureMessage) {
        if (plugin.getIslands().isSafeLocation(location)) {
            if (portal) {
                // If the desired location is safe, then that's where you'll go if there's no portal
                bestSpot = location;
            } else {
                // If this is not a portal teleport, then go to the safe location immediately
                Util.teleportAsync(entity, location).thenRun(() -> {
                    if (runnable != null) Bukkit.getScheduler().runTask(plugin, runnable);
                    result.complete(true);
                });
                return;
            }
        }
        // Get chunks to scan
        chunksToScanIterator = getChunksToScan().iterator();

        // Start a recurring task until done or cancelled
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> gatherChunks(failureMessage), 0L, SPEED);
    }

    boolean gatherChunks(String failureMessage) {
        // Set a flag so this is only run if it's not already in progress
        if (checking.get()) {
            return false;
        }
        checking.set(true);
        if (checkedChunks > MAX_CHUNKS || !chunksToScanIterator.hasNext()) {
            // Nothing left
            tidyUp(entity, failureMessage);
            return false;
        }

        // Get the chunk
        Pair<Integer, Integer> chunkPair = chunksToScanIterator.next();
        chunksToScanIterator.remove();
        checkedChunks++;
        if (checkedChunks >= MAX_CHUNKS) {
            checking.set(false);
            return false;
        }

        // Get the chunk snapshot and scan it
        Util.getChunkAtAsync(world, chunkPair.x, chunkPair.z)
        .thenApply(Chunk::getChunkSnapshot)
        .whenCompleteAsync((snapshot, e) -> {
            if (snapshot != null && scanChunk(snapshot)) {
                task.cancel();
            } else {
                checking.set(false);
            }
        });
        return true;
    }

    void tidyUp(Entity entity, String failureMessage) {
        // Still Async!
        // Nothing left to check and still not canceled
        task.cancel();
        // Check portal
        if (portal && bestSpot != null) {
            // Portals found, teleport to the best spot we found
            teleportEntity(bestSpot);
        } else if (entity instanceof Player player) {
            // Return to main thread and teleport the player
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Failed, no safe spot
                if (!failureMessage.isEmpty()) {
                    User.getInstance(entity).notify(failureMessage);
                }
                if (!plugin.getIWM().inWorld(entity.getLocation())) {
                    // Last resort
                    player.performCommand("spawn");
                } else {
                    // Create a spot for the player to be
                    if (world.getEnvironment().equals(Environment.NETHER)) {
                        makeAndTeleport(Material.NETHERRACK);
                    } else if (world.getEnvironment().equals(Environment.THE_END)) {
                        makeAndTeleport(Material.END_STONE);
                    } else {
                        makeAndTeleport(Material.COBBLESTONE);
                    }
                }
                if (failRunnable != null) {
                    Bukkit.getScheduler().runTask(plugin, failRunnable);
                }
                result.complete(false);
            });
        } else {
            if (failRunnable != null) {
                Bukkit.getScheduler().runTask(plugin, failRunnable);
            }
            result.complete(false);
        }
    }

    void makeAndTeleport(Material m) {
        location.getBlock().getRelative(BlockFace.DOWN).setType(m, false);
        location.getBlock().setType(Material.AIR, false);
        location.getBlock().getRelative(BlockFace.UP).setType(Material.AIR, false);
        location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(m, false);
        Util.teleportAsync(entity, location.clone().add(new Vector(0.5D, 0D, 0.5D))).thenRun(() -> {
            if (runnable != null) Bukkit.getScheduler().runTask(plugin, runnable);
            result.complete(true);
        });
    }

    /**
     * Gets a set of chunk coords that will be scanned.
     *
     * @return - list of chunk coords to be scanned
     */
    List<Pair<Integer, Integer>> getChunksToScan() {
        List<Pair<Integer, Integer>> chunksToScan = new ArrayList<>();
        int maxRadius = plugin.getIslands().getIslandAt(location).map(Island::getProtectionRange).orElseGet(() -> plugin.getIWM().getIslandProtectionRange(world));
        maxRadius = Math.min(MAX_RADIUS, maxRadius);
        int x = location.getBlockX();
        int z = location.getBlockZ();
        // Create ever increasing squares around the target location
        int radius = 0;
        do {
            for (int i = x - radius; i <= x + radius; i += 16) {
                for (int j = z - radius; j <= z + radius; j += 16) {
                    addChunk(chunksToScan, new Pair<>(i, j), new Pair<>(i >> 4, j >> 4));
                }
            }
            radius++;
        } while (radius < maxRadius);
        return chunksToScan;
    }

    private void addChunk(List<Pair<Integer, Integer>> chunksToScan, Pair<Integer, Integer> blockCoord, Pair<Integer, Integer> chunkCoord) {
        if (!chunksToScan.contains(chunkCoord) && plugin.getIslands().getIslandAt(location).map(is -> is.inIslandSpace(blockCoord)).orElse(true)) {
            chunksToScan.add(chunkCoord);
        }
    }

    /**
     * @param chunk - chunk snapshot
     * @return true if a safe spot was found
     */
    boolean scanChunk(ChunkSnapshot chunk) {
        int startY = location.getBlockY();
        int minY = world.getMinHeight();
        int maxY = 60; // Just a dummy value

        // Check the safe spot at the current height
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (minY >= startY && checkBlock(chunk, x, startY, z)) {
                    return true;
                }
                maxY = Math.max(chunk.getHighestBlockYAt(x, z), maxY);
            }
        }
        maxY = Math.min(maxY, maxHeight);

        // Expand the height up and down until a safe spot is found
        int upperY = startY + 1;
        int lowerY = startY - 1;
        boolean checkUpper = upperY <= maxY;
        boolean checkLower = lowerY >= minY;
        int limitRange = plugin.getSettings().getSafeSpotSearchVerticalRange(); // Limit the y-coordinate range
        while (limitRange > 0 && (checkUpper || checkLower)) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    if (checkUpper && checkBlock(chunk, x, upperY, z)) {
                        return true;
                    }
                    if (checkLower && checkBlock(chunk, x, lowerY, z)) {
                        return true;
                    }
                }
            }
            if (checkUpper) {
                upperY++;
                if (upperY > maxY) {
                    checkUpper = false;
                }
            }
            if (checkLower) {
                lowerY--;
                if (lowerY < minY) {
                    checkLower = false;
                }
            }
            limitRange--;
        }

        // We can't find a safe spot
        return false;
    }

    /**
     * Teleports entity to the safe spot
     */
    void teleportEntity(final Location loc) {
        task.cancel();
        // Return to main thread and teleport the player
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!portal && entity instanceof Player && (homeNumber > 0 || !homeName.isEmpty())) {
                // Set home if so marked
                plugin.getIslands().setHomeLocation(User.getInstance(entity), loc, homeName);
            }
            Util.teleportAsync(entity, loc).thenRun(() -> {
                if (runnable != null) Bukkit.getScheduler().runTask(plugin, runnable);
                result.complete(true);
            });
        });
    }

    /**
     * Returns true if the location is a safe one.
     *
     * @param chunk - chunk snapshot
     * @param x     - x coordinate
     * @param y     - y coordinate
     * @param z     - z coordinate
     * @return true if this is a safe spot, false if this is a portal scan
     */
    boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z) {
        Material type = chunk.getBlockType(x, y, z);
        Material space1 = chunk.getBlockType(x, Math.min(y + 1, maxHeight), z);
        Material space2 = chunk.getBlockType(x, Math.min(y + 2, maxHeight), z);
        if (space1.equals(Material.NETHER_PORTAL) || space2.equals(Material.NETHER_PORTAL)) {
            // A portal has been found, switch to non-portal mode now
            portal = false;
        }
        if (plugin.getIslands().checkIfSafe(world, type, space1, space2)) {
            return safe(chunk, x, y, z, world);
        }
        return false;
    }

    boolean safe(ChunkSnapshot chunk, int x, int y, int z, World world) {
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
        private final BentoBox plugin;
        private final CompletableFuture<Boolean> result = new CompletableFuture<>();
        private Entity entity;
        private int homeNumber = 0;
        private String homeName = "";
        private boolean portal = false;
        private String failureMessage = "";
        private Location location;
        private Runnable runnable;
        private Runnable failRunnable;

        public Builder(BentoBox plugin) {
            this.plugin = plugin;
        }

        /**
         * Set who or what is going to teleport
         *
         * @param entity entity to teleport
         * @return Builder
         */
        public Builder entity(Entity entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Set the island to teleport to
         *
         * @param island island destination
         * @return Builder
         */
        public Builder island(Island island) {
            this.location = island.getProtectionCenter();
            return this;
        }

        /**
         * Set the home number to this number
         *
         * @param homeNumber home number
         * @return Builder
         * @deprecated use {@link #homeName}
         */
        @Deprecated
        public Builder homeNumber(int homeNumber) {
            this.homeNumber = homeNumber;
            return this;
        }

        /**
         * Set the home name
         *
         * @param homeName - home name
         * @return Builder
         * @since 1.16.0
         */
        public Builder homeName(String homeName) {
            this.homeName = homeName;
            return this;
        }

        /**
         * This is a portal teleportation
         *
         * @return Builder
         */
        public Builder portal() {
            this.portal = true;
            return this;
        }

        /**
         * Set the failure message if this teleport cannot happen
         *
         * @param failureMessage failure message to report to user
         * @return Builder
         */
        public Builder failureMessage(String failureMessage) {
            this.failureMessage = failureMessage;
            return this;
        }

        /**
         * Set the desired location
         *
         * @param location the location
         * @return Builder
         */
        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        /**
         * Try to teleport the player
         *
         * @return CompletableFuture that will become true if successful and false if not
         * @since 1.14.0
         */
        @Nullable
        public CompletableFuture<Boolean> buildFuture() {
            build();
            return result;
        }

        /**
         * Try to teleport the player
         *
         * @return SafeSpotTeleport
         */
        @Nullable
        public SafeSpotTeleport build() {
            // Error checking
            if (entity == null) {
                plugin.logError("Attempt to safe teleport a null entity!");
                result.complete(null);
                return null;
            }
            if (location == null) {
                plugin.logError("Attempt to safe teleport to a null location!");
                result.complete(null);
                return null;
            }
            if (location.getWorld() == null) {
                plugin.logError("Attempt to safe teleport to a null world!");
                result.complete(null);
                return null;
            }
            if (failureMessage.isEmpty() && entity instanceof Player) {
                failureMessage = "general.errors.no-safe-location-found";
            }
            return new SafeSpotTeleport(this);
        }

        /**
         * The task to run after the player is safely teleported.
         *
         * @param runnable - task
         * @return Builder
         * @since 1.13.0
         */
        public Builder thenRun(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        /**
         * The task to run if the player is not safely teleported
         *
         * @param runnable - task
         * @return Builder
         * @since 1.18.0
         */
        public Builder ifFail(Runnable runnable) {
            this.failRunnable = runnable;
            return this;
        }

        /**
         * @return the plugin
         */
        public BentoBox getPlugin() {
            return plugin;
        }

        /**
         * @return the entity
         */
        public Entity getEntity() {
            return entity;
        }

        /**
         * @return the homeNumber
         */
        public int getHomeNumber() {
            return homeNumber;
        }

        /**
         * @return the homeName
         */
        public String getHomeName() {
            return homeName;
        }

        /**
         * @return the portal
         */
        public boolean isPortal() {
            return portal;
        }

        /**
         * @return the failureMessage
         */
        public String getFailureMessage() {
            return failureMessage;
        }

        /**
         * @return the location
         */
        public Location getLocation() {
            return location;
        }

        /**
         * @return the runnable
         */
        public Runnable getRunnable() {
            return runnable;
        }

        /**
         * @return the result
         * @since 1.14.0
         */
        public CompletableFuture<Boolean> getResult() {
            return result;
        }

        /**
         * @return the failRunnable
         */
        public Runnable getFailRunnable() {
            return failRunnable;
        }


    }
}
