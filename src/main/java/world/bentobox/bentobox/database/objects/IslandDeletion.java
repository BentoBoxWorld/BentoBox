package world.bentobox.bentobox.database.objects;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

/**
 * Data object to store islands in deletion
 * @author tastybento
 * @since 1.1
 */
public class IslandDeletion implements DataObject {

    @Expose
    private String uniqueId = "";

    @Expose
    private Location location;

    @Expose
    private int minXChunk;

    @Expose
    private int maxXChunk;

    @Expose
    private int minZChunk;

    @Expose
    private int maxZChunk;

    public IslandDeletion() {}

    public IslandDeletion(Island island) {
        uniqueId = UUID.randomUUID().toString();
        location = island.getCenter();
        minXChunk =  (location.getBlockX() - island.getMaxEverProtectionRange()) >> 4;
        maxXChunk = (island.getMaxEverProtectionRange() + location.getBlockX() - 1) >> 4;
        minZChunk = (location.getBlockZ() - island.getMaxEverProtectionRange()) >> 4;
        maxZChunk = (island.getMaxEverProtectionRange() + location.getBlockZ() - 1) >> 4;
    }

    public IslandDeletion(Location location, int minXChunk, int maxXChunk, int minZChunk, int maxZChunk) {
        this.uniqueId = UUID.randomUUID().toString();
        this.location = location;
        this.minXChunk = minXChunk;
        this.maxXChunk = maxXChunk;
        this.minZChunk = minZChunk;
        this.maxZChunk = maxZChunk;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof IslandDeletion)) {
            return false;
        }
        IslandDeletion other = (IslandDeletion) obj;
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        return true;
    }

    /**
     * @return the location
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return the maxXChunk
     */
    public int getMaxXChunk() {
        return maxXChunk;
    }

    /**
     * @return the maxZChunk
     */
    public int getMaxZChunk() {
        return maxZChunk;
    }

    /**
     * @return the minXChunk
     */
    public int getMinXChunk() {
        return minXChunk;
    }

    /**
     * @return the minZChunk
     */
    public int getMinZChunk() {
        return minZChunk;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return location.getWorld();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        return result;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * @param maxXChunk the maxXChunk to set
     */
    public void setMaxXChunk(int maxXChunk) {
        this.maxXChunk = maxXChunk;
    }

    /**
     * @param maxZChunk the maxZChunk to set
     */
    public void setMaxZChunk(int maxZChunk) {
        this.maxZChunk = maxZChunk;
    }

    /**
     * @param minXChunk the minXChunk to set
     */
    public void setMinXChunk(int minXChunk) {
        this.minXChunk = minXChunk;
    }

    /**
     * @param minZChunk the minZChunk to set
     */
    public void setMinZChunk(int minZChunk) {
        this.minZChunk = minZChunk;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}

