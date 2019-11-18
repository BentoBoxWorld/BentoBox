package world.bentobox.bentobox.blueprints.dataobjects;

import org.bukkit.entity.EntityType;

import com.google.gson.annotations.Expose;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintCreatureSpawner {

    @Expose
    private EntityType spawnedType;
    @Expose
    private int delay;
    @Expose
    private int maxNearbyEntities;
    @Expose
    private int maxSpawnDelay;
    @Expose
    private int minSpawnDelay;
    @Expose
    private int requiredPlayerRange;
    @Expose
    private int spawnRange;
    /**
     * @return the spawnedType
     */
    public EntityType getSpawnedType() {
        return spawnedType;
    }
    /**
     * @param spawnedType the spawnedType to set
     */
    public void setSpawnedType(EntityType spawnedType) {
        this.spawnedType = spawnedType;
    }
    /**
     * @return the delay
     */
    public int getDelay() {
        return delay;
    }
    /**
     * @param delay the delay to set
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }
    /**
     * @return the maxNearbyEntities
     */
    public int getMaxNearbyEntities() {
        return maxNearbyEntities;
    }
    /**
     * @param maxNearbyEntities the maxNearbyEntities to set
     */
    public void setMaxNearbyEntities(int maxNearbyEntities) {
        this.maxNearbyEntities = maxNearbyEntities;
    }
    /**
     * @return the maxSpawnDelay
     */
    public int getMaxSpawnDelay() {
        return maxSpawnDelay;
    }
    /**
     * @param maxSpawnDelay the maxSpawnDelay to set
     */
    public void setMaxSpawnDelay(int maxSpawnDelay) {
        this.maxSpawnDelay = maxSpawnDelay;
    }
    /**
     * @return the minSpawnDelay
     */
    public int getMinSpawnDelay() {
        return minSpawnDelay;
    }
    /**
     * @param minSpawnDelay the minSpawnDelay to set
     */
    public void setMinSpawnDelay(int minSpawnDelay) {
        this.minSpawnDelay = minSpawnDelay;
    }
    /**
     * @return the requiredPlayerRange
     */
    public int getRequiredPlayerRange() {
        return requiredPlayerRange;
    }
    /**
     * @param requiredPlayerRange the requiredPlayerRange to set
     */
    public void setRequiredPlayerRange(int requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
    }
    /**
     * @return the spawnRange
     */
    public int getSpawnRange() {
        return spawnRange;
    }
    /**
     * @param spawnRange the spawnRange to set
     */
    public void setSpawnRange(int spawnRange) {
        this.spawnRange = spawnRange;
    }
}
