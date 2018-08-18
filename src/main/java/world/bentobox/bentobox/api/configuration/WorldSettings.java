package world.bentobox.bentobox.api.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * Contains world-specific settings. Only getters are required, but you may need setters for your own class.
 * @author tastybento
 *
 */
public interface WorldSettings {

    /**
     * @return the Addon that registered this world
     */
    Optional<Addon> getAddon();

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
     * @return the maxIslands
     */
    int getMaxIslands();

    /**
     * @return the max team size for this world
     */
    int getMaxTeamSize();

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
     * @return visible settings for player
     */
    List<String> getVisibleSettings();

    /**
     * @return the visitorBannedCommands
     */
    List<String> getVisitorBannedCommands();

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
     * @return the netherTrees
     */
    boolean isNetherTrees();

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

}
