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
        int minX = island.getMinX();
        int minZ = island.getMinZ();
        int range = island.getRange();
        IslandData newIsland = new IslandData(island.getUniqueId(), minX, minZ, range);

        // Remove this island if it is already in the grid
        this.removeFromGrid(island);

        // compute bounds for the new island (upper bounds are exclusive)
        int newMaxX = minX + range * 2;
        int newMaxZ = minZ + range * 2;

        /*
         * Find any existing islands that could overlap:
         * - Any existing island with minX <= newMaxX could extend over newMinX, so we must consider
         *   all entries with key <= newMaxX (use headMap).
         * - For each candidate X entry, consider Z entries with minZ <= newMaxZ (use headMap).
         * This avoids missing large islands whose minX is far left of the new island.
         */
        for (Entry<Integer, TreeMap<Integer, IslandData>> xEntry : grid.headMap(newMaxX, true).entrySet()) {
            TreeMap<Integer, IslandData> zMap = xEntry.getValue();
            for (Entry<Integer, IslandData> zEntry : zMap.headMap(newMaxZ, true).entrySet()) {
                IslandData existingIsland = zEntry.getValue();
                if (isOverlapping(newIsland, existingIsland)) {
                    return false;
                }
            }
        }

        // No overlaps found, add the island
        addNewEntry(minX, minZ, newIsland);
        return true;
    }

    /**
     * Checks if two islands overlap
     * @param island1 first island
     * @param island2 second island
     * @return true if islands overlap
     */
    private boolean isOverlapping(IslandData island1, IslandData island2) {
        int island1MaxX = island1.minX() + (island1.range() * 2);
        int island1MaxZ = island1.minZ() + (island1.range() * 2);
        int island2MaxX = island2.minX() + (island2.range() * 2);
        int island2MaxZ = island2.minZ() + (island2.range() * 2);

        // Check if one rectangle is to the left of the other
        if (island1MaxX <= island2.minX() || island2MaxX <= island1.minX()) {
            return false;
        }

        // Check if one rectangle is above the other
        if (island1MaxZ <= island2.minZ() || island2MaxZ <= island1.minZ()) {
            return false;
        }

        return true;
    }

    /**
     * Helper method to add a new entry to the grid
     */
    private void addNewEntry(int minX, int minZ, IslandData islandData) {
        TreeMap<Integer, IslandData> zEntry = grid.computeIfAbsent(minX, k -> new TreeMap<>());
        zEntry.put(minZ, islandData);
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
