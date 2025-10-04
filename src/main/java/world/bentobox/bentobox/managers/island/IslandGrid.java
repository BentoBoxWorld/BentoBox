package world.bentobox.bentobox.managers.island;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.objects.Island;

/**
 * Handles the island location grid for each world
 * @author tastybento
 *
 */
public class IslandGrid {

    /**
     * Island id, minX, minZ, and range
     */
    public record IslandData(String id, int minX, int minZ, int range) {}

    private final TreeMap<Integer, TreeMap<Integer, IslandData>> grid = new TreeMap<>();
    private final IslandCache im;

    /**
     * @param im IslandsManager
     */
    public IslandGrid(IslandCache im) {
        super();
        this.im = im;
    }

    /**
     * Adds island to grid
     * @param island - island to add
     * @return true if successfully added, false if island already exists, or there is an overlap
     */
    public boolean addToGrid(Island island) {
        // Check if we know about this island already
        int minX = island.getMinX();
        int minZ = island.getMinZ();
        IslandData islandData = new IslandData(island.getUniqueId(), minX, minZ, island.getRange());
        if (grid.containsKey(minX)) {
            TreeMap<Integer, IslandData> zEntry = grid.get(minX);
            if (zEntry.containsKey(minZ)) {
                // If it is the same island then it's okay
                return island.getUniqueId().equals(zEntry.get(minZ).id());
                // Island overlap, report error
            } else {
                // Add island
                zEntry.put(minZ, islandData);
                grid.put(minX, zEntry);
            }
        } else {
            // Add island
            TreeMap<Integer, IslandData> zEntry = new TreeMap<>();
            zEntry.put(minZ, islandData);
            grid.put(minX, zEntry);
        }
        return true;
    }

    /**
     * Remove island from grid
     * @param island - the island to remove
     * @return true if island existed and was deleted, false if there was nothing to delete
     */
    public boolean removeFromGrid(Island island) {
        String id = island.getUniqueId();
        boolean removed = grid.values().stream()
                .anyMatch(innerMap -> innerMap.values().removeIf(innerValue -> innerValue.id().equals(id)));

        grid.values().removeIf(TreeMap::isEmpty);

        return removed;
    }

    /**
     * Retrieves the island located at the specified x and z coordinates, covering both the protected area
     * and the full island space. Returns null if no island exists at the given location.
     * This will load the island from the database if it is not in the cache.
     *
     * @param x the x coordinate of the location
     * @param z the z coordinate of the location
     * @return the Island at the specified location, or null if no island is found
     */
    public Island getIslandAt(int x, int z) {
        String id = getIslandStringAt(x, z);
        if (id == null) {
            return null;
        }

        // Retrieve the island using the id found - loading from database if required
        return im.getIslandById(id);
    }

    /**
     * Checks if an island is at this coordinate or not
     * @param x coord
     * @param z coord
     * @return true if there is an island registered here in the grid
     */
    public boolean isIslandAt(int x, int z) {
        return getIslandStringAt(x, z) != null;
    }

    /**
     * Get the island ID string for an island at these coordinates, or null if none.
     * @param x coord
     * @param z coord
     * @return Unique Island ID string, or null if there is no island here.
     */
    public @Nullable String getIslandStringAt(int x, int z) {
        // Attempt to find the closest x-coordinate entry that does not exceed 'x'
        Entry<Integer, TreeMap<Integer, IslandData>> xEntry = grid.floorEntry(x);
        if (xEntry == null) {
            return null; // No x-coordinate entry found, return null
        }

        // Attempt to find the closest z-coordinate entry that does not exceed 'z' within the found x-coordinate
        Entry<Integer, IslandData> zEntry = xEntry.getValue().floorEntry(z);
        if (zEntry == null) {
            return null; // No z-coordinate entry found, return null
        }
        // Check if the specified coordinates are within the island space
        if (x >= zEntry.getValue().minX() && x < (zEntry.getValue().minX() + zEntry.getValue().range() * 2)
                && z >= zEntry.getValue().minZ() && z < (zEntry.getValue().minZ() + zEntry.getValue().range() * 2)) {
            return zEntry.getValue().id();
        }
        return null;
    }

    /**
     * @return number of islands stored in the grid
     */
    public long getSize() {
        long count = 0;
        for (TreeMap<Integer, IslandData> innerMap : grid.values()) {
            count += innerMap.size();
        }
        return count;
    }

    /**
     * @return the grid
     */
    public TreeMap<Integer, TreeMap<Integer, IslandData>> getGrid() {
        return grid;
    }

}
