package world.bentobox.bentobox.api.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;

/**
 * Contains world-specific settings that must be provided by the {@link world.bentobox.bentobox.api.addons.GameModeAddon} in order to register its Worlds.
 * <br/>
 * Depending on your implementation, you may need to add setters.
 * @author tastybento
 */
public interface WorldSettings extends ConfigObject {

    /**
     * Get the default game mode for this game world, e.g. SURVIVAL
     * @return game mode
     */
    GameMode getDefaultGameMode();

    /**
     * @return default rank settings for new islands
     */
    Map<Flag, Integer> getDefaultIslandFlags();

    Map<Flag, Integer> getDefaultIslandSettings();

    /**
     * Get the world difficulty
     * @return difficulty
     */
    Difficulty getDifficulty();

    /**
     * Set the world difficulty
     * @param difficulty - difficulty
     */
    void setDifficulty(Difficulty difficulty);

    /**
     * @return the friendly name of the world. Used in player commands
     */
    String getFriendlyName();

    /**
     * @return the islandDistance
     */
    int getIslandDistance();

    /**
     * @return the islandHeight
     */
    int getIslandHeight();

    /**
     * @return the islandProtectionRange
     */
    int getIslandProtectionRange();

    /**
     * @return the islandStartX
     */
    int getIslandStartX();

    /**
     * @return the islandStartZ
     */
    int getIslandStartZ();

    /**
     * @return the islandXOffset
     */
    int getIslandXOffset();

    /**
     * @return the islandZOffset
     */
    int getIslandZOffset();

    /**
     * @return Invincible Visitor setting list
     */
    List<String> getIvSettings();

    /**
     * @return the max homes
     */
    int getMaxHomes();

    /**
     * 0 or -1 is unlimited. It will block island creation if the island count for the world is higher than this.
     * @return the maxIslands
     */
    int getMaxIslands();

    /**
     * @return the max team size for this world
     */
    int getMaxTeamSize();

    /**
     * @return the max coop size for this world
     * @since 1.13.0
     */
    default int getMaxCoopSize() {
        return 4;
    }

    /**
     * @return the max trust size for this world
     * @since 1.13.0
     */
    default int getMaxTrustSize() {
        return 4;
    }

    /**
     * @return the netherSpawnRadius
     */
    int getNetherSpawnRadius();

    /**
     * @return the permission prefix
     */
    String getPermissionPrefix();

    /**
     * Get the set of entity types that should not be removed in this world when a player teleports to their island
     * @return set of entity types
     */
    Set<EntityType> getRemoveMobsWhitelist();

    /**
     * @return the seaHeight
     */
    int getSeaHeight();

    /**
     * @return hidden flag list
     */
    List<String> getHiddenFlags();

    /**
     * @return the visitorBannedCommands
     */
    List<String> getVisitorBannedCommands();

    /**
     * Optional list of commands that are banned when falling. Not applicable to all game modes so defaults to empty.
     * @return the fallingBannedCommands
     * @since 1.8.0
     */
    default List<String> getFallingBannedCommands() {
        return Collections.emptyList();
    }

    /**
     * Get world flags
     * @return Map of world flags
     */
    Map<String, Boolean> getWorldFlags();

    /**
     * @return the worldName
     */
    String getWorldName();

    /**
     * @return the dragonSpawn
     */
    boolean isDragonSpawn();

    /**
     * @return the endGenerate
     */
    boolean isEndGenerate();

    /**
     * @return the endIslands
     */
    boolean isEndIslands();

    /**
     * @return the netherGenerate
     */
    boolean isNetherGenerate();

    /**
     * @return the netherIslands
     */
    boolean isNetherIslands();

    /**
     * @return the onJoinResetEnderChest
     */
    boolean isOnJoinResetEnderChest();

    /**
     * @return the onJoinResetInventory
     */
    boolean isOnJoinResetInventory();

    /**
     * @return the onJoinResetMoney
     */
    boolean isOnJoinResetMoney();

    /**
     * Whether the player's health should be reset upon him joining an island or creating it.
     * @return the onJoinResetHealth
     * @since 1.8.0
     */
    boolean isOnJoinResetHealth();

    /**
     * Whether the player's hunger should be reset upon him joining an island or creating it.
     * @return the onJoinResetHunger
     * @since 1.8.0
     */
    boolean isOnJoinResetHunger();

    /**
     * Whether the player's XP should be reset upon him joining an island or creating it.
     * @return the onJoinResetXP
     * @since 1.8.0
     */
    boolean isOnJoinResetXP();

    /**
     * Returns a list of commands that should be executed when the player joins an island or creates one.<br/>
     * These commands are executed by the console, unless otherwise stated using the {@code [SUDO]} prefix, in which case they are executed by the player.<br/>
     * <br/>
     * Available placeholders for the commands are the following:
     * <ul>
     *     <li>{@code [player]}: name of the player</li>
     * </ul>
     * <br/>
     * Here are some examples of valid commands to execute:
     * <ul>
     *     <li>{@code "[SUDO] bbox version"}</li>
     *     <li>{@code "bsbadmin deaths set [player] 0"}</li>
     * </ul>
     * @return a list of commands.
     * @since 1.8.0
     * @see #getOnLeaveCommands()
     */
    @NonNull
    List<String> getOnJoinCommands();

    /**
     * @return the onLeaveResetEnderChest
     */
    boolean isOnLeaveResetEnderChest();

    /**
     * @return the onLeaveResetInventory
     */
    boolean isOnLeaveResetInventory();

    /**
     * @return the onLeaveResetMoney
     */
    boolean isOnLeaveResetMoney();

    /**
     * Whether the player's health should be reset upon him leaving his island or resetting it.
     * @return the onLeaveResetHealth
     * @since 1.8.0
     */
    boolean isOnLeaveResetHealth();

    /**
     * Whether the player's hunger should be reset upon him leaving his island or resetting it.
     * @return the onLeaveResetHunger
     * @since 1.8.0
     */
    boolean isOnLeaveResetHunger();

    /**
     * Whether the player's XP should be reset upon him leaving his island or resetting it.
     * @return the onLeaveResetXP
     * @since 1.8.0
     */
    boolean isOnLeaveResetXP();

    /**
     * Returns a list of commands that should be executed when the player leaves an island, resets his island or gets kicked from it.<br/>
     * These commands are executed by the console, unless otherwise stated using the {@code [SUDO]} prefix, in which case they are executed by the player.<br/>
     * <br/>
     * Available placeholders for the commands are the following:
     * <ul>
     *     <li>{@code [player]}: name of the player</li>
     * </ul>
     * <br/>
     * Here are some examples of valid commands to execute:
     * <ul>
     *     <li>{@code "[SUDO] bbox version"}</li>
     *     <li>{@code "bsbadmin deaths set [player] 0"}</li>
     * </ul>
     * <br/>
     * Note that player-executed commands might not work, as these commands can be run with said player being offline.
     * @return a list of commands.
     * @since 1.8.0
     * @see #getOnJoinCommands()
     */
    @NonNull
    List<String> getOnLeaveCommands();
    
    /**
     * Returns a list of commands that should be executed when the player respawns after death if {@link Flags#ISLAND_RESPAWN} is true.<br/>
     * @return a list of commands.
     * @since 1.14.0
     * @see #getOnJoinCommands()
     */
    @NonNull
    default List<String> getOnRespawnCommands() {
        return Collections.emptyList();
    }

    /**
     * @return true if the default world generator should not operate in this world
     */
    boolean isUseOwnGenerator();

    /**
     * @return true if water is not safe in this world, e.g, should not be a home location
     */
    boolean isWaterUnsafe();

    /**
     * @return list of entity types that should not exit the island limits
     */
    List<String> getGeoLimitSettings();

    /**
     * Get list of entities that should not spawn in this game mode
     * @return list of entities that should NOT spawn
     * @since 1.12.0
     */
    default List<String> getMobLimitSettings() {
        return new ArrayList<>();
    }

    /**
     * @return reset limit for world
     */
    int getResetLimit();

    /**
     * Get the island reset time stamp. Any player who last logged in before this time will have resets zeroed
     */
    long getResetEpoch();

    /**
     * Set the island reset time stamp. Any player who last logged in before this time will have resets zeroed
     */
    void setResetEpoch(long timestamp);

    /**
     * @return true if the death count should be reset when joining a team in this world
     */
    boolean isTeamJoinDeathReset();

    /**
     * @return max number of deaths for this world
     */
    int getDeathsMax();

    /**
     * @return whether deaths should be counted.
     */
    boolean isDeathsCounted();

    /**
     * @return true if deaths in the world are reset when the player has a new island
     * @since 1.6.0
     */
    boolean isDeathsResetOnNewIsland();

    /**
     * @return whether a player can set their home in the Nether or not.
     */
    boolean isAllowSetHomeInNether();

    /**
     * @return whether a player can set their home in the End or not.
     */
    boolean isAllowSetHomeInTheEnd();

    /**
     * @return whether a confirmation is required when a player tries to set their home in the Nether.
     */
    boolean isRequireConfirmationToSetHomeInNether();

    /**
     * @return whether a confirmation is required when a player tries to set their home in the End.
     */
    boolean isRequireConfirmationToSetHomeInTheEnd();

    /**
     * Gets ban limit for this world.
     * Once exceeded, island members won't be able to ban any more players from their island.
     * Set it to -1 for unlimited.
     * <br/>
     * Permission to increase the limit: {@code (permissionprefix).ban.maxlimit.(value)}
     * @return the ban limit for this world.
     */
    int getBanLimit();

    /**
     * @return whether leavers should lose a reset or not
     * @since 1.5.0
     */
    boolean isLeaversLoseReset();

    /**
     * @return whether players keep their inventory when they are kicked
     * @since 1.5.0
     */
    boolean isKickedKeepInventory();

    /* Create island on first login */

    /**
     *
     * @return true if island should be created on first login
     * @since 1.9.0
     */
    boolean isCreateIslandOnFirstLoginEnabled();

    /**
     *
     * @return the island creation delay after login
     * @since 1.9.0
     */
    int getCreateIslandOnFirstLoginDelay();

    /**
     *
     * @return if island creation should abort on logout
     * @since 1.9.0
     */
    boolean isCreateIslandOnFirstLoginAbortOnLogout();

    /**
     * Check if nether or end islands should be pasted on teleporting
     * @return true if missing nether or end islands should be pasted
     * @since 1.10.0
     */
    default boolean isPasteMissingIslands() {
        // Note that glitches can enable bedrock to be removed in ways that will not generate events.
        return true;
    }

    /**
     * Toggles whether the player should be teleported on his island after it got created.
     * <br/>
     * If set to {@code true}, the player will be teleported right away.
     * <br/>
     * If set to {@code false}, the player will remain where he is and a message will be sent inviting him to teleport to his island.
     * <br/><br/>
     * This does not apply to any other occurrences such as island reset, or island join.
     * <br/><br/>
     * Default value: {@code true} (to retain backward compatibility).
     * @return {@code true} if the player should be teleported to his island, {@code false} otherwise.
     * @since 1.10.0
     */
    default boolean isTeleportPlayerToIslandUponIslandCreation() {
        return true;
    }


    /**
     * Returns all aliases for main admin command.
     * It is assumed that all aliases are split with whitespace between them.
     * String cannot be empty.
     * Default value: {@code getFriendlyName() + "admin"} (to retain backward compatibility).
     * @return String value
     * @since 1.13.0
     */
    default String getAdminCommandAliases()
    {
        return this.getFriendlyName().toLowerCase(Locale.ENGLISH) + "admin";
    }


    /**
     * Returns all aliases for main player command.
     * It is assumed that all aliases are split with whitespace between them.
     * String cannot be empty.
     * Default value: {@code getFriendlyName()} (to retain backward compatibility).
     * @return String value
     * @since 1.13.0
     */
    default String getPlayerCommandAliases()
    {
        return this.getFriendlyName().toLowerCase(Locale.ENGLISH);
    }


    /**
     * Returns sub-command for users when they execute main user command and they have an
     * island.
     * If defined sub-command does not exist in accessible user command list, then it will
     * still call "go" sub-command.
     * Default value: {@code "go"} (to retain backward compatibility)
     * @return name of default sub-command for main command if user does have an island.
     * @since 1.13.0
     */
    default String getDefaultPlayerAction()
    {
        return "go";
    }


    /**
     * Returns default sub-command for users when they execute main user command and they
     * do not have an island.
     * If defined sub-command does not exist in accessible user command list, then it will
     * still call "create" sub-command.
     * Default value: {@code "create"} (to retain backward compatibility)
     * @return name of default sub-command for main command if user does not have an island.
     * @since 1.13.0
     */
    default String getDefaultNewPlayerAction()
    {
        return "create";
    }
}
