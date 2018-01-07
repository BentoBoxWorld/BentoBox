package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.IslandLockEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.IslandUnlockEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.util.Util;

/**
 * Stores all the info about an island
 * Managed by IslandsManager
 * Responsible for team information as well.
 *
 * @author Tastybento
 * @author Poslovitch
 */
public class Island implements DataObject {

    private String uniqueId = "";

    public String getUniqueId() {
        // Island's have UUID's that are randomly assigned if they do not exist
        if (uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

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
    // Owner (Team Leader)
    private UUID owner;

    // Members (use set because each value must be unique)
    private Set<UUID> members = new HashSet<>();

    // Trustees
    private Set<UUID> trustees = new HashSet<>();
    // Coops
    private Set<UUID> coops = new HashSet<>();

    // Banned players
    private Set<UUID> banned = new HashSet<>();
    //// State ////
    private boolean locked = false;
    private boolean spawn = false;
    private boolean purgeProtected = false;
    //// Protection ////
    private HashMap<Flag, Boolean> flags = new HashMap<>();

    private int levelHandicap;

    private Location spawnPoint;

    public Island() {}
    
    public Island(Location location, UUID owner, int protectionRange) {
        this.members.add(owner);
        this.owner = owner;
        this.createdDate = System.currentTimeMillis();
        this.updatedDate = System.currentTimeMillis();
        this.world = location.getWorld();
        this.center = location;
        this.range = BSkyBlock.getInstance().getSettings().getIslandProtectionRange();
        this.minX = center.getBlockX() - range;
        this.minZ = center.getBlockZ() - range;
        this.protectionRange = protectionRange;
        this.minProtectedX = center.getBlockX() - protectionRange;
        this.minProtectedZ = center.getBlockZ() - protectionRange;
    }

    /**
     * Adds a team member. If player is on banned list, they will be removed from it.
     * @param playerUUID
     */
    public void addMember(UUID playerUUID) {
        members.add(playerUUID);
        banned.remove(playerUUID);
    }

    /**
     * Adds target to a list of banned players for this island. May be blocked by the event being cancelled.
     * If the player is a member, coop or trustee, they will be removed from those lists.
     * @param targetUUID
     * @return
     */
    public boolean addToBanList(UUID targetUUID) {
        // TODO fire ban event
        if (members.contains(targetUUID)) {
            members.remove(targetUUID);
        }
        if (coops.contains(targetUUID)) {
            coops.remove(targetUUID);
        }
        if (trustees.contains(targetUUID)) {
            trustees.remove(targetUUID);
        }
        banned.add(targetUUID);
        return true;
    }

    /**
     * @return the banned
     */
    public Set<UUID> getBanned() {
        return banned;
    }

    /**
     * @return the center Location
     */
    public Location getCenter(){
        return center;
    }

    /**
     * @return the coop players of the island
     */
    public Set<UUID> getCoops(){
        return coops;
    }

    /**
     * @return the date when the island was created
     */
    public long getCreatedDate(){
        return createdDate;
    }

    /**
     * Get the Island Guard flag status
     * @param flag
     * @return true or false, or false if flag is not in the list
     */
    public boolean getFlag(Flag flag){
        if(flags.containsKey(flag)) {
            return flags.get(flag);
        } else {
            flags.put(flag, false);
            return false;
        }
    }

    /**
     * @return the flags
     */
    public HashMap<Flag, Boolean> getFlags() {
        return flags;
    }

    /**
     * @return the members of the island (owner included)
     */
    public Set<UUID> getMembers(){
        if (members == null) {
            members = new HashSet<>();
        }
        return members;
    }

    /**
     * @return the minProtectedX
     */
    public int getMinProtectedX() {
        return minProtectedX;
    }

    /**
     * @return the minProtectedZ
     */
    public int getMinProtectedZ() {
        return minProtectedZ;
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return minX;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return minZ;
    }

    /**
     * @return the island display name or the owner's name if none is set
     */
    public String getName() {
        if (name != null) {
            return name;
        }
        if (owner != null) {
            OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(owner);
            name = player.getName();
            return player.getName();
        }
        return "";
    }

    /**
     * @return the owner (team leader)
     */
    public UUID getOwner(){
        return owner;
    }

    /**
     * @return the protectionRange
     */
    public int getProtectionRange() {
        return protectionRange;
    }

    /**
     * @return the island range
     */
    public int getRange(){
        return range;
    }

    /**
     * @return the trustees players of the island
     */
    public Set<UUID> getTrustees(){
        return trustees;
    }

    /**
     * @return the date when the island was updated (team member connection, etc...)
     */
    public long getUpdatedDate(){
        return updatedDate;
    }

    /**
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * @return the x coordinate of the island center
     */
    public int getX(){
        return center.getBlockX();
    }

    /**
     * @return the y coordinate of the island center
     */
    public int getY(){
        return center.getBlockY();
    }

    /**
     * @return the z coordinate of the island center
     */
    public int getZ(){
        return center.getBlockZ();
    }

    /**
     * Checks if coords are in the island space
     * @param x
     * @param z
     * @return true if in the island space
     */
    public boolean inIslandSpace(int x, int z) {
        //Bukkit.getLogger().info("DEBUG: center - " + center);
        return (x >= minX && x < minX + range*2 && z >= minZ && z < minZ + range*2) ? true: false;
    }

    /**
     * Check if banned
     * @param targetUUID
     * @return Returns true if target is banned on this island
     */
    public boolean isBanned(UUID targetUUID) {
        return banned.contains(targetUUID);
    }

    /**
     * @return true if the island is locked, otherwise false
     */
    public boolean getLocked(){
        return locked;
    }

    /**
     * @return true if the island is protected from the Purge, otherwise false
     */
    public boolean getPurgeProtected(){
        return purgeProtected;
    }

    /**
     * @return true if the island is the spawn otherwise false
     */
    public boolean getSpawn(){
        return spawn;
    }

    /**
     * Checks if a location is within this island's protected area
     *
     * @param target
     * @return true if it is, false if not
     */
    public boolean onIsland(Location target) {
        if (center != null && center.getWorld() != null) {
            if (target.getBlockX() >= minProtectedX && target.getBlockX() < (minProtectedX + protectionRange * 2)
                    && target.getBlockZ() >= minProtectedZ && target.getBlockZ() < (minProtectedZ + protectionRange * 2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Removes target from the banned list. May be cancelled by unban event.
     * @param targetUUID
     * @return true if successful, otherwise false.
     */
    public boolean removeFromBanList(UUID targetUUID) {
        // TODO fire unban event
        banned.remove(targetUUID);
        return true;
    }

    /**
     * @param banned the banned to set
     */
    public void setBanned(Set<UUID> banned) {
        this.banned = banned;
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @param coops - the coops to set
     */
    public void setCoops(Set<UUID> coops){
        this.coops = coops;
    }

    /**
     * @param createdDate - the createdDate to sets
     */
    public void setCreatedDate(long createdDate){
        this.createdDate = createdDate;
    }

    /**
     * Set the Island Guard flag status
     * @param flag
     * @param value
     */
    public void setFlag(Flag flag, boolean value){
        flags.put(flag, value);
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(HashMap<Flag, Boolean> flags) {
        this.flags = flags;
    }

    /**
     * Resets the flags to their default as set in config.yml for this island
     */
    public void setFlagsDefaults(){
        /*for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultIslandSettings.get(flag));
        }*/ //TODO default flags
    }

    /**
     * Locks/Unlocks the island. May be cancelled by
     * {@link IslandLockEvent} or {@link IslandUnlockEvent}.
     * @param locked - the lock state to set
     */
    public void setLocked(boolean locked){
        if(locked){
            // Lock the island
            IslandBaseEvent event = IslandEvent.builder().island(this).reason(Reason.LOCK).build();
            Bukkit.getServer().getPluginManager().callEvent(event);

            if(!event.isCancelled()){
                this.locked = locked;
            }
        } else {
            // Unlock the island
            IslandBaseEvent event = IslandEvent.builder().island(this).reason(Reason.UNLOCK).build(); 
            Bukkit.getServer().getPluginManager().callEvent(event);

            if(!event.isCancelled()){
                this.locked = locked;
            }
        }
    }

    /**
     * @param members - the members to set
     */
    public void setMembers(Set<UUID> members){
        //Bukkit.getLogger().info("DEBUG: members size = " + members.size());
        this.members = members;
    }

    /**
     * @param minProtectedX the minProtectedX to set
     */
    public void setMinProtectedX(int minProtectedX) {
        this.minProtectedX = minProtectedX;
    }

    /**
     * @param minProtectedZ the minProtectedZ to set
     */
    public void setMinProtectedZ(int minProtectedZ) {
        this.minProtectedZ = minProtectedZ;
    }

    /**
     * @param minX the minX to set
     */
    public void setMinX(int minX) {
        this.minX = minX;
    }

    /**
     * @param minZ the minZ to set
     */
    public void setMinZ(int minZ) {
        this.minZ = minZ;
    }

    /**
     * @param name - the display name to set
     *               Set to null to remove the display name
     */
    public void setName(String name){
        this.name = name;
    }

    /**
     * Sets the owner of the island. If the owner was previous banned, they are unbanned
     * @param owner - the owner/team leader to set
     */
    public void setOwner(UUID owner){
        this.owner = owner;
        this.banned.remove(owner);
    }

    /**
     * @param protectionRange the protectionRange to set
     */
    public void setProtectionRange(int protectionRange) {
        this.protectionRange = protectionRange;
    }

    /**
     * @param purgeProtected - if the island is protected from the Purge
     */
    public void setPurgeProtected(boolean purgeProtected){
        this.purgeProtected = purgeProtected;
    }

    /**
     * @param range - the range to set
     */
    public void setRange(int range){
        this.range = range;
    }

    /**
     * @param isSpawn - if the island is the spawn
     */
    public void setSpawn(boolean isSpawn){
        this.spawn = isSpawn;
    }

    /**
     * Resets the flags to their default as set in config.yml for the spawn
     */
    public void setSpawnFlagsDefaults(){
        /*for(SettingsFlag flag : SettingsFlag.values()){
            this.flags.put(flag, Settings.defaultSpawnSettings.get(flag));
        }*/ //TODO default flags
    }

    /**
     * @param trustees - the trustees to set
     */
    public void setTrustees(Set<UUID> trustees){
        this.trustees = trustees;
    }

    /**
     * @param updatedDate - the updatedDate to sets
     */
    public void setUpdatedDate(long updatedDate){
        this.updatedDate = updatedDate;
    }

    /**
     * @param world the world to set
     */
    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Toggles the Island Guard flag status if it is in the list
     * @param flag
     */
    public void toggleFlag(Flag flag){
        if(flags.containsKey(flag)) {
            flags.put(flag, !flags.get(flag));
        }
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
     * @return true if island is locked, false if not
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @return spawn
     */
    public boolean isSpawn() {
        return spawn;
    }

    /**
     * @param material
     * @return count of how many tile entities of type mat are on the island at last count. Counts are done when a player places
     * a tile entity.
     */
    public int getTileEntityCount(Material material, World world) {
        int result = 0;
        for (int x = getMinProtectedX() /16; x <= (getMinProtectedX() + getProtectionRange() - 1)/16; x++) {
            for (int z = getMinProtectedZ() /16; z <= (getMinProtectedZ() + getProtectionRange() - 1)/16; z++) {
                for (BlockState holder : world.getChunkAt(x, z).getTileEntities()) {
                    //plugin.getLogger().info("DEBUG: tile entity: " + holder.getType());
                    if (onIsland(holder.getLocation())) {
                        if (holder.getType() == material) {
                            result++;
                        } else if (material.equals(Material.REDSTONE_COMPARATOR_OFF)) {
                            if (holder.getType().equals(Material.REDSTONE_COMPARATOR_ON)) {
                                result++;
                            }
                        } else if (material.equals(Material.FURNACE)) {
                            if (holder.getType().equals(Material.BURNING_FURNACE)) {
                                result++;
                            }
                        } else if (material.toString().endsWith("BANNER")) {
                            if (holder.getType().toString().endsWith("BANNER")) {
                                result++;
                            }
                        } else if (material.equals(Material.WALL_SIGN) || material.equals(Material.SIGN_POST)) {
                            if (holder.getType().equals(Material.WALL_SIGN) || holder.getType().equals(Material.SIGN_POST)) {
                                result++;
                            }
                        }
                    }
                }
                for (Entity holder : world.getChunkAt(x, z).getEntities()) {
                    //plugin.getLogger().info("DEBUG: entity: " + holder.getType());
                    if (holder.getType().toString().equals(material.toString()) && onIsland(holder.getLocation())) {
                        result++;
                    }
                }
            }
        }
        return result;
    }

    public boolean inIslandSpace(Location location) {
        if (Util.inWorld(location)) {
            return inIslandSpace(location.getBlockX(), location.getBlockZ());
        }
        return false;
    }

    public void setSpawnPoint(Location location) {
        spawnPoint = location;

    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public void removeMember(UUID playerUUID) {
        this.members.remove(playerUUID);
    }
}