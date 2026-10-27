package world.bentobox.bentobox.managers.island;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * This class stores the islands in memory
 * 
 * @author tastybento
 */
public class IslandCache {
    /**
     * Map of all islands with island uniqueId as key
     */
    @NonNull
    private final Map<@NonNull String, Island> islandsById;
    /**
     * Every player who is associated with an island is in this map. Key is player
     * UUID, value is a set of islands
     */
    @NonNull
    private final Map<@NonNull UUID, Set<String>> islandsByUUID;

    @NonNull
    // No @NonNull on the value type: Map#get returns null for worlds that have no grid
    // yet, and a non-null value annotation makes those null checks look impossible (java:S2583)
    private final Map<World, IslandGrid> grids;
    private final @NonNull Database<Island> handler;

    public IslandCache(@NonNull Database<Island> handler) {
        islandsById = new HashMap<>();
        islandsByUUID = new HashMap<>();
        grids = new HashMap<>();
        this.handler = handler;
    }

    /**
     * Replace the island we have with this one.
     * Used by MultiLib to update database based on another server's operation.
     * @param newIsland island
     */
    public void updateMultiLibIsland(@NonNull Island newIsland) {
        // The database load must not run under the cache lock.
        Island oldIsland = getIslandById(newIsland.getUniqueId());
        Set<UUID> newMembers = newIsland.getMembers().keySet();
        synchronized (this) {
            if (oldIsland != null) {
                Set<UUID> oldMembers = oldIsland.getMembers().keySet();
                // Remove any members who are not in the new island
                for (UUID oldMember : oldMembers) {
                    if (!newMembers.contains(oldMember)) {
                        // Member has been removed - remove island
                        islandsByUUID.computeIfAbsent(oldMember, k -> new HashSet<>()).remove(oldIsland.getUniqueId());
                    }
                }
            }
            // Update the members with the new island object
            for (UUID newMember : newMembers) {
                Set<String> set = islandsByUUID.computeIfAbsent(newMember, k -> new HashSet<>());
                if (oldIsland != null) {
                    set.remove(oldIsland.getUniqueId());
                }
                set.add(newIsland.getUniqueId());
                islandsByUUID.put(newMember, set);
            }

            if (setIslandById(newIsland) == null) {
                BentoBox.getInstance().logError("islandsById failed to update");
            }
        }
    }

    /**
     * Adds an island to the grid, used for new islands
     * Caches island.
     * 
     * @param island island to add, not null
     * @return true if successfully added, false if not
     */
    public boolean addIsland(@NonNull Island island) {
        return addIsland(island, false);
    }

    /**
     * Adds an island to the grid, used for new islands
     * 
     * @param island island to add, not null
     * @param noCache - if true, island will not be cached
     * @return true if successfully added, false if not
     */
    public synchronized boolean addIsland(@NonNull Island island, boolean noCache) {
        if (island.getCenter() == null || island.getWorld() == null) {
            return false;
        }
        // Islands from the database still hold Gson's plain maps/lists.
        island.normaliseCollections();
        if (addToGrid(island)) {
            // Insert a null into the map as a placeholder for cache
            islandsById.put(island.getUniqueId().intern(), noCache ? null : island);
            // Only add islands to this map if they are owned
            if (island.isOwned()) {
                islandsByUUID.computeIfAbsent(island.getOwner(), k -> new HashSet<>()).add(island.getUniqueId());
                island.getMemberSet().forEach(member -> addPlayer(member, island));
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a player's UUID to the look-up for islands. Does no checking. The island for this player must have been added beforehand.
     * 
     * @param uuid   player's uuid
     * @param island island to associate with this uuid. Only one island can be
     *               associated per world.
     */
    public synchronized void addPlayer(@NonNull UUID uuid, @NonNull Island island) {
        this.islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).add(island.getUniqueId());
    }

    /**
     * Adds an island to the grid register
     * 
     * @param newIsland new island
     * @return true if successfully added, false if not
     */
    private boolean addToGrid(@NonNull Island newIsland) {
        return grids.computeIfAbsent(newIsland.getWorld(), k -> new IslandGrid(this)).addToGrid(newIsland);
    }

    public synchronized void clear() {
        islandsById.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the cache. Does not remove blocks.
     * 
     * @param island island to delete
     */
    public synchronized void deleteIslandFromCache(@NonNull Island island) {
        islandsById.remove(island.getUniqueId(), island);
        removeFromIslandsByUUID(island);
        // Remove from grid
        if (grids.containsKey(island.getWorld())) {
            grids.get(island.getWorld()).removeFromGrid(island);
        }
    }

    private void removeFromIslandsByUUID(Island island) {
        removeIdFromIslandsByUUID(island.getUniqueId());
    }

    private void removeIdFromIslandsByUUID(String uniqueId) {
        Iterator<Map.Entry<UUID, Set<String>>> iterator = islandsByUUID.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Set<String>> entry = iterator.next();
            Set<String> set = entry.getValue();
            set.removeIf(uniqueId::equals);
            if (set.isEmpty()) {
                // Removes the overall entry if there is nothing left in the set
                iterator.remove();
            }
        }
    }

    /**
     * Delete island from the cache by ID. Does not remove blocks.
     * 
     * @param uniqueId - island unique ID
     */
    public void deleteIslandFromCache(@NonNull String uniqueId) {
        Island island;
        synchronized (this) {
            island = islandsById.get(uniqueId);
        }
        if (island != null) {
            deleteIslandFromCache(island);
            return;
        }
        // The island is not cached (it expired, or the DB row is already gone). The UUID
        // index can still point at the ID, so drop it without touching the database.
        synchronized (this) {
            removeIdFromIslandsByUUID(uniqueId);
            // The row is gone, so its grid reservation would otherwise linger until restart and
            // make the whole region permanently un-purgeable.
            grids.values().forEach(grid -> grid.removeById(uniqueId));
        }
    }

    /**
     * Removes every player's link to this island from the per-UUID lookup index
     * without evicting the island from {@code islandsById} or the island grid.
     * <p>
     * Used when an island is soft-deleted (see
     * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland}). The
     * island becomes unowned, so it must no longer count towards any player's
     * concurrent islands or show up in {@link #getIslands(World, UUID)}, yet it
     * must stay known so its location remains reserved and the region-file purge
     * can still find and reap it. This mirrors how the index is rebuilt on
     * startup, where {@link #addIsland(Island)} only indexes owned islands.
     *
     * @param island the island to de-index
     * @since 3.23.1
     */
    public synchronized void removeIslandFromUUIDIndex(@NonNull Island island) {
        removeFromIslandsByUUID(island);
    }

    /**
     * Returns island referenced by player's UUID. Returns the island the player is
     * on now, or their last known island
     * 
     * @param world world to check. Includes nether and end worlds.
     * @param uuid  player's UUID
     * @return island or null if none
     */
    @Nullable
    public Island getIsland(@NonNull World world, @NonNull UUID uuid) {
        List<Island> islands = getIslands(world, uuid);
        if (islands.isEmpty()) {
            return null;
        }
        for (Island island : islands) {
            if (island.isPrimary(uuid)) {
                return island;
            }
        }
        // If there is no primary set, then set one - it doesn't matter which.
        Island result = islands.getFirst();
        synchronized (this) {
            result.setPrimary(uuid);
        }
        return result;
    }

    /**
     * Returns all the islands referenced by player's UUID.
     * 
     * @param world world to check. Includes nether and end worlds.
     * @param uuid  player's UUID
     * @return list of island or empty list if none sorted from oldest to youngest
     */
    public List<Island> getIslands(@NonNull World world, @NonNull UUID uuid) {
        World w = Util.getWorld(world);
        if (w == null) {
            return new ArrayList<>();
        }
        Set<String> ids;
        synchronized (this) {
            Set<String> cached = islandsByUUID.get(uuid);
            if (cached == null || cached.isEmpty()) {
                return new ArrayList<>();
            }
            ids = new HashSet<>(cached);
        }
        return ids.stream().map(this::getIslandById)
                .filter(Objects::nonNull).filter(island -> w.equals(island.getWorld()))
                .sorted(Comparator.comparingLong(Island::getCreatedDate))
                .toList();
    }

    /**
     * Sets the current island for the user as their primary island
     * 
     * @param uuid   UUID of user
     * @param island island to make primary
     */
    public void setPrimaryIsland(@NonNull UUID uuid, @NonNull Island island) {
        if (island.getPrimaries().contains(uuid)) {
            return;
        }
        // getIslands takes the cache lock only for its snapshot and loads islands outside it.
        List<Island> islands = getIslands(island.getWorld(), uuid);
        // Apply the primary changes atomically: two concurrent callers must not leave the
        // player with two primary islands.
        synchronized (this) {
            for (Island is : islands) {
                if (is.getPrimaries().contains(uuid)) {
                    is.removePrimary(uuid);
                }
                if (is.equals(island)) {
                    is.setPrimary(uuid);
                }
            }
        }
    }

    /**
     * Returns if there is island at the location. This includes
     * the full island space, not just the protected area.
     * Does not cause a database load of the island.
     *
     * @param location the location
     * @return true if there is an island there
     */
    public boolean isIslandAt(@NonNull Location location) {
        World w = Util.getWorld(location.getWorld());
        if (w == null) {
            return false;
        }
        IslandGrid grid;
        synchronized (this) {
            grid = grids.get(w);
        }
        return grid != null && grid.isIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns the island at the location or null if there is none. This includes
     * the full island space, not just the protected area
     *
     * @param location the location
     * @return Island object
     */
    @Nullable
    public Island getIslandAt(@NonNull Location location) {
        World w = Util.getWorld(location.getWorld());
        if (w == null) {
            return null;
        }
        IslandGrid grid;
        synchronized (this) {
            grid = grids.get(w);
        }
        if (grid == null) {
            return null;
        }
        String id = grid.getIslandStringAt(location.getBlockX(), location.getBlockZ());
        // Load outside the cache lock: this is the hottest path in the plugin and the
        // database read would otherwise block every region thread.
        return id == null ? null : getIslandById(id);
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those who may be unowned). Gets them from the cache or from the database if not
     * loaded. This is a very heavy operation likely to cause lag.
     * 
     * @return unmodifiable collection containing every island.
     */
    @NonNull
    public Collection<Island> getIslands() {
        // Snapshot the ids so the blocking loads below run without holding the cache lock.
        List<String> ids;
        synchronized (this) {
            ids = new ArrayList<>(islandsById.keySet());
        }
        List<Island> result = new ArrayList<>();
        for (String id : ids) {
            Island island = getIslandById(id, false);
            if (island != null) {
                result.add(island);
            }
        }

        return Collections.unmodifiableCollection(result);
    }

    /**
     * Loads the island with the uniqueId from the database. Note, this could be a blocking call
     * and lag the server, so be careful using it.
     * @param uniqueId unique ID of the island
     * @return Island or null if that uniqueID is unknown
     * @since 2.4.0
     */
    @Nullable
    public Island loadIsland(String uniqueId) {
        Island island = handler.objectExists(uniqueId) ? handler.loadObject(uniqueId) : null;
        if (island != null) {
            // Gson hands back plain maps/lists; make them safe for concurrent region-thread use.
            island.normaliseCollections();
            island.markSaved();
        }
        return island;
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those who may be unowned) that are cached.
     * 
     * @return unmodifiable collection containing every cached island.
     */
    @NonNull
    public synchronized Collection<Island> getCachedIslands() {
        return islandsById.values().stream().filter(Objects::nonNull).toList();
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those that may be unowned) in the specified world.
     * Gets islands from the cache if they have been loaded, or from the database if not
     * 
     * @param world World of the gamemode.
     * @return unmodifiable collection containing all the islands in the specified
     *         world.
     * @since 1.7.0
     */
    @NonNull
    public Collection<Island> getIslands(@NonNull World world) {
        World overworld = Util.getWorld(world);
        if (overworld == null) {
            return Collections.emptyList();
        }

        List<Entry<String, Island>> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(islandsById.entrySet());
        }
        List<Island> result = new ArrayList<>();
        for (Entry<@NonNull String, @NonNull Island> entry : snapshot) {
            Island island = entry.getValue() != null ? entry.getValue() : loadIsland(entry.getKey());
            if (island != null && overworld.equals(island.getWorld())) {
                result.add(island);
            }
        }

        return Collections.unmodifiableCollection(result);
    }

    /**
     * Checks is a player has an island and owns it in this world. Note that players
     * may have multiple islands so this means the player is an owner of ANY island.
     * 
     * @param world the world to check
     * @param uuid  the player
     * @return true if player has an island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull UUID uuid) {
        // Callers pass the world the player is standing in, which may be the nether or the end
        World w = Util.getWorld(world);
        if (w == null) {
            return false;
        }
        Set<String> ids;
        synchronized (this) {
            Set<String> cached = islandsByUUID.get(uuid);
            if (cached == null || cached.isEmpty()) {
                return false;
            }
            ids = new HashSet<>(cached);
        }
        return ids.stream().map(this::getIslandById).filter(Objects::nonNull)
                .filter(i -> w.equals(i.getWorld()))
                .anyMatch(i -> uuid.equals(i.getOwner()));
    }

    /**
     * Removes a player from the cache only: clears the membership (or ownership) and the
     * per-UUID index entries. It deliberately does <b>not</b> run the leave-player resets
     * (inventory, ender chest, pets, economy) or fire a {@code TeamEvent} - callers that need
     * those must use {@link world.bentobox.bentobox.managers.PlayersManager#cleanLeavingPlayer}
     * and the event API themselves.
     *
     * @param world world
     * @param uuid  player's UUID
     * @return set of islands player had or empty if none
     */
    public Set<Island> removePlayer(@NonNull World world, @NonNull UUID uuid) {
        World resolvedWorld = Util.getWorld(world);
        if (resolvedWorld == null) {
            return Collections.emptySet();
        }
        Set<String> ids;
        synchronized (this) {
            Set<String> playerIslandIds = islandsByUUID.get(uuid);
            if (playerIslandIds == null || playerIslandIds.isEmpty()) {
                return Collections.emptySet();
            }
            ids = new HashSet<>(playerIslandIds);
        }

        Set<Island> removedIslands = new HashSet<>();
        for (String id : ids) {
            // Load outside the lock: this can be a blocking database call.
            Island island = getIslandById(id);
            if (island == null || !resolvedWorld.equals(island.getWorld())) {
                continue;
            }
            removedIslands.add(island);
            synchronized (this) {
                if (uuid.equals(island.getOwner())) {
                    // Player is the owner, so clear the whole island and clear the ownership.
                    // Every other member loses their index entry too, not just the owner.
                    for (UUID memberId : island.getMembers().keySet()) {
                        if (memberId.equals(uuid)) {
                            continue;
                        }
                        Set<String> memberIslands = islandsByUUID.get(memberId);
                        if (memberIslands != null) {
                            memberIslands.remove(id);
                            if (memberIslands.isEmpty()) {
                                islandsByUUID.remove(memberId);
                            }
                        }
                    }
                    island.getMembers().clear();
                    island.setOwner(null);
                } else {
                    island.removeMember(uuid);
                }
                Set<String> current = islandsByUUID.get(uuid);
                if (current != null) {
                    current.remove(id);
                    if (current.isEmpty()) {
                        islandsByUUID.remove(uuid);
                    }
                }
            }
        }
        return removedIslands;
    }

    /**
     * Removes player from island and removes the cache reference
     * 
     * @param island member's island
     * @param uuid   uuid of member to remove
     */
    public synchronized void removePlayer(@NonNull Island island, @NonNull UUID uuid) {
        Set<String> islandSet = islandsByUUID.get(uuid);
        if (islandSet != null) {
            islandSet.remove(island.getUniqueId());
            if (islandSet.isEmpty()) {
                islandsByUUID.remove(uuid);
            }
        }
        island.removeMember(uuid);
        island.removePrimary(uuid);
        // Add history record
        island.log(new LogEntry.Builder(LogType.REMOVE).data(uuid.toString(), "player").build());
    }

    /**
     * Get the number of islands in the cache
     * 
     * @return the number of islands
     */
    public synchronized int size() {
        return islandsById.size();
    }

    /**
     * Gets the number of islands in this world
     * 
     * @param world world to get the number of islands in
     * @return the number of islands
     */
    public synchronized long size(World world) {
        // Get from grids because this is where we have islands by world
        return this.grids.containsKey(world) ? this.grids.get(world).getSize() : 0L;
    }

    /**
     * Sets an island owner. Clears out any other owner.
     * 
     * @param island       island
     * @param newOwnerUUID new owner
     */
    public synchronized void setOwner(@NonNull Island island, @Nullable UUID newOwnerUUID) {
        // The previous owner is demoted to a member, not removed, so their index entry stays:
        // dropping it here would disagree with the index rebuilt on startup.
        island.setOwner(newOwnerUUID);
        if (newOwnerUUID != null) {
            islandsByUUID.computeIfAbsent(newOwnerUUID, k -> new HashSet<>()).add(island.getUniqueId());
        }
        island.setRank(newOwnerUUID, RanksManager.OWNER_RANK);
        setIslandById(island);
    }

    /**
     * Get the island by unique id. The Island will be cached if it is not already.
     * 
     * @param uniqueId unique id of the Island.
     * @return island or null if none found
     * @since 1.3.0
     */
    @Nullable
    public Island getIslandById(@NonNull String uniqueId) {
        // Load from cache or database
        return getIslandById(uniqueId, true);
    }

    /**
     * Get the island by unique id
     * 
     * @param uniqueId unique id of the Island.
     * @param cache if true, then the Island will be cached if it is not already
     * @return island or null if none found
     * @since 2.4.0
     */
    @Nullable
    public Island getIslandById(@NonNull String uniqueId, boolean cache) {
        // The database load must not run under the cache lock: it is a blocking call and every
        // region thread would queue behind it.
        synchronized (this) {
            Island cached = islandsById.get(uniqueId);
            if (cached != null) {
                return cached;
            }
        }
        Island island = loadIsland(uniqueId);
        if (island == null || !cache) {
            return island;
        }
        synchronized (this) {
            Island cached = islandsById.get(uniqueId);
            if (cached != null) {
                return cached;
            }
            islandsById.put(uniqueId, island);
        }
        return island;
    }

    /**
     * Removes the island from the cache to ensure it is reloaded from the database next time.
     * @param uniqueId unique id of the Island.
     * @return true if the island was in the cache, false otherwise.
     * @since 2.4.1
     */
    public synchronized boolean expireIslandById(@NonNull String uniqueId) {
        return islandsById.containsKey(uniqueId) && islandsById.put(uniqueId, null) != null;
    }

    /**
     * Place the island into the cache map 
     * @param island island
     * @return the previous value associated with island, or null if this is a new entry
     */
    synchronized Island setIslandById(Island island) {
        island.normaliseCollections();
        return islandsById.put(island.getUniqueId().intern(), island);
    }

    /**
     * Resets all islands in this game mode to default flag settings.
     * 
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        TaskScheduler.async(BentoBox.getInstance(),
                () -> this.getIslands(world).forEach(Island::setFlagsDefaults));
    }

    /**
     * Resets a specific flag on all game mode islands in world to default setting
     * 
     * @param world - world
     * @param flag  - flag to reset
     * @since 1.8.0
     */
    public void resetFlag(World world, Flag flag) {
        int setting = BentoBox.getInstance().getIWM().getDefaultIslandFlags(world).getOrDefault(flag,
                flag.getDefaultRank());
        this.getIslands(world).forEach(i -> i.setFlag(flag, setting));
    }

    /**
     * Get all the island ids
     * 
     * @return set of ids
     * @since 1.8.0
     */
    public synchronized Set<String> getAllIslandIds() {
        return new HashSet<>(islandsById.keySet());
    }

    /**
     * Get an unmodifiable list of all islands this player is involved with
     * @param uniqueId player's UUID
     * @return list of islands
     */
    public @NonNull List<Island> getIslands(UUID uniqueId) {
        // Snapshot the ids under the lock; the database loads below must run outside it.
        Set<String> ids;
        synchronized (this) {
            ids = new HashSet<>(islandsByUUID.getOrDefault(uniqueId, Collections.emptySet()));
        }
        return ids.stream().map(this::getIslandById)
                .filter(Objects::nonNull) // Filter out null values
                .toList();
    }

    /**
     * Returns if this is a known island uniqueId. Will not load the island from the database if it is not loaded already.
     * @param uniqueId - unique id of island
     * @return true if this island exists
     */
    public synchronized boolean isIslandId(String uniqueId) {
        return this.islandsById.containsKey(uniqueId);
    }

    /**
     * Returns if this island is cached. 
     * @param uniqueId - unique id of island
     * @return true if this island is caches, false if not, or if the island is unknown
     */
    public synchronized boolean isIslandCached(String uniqueId) {
        return islandsById.get(uniqueId) != null;
    }
    
    /**
     * Return the island grid for this world
     * @param world world
     * @return IslandGrid for the world, or null if there is none or the world is unknown.
     */
    public synchronized IslandGrid getIslandGrid(World world) {
        World w = Util.getWorld(world);
        if (w == null) {
            return null;
        }
        return this.grids.get(w);
    }

}
