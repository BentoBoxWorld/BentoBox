package us.tastybento.bskyblock.database.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
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
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.IslandLockEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.IslandUnlockEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.FlagSerializer;
import us.tastybento.bskyblock.managers.RanksManager;
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

    public Island() {}

    public Island(Location location, UUID owner, int protectionRange) {
        setOwner(owner);
        this.createdDate = System.currentTimeMillis();
        this.updatedDate = System.currentTimeMillis();
        this.world = location.getWorld();
        this.center = location;
        this.range = BSkyBlock.getInstance().getSettings().getIslandDistance();
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
        if (playerUUID != null)
            members.put(playerUUID, RanksManager.MEMBER_RANK);
    }
    
    /**
     * Adds target to a list of banned players for this island. May be blocked by the event being cancelled.
     * If the player is a member, coop or trustee, they will be removed from those lists.
     * @param targetUUID
     * @return
     */
    public boolean addToBanList(UUID targetUUID) {
        // TODO fire ban event
        if (targetUUID != null)
            members.put(targetUUID, RanksManager.BANNED_RANK);
        return true;
    }

    /**
     * @return the banned
     */
    public Set<UUID> getBanned() {
        Set<UUID> result = new HashSet<>();
        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() <= RanksManager.BANNED_RANK) {
                result.add(member.getKey());
            }
        }
        return result;
    }

    /**
     * @return the center Location
     */
    public Location getCenter(){
        return center;
    }

    /**
     * @return the date when the island was created
     */
    public long getCreatedDate(){
        return createdDate;
    }

    /**
     * Get the Island Guard flag ranking
     * @param flag
     * @return flag rank. Players must have at least this rank to bypass this flag
     */
    public int getFlagReq(Flag flag){
        if(flags.containsKey(flag)) {
            return flags.get(flag);
        } else {
            flags.put(flag, RanksManager.MEMBER_RANK);
            return RanksManager.MEMBER_RANK;
        }
    }

    /**
     * @return the flags
     */
    public HashMap<Flag, Integer> getFlags() {
        return flags;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * @return true if the island is locked, otherwise false
     */
    public boolean getLocked(){
        return locked;
    }

    /**
     * @return the members
     */
    public HashMap<UUID, Integer> getMembers() {
        return members;
    }

    /**
     * @return the members of the island (owner included)
     */
    public Set<UUID> getMemberSet(){
        Set<UUID> result = new HashSet<>();
        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() >= RanksManager.MEMBER_RANK) {
                result.add(member.getKey());
            }
        }
        return result;
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
     * @return true if the island is protected from the Purge, otherwise false
     */
    public boolean getPurgeProtected(){
        return purgeProtected;
    }

    /**
     * @return the island range
     */
    public int getRange(){
        return range;
    }

    /**
     * Get the rank of user for this island
     * @param user
     * @return rank integer
     */
    public int getRank(User user) {
        //Bukkit.getLogger().info("DEBUG: user UUID = " + user.getUniqueId());
        return members.containsKey(user.getUniqueId()) ? members.get(user.getUniqueId()) : RanksManager.VISITOR_RANK;
    }

    /**
     * @return the ranks
     */
    public HashMap<UUID, Integer> getRanks() {
        return members;
    }

    /**
     * @return true if the island is the spawn otherwise false
     */
    public boolean getSpawn(){
        return spawn;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
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

    public String getUniqueId() {
        // Island's have UUID's that are randomly assigned if they do not exist
        if (uniqueId.isEmpty()) {
            uniqueId = UUID.randomUUID().toString();
        }
        return uniqueId;
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

    public boolean inIslandSpace(Location location) {
        if (Util.inWorld(location)) {
            return inIslandSpace(location.getBlockX(), location.getBlockZ());
        }
        return false;
    }

    /**
     * Check if the flag is allowed or not
     * For flags that are for the island in general and not related to rank
     * @param flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(Flag flag) {
        return this.getFlagReq(flag) >= 0 ? true : false;
    }

    /**
     * Check if a user is allowed to bypass the flag or not
     * @param user - user
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(User user, Flag flag) {
        //Bukkit.getLogger().info("DEBUG: " + flag.getID() + "  user score = " + getRank(user) + " flag req = "+ this.getFlagReq(flag));
        return (this.getRank(user) >= this.getFlagReq(flag)) ? true : false;
    }

    /**
     * Check if banned
     * @param targetUUID
     * @return Returns true if target is banned on this island
     */
    public boolean isBanned(UUID targetUUID) {
        return members.containsKey(targetUUID) && members.get(targetUUID) == RanksManager.BANNED_RANK ? true : false;
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
        members.remove(targetUUID);
        return true;
    }

    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
        this.center = center;
    }

    /**
     * @param createdDate - the createdDate to sets
     */
    public void setCreatedDate(long createdDate){
        this.createdDate = createdDate;
    }

    /**
     * Set the Island Guard flag rank
     * @param flag
     * @param value - rank value. If the flag applies to the island, a positive number = true, negative = false
     */
    public void setFlag(Flag flag, int value){
        flags.put(flag, value);
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(HashMap<Flag, Integer> flags) {
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
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
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
            if(!event.isCancelled()){
                this.locked = locked;
            }
        } else {
            // Unlock the island
            IslandBaseEvent event = IslandEvent.builder().island(this).reason(Reason.UNLOCK).build(); 
            if(!event.isCancelled()){
                this.locked = locked;
            }
        }
    }

    /**
     * @param members the members to set
     */
    public void setMembers(HashMap<UUID, Integer> members) {
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
     * Sets the owner of the island.
     * @param owner - the owner/team leader to set
     */
    public void setOwner(UUID owner){
        this.owner = owner;
        if (owner == null) return;
        // Defensive code: demote any previous owner
        for (Entry<UUID, Integer> en : members.entrySet()) {
            if (en.getValue().equals(RanksManager.OWNER_RANK)) {
                en.setValue(RanksManager.MEMBER_RANK);
            }
        }
        this.members.put(owner, RanksManager.OWNER_RANK);
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
     * Set user's rank to an arbitrary rank value
     * @param user
     * @param rank
     */
    public void setRank(User user, int rank) {
        if (user.getUniqueId() != null) members.put(user.getUniqueId(), rank);
    }

    /**
     * @param ranks the ranks to set
     */
    public void setRanks(HashMap<UUID, Integer> ranks) {
        this.members = ranks;
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

    public void setSpawnPoint(Location location) {
        spawnPoint = location;

    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
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
}