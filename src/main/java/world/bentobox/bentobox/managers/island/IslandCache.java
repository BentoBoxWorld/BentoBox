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
import java.util.Objects;
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
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * This class stores the islands in memory
 * 
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
     * Every player who is associated with an island is in this map. Key is player
     * UUID, value is a set of islands
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
     * Replace the island we have with this one
     * @param newIsland island
     */
    public void updateIsland(@NonNull Island newIsland) {
        if (newIsland.isDeleted()) {
            this.deleteIslandFromCache(newIsland);
            return;
        }
        // Get the old island
        Island oldIsland = islandsById.get(newIsland.getUniqueId());
        compareIslands(oldIsland, newIsland);
        Set<UUID> newMembers = newIsland.getMembers().keySet();
        if (oldIsland != null) {
            Set<UUID> oldMembers = oldIsland.getMembers().keySet();
            // Remove any members who are not in the new island
            for (UUID oldMember : oldMembers) {
                if (!newMembers.contains(oldMember)) {
                    // Member has been removed - remove island
                    islandsByUUID.computeIfAbsent(oldMember, k -> new HashSet<>()).remove(oldIsland);
                }
            }
        }
        // Update the members with the new island object
        for (UUID newMember : newMembers) {
            Set<Island> set = islandsByUUID.computeIfAbsent(newMember, k -> new HashSet<>());
            set.remove(oldIsland);
            set.add(newIsland);
            islandsByUUID.put(newMember, set);
        }

        if (islandsByLocation.put(newIsland.getCenter(), newIsland) == null) {
            BentoBox.getInstance().logError("islandsByLocation failed to update");

        }
        if (islandsById.put(newIsland.getUniqueId(), newIsland) == null) {
            BentoBox.getInstance().logError("islandsById failed to update");
        }

    }

    /**
     * TODO REMOVE THIS DEBUG METHOD
     * @param island1 island1
     * @param island2 island 2
     */
    public void compareIslands(Island island1, Island island2) {
        if (island1 == null || island2 == null) {
            BentoBox.getInstance().logDebug("One or both islands are null. Cannot compare.");
            return;
        }

        if (!island1.getUniqueId().equals(island2.getUniqueId())) {
            BentoBox.getInstance().logDebug("Island unique IDs are different.");
        }

        if (island1.isDeleted() != island2.isDeleted()) {
            BentoBox.getInstance().logDebug("Island deleted states are different.");
        }

        if (!Objects.equals(island1.getCenter(), island2.getCenter())) {
            BentoBox.getInstance().logDebug("Island centers are different.");
        }

        if (island1.getRange() != island2.getRange()) {
            BentoBox.getInstance().logDebug("Island ranges are different.");
        }

        if (island1.getProtectionRange() != island2.getProtectionRange()) {
            BentoBox.getInstance().logDebug("Island protection ranges are different.");
        }

        if (!island1.getBonusRanges().equals(island2.getBonusRanges())) {
            BentoBox.getInstance().logDebug("Island bonus ranges are different.");
        }

        if (island1.getMaxEverProtectionRange() != island2.getMaxEverProtectionRange()) {
            BentoBox.getInstance().logDebug("Island max ever protection ranges are different.");
        }

        if (!island1.getWorld().equals(island2.getWorld())) {
            BentoBox.getInstance().logDebug("Island worlds are different.");
        }

        if (!Objects.equals(island1.getGameMode(), island2.getGameMode())) {
            BentoBox.getInstance().logDebug("Island game modes are different.");
        }

        if (!Objects.equals(island1.getName(), island2.getName())) {
            BentoBox.getInstance().logDebug("Island names are different.");
        }

        if (island1.getCreatedDate() != island2.getCreatedDate()) {
            BentoBox.getInstance().logDebug("Island created dates are different.");
        }

        if (island1.getUpdatedDate() != island2.getUpdatedDate()) {
            BentoBox.getInstance().logDebug("Island updated dates are different.");
        }

        if (!Objects.equals(island1.getOwner(), island2.getOwner())) {
            BentoBox.getInstance().logDebug("Island owners are different.");
        }

        if (!island1.getMembers().equals(island2.getMembers())) {
            BentoBox.getInstance().logDebug("Island members are different.");
        }

        if (!Objects.equals(island1.getMaxMembers(), island2.getMaxMembers())) {
            BentoBox.getInstance().logDebug("Island max members are different.");
        }

        if (island1.isSpawn() != island2.isSpawn()) {
            BentoBox.getInstance().logDebug("Island spawn states are different.");
        }

        if (!island1.getFlags().equals(island2.getFlags())) {
            BentoBox.getInstance().logDebug("Island flags are different.");
        }

        if (!island1.getHistory().equals(island2.getHistory())) {
            BentoBox.getInstance().logDebug("Island histories are different.");
        }

        if (!island1.getSpawnPoint().equals(island2.getSpawnPoint())) {
            BentoBox.getInstance().logDebug("Island spawn points are different.");
        }

        if (island1.isDoNotLoad() != island2.isDoNotLoad()) {
            BentoBox.getInstance().logDebug("Island do not load states are different.");
        }

        if (!island1.getCooldowns().equals(island2.getCooldowns())) {
            BentoBox.getInstance().logDebug("Island cooldowns are different.");
        }

        if (!Objects.equals(island1.getCommandRanks(), island2.getCommandRanks())) {
            BentoBox.getInstance().logDebug("Island command ranks are different.");
        }

        if (!Objects.equals(island1.getMetaData(), island2.getMetaData())) {
            BentoBox.getInstance().logDebug("Island metadata are different.");
        }

        if (!Objects.equals(island1.getHomes(), island2.getHomes())) {
            BentoBox.getInstance().logDebug("Island homes are different.");
        }

        if (!Objects.equals(island1.getMaxHomes(), island2.getMaxHomes())) {
            BentoBox.getInstance().logDebug("Island max homes are different.");
        }
    }

    /**
     * Adds an island to the grid
     * 
     * @param island island to add, not null
     * @return true if successfully added, false if not
     */
    public boolean addIsland(@NonNull Island island) {
        if (island.getCenter() == null || island.getWorld() == null) {
            return false;
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
     * 
     * @param uuid   player's uuid
     * @param island island to associate with this uuid. Only one island can be
     *               associated per world.
     */
    public void addPlayer(@NonNull UUID uuid, @NonNull Island island) {
        islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).add(island);
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
        islandsByLocation.clear();
        islandsById.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the cache. Does not remove blocks.
     * 
     * @param island island to delete
     * @return true if successful, false if not
     */
    public boolean deleteIslandFromCache(@NonNull Island island) {
        if (!islandsByLocation.remove(island.getCenter(), island)) {
            // Already deleted
            return false;
        }
        islandsById.remove(island.getUniqueId());
        removeFromIslandsByUUID(island);
        // Remove from grid
        if (grids.containsKey(island.getWorld())) {
            return grids.get(island.getWorld()).removeFromGrid(island);
        }
        return false;
    }

    private void removeFromIslandsByUUID(Island island) {
        for (Set<Island> set : islandsByUUID.values()) {
            Iterator<Island> is = set.iterator();
            while (is.hasNext()) {
                Island i = is.next();
                if (i.equals(island)) {
                    is.remove();
                }
            }
            // set.removeIf(island::equals);
        }
    }

    /**
     * Delete island from the cache by ID. Does not remove blocks.
     * 
     * @param uniqueId - island unique ID
     */
    public boolean deleteIslandFromCache(@NonNull String uniqueId) {
        if (islandsById.containsKey(uniqueId)) {
            return deleteIslandFromCache(islandsById.get(uniqueId));
        }
        return false;
    }

    /**
     * Get island based on the exact center location of the island
     * 
     * @param location location to search for
     * @return island or null if it does not exist
     */
    @Nullable
    public Island get(@NonNull Location location) {
        return islandsByLocation.get(location);
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
    public Island get(@NonNull World world, @NonNull UUID uuid) {
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
        return islandsByUUID.computeIfAbsent(uuid, k -> new HashSet<>()).stream().filter(island -> w.equals(island.getWorld()))
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
     * those who may be unowned).
     * 
     * @return unmodifiable collection containing every island.
     */
    @NonNull
    public Collection<Island> getIslands() {
        return Collections.unmodifiableCollection(islandsByLocation.values());
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
        World overworld = Util.getWorld(world);
        if (overworld == null) {
            return Collections.emptyList();
        }
        return islandsByLocation.entrySet().stream()
                .filter(entry -> overworld.equals(Util.getWorld(entry.getKey().getWorld()))) // shouldn't make NPEs
                .map(Map.Entry::getValue).toList();
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
        return this.islandsByUUID.get(uuid).stream().filter(i -> world.equals(i.getWorld()))
                .anyMatch(i -> uuid.equals(i.getOwner()));
    }

    /**
     * Removes a player from the cache. If the player has an island, the island
     * owner is removed and membership cleared.
     * 
     * @param world world
     * @param uuid  player's UUID
     * @return list of islands player had or empty if none
     */
    public Set<Island> removePlayer(@NonNull World world, @NonNull UUID uuid) {
        World w = Util.getWorld(world);
        Set<Island> islandSet = islandsByUUID.get(uuid);
        if (w == null || islandSet == null) {
            return Collections.emptySet(); // Return empty list if no islands map exists for the world
        }
        // Go through all the islands associated with this player in this world and
        // remove the player from them.
        Iterator<Island> it = islandSet.iterator();
        while (it.hasNext()) {
            Island island = it.next();
            if (w.equals(island.getWorld())) {
                if (uuid.equals(island.getOwner())) {
                    // Player is the owner, so clear the whole island and clear the ownership
                    island.getMembers().clear();
                    island.setOwner(null);
                } else {
                    island.removeMember(uuid);
                }
                // Remove this island from this set of islands associated to this player
                it.remove();
            }
        }
        return islandSet;
    }

    /**
     * Removes player from island and removes the cache reference
     * 
     * @param island member's island
     * @param uuid   uuid of member to remove
     */
    public void removePlayer(@NonNull Island island, @NonNull UUID uuid) {
        Set<Island> islandSet = islandsByUUID.get(uuid);
        if (islandSet != null) {
            islandSet.remove(island);
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
        return islandsByLocation.size();
    }

    /**
     * Gets the number of islands in the cache for this world
     * 
     * @param world world to get the number of islands in
     * @return the number of islands
     */
    public long size(World world) {
        return this.islandsByLocation.keySet().stream().map(Location::getWorld).filter(world::equals).count();
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
            islandsByUUID.computeIfAbsent(newOwnerUUID, k -> new HashSet<>()).add(island);
        }
        island.setRank(newOwnerUUID, RanksManager.OWNER_RANK);
        islandsByLocation.put(island.getCenter(), island);
        islandsById.put(island.getUniqueId(), island);
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
        return islandsById.get(uniqueId);
    }

    /**
     * Resets all islands in this game mode to default flag settings
     * 
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
     * 
     * @param world - world
     * @param flag  - flag to reset
     * @since 1.8.0
     */
    public void resetFlag(World world, Flag flag) {
        World w = Util.getWorld(world);
        if (w == null) {
            return;
        }
        int setting = BentoBox.getInstance().getIWM().getDefaultIslandFlags(w).getOrDefault(flag,
                flag.getDefaultRank());
        islandsById.values().stream().filter(i -> i.getWorld().equals(w)).forEach(i -> i.setFlag(flag, setting));
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

}
