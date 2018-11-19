package world.bentobox.bentobox.api.worlds;

import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.SchemsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Represents a set of three Worlds (Overworld, Nether, End) which is registered by a GameMode addon and managed by BentoBox.
 * It features its own implementation of WorldSettings and provides Islands and Schems managers.
 *
 * @author Poslovitch
 */
public class GameWorld {

    private final String name;
    private World overWorld;
    private World netherWorld;
    private World endWorld;
    private WorldSettings settings;

    private IslandsManager islandsManager;
    private SchemsManager schemsManager;

    public GameWorld(String name, WorldSettings settings) {
        this.name = name;
        this.settings = settings;
    }

    public boolean createWorlds() {
        return true; //TODO
    }

    public boolean inWorld(Location location) {
        return Util.sameWorld(location.getWorld(), overWorld);
    }

    // Getters
    public String getName() {
        return name;
    }

    public World getOverWorld() {
        return overWorld;
    }

    public World getNetherWorld() {
        return netherWorld;
    }

    public World getEndWorld() {
        return endWorld;
    }

    public WorldSettings getSettings() {
        return settings;
    }

    public IslandsManager getIslands() {
        return islandsManager;
    }

    public SchemsManager getSchems() {
        return schemsManager;
    }
}
