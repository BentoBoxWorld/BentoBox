package us.tastybento.bskyblock.api.configuration;

import java.util.List;
import java.util.Map;

import org.bukkit.entity.EntityType;

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
    
}
