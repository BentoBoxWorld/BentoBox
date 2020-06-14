package world.bentobox.bentobox.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.DeleteIslandChunks;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 */
public class IslandsManager {

    private BentoBox plugin;

    // Tree species to boat material map
    private static final Map<TreeSpecies, Material> TREE_TO_BOAT = ImmutableMap.<TreeSpecies, Material>builder().
            put(TreeSpecies.ACACIA, Material.ACACIA_BOAT).
            put(TreeSpecies.BIRCH, Material.BIRCH_BOAT).
            put(TreeSpecies.DARK_OAK, Material.DARK_OAK_BOAT).
            put(TreeSpecies.JUNGLE, Material.JUNGLE_BOAT).
            put(TreeSpecies.GENERIC, Material.OAK_BOAT).
            put(TreeSpecies.REDWOOD, Material.SPRUCE_BOAT).build();

    /**
     * One island can be spawn, this is the one - otherwise, this value is null
     */
    @NonNull
    private Map<@NonNull World, @Nullable Island> spawn;

    @NonNull
    private Database<Island> handler;

    /**
     * The last locations where an island were put.
     * This is not stored persistently and resets when the server starts
     */
    private Map<World, Location> last;

    // Island Cache
    @NonNull
    private IslandCache islandCache;
    // Quarantined islands
    @NonNull
    private Map<UUID, List<Island>> quarantineCache;
    // Deleted islands
    @NonNull
    private List<String> deletedIslands;

    private Set<String> toSave = new HashSet<>();

    private BukkitTask task;

    /**
     * Islands Manager
     * @param plugin - plugin
     */
    public IslandsManager(@NonNull BentoBox plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        handler = new Database<>(plugin, Island.class);
        islandCache = new IslandCache();
        quarantineCache = new HashMap<>();
        spawn = new HashMap<>();
        last = new HashMap<>();
        // This list should always be empty unless database deletion failed
        // In that case a purge utility may be required in the future
        deletedIslands = new ArrayList<>();
    }

    /**
     * Used only for testing. Sets the database to a mock database.
     * @param handler - handler
     */
    public void setHandler(Database<Island> handler) {
        this.handler = handler;
    }

    /**
     * This is a generic scan that can work in the overworld or the nether
     * @param l - location around which to scan
     * @param i - the range to scan for a location less than 0 means the full island.
     * @return - safe location, or null if none can be found
     */
    @Nullable
    public Location bigScan(@NonNull Location l, int i) {
        final int height;
        final int depth;
        if (i > 0) {
            height = i;
            depth = i;
        } else {
            Optional<Island> island = getIslandAt(l);
            if (!island.isPresent()) {
                return null;
            }
            i = island.get().getProtectionRange();
            height = l.getWorld().getMaxHeight() - l.getBlockY();
            depth = l.getBlockY();
        }

        // Work outwards from l until the closest safe location is found.
        int minXradius = 0;
        int maxXradius = 0;
        int minZradius = 0;
        int maxZradius = 0;
        int minYradius = 0;
        int maxYradius = 0;

        do {
            int minX = l.getBlockX()-minXradius;
            int minZ = l.getBlockZ()-minZradius;
            int minY = l.getBlockY()-minYradius;
            int maxX = l.getBlockX()+maxXradius;
            int maxZ = l.getBlockZ()+maxZradius;
            int maxY = l.getBlockY()+maxYradius;
            for (int x = minX; x<= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        if (!((x > minX && x < maxX) && (z > minZ && z < maxZ) && (y > minY && y < maxY))) {
                            Location ultimate = new Location(l.getWorld(), x + 0.5D, y, z + 0.5D);
                            if (isSafeLocation(ultimate)) {
                                return ultimate;
                            }
                        }
                    }
                }
            }
            if (minXradius < i) {
                minXradius++;
            }
            if (maxXradius < i) {
                maxXradius++;
            }
            if (minZradius < i) {
                minZradius++;
            }
            if (maxZradius < i) {
                maxZradius++;
            }
            if (minYradius < depth) {
                minYradius++;
            }
            if (maxYradius < height) {
                maxYradius++;
            }
        } while (minXradius < i || maxXradius < i || minZradius < i || maxZradius < i || minYradius < depth
                || maxYradius < height);
        // Nothing worked
        return null;
    }

    /**
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     *
     * @param l Location to be checked, not null.
     * @return true if safe, otherwise false
     */
    public boolean isSafeLocation(@NonNull Location l) {
        Block ground = l.getBlock().getRelative(BlockFace.DOWN);
        Block space1 = l.getBlock();
        Block space2 = l.getBlock().getRelative(BlockFace.UP);
        return checkIfSafe(l.getWorld(), ground.getType(), space1.getType(), space2.getType());
    }

    /**
     * Checks if this location is safe for a player to teleport to and loads chunks async to check.
     *
     * @param l Location to be checked, not null.
     * @return a completable future that will be true if safe, otherwise false
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> isSafeLocationAsync(@NonNull Location l) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Util.getChunkAtAsync(l).thenRun(() -> {
            Block ground = l.getBlock().getRelative(BlockFace.DOWN);
            Block space1 = l.getBlock();
            Block space2 = l.getBlock().getRelative(BlockFace.UP);
            result.complete(checkIfSafe(l.getWorld(), ground.getType(), space1.getType(), space2.getType()));
        });
        return result;
    }

    /**
     * Check if a location is safe for teleporting
     * @param world - world
     * @param ground Material of the block that is going to be the ground
     * @param space1 Material of the block above the ground
     * @param space2 Material of the block that is two blocks above the ground
     * @return {@code true} if the location is considered safe, {@code false} otherwise.
     */
    public boolean checkIfSafe(@Nullable World world, @NonNull Material ground, @NonNull Material space1, @NonNull Material space2) {
        // Ground must be solid, space 1 and 2 must not be solid
        if (world == null || !ground.isSolid()
                || (space1.isSolid() && !space1.name().contains("SIGN"))
                || (space2.isSolid() && !space2.name().contains("SIGN"))) {
            return false;
        }
        // Cannot be submerged or water cannot be dangerous
        if (space1.equals(Material.WATER) && (space2.equals(Material.WATER) || plugin.getIWM().isWaterNotSafe(world))) {
            return false;
        }
        // Lava
        if (ground.equals(Material.LAVA)
                || space1.equals(Material.LAVA)
                || space2.equals(Material.LAVA)) {
            return false;
        }
        // Unsafe types
        if (((space1.equals(Material.AIR) && space2.equals(Material.AIR))
                || (space1.equals(Material.NETHER_PORTAL) && space2.equals(Material.NETHER_PORTAL)))
                && (ground.name().contains("FENCE")
                        || ground.name().contains("DOOR")
                        || ground.name().contains("GATE")
                        || ground.name().contains("PLATE")
                        || ground.name().contains("SIGN")
                        || ground.name().contains("BANNER")
                        || ground.name().contains("BUTTON")
                        || ground.name().contains("BOAT"))) {
            return false;
        }
        // Known unsafe blocks
        switch (ground) {
        // Unsafe
        case ANVIL:
        case BARRIER:
        case CACTUS:
        case END_PORTAL:
        case END_ROD:
        case FIRE:
        case FLOWER_POT:
        case LADDER:
        case LEVER:
        case TALL_GRASS:
        case PISTON_HEAD:
        case MOVING_PISTON:
        case TORCH:
        case WALL_TORCH:
        case TRIPWIRE:
        case WATER:
        case COBWEB:
        case NETHER_PORTAL:
        case MAGMA_BLOCK:
            return false;
        default:
            return true;
        }
    }

    /**
     * Create an island with no owner at location
     * @param location the location, not null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(@NonNull Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not paste blocks. It just creates the island data object.
     * @param location the location, not null
     * @param owner the island owner UUID, may be null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(@NonNull Location location, @Nullable UUID owner) {
        Island island = new Island(location, owner, plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        // Game the gamemode name and prefix the uniqueId
        String gmName = plugin.getIWM().getAddon(location.getWorld()).map(gm -> gm.getDescription().getName()).orElse("");
        island.setGameMode(gmName);
        island.setUniqueId(gmName + island.getUniqueId());
        while (handler.objectExists(island.getUniqueId())) {
            // This should never happen, so although this is a potential infinite loop I'm going to leave it here because
            // it will be bad if this does occur and the server should crash.
            plugin.logWarning("Duplicate island UUID occurred");
            island.setUniqueId(gmName + UUID.randomUUID().toString());
        }
        if (islandCache.addIsland(island)) {
            return island;
        }
        return null;
    }

    /**
     * Deletes island.
     * @param island island to delete, not null
     * @param removeBlocks whether the island blocks should be removed or not
     * @param involvedPlayer - player related to the island deletion, if any
     */
    public void deleteIsland(@NonNull Island island, boolean removeBlocks, @Nullable UUID involvedPlayer) {
        // Fire event
        IslandBaseEvent event = IslandEvent.builder().island(island).involvedPlayer(involvedPlayer).reason(Reason.DELETE).build();
        if (event.isCancelled()) {
            return;
        }
        // Set the owner of the island to no one.
        island.setOwner(null);
        island.setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
        if (removeBlocks) {
            // Remove island from the cache
            islandCache.deleteIslandFromCache(island);
            // Log the deletion (it shouldn't matter but may be useful)
            island.log(new LogEntry.Builder("DELETED").build());
            // Set the delete flag which will prevent it from being loaded even if database deletion fails
            island.setDeleted(true);
            // Save the island
            handler.saveObjectAsync(island);
            // Delete the island
            handler.deleteObject(island);
            // Remove players from island
            removePlayersFromIsland(island);
            // Remove blocks from world
            new DeleteIslandChunks(plugin, new IslandDeletion(island));
        }
    }

    public int getIslandCount() {
        return islandCache.size();
    }

    public int getIslandCount(@NonNull World world) {
        return islandCache.size(world);
    }

    /**
     * Gets the island for this player.
     * If they are in a team, the team island is returned.
     * @param world world to check
     * @param user user
     * @return Island or null
     */
    @Nullable
    public Island getIsland(@NonNull World world, @NonNull User user){
        return islandCache.get(world, user.getUniqueId());
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned.
     * @param world world to check. Includes nether and end worlds.
     * @param uuid user's uuid
     * @return Island or null
     */
    @Nullable
    public Island getIsland(@NonNull World world, @NonNull UUID uuid){
        return islandCache.get(world, uuid);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes the full island space, not just the protected area.
     * Use {@link #getProtectedIslandAt(Location)} for only the protected island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getIslandAt(@NonNull Location location) {
        return plugin.getIWM().inWorld(location) ? Optional.ofNullable(islandCache.getIslandAt(location)) : Optional.empty();
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all existing islands (even those who may be unowned).
     * @return unmodifiable collection containing every island.
     * @since 1.1
     */
    @NonNull
    public Collection<Island> getIslands() {
        return islandCache.getIslands();
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even those who may be unowned) in the specified world.
     * @param world World of the gamemode.
     * @return unmodifiable collection containing all the islands in the specified world.
     * @since 1.7.0
     */
    @NonNull
    public Collection<Island> getIslands(@NonNull World world) {
        return islandCache.getIslands(world);
    }

    /**
     * Returns the IslandCache instance.
     * @return the islandCache
     * @since 1.5.0
     */
    @NonNull
    public IslandCache getIslandCache() {
        return islandCache;
    }

    /**
     * Used for testing only to inject the islandCache mock object
     * @param islandCache - island cache
     */
    public void setIslandCache(@NonNull IslandCache islandCache) {
        this.islandCache = islandCache;
    }

    /**
     * Returns the player's island location in World
     * Returns an island location OR a team island location
     *
     * @param world - world to check
     * @param uuid - the player's UUID
     * @return Location of player's island or null if one does not exist
     */
    @Nullable
    public Location getIslandLocation(@NonNull World world, @NonNull UUID uuid) {
        Island island = getIsland(world, uuid);
        return island != null ? island.getCenter() : null;
    }

    public Location getLast(@NonNull World world) {
        return last.get(world);
    }

    /**
     * Returns a set of island member UUID's for the island of playerUUID of rank <tt>minimumRank</tt>
     * and above.
     * This includes the owner of the island. If there is no island, this set will be empty.
     *
     * @param world - world to check
     * @param playerUUID - the player's UUID
     * @param minimumRank - the minimum rank to be included in the set.
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(@NonNull World world, @NonNull UUID playerUUID, int minimumRank) {
        return islandCache.getMembers(world, playerUUID, minimumRank);
    }

    /**
     * Returns a set of island member UUID's for the island of playerUUID.
     * Only includes players of rank {@link RanksManager#MEMBER_RANK} and above.
     * This includes the owner of the island. If there is no island, this set will be empty.
     *
     * @param world - world to check
     * @param playerUUID - the player's UUID
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(@NonNull World world, @NonNull UUID playerUUID) {
        return islandCache.getMembers(world, playerUUID, RanksManager.MEMBER_RANK);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes only the protected area. Use {@link #getIslandAt(Location)}
     * for the full island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getProtectedIslandAt(@NonNull Location location) {
        return getIslandAt(location).filter(i -> i.onIsland(location));
    }

    /**
     * Get a safe home location using async chunk loading and set the home location
     * @param world - world
     * @param user - user
     * @param number - number number
     * @return CompletableFuture with the location found, or null
     * @since 1.14.0
     */
    public CompletableFuture<Location> getAsyncSafeHomeLocation(@NonNull World world, @NonNull User user, int number) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        // Check if the world is a gamemode world and the player has an island
        Location islandLoc = getIslandLocation(world, user.getUniqueId());
        if (!plugin.getIWM().inWorld(world) || islandLoc == null) {
            result.complete(null);
            return result;
        }
        // Try the numbered home location first
        Location defaultHome = plugin.getPlayers().getHomeLocation(world, user, 1);
        Location numberedHome = plugin.getPlayers().getHomeLocation(world, user, number);
        Location l = numberedHome != null ? numberedHome : defaultHome;
        if (l != null) {
            Util.getChunkAtAsync(l).thenRun(() -> {
                // Check if it is safe
                if (isSafeLocation(l)) {
                    result.complete(l);
                    return;
                }
                // To cover slabs, stairs and other half blocks, try one block above
                Location lPlusOne = l.clone().add(new Vector(0, 1, 0));
                if (isSafeLocation(lPlusOne)) {
                    // Adjust the home location accordingly
                    plugin.getPlayers().setHomeLocation(user, lPlusOne, number);
                    result.complete(lPlusOne);
                    return;
                }
                // Try island
                tryIsland(result, islandLoc, user, number);
            });
            return result;
        }
        // Try island
        tryIsland(result, islandLoc, user, number);
        return result;
    }

    private void tryIsland(CompletableFuture<Location> result, Location islandLoc, @NonNull User user, int number) {
        Util.getChunkAtAsync(islandLoc).thenRun(() -> {
            World w = islandLoc.getWorld();
            if (isSafeLocation(islandLoc)) {
                plugin.getPlayers().setHomeLocation(user, islandLoc, number);
                result.complete(islandLoc.clone().add(new Vector(0.5D,0,0.5D)));
                return;
            } else {
                // If these island locations are not safe, then we need to get creative
                // Try the default location
                Location dl = islandLoc.clone().add(new Vector(0.5D, 5D, 2.5D));
                if (isSafeLocation(dl)) {
                    plugin.getPlayers().setHomeLocation(user, dl, number);
                    result.complete(dl);
                    return;
                }
                // Try just above the bedrock
                dl = islandLoc.clone().add(new Vector(0.5D, 5D, 0.5D));
                if (isSafeLocation(dl)) {
                    plugin.getPlayers().setHomeLocation(user, dl, number);
                    result.complete(dl);
                    return;
                }
                // Try all the way up to the sky
                for (int y = islandLoc.getBlockY(); y < w.getMaxHeight(); y++) {
                    dl = new Location(w, islandLoc.getX() + 0.5D, y, islandLoc.getZ() + 0.5D);
                    if (isSafeLocation(dl)) {
                        plugin.getPlayers().setHomeLocation(user, dl, number);
                        result.complete(dl);
                        return;
                    }
                }
            }
            result.complete(null);
        });

    }

    /**
     * Determines a safe teleport spot on player's island or the team island
     * they belong to.
     *
     * @param world - world to check, not null
     * @param user - the player, not null
     * @param number - a number - starting home location, e.g. 1
     * @return Location of a safe teleport spot or {@code null} if one cannot be found or if the world is not an island world.
     */
    public Location getSafeHomeLocation(@NonNull World world, @NonNull User user, int number) {
        // Check if the world is a gamemode world
        if (!plugin.getIWM().inWorld(world)) {
            return null;
        }

        // Try the numbered home location first
        Location l = plugin.getPlayers().getHomeLocation(world, user, number);

        if (l == null) {
            // Get the default home, which may be null too, but that's okay
            number = 1;
            l = plugin.getPlayers().getHomeLocation(world, user, number);
        }
        // Check if it is safe
        if (l != null) {
            if (isSafeLocation(l)) {
                return l;
            }
            // To cover slabs, stairs and other half blocks, try one block above
            Location lPlusOne = l.clone();
            lPlusOne.add(new Vector(0, 1, 0));
            if (isSafeLocation(lPlusOne)) {
                // Adjust the home location accordingly
                plugin.getPlayers().setHomeLocation(user, lPlusOne, number);
                return lPlusOne;
            }
        }
        // Home location either isn't safe, or does not exist so try the island
        // location
        if (plugin.getIslands().inTeam(world, user.getUniqueId())) {
            l = plugin.getIslands().getIslandLocation(world, user.getUniqueId());
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(user, l, number);
                return l;
            } else {
                // try owner's home
                Location tlh = plugin.getPlayers().getHomeLocation(world, plugin.getIslands().getOwner(world, user.getUniqueId()));
                if (tlh != null && isSafeLocation(tlh)) {
                    plugin.getPlayers().setHomeLocation(user, tlh, number);
                    return tlh;
                }
            }
        } else {
            l = plugin.getIslands().getIslandLocation(world, user.getUniqueId());
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(user, l, number);
                return l.clone().add(new Vector(0.5D,0,0.5D));
            }
        }
        if (l == null) {
            plugin.logWarning(user.getName() + " player has no island in world " + world.getName() + "!");
            return null;
        }
        // If these island locations are not safe, then we need to get creative
        // Try the default location
        Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(user, dl, number);
            return dl;
        }
        // Try just above the bedrock
        dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(user, dl, number);
            return dl;
        }
        // Try all the way up to the sky
        for (int y = l.getBlockY(); y < 255; y++) {
            final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
            if (isSafeLocation(n)) {
                plugin.getPlayers().setHomeLocation(user, n, number);
                return n;
            }
        }
        // Unsuccessful
        return null;
    }

    /**
     * Gets the island that is defined as spawn in this world
     * @param world world
     * @return optional island, may be empty
     */
    @NonNull
    public Optional<Island> getSpawn(@NonNull World world){
        return Optional.ofNullable(spawn.get(world));
    }

    /**
     * Get the spawn point on the spawn island if it exists
     * @param world - world
     * @return the spawnPoint or null if spawn does not exist
     */
    public Location getSpawnPoint(@NonNull World world) {
        return spawn.containsKey(world) ? spawn.get(world).getSpawnPoint(world.getEnvironment()) : null;
    }

    /**
     * Provides UUID of this player's island owner or null if it does not exist
     * @param world world to check
     * @param playerUUID the player's UUID
     * @return island owner's UUID or null if player has no island
     */
    @Nullable
    public UUID getOwner(@NonNull World world, @NonNull UUID playerUUID) {
        return islandCache.getOwner(world, playerUUID);
    }

    /**
     * Checks if a player has an island in the world and owns it
     * @param world - world to check
     * @param user - the user
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull User user) {
        return islandCache.hasIsland(world, user.getUniqueId());
    }

    /**
     * Checks if a player has an island in the world and owns it
     * @param world - world to check
     * @param uuid - the user's uuid
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull UUID uuid) {
        return islandCache.hasIsland(world, uuid);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     * @deprecated as of 1.14.0. Use homeTeleportAsync instead.
     */
    @Deprecated
    public void homeTeleport(@NonNull World world, @NonNull Player player) {
        homeTeleport(world, player, 1, false);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     *
     * @param world - world to check
     * @param player - the player
     * @param number - a number - home location to do to
     * @deprecated as of 1.14.0. Use homeTeleportAsync instead.
     */
    @Deprecated
    public void homeTeleport(@NonNull World world, @NonNull Player player, int number) {
        homeTeleport(world, player, number, false);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     * @param newIsland - true if this is a new island teleport
     * @deprecated as of 1.14.0. Use homeTeleportAsync instead.
     */
    @Deprecated
    public void homeTeleport(@NonNull World world, @NonNull Player player, boolean newIsland) {
        homeTeleport(world, player, 1, newIsland);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     * @return CompletableFuture true if successful, false if not
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player) {
        return homeTeleportAsync(world, player, 1, false);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     *
     * @param world - world to check
     * @param player - the player
     * @param number - a number - home location to do to
     * @return CompletableFuture true if successful, false if not
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player, int number) {
        return homeTeleportAsync(world, player, number, false);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     * @param newIsland - true if this is a new island teleport
     * @return CompletableFuture true if successful, false if not
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player, boolean newIsland) {
        return homeTeleportAsync(world, player, 1, newIsland);
    }


    private CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player, int number, boolean newIsland) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        User user = User.getInstance(player);
        user.sendMessage("commands.island.go.teleport");
        // Stop any gliding
        player.setGliding(false);
        // Check if the player is a passenger in a boat
        if (player.isInsideVehicle()) {
            Entity boat = player.getVehicle();
            if (boat instanceof Boat) {
                player.leaveVehicle();
                // Remove the boat so they don't lie around everywhere
                boat.remove();
                player.getInventory().addItem(new ItemStack(TREE_TO_BOAT.getOrDefault(((Boat) boat).getWoodType(), Material.OAK_BOAT)));
                player.updateInventory();
            }
        }
        this.getAsyncSafeHomeLocation(world, user, number).thenAccept(home -> {
            if (home == null) {
                // Try to fix this teleport location and teleport the player if possible
                new SafeSpotTeleport.Builder(plugin)
                .entity(player)
                .island(plugin.getIslands().getIsland(world, user))
                .homeNumber(number)
                .thenRun(() -> teleported(world, user, number, newIsland))
                .buildFuture()
                .thenAccept(result::complete);
                return;
            }
            // Add home
            if (plugin.getPlayers().getHomeLocations(world, player.getUniqueId()).isEmpty()) {
                plugin.getPlayers().setHomeLocation(player.getUniqueId(), home);
            }
            PaperLib.teleportAsync(player, home).thenAccept(b -> {
                // Only run the commands if the player is successfully teleported
                if (Boolean.TRUE.equals(b)) {
                    teleported(world, user, number, newIsland);
                    result.complete(true);
                } else {
                    result.complete(false);
                }

            });
        });
        return result;
    }


    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     *
     * @param world - world to check
     * @param player - the player
     * @param number - a number - home location to do to
     * @param newIsland - true if this is a new island teleport
     */
    private void homeTeleport(@NonNull World world, @NonNull Player player, int number, boolean newIsland) {
        User user = User.getInstance(player);
        user.sendMessage("commands.island.go.teleport");
        Location home = getSafeHomeLocation(world, user, number);
        // Stop any gliding
        player.setGliding(false);
        // Check if the player is a passenger in a boat
        if (player.isInsideVehicle()) {
            Entity boat = player.getVehicle();
            if (boat instanceof Boat) {
                player.leaveVehicle();
                // Remove the boat so they don't lie around everywhere
                boat.remove();
                player.getInventory().addItem(new ItemStack(TREE_TO_BOAT.getOrDefault(((Boat) boat).getWoodType(), Material.OAK_BOAT)));
                player.updateInventory();
            }
        }
        if (home == null) {
            // Try to fix this teleport location and teleport the player if possible
            new SafeSpotTeleport.Builder(plugin)
            .entity(player)
            .island(plugin.getIslands().getIsland(world, user))
            .homeNumber(number)
            .thenRun(() -> teleported(world, user, number, newIsland))
            .build();
            return;
        }
        // Add home
        if (plugin.getPlayers().getHomeLocations(world, player.getUniqueId()).isEmpty()) {
            plugin.getPlayers().setHomeLocation(player.getUniqueId(), home);
        }
        PaperLib.teleportAsync(player, home).thenAccept(b -> {
            // Only run the commands if the player is successfully teleported
            if (Boolean.TRUE.equals(b)) teleported(world, user, number, newIsland);
        });
    }

    private void teleported(World world, User user, int number, boolean newIsland) {
        if (number > 1) {
            user.sendMessage("commands.island.go.teleported", TextVariables.NUMBER, String.valueOf(number));
        }
        // If this is a new island, then run commands and do resets
        if (newIsland) {
            // Execute commands
            Util.runCommands(user, plugin.getIWM().getOnJoinCommands(world), "join");

            // Remove money inventory etc.
            if (plugin.getIWM().isOnJoinResetEnderChest(world)) {
                user.getPlayer().getEnderChest().clear();
            }
            if (plugin.getIWM().isOnJoinResetInventory(world)) {
                user.getPlayer().getInventory().clear();
            }
            if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnJoinResetMoney(world)) {
                plugin.getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
            }

            // Reset the health
            if (plugin.getIWM().isOnJoinResetHealth(world)) {
                user.getPlayer().setHealth(user.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
            }

            // Reset the hunger
            if (plugin.getIWM().isOnJoinResetHunger(world)) {
                user.getPlayer().setFoodLevel(20);
            }

            // Reset the XP
            if (plugin.getIWM().isOnJoinResetXP(world)) {
                user.getPlayer().setTotalExperience(0);
            }
        }
    }

    /**
     * Teleports the player to the spawn location for this world
     * @param world world
     * @param player player to teleport
     * @since 1.1
     */
    public void spawnTeleport(@NonNull World world, @NonNull Player player) {
        User user = User.getInstance(player);
        // If there's no spawn island or the spawn location is null for some reason, then error
        Location spawnTo = getSpawn(world).map(i -> i.getSpawnPoint(World.Environment.NORMAL) == null ? i.getCenter() : i.getSpawnPoint(World.Environment.NORMAL))
                .orElse(null);
        if (spawnTo == null) {
            // There is no spawn here.
            user.sendMessage("commands.island.spawn.no-spawn");
        } else {
            // Teleport the player to the spawn
            // Stop any gliding
            player.setGliding(false);
            // Check if the player is a passenger in a boat
            if (player.isInsideVehicle()) {
                Entity boat = player.getVehicle();
                if (boat instanceof Boat) {
                    player.leaveVehicle();
                    // Remove the boat so they don't lie around everywhere
                    boat.remove();
                    Material boatMat = Material.getMaterial(((Boat) boat).getWoodType().toString() + "_BOAT");
                    if (boatMat == null) {
                        boatMat = Material.OAK_BOAT;
                    }
                    player.getInventory().addItem(new ItemStack(boatMat, 1));
                    player.updateInventory();
                }
            }

            user.sendMessage("commands.island.spawn.teleporting");
            // Safe teleport
            new SafeSpotTeleport.Builder(plugin).entity(player).location(spawnTo).build();
        }
    }

    /**
     * Indicates whether a player is at an island spawn or not
     *
     * @param playerLoc - player's location
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
        return spawn.containsKey(playerLoc.getWorld()) && spawn.get(playerLoc.getWorld()).onIsland(playerLoc);
    }

    /**
     * Sets an Island to be the spawn of its World. It will become an unowned Island.
     * <br/>
     * If there was already a spawn set for this World, it will no longer be the spawn but it will remain unowned.
     * @param spawn the Island to set as spawn.
     *              Must not be null.
     */
    public void setSpawn(@NonNull Island spawn) {
        // Checking if there is already a spawn set for this world
        if (this.spawn.containsKey(spawn.getWorld()) && this.spawn.get(spawn.getWorld()) != null) {
            Island oldSpawn = this.spawn.get(spawn.getWorld());
            if (oldSpawn.equals(spawn)) {
                return; // The spawn is already the current spawn - no need to update anything.
            } else {
                oldSpawn.setSpawn(false);
            }
        }
        this.spawn.put(spawn.getWorld(), spawn);
        spawn.setSpawn(true);
    }

    /**
     * Clears the spawn island for this world
     * @param world - world
     * @since 1.8.0
     */
    public void clearSpawn(World world) {
        Island spawnIsland = spawn.get(Util.getWorld(world));
        if (spawnIsland != null) {
            spawnIsland.setSpawn(false);
        }
        this.spawn.remove(world);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if the player is the owner of their island.
     */
    public boolean isOwner(@NonNull World world, @NonNull UUID uniqueId) {
        return hasIsland(world, uniqueId) && uniqueId.equals(getIsland(world, uniqueId).getOwner());
    }

    /**
     * Clear and reload all islands from database
     */
    public void load() {
        islandCache.clear();
        quarantineCache.clear();
        List<Island> toQuarantine = new ArrayList<>();
        int owned = 0;
        int unowned = 0;
        // Attempt to load islands
        for (Island island : handler.loadObjects()) {
            if (island == null) {
                plugin.logWarning("Null island when loading...");
                continue;
            }
            if (island.isDeleted()) {
                // These will be deleted later
                deletedIslands.add(island.getUniqueId());
            } else if (island.isDoNotLoad() && island.getWorld() != null && island.getCenter() != null) {
                // Add to quarantine cache
                quarantineCache.computeIfAbsent(island.getOwner(), k -> new ArrayList<>()).add(island);
            } else {
                // Fix island center if it is off
                fixIslandCenter(island);
                if (!islandCache.addIsland(island)) {
                    // Quarantine the offending island
                    toQuarantine.add(island);
                    // Add to quarantine cache
                    island.setDoNotLoad(true);
                    quarantineCache.computeIfAbsent(island.getOwner(), k -> new ArrayList<>()).add(island);
                    if (island.isUnowned()) {
                        unowned++;
                    } else {
                        owned++;
                    }
                } else if (island.isSpawn()) {
                    // Success, set spawn if this is the spawn island.
                    this.setSpawn(island);
                } else {
                    // Successful load
                    // Clean any null flags out of the island - these can occur for various reasons
                    island.getFlags().keySet().removeIf(f -> f.getID().startsWith("NULL_FLAG"));
                }
            }

            // Update some of their fields
            if (island.getGameMode() == null) {
                island.setGameMode(plugin.getIWM().getAddon(island.getWorld()).map(gm -> gm.getDescription().getName()).orElse(""));
            }
        }
        if (!toQuarantine.isEmpty()) {
            plugin.logError(toQuarantine.size() + " islands could not be loaded successfully; moving to trash bin.");
            plugin.logError(unowned + " are unowned, " + owned + " are owned.");

            toQuarantine.forEach(handler::saveObjectAsync);
            // Check if there are any islands with duplicate islands
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Set<UUID> duplicatedUUIDRemovedSet = new HashSet<>();
                Set<UUID> duplicated = islandCache.getIslands().stream()
                        .map(Island::getOwner)
                        .filter(Objects::nonNull)
                        .filter(n -> !duplicatedUUIDRemovedSet.add(n))
                        .collect(Collectors.toSet());
                if (!duplicated.isEmpty()) {
                    plugin.logError("**** Owners that have more than one island = " + duplicated.size());
                    for (UUID uuid : duplicated) {
                        Set<Island> set = islandCache.getIslands().stream().filter(i -> uuid.equals(i.getOwner())).collect(Collectors.toSet());
                        plugin.logError(plugin.getPlayers().getName(uuid) + "(" + uuid.toString() + ") has " + set.size() + " islands:");
                        set.forEach(i -> {
                            plugin.logError("Island at " + i.getCenter());
                            plugin.logError("Island unique ID = " + i.getUniqueId());
                        });
                        plugin.logError("You should find out which island is real and delete the uniqueID from the database for the bogus one.");
                        plugin.logError("");
                    }
                }
            });
        }
    }

    /**
     * Island coordinates should always be a multiple of the island distance x 2. If they are not, this method
     * realigns the grid coordinates.
     * @param island - island
     * @return true if coordinate is altered
     * @since 1.3.0
     */
    boolean fixIslandCenter(Island island) {
        World world = island.getWorld();
        if (world == null || island.getCenter() == null || !plugin.getIWM().inWorld(world)) {
            return false;
        }
        int distance = plugin.getIWM().getIslandDistance(island.getWorld()) * 2;
        long x = ((long) island.getCenter().getBlockX()) - plugin.getIWM().getIslandXOffset(world) - plugin.getIWM().getIslandStartX(world);
        long z = ((long) island.getCenter().getBlockZ()) - plugin.getIWM().getIslandZOffset(world) - plugin.getIWM().getIslandStartZ(world);
        if (x % distance != 0 || z % distance != 0) {
            // Island is off grid
            x = Math.round((double) x / distance) * distance + plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world);
            z = Math.round((double) z / distance) * distance + plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world);
            island.setCenter(new Location(world, x, island.getCenter().getBlockY(), z));
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * that the player is a member of (owner or member)
     *
     * @param player - the player
     * @param loc - location
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(Player player, Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island
        return getIslandAt(loc).filter(i -> i.onIsland(loc)).map(i -> i.getMemberSet().contains(player.getUniqueId())).orElse(false);
    }

    /**
     * Checks if an online player is in the protected area of an island he owns or he is part of. i.e. rank is greater than VISITOR_RANK
     *
     * @param world the World to check. Typically this is the user's world. Does not check nether or end worlds. If null the method will always return {@code false}.
     * @param user the User to check, if null or if this is not a Player the method will always return {@code false}.
     *
     * @return {@code true} if this User is located within the protected area of an island he owns or he is part of,
     *          {@code false} otherwise or if this User is not located in this World.
     */
    public boolean userIsOnIsland(World world, User user) {
        if (user == null || !user.isPlayer() || world == null) {
            return false;
        }
        return (user.getLocation().getWorld() == world)
                && getProtectedIslandAt(user.getLocation())
                .map(i -> i.getMembers().entrySet().stream()
                        .anyMatch(en -> en.getKey().equals(user.getUniqueId()) && en.getValue() > RanksManager.VISITOR_RANK))
                .orElse(false);
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param user - user
     */
    public void removePlayer(World world, User user) {
        removePlayer(world, user.getUniqueId());
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param uuid - user's uuid
     */
    public void removePlayer(World world, UUID uuid) {
        Island island = islandCache.removePlayer(world, uuid);
        if (island != null) {
            handler.saveObjectAsync(island);
        }
    }

    /**
     * This teleports players away from an island - used when reseting or deleting an island
     * @param island to remove players from
     */
    public void removePlayersFromIsland(Island island) {
        World w = island.getWorld();
        Bukkit.getOnlinePlayers().stream()
        .filter(p -> p.getGameMode().equals(plugin.getIWM().getDefaultGameMode(island.getWorld())))
        .filter(p -> island.onIsland(p.getLocation())).forEach(p -> {
            // Teleport island players to their island home
            if (!island.getMemberSet().contains(p.getUniqueId()) && (hasIsland(w, p.getUniqueId()) || inTeam(w, p.getUniqueId()))) {
                homeTeleportAsync(w, p);
            } else {
                // Move player to spawn
                if (spawn.containsKey(w)) {
                    // go to island spawn
                    PaperLib.teleportAsync(p, spawn.get(w).getSpawnPoint(w.getEnvironment()));
                }
            }
        });
    }

    /**
     * Save the all the islands to the database
     */
    public void saveAll(){
        Collection<Island> collection = islandCache.getIslands();
        for(Island island : collection){
            try {
                handler.saveObjectAsync(island);
            } catch (Exception e) {
                plugin.logError("Could not save island to database when running sync! " + e.getMessage());
            }
        }
    }

    /**
     * Saves all the players at a rate of 1 per tick. Used as a backup.
     * @since 1.8.0
     */
    public void asyncSaveAll() {
        if (!toSave.isEmpty()) return;
        // Get a list of ID's to save
        toSave = new HashSet<>(islandCache.getAllIslandIds());
        Iterator<String> it = toSave.iterator();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.isEnabled() && it.hasNext()) {
                getIslandById(it.next()).ifPresent(this::save);
            } else {
                toSave.clear();
                task.cancel();
            }
        }, 0L, 1L);
    }
    /**
     * Puts a player in a team. Removes them from their old island if required.
     * @param teamIsland - team island
     * @param playerUUID - the player's UUID
     */
    public void setJoinTeam(Island teamIsland, UUID playerUUID) {
        // Add player to new island
        teamIsland.addMember(playerUUID);
        islandCache.addPlayer(playerUUID, teamIsland);
        // Save the island
        handler.saveObjectAsync(teamIsland);
    }

    public void setLast(Location last) {
        this.last.put(last.getWorld(), last);
    }

    /**
     * Called when a player leaves a team
     * @param world - world
     * @param uuid - the player's UUID
     */
    public void setLeaveTeam(World world, UUID uuid) {
        plugin.getPlayers().clearHomeLocations(world, uuid);
        removePlayer(world, uuid);
    }

    public void shutdown(){
        // Remove all coop associations
        islandCache.getIslands().forEach(i -> i.getMembers().values().removeIf(p -> p == RanksManager.COOP_RANK));
        saveAll();
        islandCache.clear();
        handler.close();
    }

    /**
     * Checks if a player is in a team
     * @param world - world
     * @param playerUUID - player's UUID
     * @return true if in team, false if not
     */
    public boolean inTeam(World world, UUID playerUUID) {
        return getMembers(world, playerUUID).size() > 1;
    }

    /**
     * Sets this target as the owner for this island
     * @param world world
     * @param user the user who is issuing the command
     * @param targetUUID the current island member who is going to become the new owner
     */
    public void setOwner(World world, User user, UUID targetUUID) {
        setOwner(user, targetUUID, getIsland(world, targetUUID));
    }

    /**
     * Sets this target as the owner for this island
     * @param user requester
     * @param targetUUID new owner
     * @param island island to register
     */
    public void setOwner(User user, UUID targetUUID, Island island) {
        islandCache.setOwner(island, targetUUID);
        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]", plugin.getPlayers().getName(targetUUID));
        plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon -> {
            User target = User.getInstance(targetUUID);
            // Tell target. If they are offline, then they may receive a message when they login
            target.sendMessage("commands.island.team.setowner.you-are-the-owner");
            // Permission checks for range changes only work when the target is online
            if (target.isOnline() &&
                    target.getEffectivePermissions().parallelStream()
                    .map(PermissionAttachmentInfo::getPermission)
                    .anyMatch(p -> p.startsWith(addon.getPermissionPrefix() + "island.range"))) {
                // Check if new owner has a different range permission than the island size
                int range = target.getPermissionValue(
                        addon.getPermissionPrefix() + "island.range",
                        plugin.getIWM().getIslandProtectionRange(Util.getWorld(island.getWorld())));
                // Range can go up or down
                if (range != island.getProtectionRange()) {
                    user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                    target.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                    plugin.log("Setowner: Island protection range changed from " + island.getProtectionRange() + " to "
                            + range + " for " + user.getName() + " due to permission.");

                    // Get old range for event
                    int oldRange = island.getProtectionRange();
                    island.setProtectionRange(range);

                    // Call Protection Range Change event. Does not support cancelling.
                    IslandEvent.builder()
                    .island(island)
                    .location(island.getCenter())
                    .reason(IslandEvent.Reason.RANGE_CHANGE)
                    .involvedPlayer(targetUUID)
                    .admin(true)
                    .protectionRange(range, oldRange)
                    .build();
                }
            }
        });
    }

    /**
     * Clear an area of mobs as per world rules. Radius is default 5 blocks in every direction.
     * Value is set in BentoBox config.yml
     * Will not remove any named monsters.
     * @param loc - location to clear
     */
    public void clearArea(Location loc) {
        if (!plugin.getIWM().inWorld(loc)) return;
        loc.getWorld().getNearbyEntities(loc, plugin.getSettings().getClearRadius(),
                plugin.getSettings().getClearRadius(),
                plugin.getSettings().getClearRadius()).stream()
        .filter(en -> Util.isHostileEntity(en)
                && !plugin.getIWM().getRemoveMobsWhitelist(loc.getWorld()).contains(en.getType())
                && !(en instanceof PufferFish))
        .filter(en -> en.getCustomName() == null)
        .forEach(Entity::remove);
    }

    /**
     * Removes a player from any island where they hold the indicated rank.
     * Typically this is to remove temporary ranks such as coop.
     * Removal is done in all worlds.
     * @param rank - rank to clear
     * @param uniqueId - UUID of player
     */
    public void clearRank(int rank, UUID uniqueId) {
        islandCache.getIslands().forEach(i -> i.getMembers().entrySet().removeIf(e -> e.getKey().equals(uniqueId) && e.getValue() == rank));
    }

    /**
     * Save the island to the database
     * @param island - island
     */
    public void save(Island island) {
        handler.saveObjectAsync(island);
    }

    /**
     * Try to get an island by its unique id
     * @param uniqueId - unique id string
     * @return optional island
     * @since 1.3.0
     */
    @NonNull
    public Optional<Island> getIslandById(String uniqueId) {
        return Optional.ofNullable(islandCache.getIslandById(uniqueId));
    }

    /**
     * Try to get a list of quarantined islands owned by uuid in this world
     *
     * @param world - world
     * @param uuid - target player's UUID, or <tt>null</tt> = unowned islands
     * @return list of islands; may be empty
     * @since 1.3.0
     */
    @NonNull
    public List<Island> getQuarantinedIslandByUser(@NonNull World world, @Nullable UUID uuid) {
        return quarantineCache.getOrDefault(uuid, Collections.emptyList()).stream()
                .filter(i -> i.getWorld().equals(world)).collect(Collectors.toList());
    }

    /**
     * Delete quarantined islands owned by uuid in this world
     *
     * @param world - world
     * @param uuid - target player's UUID, or <tt>null</tt> = unowned islands
     * @since 1.3.0
     */
    public void deleteQuarantinedIslandByUser(World world, @Nullable UUID uuid) {
        if (quarantineCache.containsKey(uuid)) {
            quarantineCache.get(uuid).stream().filter(i -> i.getWorld().equals(world))
            .forEach(i -> handler.deleteObject(i));
            quarantineCache.get(uuid).removeIf(i -> i.getWorld().equals(world));
        }
    }

    /**
     * @return the quarantineCache
     * @since 1.3.0
     */
    @NonNull
    public Map<UUID, List<Island>> getQuarantineCache() {
        return quarantineCache;
    }

    /**
     * Remove a quarantined island and delete it from the database completely.
     * This is NOT recoverable unless you have database backups.
     * @param island island
     * @return {@code true} if island is quarantined and removed
     * @since 1.3.0
     */
    public boolean purgeQuarantinedIsland(Island island) {
        if (quarantineCache.containsKey(island.getOwner()) && quarantineCache.get(island.getOwner()).remove(island)) {
            handler.deleteObject(island);
            return true;
        }
        return false;
    }

    /**
     * Switches active island and island in trash
     * @param world  - game world
     * @param target - target player's UUID
     * @param island - island in trash
     * @return <tt>true</tt> if successful, otherwise <tt>false</tt>
     * @since 1.3.0
     */
    public boolean switchIsland(World world, UUID target, Island island) {
        // Remove trashed island from trash
        if (!quarantineCache.containsKey(island.getOwner()) || !quarantineCache.get(island.getOwner()).remove(island)) {
            plugin.logError("Could not remove island from trash");
            return false;
        }
        // Remove old island from cache if it exists
        if (this.hasIsland(world, target)) {
            Island oldIsland = islandCache.get(world, target);
            islandCache.removeIsland(oldIsland);

            // Set old island to trash
            oldIsland.setDoNotLoad(true);

            // Put old island into trash
            quarantineCache.computeIfAbsent(target, k -> new ArrayList<>()).add(oldIsland);
            // Save old island
            handler.saveObjectAsync(oldIsland).thenAccept(result -> {
                if (Boolean.FALSE.equals(result))  plugin.logError("Could not save trashed island in database");
            });
        }
        // Restore island from trash
        island.setDoNotLoad(false);
        // Add new island to cache
        if (!islandCache.addIsland(island)) {
            plugin.logError("Could not add recovered island to cache");
            return false;
        }
        // Save new island
        handler.saveObjectAsync(island).thenAccept(result -> {
            if (Boolean.FALSE.equals(result))  plugin.logError("Could not save recovered island to database");
        });
        return true;
    }

    /**
     * Resets all flags to gamemode config.yml default
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        islandCache.resetAllFlags(world);
        this.saveAll();
    }

    /**
     * Resets a flag to gamemode config.yml default
     * @param world - world
     * @param flag - flag to reset
     * @since 1.8.0
     */
    public void resetFlag(World world, Flag flag) {
        islandCache.resetFlag(world, flag);
        this.saveAll();
    }

    /**
     * Returns whether the specified island custom name exists in this world.
     * @param world World of the gamemode
     * @param name Name of an island
     * @return {@code true} if there is an island with the specified name in this world, {@code false} otherwise.
     * @since 1.7.0
     */
    public boolean nameExists(@NonNull World world, @NonNull String name) {
        return getIslands(world).stream().filter(island -> island.getName() != null).map(Island::getName)
                .anyMatch(n -> ChatColor.stripColor(n).equals(ChatColor.stripColor(name)));
    }

}
