package world.bentobox.bentobox.api.addons;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.util.Util;

/**
 * Defines the addon as a game mode.
 * A game mode creates worlds, registers world settings and has schems in a jar folder.
 * @author tastybento, Postlovitch
 *
 */
public abstract class GameModeAddon extends Addon {

    protected World islandWorld;
    protected World netherWorld;
    protected World endWorld;
    /**
     * Main player command. Addons can use this hook to into this command.
     */
    protected CompositeCommand playerCommand;
    /**
     * Main admin command. Addons can use this hook to into this command.
     */
    protected CompositeCommand adminCommand;

    /**
     * Make the worlds for this GameMode in this method. BentoBox will call it
     * after onLoad() and before onEnable().
     * {@link #islandWorld} must be created and assigned,
     * {@link #netherWorld} and {@link #endWorld} are optional and may be null.
     */
    public abstract void createWorlds();

    /**
     * @return WorldSettings for this GameMode
     */
    public abstract WorldSettings getWorldSettings();

    /**
     * Checks if a player is in any of the island worlds
     * @param loc - player to check
     * @return true if in a world or false if not
     */
    public boolean inWorld(Location loc) {
        return Util.sameWorld(loc.getWorld(), islandWorld);
    }

    /**
     * @return over world
     */
    public World getOverWorld() {
        return islandWorld;
    }

    /**
     * @return nether world, or null if it does not exist
     */
    public World getNetherWorld() {
        return netherWorld;
    }

    /**
     * @return end world, or null if it does not exist
     */
    public World getEndWorld() {
        return endWorld;
    }

    /**
     * @return the main player command for this Game Mode Addon
     */
    public Optional<CompositeCommand> getPlayerCommand() {
        return Optional.ofNullable(playerCommand);
    }

    /**
     * @return the main admin command for this Game Mode Addon
     */
    public Optional<CompositeCommand> getAdminCommand() {
        return Optional.ofNullable(adminCommand);
    }
}
