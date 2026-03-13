package world.bentobox.bentobox.managers.island;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.objects.Island;

/**
 * Handles the island location grid for each world using a cell-based spatial
 * hash for O(1) average-case point lookups and O(n) bulk loading.
 *
 * @author tastybento
 */
public class IslandGrid {

    /**
     * Island id, minX, minZ, and range
     */
    public record IslandData(String id, int minX, int minZ, int range) {}

    /** Side-length of each spatial-hash cell in blocks. */
    private static final int CELL_SIZE = 256;

    /** Cell coordinate → set of island IDs whose bounding boxes overlap that cell. */
    private final Map<Long, Set<String>> cellMap = new HashMap<>();

    /** Island ID → its metadata (bounds, range). */
    private final Map<String, IslandData> islandById = new HashMap<>();

    private final IslandCache im;

    /**
     * @param im IslandCache
     */
    public IslandGrid(IslandCache im) {
        super();
        this.im = im;
    }

    /**
     * Adds island to grid
     * @param island - island to add
     * @return true if successfully added, false if there is an overlap with a different island
     */
    public boolean addToGrid(Island island) {
        int minX = island.getMinX();
        int minZ = island.getMinZ();
        int range = island.getRange();
        IslandData newIsland = new IslandData(island.getUniqueId(), minX, minZ, range);

        // If this island is already in the grid, remove it first (handles moves/resizes)
        IslandData existing = islandById.get(newIsland.id());
        if (existing != null) {
            removeCells(existing);
            islandById.remove(existing.id());
        }

        // Compute bounding box (upper bounds exclusive)
        int newMaxX = minX + range * 2;
        int newMaxZ = minZ + range * 2;

        // Check for overlaps only against islands in the cells this island would occupy
        List<Long> coveredCells = getCoveredCells(minX, minZ, newMaxX, newMaxZ);
        for (long cellKey : coveredCells) {
            Set<String> ids = cellMap.get(cellKey);
            if (ids != null) {
                for (String candidateId : ids) {
                    IslandData candidate = islandById.get(candidateId);
                    if (candidate != null && isOverlapping(newIsland, candidate)) {
                        return false;
                    }
                }
            }
        }

        // No overlaps — register the island
        islandById.put(newIsland.id(), newIsland);
        for (long cellKey : coveredCells) {
            cellMap.computeIfAbsent(cellKey, k -> new HashSet<>()).add(newIsland.id());
        }
        return true;
    }

    /**
     * Checks if two islands overlap. Touching edges are allowed.
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
        return island1MaxZ > island2.minZ() && island2MaxZ > island1.minZ();
    }

    /**
     * Remove island from grid
     * @param island - the island to remove
     * @return true if island existed and was deleted, false if there was nothing to delete
     */
    public boolean removeFromGrid(Island island) {
        IslandData data = islandById.remove(island.getUniqueId());
        if (data == null) {
            return false;
        }
        removeCells(data);
        return true;
    }

    /**
     * Removes an island's ID from all cells it occupies.
     */
    private void removeCells(IslandData data) {
        int maxX = data.minX() + data.range() * 2;
        int maxZ = data.minZ() + data.range() * 2;
        for (long cellKey : getCoveredCells(data.minX(), data.minZ(), maxX, maxZ)) {
            Set<String> ids = cellMap.get(cellKey);
            if (ids != null) {
                ids.remove(data.id());
                if (ids.isEmpty()) {
                    cellMap.remove(cellKey);
                }
            }
        }
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
        long cellKey = packCellKey(Math.floorDiv(x, CELL_SIZE), Math.floorDiv(z, CELL_SIZE));
        Set<String> ids = cellMap.get(cellKey);
        if (ids == null) {
            return null;
        }
        for (String id : ids) {
            IslandData data = islandById.get(id);
            if (data != null
                    && x >= data.minX() && x < data.minX() + data.range() * 2
                    && z >= data.minZ() && z < data.minZ() + data.range() * 2) {
                return data.id();
            }
        }
        return null;
    }

    /**
     * @return number of islands stored in the grid
     */
    public long getSize() {
        return islandById.size();
    }

    /**
     * Returns an unmodifiable view of all islands in the grid.
     * @return all island data entries
     */
    public Collection<IslandData> getAllIslands() {
        return Collections.unmodifiableCollection(islandById.values());
    }

    /**
     * Returns all islands whose bounding boxes overlap the given rectangle.
     * Useful for region-based queries (e.g., purge commands).
     *
     * @param minX minimum X of the query rectangle (inclusive)
     * @param minZ minimum Z of the query rectangle (inclusive)
     * @param maxX maximum X of the query rectangle (inclusive)
     * @param maxZ maximum Z of the query rectangle (inclusive)
     * @return collection of island data entries overlapping the rectangle
     */
    public Collection<IslandData> getIslandsInBounds(int minX, int minZ, int maxX, int maxZ) {
        // Use maxX+1 and maxZ+1 for cell coverage since maxX/maxZ are inclusive
        List<Long> cells = getCoveredCells(minX, minZ, maxX + 1, maxZ + 1);
        Set<String> seen = new HashSet<>();
        List<IslandData> result = new ArrayList<>();
        for (long cellKey : cells) {
            collectOverlappingIslands(cellKey, minX, minZ, maxX, maxZ, seen, result);
        }
        return result;
    }

    /**
     * Collects islands from a single cell that overlap the query rectangle.
     */
    private void collectOverlappingIslands(long cellKey, int minX, int minZ, int maxX, int maxZ,
            Set<String> seen, List<IslandData> result) {
        Set<String> ids = cellMap.get(cellKey);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (!seen.add(id)) {
                continue;
            }
            IslandData data = islandById.get(id);
            if (data != null && overlapsRect(data, minX, minZ, maxX, maxZ)) {
                result.add(data);
            }
        }
    }

    /**
     * Tests whether an island's bounding box overlaps the given inclusive rectangle.
     */
    private static boolean overlapsRect(IslandData data, int minX, int minZ, int maxX, int maxZ) {
        int islandMaxX = data.minX() + 2 * data.range();
        int islandMaxZ = data.minZ() + 2 * data.range();
        return islandMaxX > minX && data.minX() <= maxX
                && islandMaxZ > minZ && data.minZ() <= maxZ;
    }

    // ---- Internal helpers ----

    /**
     * Packs two cell coordinates into a single long key.
     */
    private static long packCellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    /**
     * Returns all cell keys that a bounding box [minX, maxX) x [minZ, maxZ) overlaps.
     */
    private static List<Long> getCoveredCells(int minX, int minZ, int maxX, int maxZ) {
        int cellMinX = Math.floorDiv(minX, CELL_SIZE);
        int cellMinZ = Math.floorDiv(minZ, CELL_SIZE);
        // maxX/maxZ are exclusive, so subtract 1 before dividing to get the last covered cell
        int cellMaxX = Math.floorDiv(maxX - 1, CELL_SIZE);
        int cellMaxZ = Math.floorDiv(maxZ - 1, CELL_SIZE);
        List<Long> cells = new ArrayList<>();
        for (int cx = cellMinX; cx <= cellMaxX; cx++) {
            for (int cz = cellMinZ; cz <= cellMaxZ; cz++) {
                cells.add(packCellKey(cx, cz));
            }
        }
        return cells;
    }

}
