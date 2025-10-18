package world.bentobox.bentobox.api.addons;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.github.puregero.multilib.MultiLib;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.util.Util;

/**
 * An abstract class that defines an addon as a game mode.
 * <p>
 * A game mode is a specialized addon that manages its own set of worlds (overworld, Nether, End),
 * provides world-specific settings, and may include custom world generation. It serves as the
 * foundation for island-based game types like Skyblock or AcidIsland.
 * <p>
 * To create a game mode, you must extend this class and implement its abstract methods.
 *
 * @author tastybento, Poslovitch
 * @since 1.0
 */
public abstract class GameModeAddon extends Addon {

    /**
     * The main island world for this game mode. This world is typically the overworld.
     * It must be initialized in {@link #createWorlds()}.
     */
    protected World islandWorld;
    /**
     * The Nether world associated with this game mode. Can be null if not used.
     */
    @Nullable
    protected World netherWorld;
    /**
     * The End world associated with this game mode. Can be null if not used.
     */
    @Nullable
    protected World endWorld;
    /**
     * The main player command for this game mode. Other addons can add sub-commands to this.
     * @since 1.1
     */
    @Nullable
    protected CompositeCommand playerCommand;
    /**
     * The main admin command for this game mode. Other addons can add sub-commands to this.
     * @since 1.1
     */
    @Nullable
    protected CompositeCommand adminCommand;

    /**
     * Creates the worlds required for this game mode.
     * <p>
     * This method is called by BentoBox after {@link #onLoad()} and before {@link #onEnable()}.
     * Implementations must create and assign the {@link #islandWorld}. The {@link #netherWorld}
     * and {@link #endWorld} are optional.
     * <p>
     * Note: Do not register flags in this method. Flags must be registered in {@link #onEnable()}.
     */
    public abstract void createWorlds();

    /**
     * Gets the world-specific settings for this game mode.
     * @return The {@link WorldSettings} for this game mode.
     */
    public abstract WorldSettings getWorldSettings();

    /**
     * Checks if location is governed by this game mode
     * @param loc The location to check.
     * @return {@code true} if the location is within this game mode's primary world.
     */
    public boolean inWorld(Location loc) {
        return Util.sameWorld(loc.getWorld(), islandWorld);
    }

    /**
     * Checks if a world is the primary world governed by this game mode.
     * @param world The world to check.
     * @return {@code true} if the world is this game mode's primary world.
     * @since 1.2.0
     */
    public boolean inWorld(World world) {
        if (world == null) {
            return false;
        }
        return Util.sameWorld(world, islandWorld);
    }

    /**
     * Gets the primary overworld for this game mode.
     * @return The main {@link World} for this game mode.
     */
    public World getOverWorld() {
        return islandWorld;
    }

    /**
     * Gets the Nether world for this game mode, if it exists.
     * @return The Nether {@link World}, or {@code null} if it does not exist.
     */
    @Nullable
    public World getNetherWorld() {
        return netherWorld;
    }

    /**
     * Gets the End world for this game mode, if it exists.
     * @return The End {@link World}, or {@code null} if it does not exist.
     */
    @Nullable
    public World getEndWorld() {
        return endWorld;
    }

    /**
     * Gets the main player command for this game mode.
     * @return An {@link Optional} containing the main player {@link CompositeCommand}, or an empty optional if none is set.
     * @since 1.1
     */
    @NonNull
    public Optional<CompositeCommand> getPlayerCommand() {
        return Optional.ofNullable(playerCommand);
    }

    /**
     * Gets the main admin command for this game mode.
     * @return An {@link Optional} containing the main admin {@link CompositeCommand}, or an empty optional if none is set.
     * @since 1.1
     */
    @NonNull
    public Optional<CompositeCommand> getAdminCommand() {
        return Optional.ofNullable(adminCommand);
    }

    /**
     * Provides a custom chunk generator for this game mode's worlds.
     * <p>
     * This method is called by BentoBox during world creation. It can be used to provide a
     * custom generator for island worlds, void worlds, etc.
     *
     * @param worldName The name of the world being created.
     * @param id An optional identifier, e.g., for island deletion.
     * @return A {@link ChunkGenerator} for the world, or {@code null} to use the server's default.
     * @since 1.2.0
     */
    @Nullable
    public abstract ChunkGenerator getDefaultWorldGenerator(String worldName, String id);

    /**
     * Saves the game mode's world settings to its configuration file.
      * <p>
      * This method should be called when world settings are changed in-game and need to be persisted.
      * It also notifies other servers in a network environment about the configuration update.
      * </p>
     * @since 1.4.0
     */
    public void saveWorldSettings() {
        // Inform other servers in the network about the configuration change.
        MultiLib.notify("bentobox-config-update", "");
    }
    
    /**
     * Indicates whether this game mode uses the modern {@link ChunkGenerator} API introduced in Minecraft 1.16.
     * <p>
     * The modern API uses {@code ChunkGenerator#generateNoise}, {@code ChunkGenerator#generateSurface}, etc.,
     * while the legacy approach uses the deprecated {@link ChunkGenerator#generateChunkData}.
     *
     * @return {@code true} if the game mode uses the new chunk generation API, {@code false} otherwise.
     */
    public boolean isUsesNewChunkGeneration() {
        return false;
    }
    
    /**
     * Indicates whether BentoBox should try to align island centers on a grid, or leave them free form.
     * Free form is used with some claim-based or player-selected island location addons.
     * @return true by default
     * @since 3.8.2
     */
    public boolean isFixIslandCenter() {
        return true;
    }

}
