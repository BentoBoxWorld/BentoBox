package us.tastybento.bskyblock.api.configuration;

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
    public String getFriendlyName();

    /**
     * @return the entityLimits
     */
    public Map<EntityType, Integer> getEntityLimits();
    
    /**
     * @return the islandDistance
     */
    public int getIslandDistance();

    /**
     * @return the islandHeight
     */
    public int getIslandHeight();
    
    /**
     * @return the islandProtectionRange
     */
    public int getIslandProtectionRange();
    
    /**
     * @return the islandStartX
     */
    public int getIslandStartX();
    
    /**
     * @return the islandStartZ
     */
    public int getIslandStartZ();
    
    /**
     * @return the islandXOffset
     */
    public int getIslandXOffset();
    
    /**
     * @return the islandZOffset
     */
    public int getIslandZOffset();
    
    /**
     * @return the maxIslands
     */
    public int getMaxIslands();
    
    /**
     * @return the netherSpawnRadius
     */
    public int getNetherSpawnRadius();
    
    /**
     * @return the seaHeight
     */
    public int getSeaHeight();
    
    /**
     * @return the tileEntityLimits
     */
    public Map<String, Integer> getTileEntityLimits();
    
    /**
     * @return the worldName
     */
    public String getWorldName();
    
    /**
     * @return the endGenerate
     */
    public boolean isEndGenerate();
    
    /**
     * @return the endIslands
     */
    public boolean isEndIslands();
    
    /**
     * @return the netherGenerate
     */
    public boolean isNetherGenerate();
    
    /**
     * @return the netherIslands
     */
    public boolean isNetherIslands();
    
    /**
     * @return the netherTrees
     */
    public boolean isNetherTrees();

    /**
     * @return the dragonSpawn
     */
    public boolean isDragonSpawn();
    
}
