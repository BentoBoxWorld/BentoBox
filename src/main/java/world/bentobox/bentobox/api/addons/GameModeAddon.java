package world.bentobox.bentobox.api.addons;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.util.Util;

/**
 * Defines the addon as a game mode.
 * A game mode creates worlds, registers world settings and has schems in a jar folder.
 * @author tastybento, Poslovitch
 */
public abstract class GameModeAddon extends Addon {

    protected World islandWorld;
    @Nullable
    protected World netherWorld;
    @Nullable
    protected World endWorld;
    /**
     * Main player command. Addons can use this hook to into this command.
     * @since 1.1
     */
    @Nullable
    protected CompositeCommand playerCommand;
    /**
     * Main admin command. Addons can use this hook to into this command.
     * @since 1.1
     */
    @Nullable
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
     * Checks if world is governed by this game mode
     * @param world - world to check
     * @return true if in a world or false if not
     * @since 1.2.0
     */
    public boolean inWorld(World world) {
        if (world == null) {
            return false;
        }
        return Util.sameWorld(world, islandWorld);
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
    @Nullable
    public World getNetherWorld() {
        return netherWorld;
    }

    /**
     * @return end world, or null if it does not exist
     */
    @Nullable
    public World getEndWorld() {
        return endWorld;
    }

    /**
     * @return the main player command for this Game Mode Addon
     * @since 1.1
     */
    @NonNull
    public Optional<CompositeCommand> getPlayerCommand() {
        return Optional.ofNullable(playerCommand);
    }

    /**
     * @return the main admin command for this Game Mode Addon
     * @since 1.1
     */
    @NonNull
    public Optional<CompositeCommand> getAdminCommand() {
        return Optional.ofNullable(adminCommand);
    }

    /**
     * Defines the world generator for this game mode
     * @param worldName - name of world that this applies to
     * @param id - id if any
     * @return Chunk generator
     * @since 1.2.0
     */
    @NonNull
    public abstract ChunkGenerator getDefaultWorldGenerator(String worldName, String id);

	/**
	 * Tells the Game Mode Addon to save its settings. Used when world settings are changed
	 * in-game and need to be saved.
	 * @since 1.4.0
	 */
	public abstract void saveWorldSettings();
}
