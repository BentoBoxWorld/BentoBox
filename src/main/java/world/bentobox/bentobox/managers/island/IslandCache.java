package world.bentobox.bentobox.managers.island;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class IslandCache {
    @NonNull
    private Map<@NonNull Location, @NonNull Island> islandsByLocation;
    /**
     * Map of all islands with island uniqueId as key
     */
    @NonNull
    private Map<@NonNull String, @NonNull Island> islandsById;
    /**
     * Every player who is associated with an island is in this map.
     */
    @NonNull
    private Map<@NonNull World, @NonNull Map<@NonNull UUID, @NonNull Island>> islandsByUUID;
    @NonNull
    private Map<@NonNull World, @NonNull IslandGrid> grids;

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
            // Make world
            islandsByUUID.putIfAbsent(island.getWorld(), new HashMap<>());
            // Only add islands to this map if they are owned
            if (island.getOwner() != null) {
                islandsByUUID.get(island.getWorld()).put(island.getOwner(), island);
                island.getMemberSet().forEach(member -> islandsByUUID.get(island.getWorld()).put(member, island));
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
        islandsByUUID.putIfAbsent(island.getWorld(), new HashMap<>());
        islandsByUUID.get(island.getWorld()).put(uuid, island);
    }

    /**
     * Adds an island to the grid register
     * @param newIsland new island
     * @return true if successfully added, false if not
     */
    private boolean addToGrid(@NonNull Island newIsland) {
        grids.putIfAbsent(newIsland.getWorld(), new IslandGrid());
        return grids.get(newIsland.getWorld()).addToGrid(newIsland);
    }

    public void clear() {
        islandsByLocation.clear();
        islandsById.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the database. Does not remove blocks
     * @param island island to delete
     * @return true if successful, false if not
     */
    public boolean deleteIslandFromCache(@NonNull Island island) {
        if (!islandsByLocation.remove(island.getCenter(), island) || !islandsByUUID.containsKey(island.getWorld())) {
            return false;
        }
        islandsById.remove(island.getUniqueId());
        islandsByUUID.get(island.getWorld()).entrySet().removeIf(en -> en.getValue().equals(island));
        // Remove from grid
        grids.putIfAbsent(island.getWorld(), new IslandGrid());
        return grids.get(island.getWorld()).removeFromGrid(island);
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
     * Returns island referenced by UUID
     * @param world world to check. Includes nether and end worlds.
     * @param uuid player's UUID
     * @return island or null if none
     */
    @Nullable
    public Island get(@NonNull World world, @NonNull UUID uuid) {
        return islandsByUUID.containsKey(Util.getWorld(world)) ? islandsByUUID.get(Util.getWorld(world)).get(uuid) : null;
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
        if (!grids.containsKey(Util.getWorld(location.getWorld()))) {
            return null;
        }
        return grids.get(Util.getWorld(location.getWorld())).getIslandAt(location.getBlockX(), location.getBlockZ());
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
     * @param world world to check
     * @param uuid uuid of player to check
     * @param minimumRank minimum rank requested
     * @return set of UUID's of island members. If there is no island, this set will be empty
     */
    @NonNull
    public Set<UUID> getMembers(@NonNull World world, @NonNull UUID uuid, @NonNull int minimumRank) {
        islandsByUUID.putIfAbsent(Util.getWorld(world), new HashMap<>());
        Island island = islandsByUUID.get(Util.getWorld(world)).get(uuid);
        if (island != null) {
            return island.getMemberSet(minimumRank);
        }
        return new HashSet<>(0);
    }

    /**
     * @param world the world to check
     * @param uuid the player's UUID
     * @return island owner's UUID, the player UUID if they are not in a team, or null if there is no island
     */
    @Nullable
    public UUID getOwner(@NonNull World world, @NonNull UUID uuid) {
        islandsByUUID.putIfAbsent(Util.getWorld(world), new HashMap<>());
        Island island = islandsByUUID.get(Util.getWorld(world)).get(uuid);
        if (island != null) {
            return island.getOwner();
        }
        return null;
    }

    /**
     * @param world the world to check
     * @param uuid the player
     * @return true if player has island and owns it
     */
    public boolean hasIsland(@NonNull World world, @NonNull UUID uuid) {
        islandsByUUID.putIfAbsent(Util.getWorld(world), new HashMap<>());
        Island island = islandsByUUID.get(Util.getWorld(world)).get(uuid);
        return island != null && uuid.equals(island.getOwner());
    }

    /**
     * Removes a player from the cache. If the player has an island, the island owner is removed and membership cleared.
     * The island is removed from the islandsByUUID map, but kept in the location map.
     * @param world world
     * @param uuid player's UUID
     * @return island player had or null if none
     */
    @Nullable
    public Island removePlayer(@NonNull World world, @NonNull UUID uuid) {
        world = Util.getWorld(world);
        islandsByUUID.putIfAbsent(world, new HashMap<>());
        Island island = islandsByUUID.get(world).get(uuid);
        if (island != null) {
            if (island.getOwner() != null && island.getOwner().equals(uuid)) {
                // Clear ownership and members
                island.getMembers().clear();
                island.setOwner(null);
            } else {
                // Remove player from the island membership
                island.removeMember(uuid);
            }
        }
        islandsByUUID.get(world).remove(uuid);
        return island;
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
    public int size(World world) {
        return islandsByUUID.getOrDefault(world, new HashMap<>(0)).size();
    }

    /**
     * Sets an island owner.
     * Clears out any other owner.
     * @param island island
     * @param newOwnerUUID new owner
     */
    public void setOwner(@NonNull Island island, @Nullable UUID newOwnerUUID) {
        island.setOwner(newOwnerUUID);
        islandsByUUID.computeIfAbsent(Util.getWorld(island.getWorld()), k -> new HashMap<>()).put(newOwnerUUID, island);
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
        if (islandsByUUID.containsKey(Util.getWorld(island.getWorld()))) {
            islandsByUUID.get(Util.getWorld(island.getWorld())).values().removeIf(island::equals);
        }
        if (grids.containsKey(Util.getWorld(island.getWorld()))) {
            grids.get(Util.getWorld(island.getWorld())).removeFromGrid(island);
        }
    }

    /**
     * Resets all islands in this game mode to default flag settings
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        World w = Util.getWorld(world);
        islandsById.values().stream().filter(i -> i.getWorld().equals(w)).forEach(Island::setFlagsDefaults);
    }
}
