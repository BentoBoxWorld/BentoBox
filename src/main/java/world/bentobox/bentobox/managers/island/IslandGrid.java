package world.bentobox.bentobox.managers.island;

import java.util.Map.Entry;
import java.util.TreeMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Handles the island location grid for each world
 * @author tastybento
 *
 */
class IslandGrid {
    private final TreeMap<Integer, TreeMap<Integer, String>> grid = new TreeMap<>();
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
        if (grid.containsKey(island.getMinX())) {
            TreeMap<Integer, String> zEntry = grid.get(island.getMinX());
            if (zEntry.containsKey(island.getMinZ())) {
                if (island.getUniqueId().equals(zEntry.get(island.getMinZ()))) {
                    return true;
                }
                return false;
            } else {
                // Add island
                zEntry.put(island.getMinZ(), island.getUniqueId());
                grid.put(island.getMinX(), zEntry);
            }
        } else {
            // Add island
            TreeMap<Integer, String> zEntry = new TreeMap<>();
            zEntry.put(island.getMinZ(), island.getUniqueId());
            grid.put(island.getMinX(), zEntry);
        }
        return true;
    }

    /**
     * Remove island from grid
     * @param island - the island to remove
     * @return true if island existed and was deleted, false if there was nothing to delete
     */
    public boolean removeFromGrid(Island island) {
        // Remove from grid
        if (island != null) {
            int x = island.getMinX();
            int z = island.getMinZ();
            if (grid.containsKey(x)) {
                TreeMap<Integer, String> zEntry = grid.get(x);
                if (zEntry.containsKey(z)) {
                    // Island exists - delete it
                    zEntry.remove(z);
                    grid.put(x, zEntry);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the island located at the specified x and z coordinates, covering both the protected area
     * and the full island space. Returns null if no island exists at the given location.
     *
     * @param x the x coordinate of the location
     * @param z the z coordinate of the location
     * @return the Island at the specified location, or null if no island is found
     */
    public Island getIslandAt(int x, int z) {
        // Attempt to find the closest x-coordinate entry that does not exceed 'x'
        Entry<Integer, TreeMap<Integer, String>> xEntry = grid.floorEntry(x);
        if (xEntry == null) {
            return null; // No x-coordinate entry found, return null
        }

        // Attempt to find the closest z-coordinate entry that does not exceed 'z' within the found x-coordinate
        Entry<Integer, String> zEntry = xEntry.getValue().floorEntry(z);
        if (zEntry == null) {
            return null; // No z-coordinate entry found, return null
        }

        // Retrieve the island using the id found in the z-coordinate entry
        Island island = im.getIslandById(zEntry.getValue());
        if (island == null) {
            return null; // No island found by the id, return null
        }
        // Check if the specified coordinates are within the island space
        if (island.inIslandSpace(x, z)) {
            return island; // Coordinates are within island space, return the island
        }

        // Coordinates are outside the island space, return null
        return null;
    }

}
