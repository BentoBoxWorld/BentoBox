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
    private TreeMap<Integer, TreeMap<Integer, Island>> grid = new TreeMap<>();
    private BentoBox plugin = BentoBox.getInstance();

    /**
     * Adds island to grid
     * @param island - island to add
     * @return true if successfully added, false if island already exists, or there is an overlap
     */
    public boolean addToGrid(Island island) {
        if (grid.containsKey(island.getMinX())) {
            TreeMap<Integer, Island> zEntry = grid.get(island.getMinX());
            if (zEntry.containsKey(island.getMinZ())) {
                // There is an overlap or duplicate
                plugin.logError("Cannot load island. Overlapping: " + island.getUniqueId());
                plugin.logError("Location: " + island.getCenter());
                // Get the previously loaded island
                Island firstLoaded = zEntry.get(island.getMinZ());
                if (firstLoaded.getOwner() == null && island.getOwner() != null) {
                    // This looks fishy. We prefer to load islands that have an owner. Swap the two
                    plugin.logError("Duplicate island has an owner, so using that one. " + island.getOwner());
                    firstLoaded = new Island(island);
                    zEntry.put(island.getMinZ(), firstLoaded);
                } else if (firstLoaded.getOwner() != null && island.getOwner() != null) {
                    // Check if the owners are the same - this is a true duplicate
                    if (firstLoaded.getOwner().equals(island.getOwner())) {
                        // Find out which one is the original
                        if (firstLoaded.getCreatedDate() > island.getCreatedDate()) {
                            plugin.logError("Same owner duplicate. Swaping based on creation date.");
                            // FirstLoaded is the newer
                            firstLoaded = new Island(island);
                            zEntry.put(island.getMinZ(), firstLoaded);
                        } else {
                            plugin.logError("Same owner duplicate.");
                        }
                    } else {
                        plugin.logError("Duplicate but different owner. Keeping first loaded.");
                        plugin.logError("This is serious!");
                        plugin.logError("1st loaded ID: " + firstLoaded.getUniqueId());
                        plugin.logError("1st loaded owner: " + firstLoaded.getOwner());
                        plugin.logError("2nd loaded ID: " + island.getUniqueId());
                        plugin.logError("2nd loaded owner: " + island.getOwner());
                    }
                }
                return false;
            } else {
                // Add island
                zEntry.put(island.getMinZ(), island);
                grid.put(island.getMinX(), zEntry);
            }
        } else {
            // Add island
            TreeMap<Integer, Island> zEntry = new TreeMap<>();
            zEntry.put(island.getMinZ(), island);
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
                TreeMap<Integer, Island> zEntry = grid.get(x);
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
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x - x coordinate
     * @param z - z coordinate
     * @return Island or null
     */
    public Island getIslandAt(int x, int z) {
        Entry<Integer, TreeMap<Integer, Island>> en = grid.floorEntry(x);
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
}
