package world.bentobox.bentobox.api.addons;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.blueprints.BundleAvailability;
import world.bentobox.bentobox.managers.island.NewIslandLocationStrategy;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
     * Entity types that may always spawn in this game mode by default, regardless of the animal and
     * monster spawning flags. Used to seed the BentoBox-stored world settings the first time this
     * game mode is seen; after that the admin's edits in the settings panel apply.
     * Override to change the seed, e.g. return an empty list for a game mode with no villages.
     * @return list of {@link org.bukkit.entity.EntityType} names
     * @since 3.23.6
     */
    public List<String> getDefaultAllowedMobs() {
        return List.of("IRON_GOLEM");
    }

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
     * No longer consulted by BentoBox: island chunks are reaped at the
     * filesystem level by {@code PurgeRegionsService} regardless of which
     * generator API the gamemode uses. Will be removed in a future release.
     *
     * @return {@code true} if the game mode uses the new chunk generation API, {@code false} otherwise.
     * @deprecated since 3.16.1, for removal — no consumer remains.
     */
    @Deprecated(since = "3.16.1", forRemoval = true)
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

    /**
     * Indicates whether BentoBox should fail to run if island ranges in a Game Mode are different.
     * For some Game Modes, this is not an issue. Also disables console errors if islands cannot be made.
     * @return default is true
     * @since 3.8.2
     */
    public boolean isEnforceEqualRanges() {
        return true;
    }

    /**
     * Returns the number of islands to pre-generate chunks for in this game mode.
     * <p>
     * Override this method to customize pre-generation for your addon:
     * <ul>
     *   <li>{@code -1} — use the global BentoBox setting (default)</li>
     *   <li>{@code 0} — disable pre-generation for this addon</li>
     *   <li>positive value — pre-generate this many islands ahead</li>
     * </ul>
     *
     * @return number of islands ahead, -1 for global default, 0 to disable
     * @since 3.15.0
     */
    public int getPregenIslandsAhead() {
        return -1;
    }

    /**
     * Lets the game mode decide where a new island is placed. It is called whenever an island
     * is created or reset without an explicit location strategy. The blueprint bundle the
     * player picked is given so that the location can depend on it, for example to keep
     * different kinds of island in different parts of the world.
     *
     * <p>
     * The game mode can also refuse the island here, for example because the player has not
     * unlocked that kind of island yet, by throwing an exception. Nothing has been created at
     * that point, so there is nothing to undo.
     *
     * @param user the player the island is for
     * @param blueprintBundle the unique id of the chosen blueprint bundle, may be null
     * @param oldIsland the island that the new one replaces when an island is being reset,
     *                  otherwise null
     * @return the strategy to use, or null to use the default
     *         {@link world.bentobox.bentobox.managers.island.DefaultNewIslandLocationStrategy}
     * @throws IOException if the island must not be made. The message is a locale reference
     *                     that is shown to the player.
     * @since 3.25.0
     */
    @Nullable
    public NewIslandLocationStrategy getNewIslandLocationStrategy(@NonNull User user,
            @Nullable String blueprintBundle, @Nullable Island oldIsland) throws IOException {
        return null;
    }

    /**
     * Says whether a player can use a blueprint bundle right now. Game modes can lock bundles,
     * for example until the player has unlocked them in the game, or hide them completely.
     * A locked bundle is shown in the selection panel with the reason but cannot be picked,
     * and a hidden one is not shown. Either way the bundle is refused if the player asks for
     * it by name. This is checked before anything is done to the player's current island.
     * Permissions, costs and usage limits of bundles are checked separately.
     *
     * @param user the player
     * @param bundle the blueprint bundle
     * @param oldIsland the island that would be replaced if this is a reset, otherwise null
     * @return whether the bundle can be used. The default is that it can.
     * @since 3.25.0
     */
    @NonNull
    public BundleAvailability getBundleAvailability(@NonNull User user, @NonNull BlueprintBundle bundle,
            @Nullable Island oldIsland) {
        return BundleAvailability.available();
    }

    /**
     * Gets the text that stands for one of a player's islands when it has no name, in lists
     * the player picks from such as the homes panel and the go command. Game modes where a
     * player's islands differ in a way the player knows, for example by the kind of land they
     * are on, can name them by that instead of numbering them.
     *
     * @param user the player who will see the label
     * @param island the island, which has no name
     * @return the label, or null to use the default of the owner's name and a number
     * @since 3.25.0
     */
    @Nullable
    public String getIslandLabel(@NonNull User user, @NonNull Island island) {
        return null;
    }

    /**
     * Gets the settings this game mode asks another addon to use in this game mode's worlds.
     * A game mode that only plays properly when a helper addon is set up a certain way can
     * ask for that here, so the admin does not have to edit the other addon's config.
     * <p>
     * The keys are paths in the other addon's config file, for example
     * {@code options.islands.advancements}, and the values are booleans, numbers, strings or
     * enum names. It is up to the other addon whether it supports requests and which paths it
     * accepts, and the admin can turn requests off in that addon's config. A request never
     * changes the other addon's config file or its behaviour in other game modes.
     *
     * @param addonName name of the addon being asked, as in its addon.yml
     * @return config path to requested value. The default is no requests.
     * @since 3.25.0
     * @see world.bentobox.bentobox.api.configuration.RequestedSettings
     */
    @NonNull
    public Map<String, Object> getRequestedSettings(@NonNull String addonName) {
        return Map.of();
    }

}
