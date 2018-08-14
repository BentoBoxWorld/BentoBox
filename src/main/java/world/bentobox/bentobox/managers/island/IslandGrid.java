package world.bentobox.bentobox.managers.island;

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
                .filter(cell -> (cell.getValue().getState().equals(CellState.OCCUPIED)))
                .filter(cell -> ((Island) cell.getValue().getObject()).inIslandSpace(x, z))
                .findFirst().orElse(null);

        if (tableCell != null) {
            return (Island) tableCell.getValue().getObject();
        }
        return null;
    }

    /**
     * @author Poslovitch
     */
    private class Cell {
        private CellState state;
        private Object object;

        private Cell(CellState state, Object object) {
            this.state = state;
            this.object = object;
        }

        private CellState getState() {
            return state;
        }

        private Object getObject() {
            return object;
        }
    }

    /**
     * @author Poslovitch
     */
    private enum CellState {
        //RESERVED,
        OCCUPIED
    }
}
