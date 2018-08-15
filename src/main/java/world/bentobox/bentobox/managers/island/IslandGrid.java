package world.bentobox.bentobox.managers.island;

import org.apache.commons.lang.Validate;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import world.bentobox.bentobox.database.objects.Island;

/**
 * Handles the island location grid for each world.
 * @author tastybento
 */
public class IslandGrid {

    /**
     * Row : x location
     * Column : z location
     * Value : Island
     */
    private Table<Integer, Integer, Cell> grid = TreeBasedTable.create();

    /**
     * Adds island to grid
     * @param island - island to add
     * @return true if successfully added, false if island already exists, or there is an overlap
     */
    public boolean addToGrid(Island island) {
        if (grid.contains(island.getMinX(), island.getMinZ())) {
            // It is either occupied or reserved, so return false
            return false;
        }

        // All clear, add the island to the grid.
        grid.put(island.getMinX(), island.getMinZ(), new Cell(CellState.OCCUPIED, island));
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
            if (grid.contains(island.getMinX(), island.getMinZ())) {
                // TODO add support for RESERVED cells
                grid.remove(island.getMinX(), island.getMinZ());
                return true;
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
        Table.Cell<Integer, Integer, Cell> tableCell = grid.cellSet()
                .stream()
                .filter(cell -> cell.getValue().getState().equals(CellState.OCCUPIED))
                .filter(cell -> cell.getValue().getIsland().inIslandSpace(x, z))
                .findFirst().orElse(null);

        if (tableCell != null) {
            return tableCell.getValue().getIsland();
        }
        return null;
    }

    /**
     * @author Poslovitch
     */
    private class Cell {
        private final CellState state;
        private final Island island;

        private Cell(CellState state, Island island) {
            // Make sure the hard way that none of the parameters are null
            Validate.notNull(state, "Cell state cannot be null");
            Validate.notNull(island, "Island cannot be null");

            this.state = state;
            this.island = island;
        }

        private CellState getState() {
            return state;
        }

        private Island getIsland() {
            return island;
        }
    }

    /**
     * @author Poslovitch
     */
    private enum CellState {
        OCCUPIED
    }
}
