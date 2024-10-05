package world.bentobox.bentobox.managers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.github.puregero.multilib.MultiLib;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.json.BentoboxTypeAdapterFactory;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * The job of this class is manage all island related data. It also handles
 * island ownership, including team, trustees, coops, etc. The data object that
 * it uses is Island
 * 
 * @author tastybento
 */
public class IslandsManager {

    private final BentoBox plugin;

    private Map<World, Island> spawns = new ConcurrentHashMap<>();

    private Map<World, Location> last = new ConcurrentHashMap<>();

    @NonNull
    private static Database<Island> handler;

    /**
     * Island Cache
     */
    @NonNull
    private IslandCache islandCache;
    // Deleted islands
    @NonNull
    private final List<String> deletedIslands;

    private boolean isSaveTaskRunning;

    private final Set<UUID> goingHome;

    /**
     * Islands Manager
     * 
     * @param plugin - plugin
     */
    public IslandsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        handler = new Database<>(plugin, Island.class);
        islandCache = new IslandCache(handler);
        // This list should always be empty unless database deletion failed
        // In that case a purge utility may be required in the future
        deletedIslands = new ArrayList<>();
        // Mid-teleport players going home
        goingHome = new HashSet<>();
        // Set handler in Island

        // Listen for Island Updates
        MultiLib.onString(plugin, "bentobox-updateIsland", id -> {
            Island island = handler.loadObject(id);
            if (island != null) {
                islandCache.updateIsland(island);
            }
        });

        // Delete island blocks
        MultiLib.onString(plugin, "bentobox-deleteIsland", id -> {
            IslandDeletion idd = getGson().fromJson(id, IslandDeletion.class);
            plugin.getIslandDeletionManager().getIslandChunkDeletionManager().add(idd);
        });
        // List for new islands
        MultiLib.onString(plugin, "bentobox-newIsland", id -> {
            Island island = handler.loadObject(id);
            if (island != null) {
                islandCache.addIsland(island);
            }
        });
        // Set or clear spawn
        MultiLib.onString(plugin, "bentobox-setspawn", sp -> {
            String[] split = sp.split(",");
            if (split.length == 1) {
                World world = Bukkit.getWorld(split[0]);
                this.clearSpawn(world);
            } else if (split.length == 2) {
                World world = Bukkit.getWorld(split[0]);
                if (world != null) {
                    getIslandById(split[1]).ifPresent(i -> this.setSpawn(i));
                }
            }

        });
    }

    /**
     * Used only for testing. Sets the database to a mock database.
     * 
     * @param h - handler
     */
    public void setHandler(@NonNull Database<Island> h) {
        handler = h;
    }

    /**
     * Checks if this location is safe for a player to teleport to. Used by warps
     * and boat exits Unsafe is any liquid or air and also if there's no space
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
     * Checks if this location is safe for a player to teleport to and loads chunks
     * async to check.
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
     * 
     * @param world  - world
     * @param ground Material of the block that is going to be the ground
     * @param space1 Material of the block above the ground
     * @param space2 Material of the block that is two blocks above the ground
     * @return {@code true} if the location is considered safe, {@code false}
     *         otherwise.
     */
    public boolean checkIfSafe(@Nullable World world, @NonNull Material ground, @NonNull Material space1,
            @NonNull Material space2) {
        // Ground must be solid, space 1 and 2 must not be solid
        if (world == null || !ground.isSolid() || (space1.isSolid() && !Tag.SIGNS.isTagged(space1))
                || (space2.isSolid() && !Tag.SIGNS.isTagged(space2))) {
            return false;
        }
        // Cannot be submerged or water cannot be dangerous
        if (space1.equals(Material.WATER) && (space2.equals(Material.WATER) || plugin.getIWM().isWaterNotSafe(world))) {
            return false;
        }
        // Unsafe
        if (ground.equals(Material.LAVA) || space1.equals(Material.LAVA) || space2.equals(Material.LAVA)
                || Tag.SIGNS.isTagged(ground) || Tag.TRAPDOORS.isTagged(ground) || Tag.BANNERS.isTagged(ground)
                || Tag.PRESSURE_PLATES.isTagged(ground) || Tag.FENCE_GATES.isTagged(ground)
                || Tag.DOORS.isTagged(ground) || Tag.FENCES.isTagged(ground) || Tag.BUTTONS.isTagged(ground)
                || Tag.ITEMS_BOATS.isTagged(ground) || Tag.ITEMS_CHEST_BOATS.isTagged(ground)
                || Tag.CAMPFIRES.isTagged(ground) || Tag.FIRE.isTagged(ground) || Tag.FIRE.isTagged(space1)
                || space1.equals(Material.END_PORTAL) || space2.equals(Material.END_PORTAL)
                || space1.equals(Material.END_GATEWAY) || space2.equals(Material.END_GATEWAY)) {
            return false;
        }
        // Known unsafe blocks
        return switch (ground) {
        // Unsafe
        case ANVIL, BARRIER, CACTUS, END_PORTAL, END_ROD, FIRE, FLOWER_POT, LADDER, LEVER, TALL_GRASS, PISTON_HEAD,
                MOVING_PISTON, TORCH, WALL_TORCH, TRIPWIRE, WATER, COBWEB, NETHER_PORTAL, MAGMA_BLOCK ->
            false;
        default -> true;
        };
    }

    /**
     * Create an island with no owner at location
     * 
     * @param location the location, not null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(@NonNull Location location) {
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not paste blocks. It just creates
     * the island data object.
     * 
     * @param location the location, not null
     * @param owner    the island owner UUID, may be null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(@NonNull Location location, @Nullable UUID owner) {
        Island island = new Island(location, owner, plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        // Game the gamemode name and prefix the uniqueId
        String gmName = plugin.getIWM().getAddon(location.getWorld()).map(gm -> gm.getDescription().getName())
                .orElse("");
        island.setGameMode(gmName);
        island.setUniqueId(gmName + island.getUniqueId());
        if (islandCache.addIsland(island)) {
            // Save to database and notify other servers
            saveIsland(island).thenAccept(b -> {
                if (b.equals(Boolean.TRUE)) {
                    MultiLib.notify("bentobox-newIsland", island.getUniqueId());
                }
            });
            return island;
        }
        return null;
    }

    /**
     * Deletes island.
     * 
     * @param island         island to delete, not null
     * @param removeBlocks   whether the island blocks should be removed or not
     * @param involvedPlayer - player related to the island deletion, if any
     */
    public void deleteIsland(@NonNull Island island, boolean removeBlocks, @Nullable UUID involvedPlayer) {
        // Fire event
        IslandBaseEvent event = IslandEvent.builder().island(island).involvedPlayer(involvedPlayer)
                .reason(Reason.DELETE).build();
        if (event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled())) {
            return;
        }
        // Set the owner of the island to no one.
        island.setOwner(null);
        island.setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
        island.setDeleted(true);
        if (removeBlocks) {
            // Remove island from the cache
            islandCache.deleteIslandFromCache(island);
            // Remove players from island
            removePlayersFromIsland(island);
            if (!plugin.getSettings().isKeepPreviousIslandOnReset()) {
                // Remove blocks from world
                IslandDeletion id = new IslandDeletion(island);
                plugin.getIslandDeletionManager().getIslandChunkDeletionManager().add(id);
                // Tell other servers
                MultiLib.notify("bentobox-deleteIsland", getGson().toJson(id));
            }
            // Delete the island from the database
            handler.deleteObject(island);
        }
    }

    private Gson getGson() {

        // Build the Gson

        // excludeFieldsWithoutExposeAnnotation - this means that every field to be stored should use @Expose
        // enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for Map keys
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                .enableComplexMapKeySerialization().setPrettyPrinting();
        // Register adapter factory
        builder.registerTypeAdapterFactory(new BentoboxTypeAdapterFactory(plugin));
        // Allow characters like < or > without escaping them
        builder.disableHtmlEscaping();

        return builder.create();
    }

    /**
     * Get the number of islands made on this server. Used by stats.
     * 
     * @return total number of islands known to this server
     */
    public int getIslandCount() {
        return islandCache.size();
    }

    /**
     * Get the number of islands made on this server in a particular world. Used to
     * limit the number of islands if required by settings.
     * 
     * @param world game world
     * @return number of islands
     */
    public long getIslandCount(@NonNull World world) {
        return islandCache.size(world);
    }

    /**
     * Gets the current active island for this player. If they are in a team, the
     * team island is returned. If they have more than one island, then the island
     * they are on now, or the last island they were on is returned.
     * 
     * @param world world to check
     * @param user  user
     * @return Island or null if not found or null user
     */
    @Nullable
    public Island getIsland(@NonNull World world, @Nullable User user) {
        return user == null || user.getUniqueId() == null ? null : getIsland(world, user.getUniqueId());
    }

    /**
     * Gets the islands for this player. If they are in a team, the team island is
     * returned.
     * 
     * @param world world to check
     * @param user  user
     * @return List of islands or empty list if none found for user
     */
    @NonNull
    public List<Island> getIslands(@NonNull World world, @NonNull User user) {
        return getIslands(world, user.getUniqueId());
    }

    /**
     * Gets all the islands for this player in this world including team islands.
     * 
     * @param world world to check
     * @param uniqueId  user's UUID
     * @return List of islands or empty list if none found for user
     */
    @NonNull
    public List<Island> getIslands(@NonNull World world, UUID uniqueId) {
        return islandCache.getIslands(world, uniqueId);
    }

    /**
     * Gets all the islands for this player in any world where this player has any presence
     * 
     * @param uniqueId  user's UUID
     * @return List of islands or empty list if none found for user
     */
    @NonNull
    public List<Island> getIslands(UUID uniqueId) {
        return islandCache.getIslands(uniqueId);
    }

    /**
     * Gets all the islands for this player in this world that this player owns.
     * 
     * @param world world to check
     * @param user user
     * @return List of islands or empty list if none found for user
     * @since 2.1.0
     */
    @NonNull
    public Set<Island> getOwnedIslands(@NonNull World world, @NonNull User user) {
        if (user.getUniqueId() == null) {
            return Collections.emptySet();
        }
        return getOwnedIslands(world, user.getUniqueId());
    }

    /**
     * Gets all the islands for this player in this world that this player owns.
     * 
     * @param world world to check
     * @param uniqueId  user's UUID
     * @return List of islands or empty list if none found for user
     * @since 2.1.0
     */
    @NonNull
    public Set<Island> getOwnedIslands(@NonNull World world, @NonNull UUID uniqueId) {
        return islandCache.getIslands(world, uniqueId).stream().filter(island -> uniqueId.equals(island.getOwner()))
                .collect(Collectors.toSet());
    }

    /**
     * Gets the active island for this player. If they are in a team, the team
     * island is returned. User may have more than one island. Returns the island
     * the player is on now, or their last known island.
     * 
     * @param world world to check. Includes nether and end worlds.
     * @param uuid  user's uuid
     * @return Island or null
     */
    @Nullable
    public Island getIsland(@NonNull World world, @NonNull UUID uuid) {
        return islandCache.getIsland(world, uuid);
    }

    /**
     * Returns the island at the location or Optional empty if there is none. This
     * includes the full island space, not just the protected area. Use
     * {@link #getProtectedIslandAt(Location)} for only the protected island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getIslandAt(@NonNull Location location) {
        return plugin.getIWM().inWorld(location) ? Optional.ofNullable(islandCache.getIslandAt(location))
                : Optional.empty();
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all existing islands
     * (even those who may be unowned).
     * 
     * @return unmodifiable collection containing every island.
     * @since 1.1
     */
    @NonNull
    public Collection<Island> getIslands() {
        return handler.loadObjects().stream().toList();
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those who may be unowned) in the specified world.
     * 
     * @param world World of the gamemode.
     * @return unmodifiable collection containing all the islands in the specified
     *         world.
     * @since 1.7.0
     */
    @NonNull
    public Collection<Island> getIslands(@NonNull World world) {
        return handler.loadObjects().stream().filter(i -> world.equals(i.getWorld())).toList();
    }

    /**
     * Return island with uniqueId. Loads from database. Will block, so be careful.
     * @param uniqueID id of island
     * @return Optional Island object
     * @since 2.4.0
     */
    public Optional<Island> loadIsland(String uniqueID) {
        return Optional.ofNullable(handler.loadObject(uniqueID));
    }

    /**
     * Returns the IslandCache instance.
     * 
     * @return the islandCache
     * @since 1.5.0
     */
    @NonNull
    public IslandCache getIslandCache() {
        return islandCache;
    }

    /**
     * Used for testing only to inject the islandCache mock object
     * 
     * @param islandCache - island cache
     */
    public void setIslandCache(@NonNull IslandCache islandCache) {
        this.islandCache = islandCache;
    }

    /**
     * Returns the player's current island location in World based on the island
     * protection center. If you need the actual island center location for some
     * reason use {@link Island#getCenter()}
     * <p>
     *
     * @param world - world to check
     * @param uuid  - the player's UUID
     * @return Location of the center of the player's protection area or null if an
     *         island does not exist. Returns an island location OR a team island
     *         location
     */
    @Nullable
    public Location getIslandLocation(@NonNull World world, @NonNull UUID uuid) {
        Island island = getIsland(world, uuid);
        return island != null ? island.getProtectionCenter() : null;
    }

    /**
     * Get the last location where an island was created
     * 
     * @param world - world
     * @return location or null if none found
     */
    public Location getLast(@NonNull World world) {
        return last.get(world);
    }

    /**
     * Gets the maximum number of island members allowed on this island. Will update
     * the value based on world settings or island owner permissions (if online). If
     * the island is unowned, then this value will be 0. The number given for
     * MEMBER_RANK is meant to include this rank and higher, e.g.
     * {@link RanksManager#SUB_OWNER_RANK} and {@link RanksManager#OWNER_RANK}
     * 
     * @param island - island
     * @param rank   {@link RanksManager#MEMBER_RANK},
     *               {@link RanksManager#COOP_RANK}, or
     *               {@link RanksManager#TRUSTED_RANK}
     * @return max number of members. If negative, then this means unlimited.
     * @since 1.16.0
     */
    public int getMaxMembers(@NonNull Island island, int rank) {
        if (island.getOwner() == null) {
            // No owner, no rank settings
            island.setMaxMembers(null);
            updateIsland(island);
            return 0;
        }
        // Island max is either the world default or specified amount for this island
        int worldDefault = plugin.getIWM().getMaxTeamSize(island.getWorld());
        String perm = "team.maxsize";
        if (rank == RanksManager.COOP_RANK) {
            worldDefault = plugin.getIWM().getMaxCoopSize(island.getWorld());
            perm = "coop.maxsize";
        } else if (rank == RanksManager.TRUSTED_RANK) {
            worldDefault = plugin.getIWM().getMaxTrustSize(island.getWorld());
            perm = "trust.maxsize";
        }

        int islandMax = island.getMaxMembers(rank) == null ? worldDefault : island.getMaxMembers(rank);
        // Update based on owner permissions if online
        if (island.getOwner() != null && Bukkit.getPlayer(island.getOwner()) != null) {
            User owner = User.getInstance(island.getOwner());
            islandMax = owner.getPermissionValue(plugin.getIWM().getPermissionPrefix(island.getWorld()) + perm,
                    islandMax);
        }
        Integer change = islandMax == worldDefault ? null : islandMax;
        if (island.getMaxMembers().get(rank) != change) {
            island.setMaxMembers(rank, change);
            updateIsland(island);
        }
        return islandMax;
    }

    /**
     * Sets the island max member size.
     * 
     * @param island     - island
     * @param rank       {@link RanksManager#MEMBER_RANK},
     *                   {@link RanksManager#COOP_RANK}, or
     *                   {@link RanksManager#TRUSTED_RANK}
     * @param maxMembers - max number of members. If negative, then this means
     *                   unlimited. Null means the world default will be used.
     * @since 1.16.0
     */
    public void setMaxMembers(@NonNull Island island, int rank, Integer maxMembers) {
        island.setMaxMembers(rank, maxMembers);
    }

    /**
     * Get the maximum number of homes allowed on this island. Will be updated with
     * the owner's permission settings if they exist and the owner is online
     * 
     * @param island - island
     * @return maximum number of homes
     * @since 1.16.0
     */
    public int getMaxHomes(@NonNull Island island) {
        int islandMax = island.getMaxHomes() == null ? plugin.getIWM().getMaxHomes(island.getWorld())
                : island.getMaxHomes();
        // Update based on owner permissions if online
        if (island.getOwner() != null && Bukkit.getPlayer(island.getOwner()) != null) {
            User owner = User.getInstance(island.getOwner());
            islandMax = owner.getPermissionValue(
                    plugin.getIWM().getPermissionPrefix(island.getWorld()) + "island.maxhomes", islandMax);
        }
        // If the island maxHomes is just the same as the world default, then set to
        // null
        Integer change = islandMax == plugin.getIWM().getMaxHomes(island.getWorld()) ? null : islandMax;
        if (island.getMaxHomes() != change) {
            island.setMaxHomes(change);
            updateIsland(island);
        }
        return islandMax;
    }

    /**
     * Set the maximum number of homes allowed on this island
     * 
     * @param island   - island
     * @param maxHomes - max number of homes allowed, or null if the world default
     *                 should be used
     * @since 1.16.0
     */
    public void setMaxHomes(@NonNull Island island, @Nullable Integer maxHomes) {
        island.setMaxHomes(maxHomes);
    }

    /**
     * Returns the island at the location or Optional empty if there is none. This
     * includes only the protected area. Use {@link #getIslandAt(Location)} for the
     * full island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getProtectedIslandAt(@NonNull Location location) {
        return getIslandAt(location).filter(i -> i.onIsland(location));
    }

    /**
     * Get a safe home location using async chunk loading and set the home location
     * 
     * @param world    - world
     * @param user     - user
     * @param homeName - home name
     * @return CompletableFuture with the location found, or null
     * @since 1.14.0
     */
    private CompletableFuture<Location> getAsyncSafeHomeLocation(@NonNull World world, @NonNull User user,
            String homeName) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        // Check if the world is a gamemode world and the player has an island
        Location islandLoc = getIslandLocation(world, user.getUniqueId());
        if (!plugin.getIWM().inWorld(world) || islandLoc == null) {
            result.complete(null);
            return result;
        }
        // Check if the user is switching island and if so, switch name
        String name = this.getIslands(world, user).stream().filter(i -> !homeName.isBlank() && i.getName() != null
                && !i.getName().isBlank() && i.getName().equalsIgnoreCase(homeName)).findFirst().map(island -> {
                    // This is an island, so switch to that island and then go to the default home
                    this.setPrimaryIsland(user.getUniqueId(), island);
                    return "";
                }).orElse(homeName);
        // Try the home location first
        Location defaultHome = getHomeLocation(world, user);
        Location namedHome = homeName.isBlank() ? null : getHomeLocation(world, user, name);
        Location l = namedHome != null ? namedHome : defaultHome;
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
                    setHomeLocation(user, lPlusOne, name);
                    result.complete(lPlusOne);
                    return;
                }
                // Try island
                tryIsland(result, islandLoc, user, name);
            });
            return result;
        }
        // Try island
        tryIsland(result, islandLoc, user, name);
        return result;
    }

    private void tryIsland(CompletableFuture<Location> result, Location islandLoc, @NonNull User user, String name) {
        Util.getChunkAtAsync(islandLoc).thenRun(() -> {
            World w = islandLoc.getWorld();
            if (isSafeLocation(islandLoc)) {
                setHomeLocation(user, islandLoc, name);
                result.complete(islandLoc.clone().add(new Vector(0.5D, 0, 0.5D)));
                return;
            } else {
                // If these island locations are not safe, then we need to get creative
                // Try the default location
                Location dl = islandLoc.clone().add(new Vector(0.5D, 5D, 2.5D));
                if (isSafeLocation(dl)) {
                    setHomeLocation(user, dl, name);
                    result.complete(dl);
                    return;
                }
                // Try just above the bedrock
                dl = islandLoc.clone().add(new Vector(0.5D, 5D, 0.5D));
                if (isSafeLocation(dl)) {
                    setHomeLocation(user, dl, name);
                    result.complete(dl);
                    return;
                }
                // Try all the way up to the sky
                for (int y = islandLoc.getBlockY(); y < w.getMaxHeight(); y++) {
                    dl = new Location(w, islandLoc.getX() + 0.5D, y, islandLoc.getZ() + 0.5D);
                    if (isSafeLocation(dl)) {
                        setHomeLocation(user, dl, name);
                        result.complete(dl);
                        return;
                    }
                }
            }
            result.complete(null);
        });

    }

    /**
     * Sets a default home location on user's island. Replaces previous default
     * location.
     * 
     * @param user     - user
     * @param location - location on island
     * @return true if home location was set. False if this location is not on the
     *         island.
     * @since 1.18.0
     */
    public boolean setHomeLocation(@NonNull User user, Location location) {
        return setHomeLocation(user.getUniqueId(), location, "");
    }

    /**
     * Sets a home location on user's island. Replaces previous location if the same
     * name is used
     * 
     * @param user     - user
     * @param location - location on island
     * @param name     - name of home, or blank for default home
     * @return true if home location was set. False if this location is not on the
     *         island.
     * @since 1.16.0
     */
    public boolean setHomeLocation(@NonNull User user, Location location, String name) {
        return setHomeLocation(user.getUniqueId(), location, name);
    }

    /**
     * Sets a home location on user's island. Replaces previous location if the same
     * name is used
     * 
     * @param uuid     - user uuid
     * @param location - location on island
     * @param name     - name of home, or blank for default home
     * @return true if home location was set. False if this location is not on the
     *         island.
     * @since 1.16.0
     */
    public boolean setHomeLocation(@NonNull UUID uuid, Location location, String name) {
        return setHomeLocation(this.getIsland(location.getWorld(), uuid), location, name);
    }

    /**
     * Set a default home location for user on their island
     * 
     * @param uuid     - user uuid
     * @param location - location on island
     * @return true if home location was set. False if this location is not on the
     *         island.
     * @since 1.16.0
     */
    public boolean setHomeLocation(@NonNull UUID uuid, Location location) {
        return setHomeLocation(uuid, location, "");
    }

    /**
     * Set a home location for island
     * 
     * @param island   - island
     * @param location - location
     * @param name     - name of home, or blank for default home
     * @return true if home location was set. False if this location is not on the
     *         island.
     * @since 1.16.0
     */
    public boolean setHomeLocation(@Nullable Island island, Location location, String name) {
        if (island != null && (island.getHome(name) == null || !island.getHome(name).equals(location))) {
            island.addHome(name, location);
            updateIsland(island);
            return true;
        }
        return false;
    }

    /**
     * Get the home location for user in world for their primary island
     * 
     * @param world - world
     * @param user  - user
     * @return home location or the protection center location if no home defined
     * @since 2.0.0
     */
    @Nullable
    public Location getHomeLocation(@NonNull World world, @NonNull User user) {
        return this.getPrimaryIsland(world, user.getUniqueId()).getHome("");
    }

    /**
     * Get the home location for player's UUID in world for their primary island
     * 
     * @param world - world
     * @param uuid  - uuid of player
     * @return home location or the protection center location if no home defined
     * @since 1.16.0
     */
    @Nullable
    public Location getHomeLocation(@NonNull World world, @NonNull UUID uuid) {
        return this.getPrimaryIsland(world, uuid).getHome("");
    }

    /**
     * Get the named home location for user in world
     * 
     * @param world - world
     * @param user  - user
     * @param name  - name of home, or blank for default
     * @return home location or null if there is no home
     * @since 1.16.0
     */
    @Nullable
    public Location getHomeLocation(@NonNull World world, @NonNull User user, String name) {
        return getHomeLocation(world, user.getUniqueId(), name);
    }

    /**
     * Get the named home location for user in world
     * 
     * @param world - world
     * @param uuid  - uuid of player
     * @param name  - name of home, or blank for default
     * @return home location or null if there is no home
     * @since 1.16.0
     */
    @Nullable
    public Location getHomeLocation(@NonNull World world, @NonNull UUID uuid, String name) {
        return getIslands(world, uuid).stream().filter(is -> is.getHomes().containsKey(name))
                .map(is -> is.getHome(name)).findFirst()
                .orElse(null);
    }

    /**
     * Get the default home location for this island
     * 
     * @param island - island
     * @return home location
     * @since 1.16.0
     */
    @NonNull
    public Location getHomeLocation(@NonNull Island island) {
        return getHomeLocation(island, "");
    }

    /**
     * Get the named home location for this island
     * 
     * @param island - island
     * @param name   - name of home, or blank for default
     * @return home location or if there is none, then the island's center
     * @since 1.16.0
     */
    @NonNull
    public Location getHomeLocation(@NonNull Island island, @NonNull String name) {
        return Objects.requireNonNullElse(island.getHome(name), island.getProtectionCenter());
    }

    /**
     * Remove the named home location from this island
     * 
     * @param island - island
     * @param name   - name of home, or blank for default
     * @return true if successful, false if not
     * @since 1.16.0
     */
    public boolean removeHomeLocation(@NonNull Island island, @NonNull String name) {
        return island.removeHome(name);
    }

    /**
     * Rename a home
     * 
     * @param island  - island
     * @param oldName - old name
     * @param newName - new name
     * @return true if successful, false if not
     */
    public boolean renameHomeLocation(@NonNull Island island, @NonNull String oldName, @NonNull String newName) {
        return island.renameHome(oldName, newName);
    }

    /**
     * Get the all the home locations for this island
     * 
     * @param island - island
     * @return map of home locations with the name as the key
     * @since 1.16.0
     */
    @NonNull
    public Map<String, Location> getHomeLocations(@NonNull Island island) {
        return island.getHomes();
    }

    /**
     * Check if a home name exists or not
     * 
     * @param island - island
     * @param name   - name being checked
     * @return true if it exists or not
     */
    public boolean isHomeLocation(@NonNull Island island, @NonNull String name) {
        return island.getHomes().containsKey(name.toLowerCase());
    }

    /**
     * Get the number of homes on this island if this home were added
     * 
     * @param island - island
     * @param name   - name
     * @return number of homes after adding this one
     */
    public int getNumberOfHomesIfAdded(@NonNull Island island, @NonNull String name) {
        return isHomeLocation(island, name) ? getHomeLocations(island).size() : getHomeLocations(island).size() + 1;
    }

    /**
     * Gets the island that is defined as spawn in this world
     * 
     * @param world world
     * @return optional island, may be empty
     */
    @NonNull
    public Optional<Island> getSpawn(@NonNull World world) {
        return Optional.ofNullable(spawns.get(world));
    }

    /**
     * Get the spawn point on the spawn island if it exists
     * 
     * @param world - world
     * @return the spawnPoint or null if spawn does not exist
     */
    @Nullable
    public Location getSpawnPoint(@NonNull World world) {
        return getSpawn(world).map(i -> i.getSpawnPoint(world.getEnvironment())).orElse(null);
    }

    /**
     * Checks if a player has an island in the world and owns it. Note that players
     * may have more than one island
     * 
     * @param world - world to check
     * @param user  - the user
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull User user) {
        return islandCache.hasIsland(world, user.getUniqueId());
    }

    /**
     * Checks if a player has an island in the world and owns it
     * 
     * @param world - world to check
     * @param uuid  - the user's uuid
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull UUID uuid) {
        return islandCache.hasIsland(world, uuid);
    }

    /**
     * This teleports player to their island. If not safe place can be found then
     * the player is sent to spawn via /spawn command
     *
     * @param world  - world to check
     * @param player - the player
     * @return CompletableFuture true if successful, false if not
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player) {
        return homeTeleportAsync(world, player, "", false);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done
     * to find a safe place.
     *
     * @param world  - world to check
     * @param player - the player
     * @param name   - a named home location or island name. Blank means default
     *               home for current island.
     * @return CompletableFuture true if successful, false if not
     * @since 1.16.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player, String name) {
        return homeTeleportAsync(world, player, name, false);
    }

    /**
     * This teleports player to their island. If no safe place can be found then the
     * player is sent to spawn via /spawn command
     *
     * @param world     - world to check
     * @param player    - the player
     * @param newIsland - true if this is a new island teleport
     * @return CompletableFuture true if successful, false if not
     * @since 1.14.0
     */
    public CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player,
            boolean newIsland) {
        return homeTeleportAsync(world, player, "", newIsland);
    }

    /**
     * Teleports player async
     * 
     * @param world     world
     * @param player    player
     * @param name      - a named home location or island name. Blank means default
     *                  home for current island.
     * @param newIsland true if this is a new island
     * @return completable future that is true when the teleport has been completed
     */
    private CompletableFuture<Boolean> homeTeleportAsync(@NonNull World world, @NonNull Player player, String name,
            boolean newIsland) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        User user = User.getInstance(player);
        user.sendMessage("commands.island.go.teleport");
        goingHome.add(user.getUniqueId());
        readyPlayer(player);
        this.getAsyncSafeHomeLocation(world, user, name).thenAccept(home -> {
            Island island = getIsland(world, user);
            if (home == null) {
                // Try to fix this teleport location and teleport the player if possible
                new SafeSpotTeleport.Builder(plugin).entity(player).island(island).homeName(name)
                        .thenRun(() -> teleported(world, user, name, newIsland, island))
                        .ifFail(() -> goingHome.remove(user.getUniqueId())).buildFuture().thenAccept(result::complete);
                return;
            }
            PaperLib.teleportAsync(Objects.requireNonNull(player), home).thenAccept(b -> {
                // Only run the commands if the player is successfully teleported
                if (Boolean.TRUE.equals(b)) {
                    teleported(world, user, name, newIsland, island);
                    result.complete(true);
                } else {
                    // Remove from mid-teleport set
                    goingHome.remove(user.getUniqueId());
                    result.complete(false);
                }
            });
        });
        return result;
    }

    /**
     * Called when a player is teleported to their island
     * 
     * @param world     - world
     * @param user      - user
     * @param name      - name of home
     * @param newIsland - true if this is a new island
     * @param island    - island
     */
    private void teleported(World world, User user, String name, boolean newIsland, Island island) {
        if (!name.isEmpty()) {
            user.sendMessage("commands.island.go.teleported", TextVariables.NUMBER, name);
        }
        // Remove from mid-teleport set
        goingHome.remove(user.getUniqueId());
        // If this is a new island, then run commands and do resets
        if (newIsland) {
            // Fire event
            if (IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.NEW_ISLAND).island(island)
                    .location(island.getCenter()).build().isCancelled()) {
                // Do nothing
                return;
            }
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
                Util.resetHealth(user.getPlayer());
            }

            // Reset the hunger
            if (plugin.getIWM().isOnJoinResetHunger(world)) {
                user.getPlayer().setFoodLevel(20);
            }

            // Reset the XP
            if (plugin.getIWM().isOnJoinResetXP(world)) {
                // Player collected XP (displayed)
                user.getPlayer().setLevel(0);
                user.getPlayer().setExp(0);
                // Player total XP (not displayed)
                user.getPlayer().setTotalExperience(0);
            }

            // Set the game mode
            user.setGameMode(plugin.getIWM().getDefaultGameMode(world));

            // Execute commands
            Util.runCommands(user, user.getName(), plugin.getIWM().getOnJoinCommands(world), "join");
        }
        // Remove from mid-teleport set
        goingHome.remove(user.getUniqueId());
    }

    /**
     * Teleports the player to the spawn location for this world
     * 
     * @param world  world
     * @param player player to teleport
     * @since 1.1
     */
    public void spawnTeleport(@NonNull World world, @NonNull Player player) {
        User user = User.getInstance(player);
        // If there's no spawn island or the spawn location is null for some reason,
        // then error
        Optional<Location> spawnTo = getSpawn(world).map(island -> {
            Location spawnPoint = island.getSpawnPoint(World.Environment.NORMAL);
            return spawnPoint != null ? spawnPoint : island.getCenter();
        });
        if (spawnTo.isEmpty()) {
            // There is no spawn here.
            user.sendMessage("commands.island.spawn.no-spawn");
        } else {
            // Teleport the player to the spawn
            readyPlayer(player);

            user.sendMessage("commands.island.spawn.teleporting");
            // Safe teleport
            new SafeSpotTeleport.Builder(plugin).entity(player).location(spawnTo.get()).build();
        }
    }

    /**
     * Prepares the player for teleporting by: stopping gliding, exiting any boats
     * and giving the player the boat
     * 
     * @param player player
     */
    private void readyPlayer(@NonNull Player player) {
        // Stop any gliding
        player.setGliding(false);
        // Check if the player is a passenger in a boat
        if (player.isInsideVehicle()) {
            Entity boat = player.getVehicle();
            if (boat instanceof Boat boaty) {
                player.leaveVehicle();
                // Remove the boat so they don't lie around everywhere
                boat.remove();
                player.getInventory().addItem(new ItemStack(boaty.getBoatType().getMaterial()));
                player.updateInventory();
            }
        }

    }

    /**
     * Indicates whether a player is at an island spawn or not
     *
     * @param playerLoc - player's location
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
        return getSpawn(playerLoc.getWorld()).map(i -> i.onIsland(playerLoc)).orElse(false);
    }

    /**
     * Sets an Island to be the spawn of its World. It will become an unowned
     * Island. <br/>
     * If there was already a spawn set for this World, it will no longer be the
     * spawn but it will remain unowned.
     * 
     * @param spawn the Island to set as spawn. Must not be null.
     */
    public void setSpawn(@NonNull Island spawn) {
        if (spawn.getWorld() != null) {
            spawn.setSpawn(true);
            spawns.put(Util.getWorld(spawn.getWorld()), spawn);
            // Tell other servers
            MultiLib.notify("bentobox-setspawn", spawn.getWorld().getUID().toString() + "," + spawn.getUniqueId());
        }
    }


    /**
     * Clears the spawn island for this world
     * 
     * @param world - world
     * @since 1.8.0
     */
    public void clearSpawn(World world) {
        if (spawns.containsKey(world)) {
            spawns.get(world).setSpawn(false);
            spawns.remove(world);
            // Tell other servers
            MultiLib.notify("bentobox-setspawn", world.getUID().toString());
        }
    }

    /**
     * Check is a player has an island and owns it in world
     * 
     * @param uniqueId - unique ID
     * @return true if the player is the owner of any island in the world.
     * @deprecated Duplicate of {@link #hasIsland(World, UUID)}. Players can have
     *             multiple islands.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public boolean isOwner(@NonNull World world, @NonNull UUID uniqueId) {
        return hasIsland(world, uniqueId);
    }

    /**
     * Clear and reload all islands from database
     * 
     * @throws IOException - if a loaded island distance does not match the expected
     *                     distance in config.yml
     */
    public void load() throws IOException {
        islandCache.clear();
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
            } // Check island distance and if incorrect stop BentoBox
            else if (!plugin.getSettings().isOverrideSafetyCheck() && island.getWorld() != null
                    && plugin.getIWM().inWorld(island.getWorld())
                    && island.getRange() != plugin.getIWM().getIslandDistance(island.getWorld())) {
                throw new IOException("Island distance mismatch!\n" + "World '" + island.getWorld().getName()
                        + "' distance " + plugin.getIWM().getIslandDistance(island.getWorld()) + " != island range "
                        + island.getRange() + "!\n" + "Island ID in database is " + island.getUniqueId() + ".\n"
                        + "Island distance in config.yml cannot be changed mid-game! Fix config.yml or clean database.");
            } else {
                if (!plugin.getSettings().isOverrideSafetyCheck()) {
                    // Fix island center if it is off
                    fixIslandCenter(island);
                }
                islandCache.addIsland(island, true);

                if (island.isSpawn()) {
                    // Success, set spawn if this is the spawn island.
                    this.setSpawn(island);
                } else {
                    // Successful load
                    // Clean any null flags out of the island - these can occur for various reasons
                    island.getFlags().keySet().removeIf(f -> f.startsWith("NULL_FLAG"));
                }
            }

            // Update some of their fields
            if (island.getGameMode() == null) {
                island.setGameMode(plugin.getIWM().getAddon(island.getWorld()).map(gm -> gm.getDescription().getName())
                        .orElse(""));
            }
        }
        if (!toQuarantine.isEmpty()) {
            plugin.logError(toQuarantine.size() + " islands could not be loaded successfully; moving to trash bin.");
            plugin.logError(unowned + " are unowned, " + owned + " are owned.");

            toQuarantine.forEach(handler::saveObjectAsync);
            // Check if there are any islands with duplicate islands
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Set<UUID> duplicatedUUIDRemovedSet = new HashSet<>();
                Set<UUID> duplicated = islandCache.getIslands().stream().map(Island::getOwner).filter(Objects::nonNull)
                        .filter(n -> !duplicatedUUIDRemovedSet.add(n)).collect(Collectors.toSet());
                if (!duplicated.isEmpty()) {
                    plugin.logError("**** Owners that have more than one island = " + duplicated.size());
                    for (UUID uuid : duplicated) {
                        Set<Island> set = islandCache.getIslands().stream().filter(i -> uuid.equals(i.getOwner()))
                                .collect(Collectors.toSet());
                        plugin.logError(plugin.getPlayers().getName(uuid) + "(" + uuid.toString() + ") has "
                                + set.size() + " islands:");
                        set.forEach(i -> {
                            plugin.logError("Island at " + i.getCenter());
                            plugin.logError("Island unique ID = " + i.getUniqueId());
                        });
                        plugin.logError(
                                "You should find out which island is real and delete the uniqueID from the database for the bogus one.");
                        plugin.logError("");
                    }
                }
            });
        }
    }

    /**
     * Island coordinates should always be a multiple of the island distance x 2. If
     * they are not, this method realigns the grid coordinates.
     * 
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
        long x = ((long) island.getCenter().getBlockX()) - plugin.getIWM().getIslandXOffset(world)
                - plugin.getIWM().getIslandStartX(world);
        long z = ((long) island.getCenter().getBlockZ()) - plugin.getIWM().getIslandZOffset(world)
                - plugin.getIWM().getIslandStartZ(world);
        if (x % distance != 0 || z % distance != 0) {
            // Island is off grid
            x = Math.round((double) x / distance) * distance + plugin.getIWM().getIslandXOffset(world)
                    + plugin.getIWM().getIslandStartX(world);
            z = Math.round((double) z / distance) * distance + plugin.getIWM().getIslandZOffset(world)
                    + plugin.getIWM().getIslandStartZ(world);
            island.setCenter(new Location(world, x, island.getCenter().getBlockY(), z));
            return true;
        }
        return false;
    }

    /**
     * Checks if a specific location is within the protected range of an island that
     * the player is a member of (owner or member) or Op.
     *
     * @param player - the player
     * @param loc    - location
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(Player player, Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island
        // If Op then all islands are Ops
        return player.isOp()
                || getIslandAt(loc).filter(i -> i.onIsland(loc)).map(i -> i.inTeam(player.getUniqueId()))
                .orElse(false);
    }

    /**
     * Checks if an online player is in the protected area of an island he owns or
     * he is part of. i.e. rank is greater than VISITOR_RANK
     *
     * @param world the World to check. Typically this is the user's world. Does not
     *              check nether or end worlds. If null the method will always
     *              return {@code false}.
     * @param user  the User to check, if null or if this is not a Player the method
     *              will always return {@code false}.
     *
     * @return {@code true} if this User is located within the protected area of an
     *         island he owns or he is part of, {@code false} otherwise or if this
     *         User is not located in this World.
     */
    public boolean userIsOnIsland(World world, User user) {
        if (user == null || !user.isPlayer() || world == null) {
            return false;
        }
        return (user.getLocation().getWorld() == world) && getProtectedIslandAt(user.getLocation())
                .map(i -> i.getMembers().entrySet().stream().anyMatch(
                        en -> en.getKey().equals(user.getUniqueId()) && en.getValue() > RanksManager.VISITOR_RANK))
                .orElse(false);
    }

    /**
     * Removes this player from any and all islands in world
     * 
     * @param world - world
     * @param user  - user
     */
    public void removePlayer(World world, User user) {
        removePlayer(world, user.getUniqueId());
    }

    /**
     * Removes this player from any and all islands in world
     * 
     * @param world - world
     * @param uuid  - user's uuid
     */
    public void removePlayer(World world, UUID uuid) {
        islandCache.removePlayer(world, uuid).forEach(handler::saveObjectAsync);
    }

    /**
     * Remove this player from this island
     * 
     * @param island island
     * @param uuid   uuid of member
     */
    public void removePlayer(Island island, UUID uuid) {
        islandCache.removePlayer(island, uuid);
    }

    /**
     * This teleports players away from an island - used when reseting or deleting
     * an island
     * 
     * @param island to remove players from
     */
    public void removePlayersFromIsland(Island island) {
        World w = island.getWorld();
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getGameMode().equals(plugin.getIWM().getDefaultGameMode(island.getWorld())))
                .filter(p -> island.onIsland(p.getLocation())).forEach(p -> {
                    // Teleport island players to their island home
                    if (!island.inTeam(p.getUniqueId())
                            && (hasIsland(w, p.getUniqueId()) || inTeam(w, p.getUniqueId()))) {
                        homeTeleportAsync(w, p);
                    } else {
                        // Move player to spawn
                        getSpawn(w).map(i -> i.getSpawnPoint(w.getEnvironment())).filter(Objects::nonNull)
                                .ifPresentOrElse(sp -> PaperLib.teleportAsync(p, sp),
                                        () -> plugin.logWarning("Spawn exists but its location is null!"));

                    }
                });
    }

    public boolean isSaveTaskRunning() {
        return isSaveTaskRunning;
    }

    /**
     * Save the all the islands to the database
     */
    public void saveAll() {
        saveAll(false);
    }

    /**
     * Save the all the cached islands to the database
     * 
     * @param schedule true if we should let the task run over multiple ticks to
     *                 reduce lag spikes
     */
    public void saveAll(boolean schedule) {
        if (!schedule) {
            for (Island island : islandCache.getCachedIslands()) {
                if (island.isChanged()) {
                    try {
                        saveIsland(island);
                    } catch (Exception e) {
                        plugin.logError("Could not save island to database when running sync! " + e.getMessage());
                    }
                }
            }
            return;
        }

        isSaveTaskRunning = true;
        Queue<Island> queue = new LinkedList<>(islandCache.getCachedIslands());
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < plugin.getSettings().getMaxSavedIslandsPerTick(); i++) {
                    Island island = queue.poll();
                    if (island == null) {
                        isSaveTaskRunning = false;
                        cancel();
                        return;
                    }
                    if (island.isChanged()) {
                        try {
                            saveIsland(island);
                        } catch (Exception e) {
                            plugin.logError("Could not save island to database when running sync! " + e.getMessage());
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    /**
     * Puts a player in a team. Removes them from their old island if required.
     * 
     * @param teamIsland - team island
     * @param playerUUID - the player's UUID
     */
    public void setJoinTeam(Island teamIsland, UUID playerUUID) {
        // Add player to new island
        teamIsland.addMember(playerUUID);
        islandCache.addPlayer(playerUUID, teamIsland);
        // Save the island
        updateIsland(teamIsland);
    }

    /**
     * Set the last island location
     * @param last location
     */
    public void setLast(Location last) {
        this.last.put(last.getWorld(), last);
    }

    public void shutdown() {
        plugin.log("Removing coops from islands...");
        // Remove all coop associations
        islandCache.getCachedIslands().forEach(i -> i.getMembers().values().removeIf(p -> p == RanksManager.COOP_RANK));
        plugin.log("Saving islands - this has to be done sync so it may take a while with a lot of islands...");
        saveAll();
        plugin.log("Islands saved.");
        islandCache.clear();
        plugin.log("Closing database.");
        handler.close();
    }

    /**
     * Checks if a player is in any team in this world. Note that the player may have
     * multiple islands in the world, any one of which may have a team.
     * Consider checking the island itself {@link Island#inTeam(UUID)}
     * 
     * @param world      - world
     * @param playerUUID - player's UUID
     * @return true if in team, false if not
     */
    public boolean inTeam(World world, @NonNull UUID playerUUID) {
        return this.islandCache.getIslands(world, playerUUID).stream()
                .anyMatch(island -> island.getMemberSet().size() > 1 && island.inTeam(playerUUID));
    }

    /**
     * Sets this target as the owner for this island
     * 
     * @param world      world
     * @param user       the user who is issuing the command
     * @param targetUUID the current island member who is going to become the new
     *                   owner
     */
    public void setOwner(World world, User user, UUID targetUUID) {
        setOwner(user, targetUUID, getIsland(world, user.getUniqueId()), RanksManager.SUB_OWNER_RANK);
    }

    /**
     * Sets this target as the owner for this island
     * 
     * @param user       user making the change
     * @param targetUUID new owner
     * @param island     island to register
     * @param rank      rank to which to set old owner.
     */
    public void setOwner(User user, UUID targetUUID, Island island, int rank) {
        // Demote the old owner
        if (rank >= RanksManager.OWNER_RANK) {
            plugin.logWarning("Setowner: previous owner's rank cannot be higher than SubOwner");
            rank = RanksManager.SUB_OWNER_RANK;
        }
        if (rank > RanksManager.VISITOR_RANK && island.getOwner() != null) {
            island.setRank(island.getOwner(), rank);
        }
        // Make the new owner
        islandCache.setOwner(island, targetUUID);

        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]",
                plugin.getPlayers().getName(targetUUID));
        plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon -> {
            User target = User.getInstance(targetUUID);
            // Tell target. If they are offline, then they may receive a message when they
            // login
            target.sendMessage("commands.island.team.setowner.you-are-the-owner");
            // Permission checks for range changes only work when the target is online
            if (target.isOnline()
                    && target.getEffectivePermissions().parallelStream().map(PermissionAttachmentInfo::getPermission)
                            .anyMatch(p -> p.startsWith(addon.getPermissionPrefix() + "island.range"))) {
                // Check if new owner has a different range permission than the island size
                int range = target.getPermissionValue(addon.getPermissionPrefix() + "island.range",
                        plugin.getIWM().getIslandProtectionRange(Util.getWorld(island.getWorld())));
                // Range can go up or down
                if (range != island.getProtectionRange()) {
                    user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER,
                            String.valueOf(range));
                    target.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER,
                            String.valueOf(range));
                    plugin.log("Setowner: Island protection range changed from " + island.getProtectionRange() + " to "
                            + range + " for " + user.getName() + " due to permission.");

                    // Get old range for event
                    int oldRange = island.getProtectionRange();
                    island.setProtectionRange(range);

                    // Call Protection Range Change event. Does not support canceling.
                    IslandEvent.builder().island(island).location(island.getCenter())
                            .reason(IslandEvent.Reason.RANGE_CHANGE).involvedPlayer(targetUUID).admin(true)
                            .protectionRange(range, oldRange).build();
                }
            }
        });
    }

    /**
     * Clear an area of mobs as per world rules. Radius is default 5 blocks in every
     * direction. Value is set in BentoBox config.yml Will not remove any named
     * monsters.
     * 
     * @param loc - location to clear
     */
    public void clearArea(Location loc) {
        if (!plugin.getIWM().inWorld(loc))
            return;
        loc.getWorld()
                .getNearbyEntities(loc, plugin.getSettings().getClearRadius(), plugin.getSettings().getClearRadius(),
                        plugin.getSettings().getClearRadius())
                .stream().filter(LivingEntity.class::isInstance)
                .filter(en -> Util.isHostileEntity(en)
                        && !plugin.getIWM().getRemoveMobsWhitelist(loc.getWorld()).contains(en.getType())
                        && !(en instanceof PufferFish) && ((LivingEntity) en).getRemoveWhenFarAway())
                .filter(en -> en.getCustomName() == null).forEach(Entity::remove);
    }

    /**
     * Removes a player from any island where they hold the indicated rank.
     * Typically this is to remove temporary ranks such as coop. Removal is done in
     * all worlds.
     * 
     * @param rank     - rank to clear
     * @param uniqueId - UUID of player
     */
    public void clearRank(int rank, UUID uniqueId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> clearRankSync(rank, uniqueId));
    }

    void clearRankSync(int rank, UUID uniqueId) {
        islandCache.getCachedIslands().forEach(
                i -> i.getMembers().entrySet().removeIf(e -> e.getKey().equals(uniqueId) && e.getValue() == rank));
    }

    /**
     * Update island data in database
     * 
     * @param island - island
     */
    public static void updateIsland(Island island) {
        // When mocking, handler can be null so this null check avoids errors
        if (handler != null && handler.objectExists(island.getUniqueId())) {
            island.clearChanged();
            saveIsland(island).thenAccept(b -> MultiLib.notify("bentobox-updateIsland", island.getUniqueId()));
        }
    }

    /**
     * Saves the island async to the database
     * @param island Island object to be saved
     * @return CompletableFuture<Boolean> when done
     * @since 2.4.0
     */
    public static CompletableFuture<Boolean> saveIsland(Island island) {
        return handler.saveObjectAsync(island);
    }

    /**
     * Try to get an island by its unique id
     * 
     * @param uniqueId - unique id of island
     * @return optional island
     * @since 1.3.0
     */
    @NonNull
    public Optional<Island> getIslandById(String uniqueId) {
        return Optional.ofNullable(islandCache.getIslandById(uniqueId));
    }

    /**
     * Try to get an island by its unique id. If you are needing to load all the islands to check something
     * but do not need to have them cached, then use this method and set cache to false.
     * 
     * @param uniqueId - unique id of island
     * @param cache - if false, island will not be cached if it is not already
     * @return optional island
     * @since 2.4.0
     */
    @NonNull
    public Optional<Island> getIslandById(String uniqueId, boolean cache) {
        return Optional.ofNullable(islandCache.getIslandById(uniqueId, cache));
    }

    /**
     * Returns if this is a known island uniqueId. Will not load the island from the database if it is not loaded already.
     * @param uniqueId - unique id of island
     * @return true if this island exists
     */
    public boolean isIslandId(String uniqueId) {
        return islandCache.isIslandId(uniqueId);
    }

    /**
     * Resets all flags to gamemode config.yml default
     * 
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        islandCache.resetAllFlags(world);
        this.saveAll();
    }

    /**
     * Resets a flag to gamemode config.yml default
     * 
     * @param world - world
     * @param flag  - flag to reset
     * @since 1.8.0
     */
    public void resetFlag(World world, Flag flag) {
        islandCache.resetFlag(world, flag);
        this.saveAll();
    }

    /**
     * Is user mid home teleport?
     * 
     * @return true or false
     */
    public boolean isGoingHome(User user) {
        return goingHome.contains(user.getUniqueId());
    }

    /**
     * Get the number of concurrent islands for this player
     * 
     * @param uuid  UUID of player
     * @param world world to check
     * @return number of islands this player owns in this game world
     */
    public int getNumberOfConcurrentIslands(UUID uuid, World world) {
        return islandCache.getIslands(world, uuid).size();
    }

    /**
     * Sets the user's primary island
     * 
     * @param uuid user's uuid
     * @param i    island
     */
    public void setPrimaryIsland(UUID uuid, Island i) {
        this.getIslandCache().setPrimaryIsland(uuid, i);
    }

    /**
     * Convenience method. See {@link IslandCache#getIsland(World, UUID)}
     * 
     * @param world world
     * @param uuid  player's UUID
     * @return Island of player or null if there isn't one
     */
    public Island getPrimaryIsland(World world, UUID uuid) {
        return this.getIslandCache().getIsland(world, uuid);
    }

}
