package world.bentobox.bentobox.api.commands.admin;


import world.bentobox.bentobox.database.objects.Island;

/**
 * Deletes all named homes from an island
 */
public class AdminDeleteHomesManager {

    private final Island island;

    public AdminDeleteHomesManager(Island island) {
        this.island = island;
    }

    /**
     * Deletes all homes from an island
     */
    public void deleteHomes() {
        this.island.removeHomes();
    }
}
