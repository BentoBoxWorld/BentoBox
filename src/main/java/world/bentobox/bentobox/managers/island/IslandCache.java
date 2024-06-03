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
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
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
    private final Map<@NonNull String, @NonNull Island> islandsById;
    /**
     * Every player who is associated with an island is in this map. Key is player
     * UUID, value is a set of islands
     */
    @NonNull
    private final Map<@NonNull UUID, Set<String>> islandsByUUID;

    @NonNull
    private final Map<@NonNull World, @NonNull IslandGrid> grids;
    private final @NonNull Database<Island> handler;

    public IslandCache(@NonNull Database<Island> handler) {
        islandsById = new HashMap<>();
        islandsByUUID = new HashMap<>();
        grids = new HashMap<>();
        this.handler = handler;
    }

    /**
     * Replace the island we have with this one
     * @param newIsland island
     */
    public void updateIsland(@NonNull Island newIsland) {
        if (newIsland.isDeleted()) {
            this.deleteIslandFromCache(newIsland);
            return;
        }
        // Get the old island
        Island oldIsland = getIslandById(newIsland.getUniqueId());
        Set<UUID> newMembers = newIsland.getMembers().keySet();
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
    public boolean addIsland(@NonNull Island island, boolean noCache) {
        if (island.getCenter() == null || island.getWorld() == null) {
            return false;
        }
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
     * Adds a player's UUID to the look up for islands. Does no checking. The island for this player must have been added beforehand.
     * 
     * @param uuid   player's uuid
     * @param island island to associate with this uuid. Only one island can be
     *               associated per world.
     */
    public void addPlayer(@NonNull UUID uuid, @NonNull Island island) {
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

    public void clear() {
        islandsById.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the cache. Does not remove blocks.
     * 
     * @param island island to delete
     */
    public void deleteIslandFromCache(@NonNull Island island) {
        islandsById.remove(island.getUniqueId(), island);
        removeFromIslandsByUUID(island);
        // Remove from grid
        if (grids.containsKey(island.getWorld())) {
            grids.get(island.getWorld()).removeFromGrid(island);
        }
    }

    private void removeFromIslandsByUUID(Island island) {
        for (Set<String> set : islandsByUUID.values()) {
            set.removeIf(island.getUniqueId()::equals);
        }
    }

    /**
     * Delete island from the cache by ID. Does not remove blocks.
     * 
     * @param uniqueId - island unique ID
     */
    public void deleteIslandFromCache(@NonNull String uniqueId) {
        if (islandsById.containsKey(uniqueId)) {
            deleteIslandFromCache(getIslandById(uniqueId));
        }
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
        Island result = islands.iterator().next();
        result.setPrimary(uuid);
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
        return islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).stream().map(this::getIslandById)
                .filter(Objects::nonNull).filter(island -> w.equals(island.getWorld()))
                .sorted(Comparator.comparingLong(Island::getCreatedDate))
                .collect(Collectors.toList());
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
        for (Island is : getIslands(island.getWorld(), uuid)) {
            if (is.getPrimaries().contains(uuid)) {
                is.removePrimary(uuid);
            }
            if (is.equals(island)) {
                is.setPrimary(uuid);
            }
        }
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
        if (w == null || !grids.containsKey(w)) {
            return null;
        }
        return grids.get(w).getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those who may be unowned). Gets them from the cache or from the database if not
     * loaded.
     * 
     * @return unmodifiable collection containing every island.
     */
    @NonNull
    public Collection<Island> getIslands() {
        List<Island> result = new ArrayList<>();
        for (Entry<@NonNull String, @NonNull Island> entry : islandsById.entrySet()) {
            Island island = entry.getValue() != null ? entry.getValue() : handler.loadObject(entry.getKey());
            if (island != null) {
                result.add(island);
            }
        }

        return Collections.unmodifiableCollection(result);
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even
     * those who may be unowned) that are cached.
     * 
     * @return unmodifiable collection containing every cached island.
     */
    @NonNull
    public Collection<Island> getCachedIslands() {
        return islandsById.entrySet().stream().filter(en -> Objects.nonNull(en.getValue())).map(Map.Entry::getValue)
                .toList();
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

        List<Island> result = new ArrayList<>();
        for (Entry<@NonNull String, @NonNull Island> entry : islandsById.entrySet()) {
            Island island = entry.getValue() != null ? entry.getValue() : handler.loadObject(entry.getKey());
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
        if (!islandsByUUID.containsKey(uuid)) {
            return false;
        }
        return this.islandsByUUID.get(uuid).stream().map(this::getIslandById).filter(Objects::nonNull)
                .filter(i -> world.equals(i.getWorld()))
                .anyMatch(i -> uuid.equals(i.getOwner()));
    }

    /**
     * Removes a player from the cache. If the player has an island, the island
     * owner is removed and membership cleared.
     * 
     * @param world world
     * @param uuid  player's UUID
     * @return set of islands player had or empty if none
     */
    public Set<Island> removePlayer(@NonNull World world, @NonNull UUID uuid) {
        World resolvedWorld = Util.getWorld(world);
        Set<String> playerIslandIds = islandsByUUID.get(uuid);
        Set<Island> removedIslands = new HashSet<>();

        if (resolvedWorld == null || playerIslandIds == null) {
            return Collections.emptySet(); // Return empty set if no islands map exists for the world
        }

        // Iterate over the player's island IDs and process each associated island
        Iterator<String> iterator = playerIslandIds.iterator();
        while (iterator.hasNext()) {
            Island island = this.getIslandById(iterator.next());
            if (island != null && resolvedWorld.equals(island.getWorld())) {
                removedIslands.add(island);

                if (uuid.equals(island.getOwner())) {
                    // Player is the owner, so clear the whole island and clear the ownership
                    island.getMembers().clear();
                    island.setOwner(null);
                } else {
                    island.removeMember(uuid);
                }

                // Remove this island from the set of islands associated with this player
                iterator.remove();
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
    public void removePlayer(@NonNull Island island, @NonNull UUID uuid) {
        Set<String> islandSet = islandsByUUID.get(uuid);
        if (islandSet != null) {
            islandSet.remove(island.getUniqueId());
        }
        island.removeMember(uuid);
        island.removePrimary(uuid);
    }

    /**
     * Get the number of islands in the cache
     * 
     * @return the number of islands
     */
    public int size() {
        return islandsById.size();
    }

    /**
     * Gets the number of islands in this world
     * 
     * @param world world to get the number of islands in
     * @return the number of islands
     */
    public long size(World world) {
        // Get from grids because this is where we have islands by world
        return this.grids.containsKey(world) ? this.grids.get(world).getSize() : 0L;
    }

    /**
     * Sets an island owner. Clears out any other owner.
     * 
     * @param island       island
     * @param newOwnerUUID new owner
     */
    public void setOwner(@NonNull Island island, @Nullable UUID newOwnerUUID) {
        island.setOwner(newOwnerUUID);
        if (newOwnerUUID != null) {
            islandsByUUID.computeIfAbsent(newOwnerUUID, k -> new HashSet<>()).add(island.getUniqueId());
        }
        island.setRank(newOwnerUUID, RanksManager.OWNER_RANK);
        setIslandById(island);
    }

    /**
     * Get the island by unique id
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
        Island island = islandsById.get(uniqueId);
        if (island != null) {
            return island;
        }

        island = handler.loadObject(uniqueId);
        if (cache && island != null) {
            islandsById.put(uniqueId, island);
        }
        return island;
    }

    /**
     * Place the island into the cache map 
     * @param island island
     * @return the previous value associated with island, or null if this is a new entry
     */
    Island setIslandById(Island island) {
        return islandsById.put(island.getUniqueId().intern(), island);
    }

    /**
     * Resets all islands in this game mode to default flag settings.
     * 
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(),
                () -> this.getIslands(world).stream().forEach(Island::setFlagsDefaults));
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
        this.getIslands(world).stream().forEach(i -> i.setFlag(flag, setting));
    }

    /**
     * Get all the island ids
     * 
     * @return set of ids
     * @since 1.8.0
     */
    public Set<String> getAllIslandIds() {
        return islandsById.keySet();
    }

    /**
     * Get a unmodifiable list of all islands this player is involved with
     * @param uniqueId player's UUID
     * @return list of islands
     */
    public @NonNull List<Island> getIslands(UUID uniqueId) {
        return islandsByUUID.getOrDefault(uniqueId, Collections.emptySet()).stream().map(this::getIslandById).toList();
    }

    /**
     * Returns if this is a known island uniqueId. Will not load the island from the database if it is not loaded already.
     * @param uniqueId - unique id of island
     * @return true if this island exists
     */
    public boolean isIslandId(String uniqueId) {
        return this.islandsById.containsKey(uniqueId);
    }

}
