package world.bentobox.bentobox.database.objects;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;

/**
 * Data object to store islands in deletion
 * @author tastybento
 * @since 1.1
 */
@Table(name = "IslandDeletion")
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

    @Expose
    private int minX;

    @Expose
    private int minZ;

    @Expose
    private int maxX;

    @Expose
    private int maxZ;

    @Expose
    BoundingBox box;

    public IslandDeletion() {}

    public IslandDeletion(Island island) {
        // Get the world's island distance
        int islandDistance = BentoBox.getInstance().getIWM().getIslandDistance(island.getWorld());
        int range = Math.min(island.getMaxEverProtectionRange(), islandDistance);
        uniqueId = UUID.randomUUID().toString();
        location = island.getCenter();
        minX = location.getBlockX() - range;
        minXChunk =  minX >> 4;
        maxX = range + location.getBlockX();
        maxXChunk = maxX >> 4;
        minZ = location.getBlockZ() - range;
        minZChunk = minZ >> 4;
        maxZ = range + location.getBlockZ();
        maxZChunk = maxZ >> 4;
        box = BoundingBox.of(new Vector(minX, 0, minZ), new Vector(maxX, 255, maxZ));
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
            return other.uniqueId == null;
        } else return uniqueId.equals(other.uniqueId);
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

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public boolean inBounds(int x, int z) {
        return box.contains(new Vector(x, 0, z));
    }

    /**
     * @return the box
     */
    public BoundingBox getBox() {
        return box;
    }

    /**
     * @param box the box to set
     */
    public void setBox(BoundingBox box) {
        this.box = box;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "IslandDeletion [uniqueId=" + uniqueId + ", location=" + location + ", minXChunk=" + minXChunk
                + ", maxXChunk=" + maxXChunk + ", minZChunk=" + minZChunk + ", maxZChunk=" + maxZChunk + ", minX="
                + minX + ", minZ=" + minZ + ", maxX=" + maxX + ", maxZ=" + maxZ + ", box=" + box + "]";
    }

}

