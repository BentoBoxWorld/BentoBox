package world.bentobox.bentobox.database.objects;

import org.bukkit.Location;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.util.Util;

/**
 * Data object to store islands in deletion
 *
 */
public class DeletedIslandDO implements DataObject {

    @Expose
    private String uniqueId = "";

    @Expose
    private World world;

    @Expose
    private int minXChunk;

    @Expose
    private int maxXChunk;

    @Expose
    private int minZChunk;

    @Expose
    private int maxZChunk;

    public DeletedIslandDO() {}

    public DeletedIslandDO(Location location, int minXChunk, int maxXChunk, int minZChunk, int maxZChunk) {
        this.uniqueId = Util.getStringLocation(location);
        this.world = location.getWorld();
        this.minXChunk = minXChunk;
        this.maxXChunk = maxXChunk;
        this.minZChunk = minZChunk;
        this.maxZChunk = maxZChunk;
    }

    public DeletedIslandDO(Island island) {
        uniqueId = Util.getStringLocation(island.getCenter());
        world = island.getCenter().getWorld();
        minXChunk =  island.getMinX() >> 4;
        maxXChunk = (island.getRange() * 2 + island.getMinX() - 1) >> 4;
        minZChunk = island.getMinZ() >> 4;
        maxZChunk = (island.getRange() * 2 + island.getMinZ() - 1) >> 4;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the minXChunk
     */
    public int getMinXChunk() {
        return minXChunk;
    }

    /**
     * @return the maxXChunk
     */
    public int getMaxXChunk() {
        return maxXChunk;
    }

    /**
     * @return the minZChunk
     */
    public int getMinZChunk() {
        return minZChunk;
    }

    /**
     * @return the maxZChunk
     */
    public int getMaxZChunk() {
        return maxZChunk;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * @param minXChunk the minXChunk to set
     */
    public void setMinXChunk(int minXChunk) {
        this.minXChunk = minXChunk;
    }

    /**
     * @param maxXChunk the maxXChunk to set
     */
    public void setMaxXChunk(int maxXChunk) {
        this.maxXChunk = maxXChunk;
    }

    /**
     * @param minZChunk the minZChunk to set
     */
    public void setMinZChunk(int minZChunk) {
        this.minZChunk = minZChunk;
    }

    /**
     * @param maxZChunk the maxZChunk to set
     */
    public void setMaxZChunk(int maxZChunk) {
        this.maxZChunk = maxZChunk;
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
        if (!(obj instanceof DeletedIslandDO)) {
            return false;
        }
        DeletedIslandDO other = (DeletedIslandDO) obj;
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        return true;
    }


}

