package us.tastybento.bskyblock.managers.island;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandCache {
    private BSkyBlock plugin;
    private BiMap<Location, Island> islandsByLocation;
    /**
     * Every player who is associated with an island is in this map.
     */
    private HashMap<UUID, Island> islandsByUUID;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<>();

    public IslandCache(BSkyBlock plugin) {
        this.plugin = plugin;
        islandsByLocation = HashBiMap.create();
        islandsByUUID = new HashMap<>();
    }

    /**
     * Adds an island to the grid
     * @param island
     * @return true if successfully added, false if not
     */
    public boolean addIsland(Island island) {
        islandsByLocation.put(island.getCenter(), island);
        islandsByUUID.put(island.getOwner(), island);
        for (UUID member: island.getMemberSet()) {
            islandsByUUID.put(member, island);
        }
        return addToGrid(island);
    }

    /**
     * Adds a player's UUID to the look up for islands. Does no checking
     * @param playerUUID
     * @param teamIsland
     */
    public void addPlayer(UUID playerUUID, Island teamIsland) {
        islandsByUUID.put(playerUUID, teamIsland);
    }

    /**
     * Adds an island to the grid register
     * @param newIsland
     * @return true if successfully added, false if not
     */
    private boolean addToGrid(Island newIsland) {
        if (islandGrid.containsKey(newIsland.getMinX())) {
            TreeMap<Integer, Island> zEntry = islandGrid.get(newIsland.getMinX());
            if (zEntry.containsKey(newIsland.getMinZ())) {
                // Island already exists
                Island conflict = islandGrid.get(newIsland.getMinX()).get(newIsland.getMinZ());
                plugin.logWarning("*** Duplicate or overlapping islands! ***");
                plugin.logWarning(
                        "Island at (" + newIsland.getCenter().getBlockX() + ", " + newIsland.getCenter().getBlockZ() + ") conflicts with ("
                                + conflict.getCenter().getBlockX() + ", " + conflict.getCenter().getBlockZ() + ")");
                if (conflict.getOwner() != null) {
                    plugin.logWarning("Accepted island is owned by " + plugin.getPlayers().getName(conflict.getOwner()));
                    plugin.logWarning(conflict.getOwner().toString() + ".yml");
                } else {
                    plugin.logWarning("Accepted island is unowned.");
                }
                if (newIsland.getOwner() != null) {
                    plugin.logWarning("Denied island is owned by " + plugin.getPlayers().getName(newIsland.getOwner()));
                    plugin.logWarning(newIsland.getOwner().toString() + ".yml");
                } else {
                    plugin.logWarning("Denied island is unowned and is a database duplicate. Skipping it...");
                }
                plugin.logWarning("Recommend that the denied player file is deleted otherwise weird things can happen.");
                return false;
            } else {
                // Add island
                zEntry.put(newIsland.getMinZ(), newIsland);
                islandGrid.put(newIsland.getMinX(), zEntry);
            }
        } else {
            // Add island
            TreeMap<Integer, Island> zEntry = new TreeMap<>();
            zEntry.put(newIsland.getMinZ(), newIsland);
            islandGrid.put(newIsland.getMinX(), zEntry);
        }
        return true;
    }

    public void clear() {
        islandsByLocation.clear();
        islandsByUUID.clear();
    }

    /**
     * Deletes an island from the database. Does not remove blocks
     * @param island
     * @return true if successful, false if not
     */
    public boolean deleteIslandFromCache(Island island) {
        if (!islandsByLocation.remove(island.getCenter(), island)) {
            return false;
        }
        Iterator<Entry<UUID, Island>> it = islandsByUUID.entrySet().iterator();
        while (it.hasNext()) {
            Entry<UUID, Island> en = it.next();
            if (en.getValue().equals(island)) {
                it.remove();
            }
        }
        // Remove from grid
        if (island != null) {
            int x = island.getMinX();
            int z = island.getMinZ();
            if (islandGrid.containsKey(x)) {
                TreeMap<Integer, Island> zEntry = islandGrid.get(x);
                if (zEntry.containsKey(z)) {
                    // Island exists - delete it
                    zEntry.remove(z);
                    islandGrid.put(x, zEntry);
                }
            }
        }
        return true;
    }

    /**
     * Get island based on the exact center location of the island
     * @param location
     * @return island or null if it does not exist
     */
    public Island get(Location location) {
        return islandsByLocation.get(location);
    }

    /**
     * Returns island referenced by UUID
     * @param uuid - uuid of player
     * @return island or null if none
     */
    public Island get(UUID uuid) {
        return islandsByUUID.get(uuid);
    }

    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x - x coordinate
     * @param z - z coordinate
     * @return Island or null
     */
    public Island getIslandAt(int x, int z) {
        Entry<Integer, TreeMap<Integer, Island>> en = islandGrid.floorEntry(x);
        if (en != null) {
            Entry<Integer, Island> ent = en.getValue().floorEntry(z);
            if (ent != null) {
                // Check if in the island range
                Island island = ent.getValue();
                if (island.inIslandSpace(x, z)) {
                    return island;
                }
            }
        }
        return null;
    }

    /**
     * Returns the island at the location or null if there is none.
     * This includes the full island space, not just the protected area
     *
     * @param location - the location
     * @return Island object
     */
    public Island getIslandAt(Location location) {
        if (location == null) {
            return null;
        }
        // World check
        if (!Util.inWorld(location)) {
            return null;
        }
        return getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Get name of the island owned by owner
     * @param owner - the island owner
     * @return Returns the name of owner's island, or the owner's name if there is none.
     */
    public String getIslandName(UUID owner) {
        String result = plugin.getPlayers().getName(owner);
        if (islandsByUUID.containsKey(owner)) {
            Island island = islandsByUUID.get(owner);
            if (island.getName() != null && !island.getName().isEmpty()) {
                result = island.getName();
            }
        }
        return ChatColor.translateAlternateColorCodes('&', result) + ChatColor.RESET;
    }

    public Collection<Island> getIslands() {
        return Collections.unmodifiableCollection(islandsByLocation.values());
    }

    public Set<UUID> getMembers(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null) {
            return island.getMemberSet();
        }
        return new HashSet<>(0);
    }

    public UUID getTeamLeader(UUID playerUUID) {
        if (islandsByUUID.containsKey(playerUUID)) {
            return islandsByUUID.get(playerUUID).getOwner();
        }
        return null;
    }

    /**
     * @param playerUUID - the player's UUID
     * @return true if player has island and owns it
     */
    public boolean hasIsland(UUID playerUUID) {
        return (islandsByUUID.containsKey(playerUUID) && islandsByUUID.get(playerUUID).getOwner() != null 
                && (islandsByUUID.get(playerUUID).getOwner().equals(playerUUID))) ? true : false;
    }

    /**
     * Removes a player from the cache. If the player has an island, the island owner is removed and membership cleared.
     * The island is removed from the islandsByUUID map, but kept in the location map.
     * @param playerUUID - player's UUID
     */
    public void removePlayer(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null) {
            if (island.getOwner() != null && island.getOwner().equals(playerUUID)) {
                // Clear ownership and members
                island.getMembers().clear();
                island.setOwner(null);
            } else {
                // Remove player from the island membership
                island.removeMember(playerUUID);
            }
        }
        islandsByUUID.remove(playerUUID);
    }

    /**
     * Sets the island name
     * @param owner - owner of island
     * @param name - new island name
     * @return true if successfull, false if no island for owner
     */
    public boolean setIslandName(UUID owner, String name) {
        if (islandsByUUID.containsKey(owner)) {
            Island island = islandsByUUID.get(owner);
            island.setName(name);
            return true;
        }
        return false;
    }

    /**
     * Get the number of islands in the cache
     * @return the number of islands 
     */
    public int size() {
        return islandsByLocation.size();
    }
    
}
