package us.tastybento.bskyblock.database.mysql;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.DataObject;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.FlagSerializer;


public class MySQLDatabaseHandlerTestDataObject implements DataObject {

    private String uniqueId = "";

    //// Island ////
    // The center of the island itself
    private Location center;

    // Island range
    private int range;

    // Coordinates of the island area
    private int minX;

    private int minZ;

    // Coordinates of minimum protected area
    private int minProtectedX;

    private int minProtectedZ;

    // Protection size
    private int protectionRange;

    // World the island is in
    private World world;

    // Display name
    private String name;

    // Time parameters
    private long createdDate;

    private long updatedDate;

    //// Team ////
    private UUID owner;
    private HashMap<UUID, Integer> members = new HashMap<>();

    //// State ////
    private boolean locked = false;
    private boolean spawn = false;

    private boolean purgeProtected = false;

    //// Protection flags ////
    @Adapter(FlagSerializer.class)
    private HashMap<Flag, Integer> flags = new HashMap<>();

    private int levelHandicap;
    private Location spawnPoint;

    public MySQLDatabaseHandlerTestDataObject() {}

    /**
     * @return the uniqueId
     */
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId - unique ID the uniqueId to set
     */
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the center
     */
    public Location getCenter() {
        return center;
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @return the range
     */
    public int getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(int range) {
        this.range = range;
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return minX;
    }

    /**
     * @param minX the minX to set
     */
    public void setMinX(int minX) {
        this.minX = minX;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return minZ;
    }

    /**
     * @param minZ the minZ to set
     */
    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    /**
     * @return the minProtectedX
     */
    public int getMinProtectedX() {
        return minProtectedX;
    }

    /**
     * @param minProtectedX the minProtectedX to set
     */
    public void setMinProtectedX(int minProtectedX) {
        this.minProtectedX = minProtectedX;
    }

    /**
     * @return the minProtectedZ
     */
    public int getMinProtectedZ() {
        return minProtectedZ;
    }

    /**
     * @param minProtectedZ the minProtectedZ to set
     */
    public void setMinProtectedZ(int minProtectedZ) {
        this.minProtectedZ = minProtectedZ;
    }

    /**
     * @return the protectionRange
     */
    public int getProtectionRange() {
        return protectionRange;
    }

    /**
     * @param protectionRange the protectionRange to set
     */
    public void setProtectionRange(int protectionRange) {
        this.protectionRange = protectionRange;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the createdDate
     */
    public long getCreatedDate() {
        return createdDate;
    }

    /**
     * @param createdDate the createdDate to set
     */
    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * @return the updatedDate
     */
    public long getUpdatedDate() {
        return updatedDate;
    }

    /**
     * @param updatedDate the updatedDate to set
     */
    public void setUpdatedDate(long updatedDate) {
        this.updatedDate = updatedDate;
    }

    /**
     * @return the owner
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * @param owner - the island owner the owner to set
     */
    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    /**
     * @return the members
     */
    public HashMap<UUID, Integer> getMembers() {
        return members;
    }

    /**
     * @param members the members to set
     */
    public void setMembers(HashMap<UUID, Integer> members) {
        this.members = members;
    }

    /**
     * @return the locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked the locked to set
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * @return the spawn
     */
    public boolean isSpawn() {
        return spawn;
    }

    /**
     * @param spawn the spawn to set
     */
    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    /**
     * @return the purgeProtected
     */
    public boolean isPurgeProtected() {
        return purgeProtected;
    }

    /**
     * @param purgeProtected the purgeProtected to set
     */
    public void setPurgeProtected(boolean purgeProtected) {
        this.purgeProtected = purgeProtected;
    }

    /**
     * @return the flags
     */
    public HashMap<Flag, Integer> getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(HashMap<Flag, Integer> flags) {
        this.flags = flags;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * @return the spawnPoint
     */
    public Location getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * @param spawnPoint the spawnPoint to set
     */
    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

}