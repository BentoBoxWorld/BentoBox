package us.tastybento.bskyblock.api.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;

import us.tastybento.bskyblock.api.addons.Addon;
import us.tastybento.bskyblock.api.flags.Flag;

/**
 * Contains world-specific settings. Only getters are required, but you may need setters for your own class.
 * @author tastybento
 *
 */
public interface WorldSettings {

    /**
     * @return the friendly name of the world. Used in player commands
     */
    String getFriendlyName();

    /**
     * @return the entityLimits
     */
    Map<EntityType, Integer> getEntityLimits();

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
     * @return the maxIslands
     */
    int getMaxIslands();

    /**
     * @return the netherSpawnRadius
     */
    int getNetherSpawnRadius();

    /**
     * @return the seaHeight
     */
    int getSeaHeight();

    /**
     * @return the tileEntityLimits
     */
    Map<String, Integer> getTileEntityLimits();

    /**
     * @return the worldName
     */
    String getWorldName();

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
     * @return the netherTrees
     */
    boolean isNetherTrees();

    /**
     * @return the dragonSpawn
     */
    boolean isDragonSpawn();

    /**
     * @return the max team size for this world
     */
    int getMaxTeamSize();

    /**
     * @return the max homes
     */
    int getMaxHomes();

    /**
     * @return the permission prefix
     */
    String getPermissionPrefix();

    /**
     * @return Invincible Visitor setting list
     */
    List<String> getIvSettings();

    /**
     * Get world flags
     * @return Map of world flags
     */
    Map<String, Boolean> getWorldFlags();

    /**
     * Get the default game mode for this game world, e.g. SURVIVAL
     * @return game mode
     */
    GameMode getDefaultGameMode();

    /**
     * Get the set of entity types that should not be removed in this world when a player teleports to their island
     * @return set of entity types
     */
    Set<EntityType> getRemoveMobsWhitelist();

    /**
     * @return the onJoinResetMoney
     */
    boolean isOnJoinResetMoney();

    /**
     * @return the onJoinResetInventory
     */
    boolean isOnJoinResetInventory();

    /**
     * @return the onJoinResetEnderChest
     */
    boolean isOnJoinResetEnderChest();

    /**
     * @return the onLeaveResetMoney
     */
    boolean isOnLeaveResetMoney();

    /**
     * @return the onLeaveResetInventory
     */
    boolean isOnLeaveResetInventory();

    /**
     * @return the onLeaveResetEnderChest
     */
    boolean isOnLeaveResetEnderChest();

    /**
     * @return the Addon that registered this world
     */
    Optional<Addon> getAddon();

    /**
     * @return default rank settings for new islands
     */
    Map<Flag, Integer> getDefaultIslandFlags();

    /**
     * @return visible settings for player
     */
    List<String> getVisibleSettings();

    Map<Flag, Integer> getDefaultIslandSettings();

    /**
     * @return true if the default world generator should not operate in this world
     */
    boolean isUseOwnGenerator();

    /**
     * @return the visitorBannedCommands
     */
    public List<String> getVisitorBannedCommands();

}
