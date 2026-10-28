//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.teleports;


import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalForcer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.ExpiringSet;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;


/**
 * This abstract class contains all common methods for entity and player teleportation.
 */
public abstract sealed class AbstractTeleportListener
        permits EntityTeleportListener, PlayerTeleportListener
{
    /**
     * Instance of Teleportation processor.
     * @param bentoBox BentoBox plugin.
     */
    AbstractTeleportListener(@NonNull BentoBox bentoBox)
    {
        this.plugin = bentoBox;
        this.inPortal = new ExpiringSet<>(10, TimeUnit.SECONDS);
        this.inTeleport = new ExpiringSet<>(10, TimeUnit.SECONDS);
        this.teleportOrigin = new ConcurrentHashMap<>();
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Get island at the given location
     * @return optional island at given location
     */
    protected Optional<Island> getIsland(Location location)
    {
        return this.plugin.getIslands().getProtectedIslandAt(location);
    }


    /**
     * Get island for given player at the given world.
     * @return optional island at given world.
     */
    protected Optional<Island> getIsland(World world, Player player)
    {
        return Optional.ofNullable(this.plugin.getIslandsManager().getIsland(world, player.getUniqueId()));
    }


    /**
     * Check if vanilla portals should be used
     *
     * @param world - game mode world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isMakePortals(World world, World.Environment environment)
    {
        return this.plugin.getIWM().getAddon(world).
                map(gameMode -> this.isMakePortals(gameMode, environment)).
                orElse(false);
    }


    /**
     * Check if vanilla portals should be used
     *
     * @param gameMode - game mode
     * @param environment - environment
     * @return true or false
     */
    protected boolean isMakePortals(GameModeAddon gameMode, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> gameMode.getWorldSettings().isMakeNetherPortals();
        case THE_END -> gameMode.getWorldSettings().isMakeEndPortals();
        default -> false;
        };
    }


    /**
     * Check if nether or end are generated
     *
     * @param overWorld - game world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isAllowedInConfig(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().isNetherGenerate(overWorld);
        case THE_END -> this.plugin.getIWM().isEndGenerate(overWorld);
        default -> true;
        };
    }


    /**
     * Check if the default nether or end are allowed by the server settings
     *
     * @param environment - environment
     * @return true or false
     */
    protected boolean isAllowedOnServer(World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> Bukkit.getAllowNether();
        case THE_END -> Bukkit.getAllowEnd();
        default -> true;
        };
    }


    /**
     * Check if nether or end islands are generated
     *
     * @param overWorld - over world
     * @param environment - environment
     * @return true or false
     */
    protected boolean isIslandWorld(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().isNetherIslands(overWorld);
        case THE_END -> this.plugin.getIWM().isEndIslands(overWorld);
        default -> true;
        };
    }


    /**
     * Get the nether or end world
     *
     * @param overWorld - over world
     * @param environment - environment
     * @return nether or end world
     */
    protected World getNetherEndWorld(World overWorld, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> this.plugin.getIWM().getNetherWorld(overWorld);
        case THE_END -> this.plugin.getIWM().getEndWorld(overWorld);
        default -> Util.getWorld(overWorld);
        };
    }


    /**
     * Check if the island has a nether or end island already
     *
     * @param island - island
     * @param environment - environment
     * @return true or false
     */
    protected java.util.concurrent.CompletableFuture<Boolean> hasPartnerIsland(Island island, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> island.hasNetherIslandAsync();
        case THE_END -> island.hasEndIslandAsync();
        default -> java.util.concurrent.CompletableFuture.completedFuture(true);
        };
    }


    /**
     * Synchronous variant for portal events that already run on the island's owner thread.
     *
     * @param island - island
     * @param environment - environment
     * @return true if the island already has that dimension
     */
    protected boolean hasPartnerIslandSync(Island island, World.Environment environment)
    {
        return switch (environment) {
        case NETHER -> island.hasNetherIsland();
        case THE_END -> island.hasEndIsland();
        default -> true;
        };
    }

    /**
     * Pastes at most once per island dimension. Two members entering the same portal before the
     * first paste finishes must not run two pastes over the same island: that duplicates the
     * blueprint's entities and interleaves block writes.
     */
    private static final ConcurrentHashMap<String, java.util.concurrent.CompletableFuture<Void>> pastes =
            new ConcurrentHashMap<>();

    java.util.concurrent.CompletableFuture<Void> pasteOnce(Island island, World.Environment environment,
            java.util.function.Supplier<java.util.concurrent.CompletableFuture<Void>> paste)
    {
        String key = island.getUniqueId() + ":" + environment;
        var result = pastes.computeIfAbsent(key, ignored -> paste.get());
        result.whenComplete((ignored, error) -> pastes.remove(key, result));
        return result;
    }

    private static final ThreadLocal<java.util.Map<org.bukkit.event.Event, Runnable>> dispatch = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> nativeTransfer = ThreadLocal.withInitial(() -> false);

    /** Synthetic events have no server continuation: commit only after the entire event bus returns. */
    static boolean dispatchPortal(org.bukkit.event.Event event, boolean nativePortal) {
        var previous = dispatch.get();
        boolean previousNative = nativeTransfer.get();
        var pending = new java.util.IdentityHashMap<org.bukkit.event.Event, Runnable>();
        dispatch.set(pending);
        nativeTransfer.set(nativePortal);
        try {
            Bukkit.getPluginManager().callEvent(event);
            if (!((org.bukkit.event.Cancellable) event).isCancelled() && pending.containsKey(event)) {
                pending.get(event).run();
            }
            return pending.containsKey(event);
        } finally {
            if (previous == null) dispatch.remove(); else dispatch.set(previous);
            nativeTransfer.set(previousNative);
        }
    }

    protected void afterPortal(org.bukkit.event.Event event, org.bukkit.entity.Entity entity, Runnable action) {
        if (nativeTransfer.get()) return;
        if (dispatch.get() != null) dispatch.get().put(event, action);
        else TaskScheduler.entity(plugin, entity, () -> {
            if (!((org.bukkit.event.Cancellable) event).isCancelled()) action.run();
        });
    }

    protected boolean isSyntheticPortal() {
        return dispatch.get() != null;
    }


    /**
     * This method calculates the maximal search area for portal.
     * @param location Location from which search should happen.
     * @param island Island that contains the search point.
     * @return Search range for portal.
     */
    protected int calculateSearchRadius(Location location, Island island)
    {
        int diff;

        if (island.onIsland(location))
        {
            // Find max x or max z
            int x = Math.abs(island.getProtectionCenter().getBlockX() - location.getBlockX());
            int z = Math.abs(island.getProtectionCenter().getBlockZ() - location.getBlockZ());

            diff = Math.min(this.plugin.getSettings().getMinPortalSearchRadius(),
                    island.getProtectionRange() + (island.getRange() - island.getProtectionRange())*2 - Math.max(x, z));
        }
        else
        {
            diff = this.plugin.getSettings().getMinPortalSearchRadius();
        }

        return diff;
    }


    /**
     * This method calculates location for portal.
     * @param fromLocation Location from which teleportation happens.
     * @param fromWorld World from which teleportation happens.
     * @param toWorld The target world.
     * @param environment Portal variant.
     * @param canCreatePortal Indicates if portal should be created or not.
     * @return Location for new portal.
     */
    protected Location calculateLocation(Location fromLocation,
            World fromWorld,
            World toWorld,
            World.Environment environment,
            boolean canCreatePortal)
    {
        // Null check - not that useful
        if (fromWorld == null || toWorld == null)
        {
            return null;
        }

        Optional<Location> endSpawn = getEndIslandSpawn(fromLocation, toWorld);
        if (endSpawn.isPresent()) return endSpawn.get();

        Location toLocation = fromLocation.toVector().toLocation(toWorld);

        if (!this.isMakePortals(fromWorld, environment))
        {
            toLocation = Objects.requireNonNullElse(this.getIsland(fromLocation).
                    map(island -> island.getSpawnPoint(toWorld.getEnvironment())).
                    orElse(toLocation), toLocation);
        }

        // Limit Y to the min/max world height.
        toLocation.setY(Math.max(Math.min(toLocation.getY(), toWorld.getMaxHeight()), toWorld.getMinHeight()));

        if (!canCreatePortal)
        {
            // Legacy portaling
            return toLocation;
        }

        // Make portals
        // For anywhere other than the end - it is the player's location that is used
        if (!environment.equals(World.Environment.THE_END))
        {
            return toLocation;
        }

        // If the-end then we want the platform to always be generated in the same place no matter where
        // they enter the portal
        final int x = fromLocation.getBlockX();
        final int z = fromLocation.getBlockZ();
        final int y = fromLocation.getBlockY();
        int i = x;
        int j = z;
        int k = y;

        // If the from is not a portal, then we have to find it
        if (!fromLocation.getBlock().getType().equals(Material.END_PORTAL))
        {
            // Search portal block 5 blocks in all directions from starting location. Return the first one.
            boolean continueSearch = true;

            // simplistic search pattern to look at all blocks from the middle outwards by preferring
            // Y location first, then Z and as last X
            // Proper implementation would require queue and distance calculation.

            for (int offsetX = 0; continueSearch && offsetX < 10; offsetX++)
            {
                // Change sign based on mod value.
                int posX = x + ((offsetX % 2 == 0) ? 1 : -1) * (offsetX / 2);

                for (int offsetZ = 0; continueSearch && offsetZ < 10; offsetZ++)
                {
                    // Change sign based on mod value.
                    int posZ = z + ((offsetZ % 2 == 0) ? 1 : -1) * (offsetZ / 2);

                    for (int offsetY = 0; continueSearch && offsetY < 10; offsetY++)
                    {
                        // Change sign based on mod value.
                        int posY = y + ((offsetY % 2 == 0) ? 1 : -1) * (offsetY / 2);

                        if (fromWorld.getBlockAt(posX, posY, posZ).getType().equals(Material.END_PORTAL))
                        {
                            i = posX;
                            j = posZ;
                            k = posY;
                            continueSearch = false;
                        }
                    }
                }
            }
        }

        // Find the maximum x and z corner using relative block search.
        Block portalBlock = fromWorld.getBlockAt(i, k, j);

        while (portalBlock.getRelative(1, 0, 0).getType() == Material.END_PORTAL)
        {
            portalBlock = portalBlock.getRelative(1, 0, 0);
            i++;
        }

        while (portalBlock.getRelative(0, 0, 1).getType() == Material.END_PORTAL)
        {
            portalBlock = portalBlock.getRelative(0, 0, 1);
            j++;
        }

        // Mojang end platform generation is:
        // AIR
        // AIR
        // OBSIDIAN
        // and player is placed on second air block above obsidian.
        // If Y coordinate is below 2, then obsidian platform is not generated and player falls in void.
        return new Location(toWorld, i - 0.5, Math.min(toWorld.getMaxHeight() - 2, Math.max(toWorld.getMinHeight() + 2, k)), j - 0.5);
    }


    protected Optional<Location> getEndIslandSpawn(Location from, World target) {
        World overWorld = Util.getWorld(from.getWorld());
        if (target == null || target.getEnvironment() != World.Environment.THE_END
                || overWorld == null || !isIslandWorld(overWorld, World.Environment.THE_END)) return Optional.empty();
        return getIsland(from).map(island -> island.getSpawnPoint(World.Environment.THE_END))
                .filter(spawn -> target.equals(spawn.getWorld()) && Double.isFinite(spawn.getX())
                        && Double.isFinite(spawn.getY()) && Double.isFinite(spawn.getZ())
                        && spawn.getY() >= target.getMinHeight() && spawn.getY() < target.getMaxHeight())
                .map(Location::clone);
    }

    /**
     * Builds the End obsidian arrival platform at the given destination.
     * <p>
     * Vanilla creates the platform at the fixed {@code ServerLevel.END_SPAWN_POINT}
     * (around 100, 50, 0). When BentoBox redirects the player to a different
     * location in The End via {@link org.bukkit.event.player.PlayerPortalEvent#setTo(Location)},
     * no platform exists at the destination and the player falls into the void.
     * This method places a 5x5 obsidian floor with 3 layers of clear space above so
     * the player has a safe landing.
     *
     * @param destination location in The End where the player will arrive.
     */
    protected void createEndPlatform(@NonNull Location destination)
    {
        World world = destination.getWorld();
        if (world == null || !World.Environment.THE_END.equals(world.getEnvironment()))
        {
            return;
        }

        int baseX = destination.getBlockX();
        int baseY = destination.getBlockY();
        int baseZ = destination.getBlockZ();

        // Only touch fully empty columns: the destination is normally void, but a player may
        // already have built there. Overwriting that would destroy blocks and container contents.
        for (int dx = -2; dx <= 2; dx++)
        {
            for (int dz = -2; dz <= 2; dz++)
            {
                Block floor = world.getBlockAt(baseX + dx, baseY - 1, baseZ + dz);
                boolean empty = floor.isEmpty();
                for (int dy = 0; empty && dy < 3; dy++)
                {
                    empty = world.getBlockAt(baseX + dx, baseY + dy, baseZ + dz).isEmpty();
                }
                if (empty)
                {
                    floor.setType(Material.OBSIDIAN, false);
                }
            }
        }
    }

    /** Each column may belong to a different region at a chunk boundary. */
    protected java.util.concurrent.CompletableFuture<Void> createEndPlatformAsync(Location destination) {
        var world = destination.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        var columns = new java.util.ArrayList<java.util.concurrent.CompletableFuture<Void>>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Location column = new Location(world, destination.getBlockX() + dx,
                        destination.getBlockY(), destination.getBlockZ() + dz);
                columns.add(Util.getChunkAtAsync(column).thenCompose(chunk ->
                        TaskScheduler.regionFuture(plugin, column, () -> {
                            // Never overwrite an existing build: only fill columns that are void.
                            Block floor = column.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                            boolean empty = floor.isEmpty();
                            for (int dy = 0; empty && dy < 3; dy++) {
                                empty = world.getBlockAt(column.getBlockX(), column.getBlockY() + dy,
                                        column.getBlockZ()).isEmpty();
                            }
                            if (empty) floor.setType(Material.OBSIDIAN, false);
                        })));
            }
        }
        return java.util.concurrent.CompletableFuture.allOf(columns.toArray(java.util.concurrent.CompletableFuture[]::new));
    }


    /**
     * Creates the return Nether portal exactly the way the server does it.
     * <p>
     * On Folia BentoBox cancels the server's portal transfer and teleports the player itself, so
     * vanilla's "create a portal on arrival" step never runs. This hands that job back to vanilla:
     * the same PortalForcer lookup and creation that Entity#changeDimension uses, which picks the
     * site, builds the obsidian frame with the right axis and lights it.
     *
     * @param destination location in the island Nether where the player arrives.
     * @param axis orientation of the portal the player came through.
     * @return the portal block to arrive at, or {@code null} if there is no portal.
     */
    protected java.util.concurrent.CompletableFuture<Location> ensureNetherPortal(@NonNull Location destination, Axis axis)
    {
        // Never build portals automatically: every trip landed at a slightly different Y, so each one
        // created another portal. Reuse an existing one, or give up and let the caller fall back.
        return ensureNetherPortal(destination, axis, false);
    }

    /**
     * Finds or optionally builds the paired portal at the destination.
     *
     * @param destination where the entity is going
     * @param axis        orientation for a newly built portal
     * @param create      whether a missing portal may be built. Entities must never build one:
     *                    every mob would get its own portal and spawn more mobs around it.
     * @return the portal entrance, or null when none exists (and creating is not allowed)
     */
    protected java.util.concurrent.CompletableFuture<Location> ensureNetherPortal(@NonNull Location destination, Axis axis,
            boolean create)
    {
        World world = destination.getWorld();
        // Any dimension with a portal forcer can host the paired portal: the destination may be the
        // nether (overworld -> nether) or the island overworld (nether -> overworld).
        if (world == null || World.Environment.THE_END.equals(world.getEnvironment()))
        {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        Location anchor = destination.clone();
        java.util.concurrent.atomic.AtomicReference<Location> arrival = new java.util.concurrent.atomic.AtomicReference<>();
        return Util.getChunkAtAsync(anchor).thenCompose(chunk ->
                TaskScheduler.regionFuture(plugin, anchor, () -> {
                    clearMisalignedPortals(world, anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
                    ServerLevel level = ((CraftWorld) world).getHandle();
                    BlockPos pos = new BlockPos(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
                    PortalForcer forcer = level.getPortalForcer();
                    // Same order as vanilla: reuse a portal that is already in range, else build one.
                    BlockPos portal = forcer.findClosestPortalPosition(pos, true, level.getWorldBorder()).orElse(null);
                    if (portal == null && create)
                    {
                        forcer.createPortal(pos, Axis.X == axis ? Direction.Axis.X : Direction.Axis.Z);
                        portal = forcer.findClosestPortalPosition(pos, true, level.getWorldBorder()).orElse(null);
                    }
                    if (portal != null)
                    {
                        // Vanilla puts the entity in the portal, not at the raw coordinate.
                        arrival.set(new Location(world, portal.getX() + 0.5, portal.getY(), portal.getZ() + 0.5));
                    }
                })).thenApply(ignored -> arrival.get());
    }


    /**
     * Reads the axis of the portal a player walked into, so the paired portal is built with the
     * same orientation the way vanilla does it.
     *
     * @param from portal block location, may be null.
     * @return portal axis, X when unknown.
     */
    protected static Axis portalAxis(@Nullable Location from)
    {
        if (from != null && from.getBlock().getBlockData() instanceof Orientable data)
        {
            return data.getAxis();
        }
        return Axis.X;
    }


    /**
     * Removes portal blocks left by an earlier build with the wrong axis. Such blocks have no
     * neighbour along Z but one along X, which no correctly oriented portal ever has, so real
     * player portals are left alone.
     */
    private static void clearMisalignedPortals(World world, int ax, int ay, int az)
    {
        for (int dx = -8; dx <= 8; dx++)
        {
            for (int dz = -8; dz <= 8; dz++)
            {
                for (int dy = -4; dy <= 6; dy++)
                {
                    Block block = world.getBlockAt(ax + dx, ay + dy, az + dz);
                    if (!Material.NETHER_PORTAL.equals(block.getType()))
                    {
                        continue;
                    }
                    if (!(block.getBlockData() instanceof Orientable data) || Axis.Z != data.getAxis())
                    {
                        continue;
                    }
                    boolean alongZ = Material.NETHER_PORTAL.equals(world.getBlockAt(block.getX(), block.getY(), block.getZ() - 1).getType())
                            || Material.NETHER_PORTAL.equals(world.getBlockAt(block.getX(), block.getY(), block.getZ() + 1).getType());
                    boolean alongX = Material.NETHER_PORTAL.equals(world.getBlockAt(block.getX() - 1, block.getY(), block.getZ()).getType())
                            || Material.NETHER_PORTAL.equals(world.getBlockAt(block.getX() + 1, block.getY(), block.getZ()).getType());
                    if (alongX && !alongZ)
                    {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }


    /**
     * This method returns spawn location for given world.
     * @param world World which spawn point must be returned.
     * @return Spawn location for world or null.
     */
    @Nullable
    protected Location getSpawnLocation(World world)
    {
        return this.plugin.getIslandsManager().getSpawn(world).map(island ->
        island.getSpawnPoint(World.Environment.NORMAL) == null ?
                island.getCenter() :
                    island.getSpawnPoint(World.Environment.NORMAL)).
                orElseGet(() -> {
                    // Folia must not inspect another world from this region thread. The safe spot
                    // teleport sorts out an unsafe spawn instead of this check.
                    if (TaskScheduler.isFolia())
                    {
                        return world.getSpawnLocation();
                    }
                    return this.plugin.getIslands().isSafeLocation(world.getSpawnLocation()) ?
                            world.getSpawnLocation() : null;
                });
    }


    /**
     * This method returns if missing islands should be generated upon teleportation.
     * Can happen only in non-custom generators.
     * @param overWorld OverWorld
     * @return {@code true} if missing islands must be pasted, {@code false} otherwise.
     */
    protected boolean isPastingMissingIslands(World overWorld)
    {
        return this.plugin.getIWM().isPasteMissingIslands(overWorld) &&
                !this.plugin.getIWM().isUseOwnGenerator(overWorld);
    }


    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------


    /**
     * BentoBox plugin instance.
     */
    @NonNull
    protected final BentoBox plugin;

    /**
     * Set of entities that currently is inside portal.
     */
    protected final ExpiringSet<UUID> inPortal;

    /**
     * Map that links entities origin of teleportation. Used for respawning.
     */
    protected final Map<UUID, World> teleportOrigin;

    /**
     * Set of entities that currently is in teleportation.
     */
    protected final Set<UUID> inTeleport;

    /**
     * @return the inTeleport
     */
    public Set<UUID> getInTeleport() {
        return inTeleport;
    }

    /**
     * @return the inPortal
     */
    public Set<UUID> getInPortal() {
        return inPortal;
    }

    /**
     * @return the teleportOrigin
     */
    public Map<UUID, World> getTeleportOrigin() {
        return teleportOrigin;
    }
}
