package world.bentobox.bentobox.managers.island;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * This class stores the islands in memory
 * @author tastybento
 */
public class IslandCache {
    @NonNull
    private final Map<@NonNull Location, @NonNull Island> islandsByLocation;
    /**
     * Map of all islands with island uniqueId as key
     */
    @NonNull
    private final Map<@NonNull String, @NonNull Island> islandsById;
    /**
     * Every player who is associated with an island is in this map.
     */
    @NonNull
    private final Map<@NonNull UUID, Set<Island>> islandsByUUID;

    @NonNull
    private final Map<@NonNull World, @NonNull IslandGrid> grids;

    public IslandCache() {
        islandsByLocation = new HashMap<>();
        islandsById = new HashMap<>();
        islandsByUUID = new HashMap<>();
        grids = new HashMap<>();
    }

    /**
     * Adds an island to the grid
     * @param island island to add, not null
     * @return true if successfully added, false if not
     */
    public boolean addIsland(@NonNull Island island) {
        if (island.getCenter() == null || island.getWorld() == null) {
            /* Special handling - return true.
               The island will not be quarantined, but just not loaded
               This can occur when a gamemode is removed temporarily from the server
               TODO: have an option to remove these when the purge command is added
             */
            return true;
        }
        if (addToGrid(island)) {
            islandsByLocation.put(island.getCenter(), island);
            islandsById.put(island.getUniqueId(), island);
            // Only add islands to this map if they are owned
            if (island.isOwned()) {
                islandsByUUID.computeIfAbsent(island.getOwner(), k -> new HashSet<>()).add(island);
                island.getMemberSet().forEach(member -> addPlayer(member, island));
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a player's UUID to the look up for islands. Does no checking
     * @param uuid player's uuid
     * @param island island to associate with this uuid. Only one island can be associated per world.
     */
    public void addPlayer(@NonNull UUID uuid, @NonNull Island island) {
        islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).add(island);
    }

    /**
     * Adds an island to the grid register
     * @param newIsland new island
     * @return true if successfully added, false if not
     */
    private boolean addToGrid(@NonNull Island newIsland) {
        return grids.computeIfAbsent(newIsland.getWorld(), k -> new IslandGrid()).addToGrid(newIsland);
    }

    public void clear() {
        islandsByLocation.clear();
        islandsById.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the cache. Does not remove blocks.
     * @param island island to delete
     * @return true if successful, false if not
     */
    public boolean deleteIslandFromCache(@NonNull Island island) {
        if (!islandsByLocation.remove(island.getCenter(), island)) {
            return false;
        }
        islandsById.remove(island.getUniqueId());
        removeFromIslandsByUUID(island);
        // Remove from grid
        grids.putIfAbsent(island.getWorld(), new IslandGrid());
        return grids.get(island.getWorld()).removeFromGrid(island);
    }

    private void removeFromIslandsByUUID(Island island) {
        for (Set<Island> set : islandsByUUID.values()) {
            set.removeIf(island::equals);
        }
    }

    /**
     * Delete island from the cache by ID. Does not remove blocks.
     * @param uniqueId - island unique ID
     */
    public void deleteIslandFromCache(@NonNull String uniqueId) {
        islandsById.remove(uniqueId);
        islandsByLocation.values().removeIf(i -> i.getUniqueId().equals(uniqueId));
        for (Set<Island> set : islandsByUUID.values()) {
            set.removeIf(i -> i.getUniqueId().equals(uniqueId));
        }
    }

    /**
     * Get island based on the exact center location of the island
     * @param location location to search for
     * @return island or null if it does not exist
     */
    @Nullable
    public Island get(@NonNull Location location) {
        return islandsByLocation.get(location);
    }

    /**
     * Returns island referenced by player's UUID.
     * Returns the island the player is on now, or their last known island
     * @param world world to check. Includes nether and end worlds.
     * @param uuid player's UUID
     * @return island or null if none
     */
    @Nullable
    public Island get(@NonNull World world, @NonNull UUID uuid) {
        Set<Island> islands = getIslands(world, uuid);
        if (islands.isEmpty()) {
            return null;
        }
        for (Island island : islands) {
            if (island.isPrimary()) {
                return island;
            }
        }
        // If there is no primary set, then set one - it doesn't matter which.
        Island result = islands.iterator().next();
        result.setPrimary(true);
        return result;
    }

    /**
     * Returns all the islands referenced by player's UUID.
     * @param world world to check. Includes nether and end worlds.
     * @param uuid player's UUID
     * @return list of island or empty list if none
     */
    public Set<Island> getIslands(@NonNull World world, @NonNull UUID uuid) {
        World w = Util.getWorld(world);
        if (w == null) {
            return new HashSet<>();
        }
        return islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).stream().filter(i -> w.equals(i.getWorld())).collect(Collectors.toSet());
    }

    /**
     * Sets the current island for the user as their primary island
     * @param uuid UUID of user
     * @param island island to make primary
     */
    public void setPrimaryIsland(@NonNull UUID uuid, @NonNull Island island) {
        for (Island is : getIslands(island.getWorld(), uuid)) {
            is.setPrimary(island.equals(is));
        }
    }

    /**
     * Returns the island at the location or null if there is none.
     * This includes the full island space, not just the protected area
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
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even those who may be unowned).
     * @return unmodifiable collection containing every island.
     */
    @NonNull
    public Collection<Island> getIslands() {
        return Collections.unmodifiableCollection(islandsByLocation.values());
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even those who may be unowned) in the specified world.
     * @param world World of the gamemode.
     * @return unmodifiable collection containing all the islands in the specified world.
     * @since 1.7.0
     */
    @NonNull
    public Collection<Island> getIslands(@NonNull World world) {
        World overworld = Util.getWorld(world);
        if (overworld == null) {
            return Collections.emptyList();
        }
        return islandsByLocation.entrySet().stream()
                .filter(entry -> overworld.equals(Util.getWorld(entry.getKey().getWorld()))) // shouldn't make NPEs
                .map(Map.Entry::getValue).toList();
    }

    /**
     * Get the members of the user's team
     * @param world world to check
     * @param uuid uuid of player to check
     * @param minimumRank minimum rank requested
     * @return set of UUID's of island members. If there are no islands, this set will be empty
     */
    @NonNull
    public Set<UUID> getMembers(@NonNull World world, @NonNull UUID uuid, int minimumRank) {
        return getIslands(world, uuid)
                .stream()
                .flatMap(island -> island.getMemberSet(minimumRank).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Get the UUID of the owner of the island of the player, which may be their UUID
     * @param world the world to check
     * @param uuid the player's UUID
     * @return island owner's UUID or null if there is no island owned by the player in this world
     */
    @Nullable
    public UUID getOwner(@NonNull World world, @NonNull UUID uuid) {
        World w = Util.getWorld(world);
        Set<Island> islands = islandsByUUID.get(uuid);
        if (w == null || islands == null || islands.isEmpty()) {
            return null;
        }
        // Find the island for this world
        return islands.stream().filter(i -> w.equals(i.getWorld())).findFirst().map(Island::getOwner).orElse(null);
    }

    /**
     * Checks is a player has an island and owns it
     * @param world the world to check
     * @param uuid the player
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull UUID uuid) {
        return uuid.equals(getOwner(world, uuid));
    }

    /**
     * Removes a player from the cache. If the player has an island, the island owner is removed and membership cleared.
     * The island is removed from the islandsByUUID map, but kept in the location map.
     * @param world world
     * @param uuid player's UUID
     * @return list of islands player had or empty if none
     */
    public Set<Island> removePlayer(@NonNull World world, @NonNull UUID uuid) {
        World w = Util.getWorld(world);
        Set<Island> islandSet = islandsByUUID.get(uuid);
        if (w == null || islandSet == null) {
            return Collections.emptySet(); // Return empty list if no islands map exists for the world
        }

        islandSet.forEach(island -> {
            if (uuid.equals(island.getOwner())) {
                island.getMembers().clear();
                island.setOwner(null);
            } else {
                island.removeMember(uuid);
            }
        });

        islandsByUUID.remove(uuid);

        return islandSet;
    }

    /**
     * Get the number of islands in the cache
     * @return the number of islands
     */
    public int size() {
        return islandsByLocation.size();
    }

    /**
     * Gets the number of islands in the cache for this world
     * @param world world to get the number of islands in
     * @return the number of islands
     */
    public long size(World world) {
        return this.islandsByLocation.keySet().stream().map(Location::getWorld).filter(world::equals).count();
    }

    /**
     * Sets an island owner.
     * Clears out any other owner.
     * @param island island
     * @param newOwnerUUID new owner
     */
    public void setOwner(@NonNull Island island, @Nullable UUID newOwnerUUID) {
        island.setOwner(newOwnerUUID);
        if (newOwnerUUID != null) {
            islandsByUUID.computeIfAbsent(newOwnerUUID, k -> new HashSet<>()).add(island);
        }
        islandsByLocation.put(island.getCenter(), island);
        islandsById.put(island.getUniqueId(), island);
    }

    /**
     * Get the island by unique id
     * @param uniqueId unique id of the Island.
     * @return island or null if none found
     * @since 1.3.0
     */
    @Nullable
    public Island getIslandById(@NonNull String uniqueId) {
        return islandsById.get(uniqueId);
    }

    /**
     * Removes an island from the cache completely without altering the island object
     * @param island - island to remove
     * @since 1.3.0
     */
    public void removeIsland(@NonNull Island island) {
        islandsByLocation.values().removeIf(island::equals);
        islandsById.values().removeIf(island::equals);
        islandsByUUID.values().removeIf(island::equals);
        World w = Util.getWorld(island.getWorld());
        if (w == null) {
            return;
        }

        if (grids.containsKey(w)) {
            grids.get(w).removeFromGrid(island);
        }
    }

    /**
     * Resets all islands in this game mode to default flag settings
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        World w = Util.getWorld(world);
        if (w == null) {
            return;
        }
        islandsById.values().stream().filter(i -> i.getWorld().equals(w)).forEach(Island::setFlagsDefaults);
    }

    /**
     * Resets a specific flag on all game mode islands in world to default setting
     * @param world - world
     * @param flag - flag to reset
     * @since 1.8.0
     */
    public void resetFlag(World world, Flag flag) {
        World w = Util.getWorld(world);
        if (w == null) {
            return;
        }
        int setting = BentoBox.getInstance().getIWM().getDefaultIslandFlags(w).getOrDefault(flag, flag.getDefaultRank());
        islandsById.values().stream().filter(i -> i.getWorld().equals(w)).forEach(i -> i.setFlag(flag, setting));
    }

    /**
     * Get all the island ids
     * @return set of ids
     * @since 1.8.0
     */
    public Set<String> getAllIslandIds() {
        return islandsById.keySet();
    }


}
