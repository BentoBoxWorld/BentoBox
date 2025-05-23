//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.util.teleport;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;


public class ClosestSafeSpotTeleport
{
    // ---------------------------------------------------------------------
    // Section: Constants
    // ---------------------------------------------------------------------

    /**
     * This comparator sorts position data based in order:
     * - the smallest distance value
     * - the smallest x value
     * - the smallest z value
     * - the smallest y value
     */
    private static final Comparator<PositionData> POSITION_COMPARATOR = Comparator
            .comparingDouble(PositionData::distance).thenComparingInt(position -> position.vector().getBlockX())
            .thenComparingInt(position -> position.vector().getBlockZ())
            .thenComparingInt(position -> position.vector().getBlockY());

    /**
     * Stores chunk load speed.
     */
    private static final long CHUNK_LOAD_SPEED = 1;

    /**
     * Range to scan
     */
    private static final int SCAN_RANGE = 20;

    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * BentoBox plugin instance.
     */
    private final BentoBox plugin;

    /**
     * Entity that will be teleported.
     */
    private final Entity entity;

    /**
     * Start location of teleportation.
     */
    private final Location location;

    /**
     * World where teleportation happens.
     */
    private final World world;

    /**
     * Runnable that will be triggered after successful teleportation.
     */
    private final Runnable successRunnable;

    /**
     * CompletableFuture that is triggered upon finishing position searching.
     */
    private final CompletableFuture<Boolean> result;

    /**
     * Boolean that indicates if teleportation should search for portal.
     */
    private final boolean portal;

    /**
     * Boolean that indicates if failing teleport should cancel it or create spot for player.
     */
    private final boolean cancelIfFail;

    /**
     * Local variable that indicates if current process is running.
     */
    private final AtomicBoolean checking = new AtomicBoolean();

    /**
     * The distance from starting location in all directions where new position will be searched.
     */
    private int range;

    /**
     * Block Queue for all blocks that should be validated.
     */
    private Queue<PositionData> blockQueue;

    /**
     * List of chunks that will be scanned for positions.
     */
    private Iterator<Pair<Integer, Integer>> chunksToScanIterator;

    /**
     * BoundingBox where teleportation can happen. Areas outside are illegal.
     */
    private BoundingBox boundingBox;

    /**
     * This method returns first best available spot if portal was not found in search area.
     */
    private Location noPortalPosition;

    /**
     * Bukkit task that processes chunks.
     */
    private BukkitTask task;

    /**
     * Teleports an entity to a safe spot on island
     *
     * @param builder - safe spot teleport builder
     */
    ClosestSafeSpotTeleport(Builder builder)
    {
        this.plugin = builder.getPlugin();
        this.entity = builder.getEntity();
        this.location = builder.getLocation();
        this.portal = builder.isPortal();

        this.successRunnable = builder.getSuccessRunnable();

        this.result = builder.getResult();
        this.world = Objects.requireNonNull(this.location.getWorld());

        this.cancelIfFail = builder.isCancelIfFail();

        // Try starting location
        Util.getChunkAtAsync(this.location).thenRun(this::checkLocation);
    }


    /**
     * This is main method that triggers safe spot search.
     * It starts with the given location and afterwards checks all blocks in required area.
     */
    void checkLocation()
    {
        if (!this.portal && this.plugin.getIslandsManager().isSafeLocation(this.location))
        {
            // If this is not a portal teleport, then go to the safe location immediately
            this.teleportEntity(this.location);
            // Position search is completed. Quit faster.
            return;
        }


        // Players should not be teleported outside protection range if they already are in it.
        this.boundingBox = this.plugin.getIslandsManager().getIslandAt(this.location).
                map(Island::getProtectionBoundingBox).
                orElseGet(() -> {
                    double protectionRange = this.plugin.getIWM().getIslandProtectionRange(this.world);

                    return new BoundingBox(this.location.getBlockX() - protectionRange,
                            Math.max(this.world.getMinHeight(), this.location.getBlockY() - protectionRange),
                            this.location.getBlockZ() - protectionRange,
                            this.location.getBlockX() + protectionRange,
                            Math.min(this.world.getMaxHeight(), this.location.getBlockY() + protectionRange),
                            this.location.getBlockZ() + protectionRange);
                });

        // The maximal range of search.
        this.range = Math.min(this.plugin.getSettings().getSafeSpotSearchRange(), (int) this.boundingBox.getWidthX() / 2);

        // The block queue contains all possible positions where player can be teleported. The queue will not be populated
        // with all blocks, as the validation would not allow it.ss
        this.blockQueue = new PriorityQueue<>(this.range * 2, ClosestSafeSpotTeleport.POSITION_COMPARATOR);

        // Get chunks to scan
        this.chunksToScanIterator = this.getChunksToScan().iterator();

        // Start a recurring task until done or cancelled
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::gatherChunks, 0L, CHUNK_LOAD_SPEED);
    }


    /**
     * This method loads all chunks in async and populates blockQueue with all blocks.
     */
    void gatherChunks()
    {
        // Set a flag so this is only run if it's not already in progress
        if (this.checking.get())
        {
            return;
        }

        this.checking.set(true);

        if (!this.portal && !this.blockQueue.isEmpty() && this.blockQueue.peek().distance() < 5)
        {
            // Position is found? Well most likely (not in all situations) position in block queue is already
            // the best position. The only bad situations could happen if position is on chunk borders.
            this.finishTask();
            return;
        }

        if (!this.chunksToScanIterator.hasNext())
        {
            // Chunk scanning has completed. Now check positions.
            this.finishTask();
            return;
        }

        // Get the chunk
        Pair<Integer, Integer> chunkPair = this.chunksToScanIterator.next();
        this.chunksToScanIterator.remove();

        // Get the chunk snapshot and scan it
        Util.getChunkAtAsync(Objects.requireNonNull(world), chunkPair.x, chunkPair.z)
                .thenApply(Chunk::getChunkSnapshot).whenCompleteAsync((snapshot, e) -> {
            if (snapshot != null)
            {
                // Find best spot based on collected information chunks.
                this.scanAndPopulateBlockQueue(snapshot);
            }

            this.checking.set(false);
        });
    }


    /**
     * Gets a set of chunk coordinates that will be scanned.
     *
     * @return - list of chunk coordinates to be scanned
     */
    List<Pair<Integer, Integer>> getChunksToScan()
    {
        List<Pair<Integer, Integer>> chunksToScan = new ArrayList<>();

        int x = this.location.getBlockX();
        int z = this.location.getBlockZ();

        // Normalize block coordinates to chunk coordinates and add extra 1 for visiting.
        int numberOfChunks = (((x + SCAN_RANGE) >> 4) - ((x - SCAN_RANGE) >> 4) + 1) *
                (((z + SCAN_RANGE) >> 4) - ((z - SCAN_RANGE) >> 4) + 1);

        // Ideally it would be if visitor switch from clockwise to counter-clockwise if X % 16 < 8 and
        // up to down if Z % 16 < 8.

        int offsetX = 0;
        int offsetZ = 0;

        for (int i = 0; i < numberOfChunks; ++i)
        {
            int locationX = x + (offsetX << 4);
            int locationZ = z + (offsetZ << 4);

            this.addChunk(chunksToScan, new Pair<>(locationX, locationZ), new Pair<>(locationX >> 4, locationZ >> 4));

            if (Math.abs(offsetX) <= Math.abs(offsetZ) && (offsetX != offsetZ || offsetX >= 0))
            {
                offsetX += ((offsetZ >= 0) ? 1 : -1);
            }
            else
            {
                offsetZ += ((offsetX >= 0) ? -1 : 1);
            }
        }

        return chunksToScan;
    }


    /**
     * This method adds chunk coordinates to the given chunksToScan list.
     * The limitation is that if location is in island, then block coordinate must also be in island space.
     * @param chunksToScan List of chunks that will be scanned.
     * @param blockCoord Block coordinates that must be in island.
     * @param chunkCoord Chunk coordinate.
     */
    void addChunk(List<Pair<Integer, Integer>> chunksToScan,
            Pair<Integer, Integer> blockCoord,
            Pair<Integer, Integer> chunkCoord)
    {
        if (!chunksToScan.contains(chunkCoord) &&
                this.plugin.getIslandsManager().getIslandAt(this.location).
                map(is -> is.inIslandSpace(blockCoord)).orElse(true))
        {
            chunksToScan.add(chunkCoord);
        }
    }


    /**
     * This method populates block queue with all blocks that player can be teleported to.
     * Add only positions that are inside BoundingBox and is safe for teleportation.
     * @param chunkSnapshot Spigot Chunk Snapshot with blocks.
     */
    void scanAndPopulateBlockQueue(ChunkSnapshot chunkSnapshot)
    {
        int startY = this.location.getBlockY();
        int minY = this.world.getMinHeight();
        int maxY = this.world.getMaxHeight();

        Vector blockVector = new Vector(this.location.getBlockX(), this.location.getBlockY(), this.location.getBlockZ());

        int chunkX = chunkSnapshot.getX() << 4;
        int chunkZ = chunkSnapshot.getZ() << 4;
        for (int x = 0; x < 16; x++)
        {
            for (int z = 0; z < 16; z++)
            {

                for (int y = Math.max(minY, startY - this.range); y < Math.min(maxY, startY + this.range); y++)
                {
                    Vector positionVector = new Vector(chunkX + x, y, chunkZ + z);
                    if (this.boundingBox.contains(positionVector))
                    {
                        // Process positions that are inside bounding box of search area.

                        PositionData positionData = new PositionData(
                                positionVector,
                                chunkSnapshot.getBlockType(x, y - 1, z),
                                y < maxY ? chunkSnapshot.getBlockType(x, y, z) : null,
                                        y + 1 < maxY ? chunkSnapshot.getBlockType(x, y + 1, z) : null,
                                                blockVector.distanceSquared(positionVector));

                        if (this.plugin.getIslandsManager().checkIfSafe(this.world,
                                positionData.block,
                                positionData.spaceOne,
                                positionData.spaceTwo))
                        {
                            // Add only safe locations to the queue.
                            this.blockQueue.add(positionData);
                        }
                    }
                }
            }
        }
    }


    /**
     * This method finishes the chunk loading task and checks from all remaining positions in block queue
     * to find the best location for teleportation.
     * <p>
     * This method stops position finding task and process teleporation.
     */
    void finishTask()
    {
        // Still Async!
        // Nothing left to check and still not canceled
        this.task.cancel();

        if (this.scanBlockQueue())
        {
            return;
        }

        if (this.portal && this.noPortalPosition != null)
        {
            this.teleportEntity(this.noPortalPosition);
        }
        else if (this.entity instanceof Player player)
        {
            // Return to main thread and teleport the player
            Bukkit.getScheduler().runTask(this.plugin, () -> returnAndTeleport(player));
        }
        // We do not teleport entities if position failed.
        // Fail the completion
        this.result.complete(false);

    }

    void returnAndTeleport(Player player) {
        // Notify player
        User.getInstance(this.entity).notify("general.errors.no-safe-location-found");

        // Check highest block
        Block highestBlock = this.world.getHighestBlockAt(this.location);

        if (highestBlock.getType().isSolid() &&
                this.plugin.getIslandsManager().isSafeLocation(highestBlock.getLocation()))
        {
            // Try to teleport player to the highest block.
            this.asyncTeleport(highestBlock.getLocation().add(new Vector(0.5D, 0D, 0.5D)));
        }
        else if (!this.plugin.getIWM().inWorld(this.entity.getLocation()))
        {
            // Last resort
            player.performCommand("spawn");
        }
        else if (!this.cancelIfFail)
        {
            // Create a spot for the player to be
            switch(world.getEnvironment()) {
            case NETHER -> this.makeAndTeleport(Material.NETHERRACK);
            case THE_END -> this.makeAndTeleport(Material.END_STONE);
            default -> this.makeAndTeleport(Material.COBBLESTONE);
            }
        }
    }


    /**
     * This method creates a spot in start location for player to be teleported to. It creates 2 base material blocks
     * above location and fills the space between them with air.
     * @param baseMaterial Material that will be for top and bottom block.
     */
    void makeAndTeleport(Material baseMaterial)
    {
        this.location.getBlock().getRelative(BlockFace.DOWN).setType(baseMaterial, false);
        this.location.getBlock().setType(Material.AIR, false);
        this.location.getBlock().getRelative(BlockFace.UP).setType(Material.AIR, false);
        this.location.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).setType(baseMaterial, false);

        // Teleport player to the location of the empty space.
        this.asyncTeleport(this.location.clone().add(new Vector(0.5D, 0D, 0.5D)));
    }


    /**
     * This method scans all populated positions and returns true if position is found, or false, if not.
     * @return {@code true} if safe position is found, otherwise false.
     */
    boolean scanBlockQueue()
    {
        boolean blockFound = false;

        while (!this.blockQueue.isEmpty() && !blockFound)
        {
            blockFound = this.checkPosition(this.blockQueue.poll());
        }

        return blockFound;
    }


    /**
     * This method triggers a task that will teleport entity in a main thread.
     */
    void teleportEntity(final Location location)
    {
        // Return to main thread and teleport the player
        Bukkit.getScheduler().runTask(this.plugin, () -> this.asyncTeleport(location));
    }


    /**
     * This method performs async teleportation and runs end tasks for spot-finder.
     * @param location Location where player should be teleported.
     */
    void asyncTeleport(final Location location)
    {
        Util.teleportAsync(Objects.requireNonNull(entity), Objects.requireNonNull(location)).thenRun(() ->
        {
            if (this.successRunnable != null)
            {
                Bukkit.getScheduler().runTask(this.plugin, this.successRunnable);
            }

            this.result.complete(true);
        });
    }


    /**
     * This method checks if given position is valid for teleportation.
     * If query should find portal, then it marks first best position as noPortalPosition and continues
     * to search for a valid portal.
     * If query is not in portal mode, then return first valid position.
     * @param positionData Position data that must be checked.
     * @return {@code true} if position is found and no extra processing required, {@code false} otherwise.
     */
    boolean checkPosition(PositionData positionData)
    {
        if (this.portal)
        {
            if (Material.NETHER_PORTAL.equals(positionData.spaceOne()) ||
                    Material.NETHER_PORTAL.equals(positionData.spaceTwo()))
            {
                // Portal is found. Teleport entity to the portal location.
                this.teleportEntity(new Location(this.world,
                        positionData.vector().getBlockX() + 0.5,
                        positionData.vector().getBlockY() + 0.1,
                        positionData.vector().getBlockZ() + 0.5,
                        this.location.getYaw(),
                        this.location.getPitch()));

                // Position found and player can is already teleported to it.
                return true;
            }
            else if (this.noPortalPosition == null)
            {
                // Mark first incoming position as the best for teleportation.
                this.noPortalPosition = new Location(this.world,
                        positionData.vector().getBlockX() + 0.5,
                        positionData.vector().getBlockY() + 0.1,
                        positionData.vector().getBlockZ() + 0.5,
                        this.location.getYaw(),
                        this.location.getPitch());
            }
        }
        else
        {
            // First best position should be valid for teleportation.
            this.teleportEntity(new Location(this.world,
                    positionData.vector().getBlockX() + 0.5,
                    positionData.vector().getBlockY() + 0.1,
                    positionData.vector().getBlockZ() + 0.5,
                    this.location.getYaw(),
                    this.location.getPitch()));
            return true;
        }

        return false;
    }


    /**
     * PositionData record holds information about position where player will be teleported.
     * @param vector Vector of the position.
     * @param block Block material on which player will be placed.
     * @param spaceOne Material one block above block.
     * @param spaceTwo Material two blocks above block.
     * @param distance Distance till the position.
     */
    record PositionData(Vector vector, Material block, Material spaceOne, Material spaceTwo, double distance) {}


    public static Builder builder(BentoBox plugin)
    {
        return new Builder(plugin);
    }


    // ---------------------------------------------------------------------
    // Section: Builder
    // ---------------------------------------------------------------------


    /**
     * Builder for ClosestSafeSpotTeleport
     *
     */
    public static class Builder
    {
        private Builder(BentoBox plugin)
        {
            this.plugin = plugin;
            this.result = new CompletableFuture<>();
        }

        // ---------------------------------------------------------------------
        // Section: Builders
        // ---------------------------------------------------------------------



        /**
         * Set who or what is going to teleport
         *
         * @param entity entity to teleport
         * @return Builder
         */
        public Builder entity(Entity entity)
        {
            this.entity = entity;
            return this;
        }


        /**
         * Set the desired location
         *
         * @param location the location
         * @return Builder
         */
        public Builder location(Location location)
        {
            this.location = location;
            return this;
        }


        /**
         * This is a portal teleportation
         *
         * @return Builder
         */
        public Builder portal()
        {
            this.portal = true;
            return this;
        }


        /**
         * This is a successRunnable for teleportation
         *
         * @return Builder
         */
        public Builder successRunnable(Runnable successRunnable)
        {
            this.successRunnable = successRunnable;
            return this;
        }


        /**
         * Try to teleport the player
         *
         * @return ClosestSafeSpotTeleport
         */
        @Nullable
        public ClosestSafeSpotTeleport build()
        {
            // Error checking
            if (this.entity == null)
            {
                this.plugin.logError("Attempt to safe teleport a null entity!");
                this.result.complete(null);
                return null;
            }

            if (this.location == null)
            {
                this.plugin.logError("Attempt to safe teleport to a null location!");
                this.result.complete(null);
                return null;
            }

            if (this.location.getWorld() == null)
            {
                this.plugin.logError("Attempt to safe teleport to a null world!");
                this.result.complete(null);
                return null;
            }

            return new ClosestSafeSpotTeleport(this);
        }


        // ---------------------------------------------------------------------
        // Section: Getters
        // ---------------------------------------------------------------------


        /**
         * Gets plugin.
         *
         * @return the plugin
         */
        public BentoBox getPlugin()
        {
            return this.plugin;
        }


        /**
         * Gets result.
         *
         * @return the result
         */
        public CompletableFuture<Boolean> getResult()
        {
            return this.result;
        }


        /**
         * Gets entity.
         *
         * @return the entity
         */
        public Entity getEntity()
        {
            return this.entity;
        }


        /**
         * Gets location.
         *
         * @return the location
         */
        public Location getLocation()
        {
            return this.location;
        }


        /**
         * Gets world.
         *
         * @return the world
         */
        public World getWorld()
        {
            return this.world;
        }


        /**
         * Gets success runnable.
         *
         * @return the success runnable
         */
        public Runnable getSuccessRunnable()
        {
            return this.successRunnable;
        }

        /**
         * Is portal boolean.
         *
         * @return the boolean
         */
        public boolean isPortal()
        {
            return this.portal;
        }


        /**
         * Is cancel if fail boolean.
         *
         * @return the boolean
         */
        public boolean isCancelIfFail()
        {
            return this.cancelIfFail;
        }


        // ---------------------------------------------------------------------
        // Section: Variables
        // ---------------------------------------------------------------------


        /**
         * BentoBox plugin instance.
         */
        private final BentoBox plugin;

        /**
         * CompletableFuture that is triggered upon finishing position searching.
         */
        private final CompletableFuture<Boolean> result;

        /**
         * Entity that will be teleported.
         */
        private Entity entity;

        /**
         * Start location of teleportation.
         */
        private Location location;

        /**
         * World where teleportation happens.
         */
        private World world;

        /**
         * Runnable that will be triggered after successful teleportation.
         */
        private Runnable successRunnable;

        /**
         * Boolean that indicates if teleportation should search for portal.
         */
        private boolean portal;

        /**
         * Boolean that indicates if failing teleport should cancel it or create spot for player.
         */
        private boolean cancelIfFail;
    }
}

