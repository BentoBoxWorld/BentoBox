package world.bentobox.bentobox.database.objects;

import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.adapters.Adapter;
import world.bentobox.bentobox.database.objects.adapters.FlagSerializer;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

/**
 * Stores all the info about an island
 * Managed by IslandsManager
 * Responsible for team information as well.
 *
 * @author Tastybento
 * @author Poslovitch
 */
public class Island implements DataObject {

    @Expose
    private String uniqueId = UUID.randomUUID().toString();

    //// Island ////
    // The center of the island itself
    @Expose
    private Location center;

    // Island range
    @Expose
    private int range;

    // Protection size
    @Expose
    private int protectionRange;

    // World the island started in. This may be different from the island location
    @Expose
    private World world;

    // Display name
    @Expose
    private String name;

    // Time parameters
    @Expose
    private long createdDate;
    @Expose
    private long updatedDate;

    //// Team ////
    @Expose
    private UUID owner;

    @Expose
    private Map<UUID, Integer> members = new HashMap<>();

    //// State ////
    @Expose
    private boolean spawn = false;
    @Expose
    private boolean purgeProtected = false;

    //// Protection flags ////
    @Adapter(FlagSerializer.class)
    @Expose
    private Map<Flag, Integer> flags = new HashMap<>();

    @Expose
    private int levelHandicap;
    @Expose
    private Map<Environment, Location> spawnPoint = new EnumMap<>(Environment.class);

    public Island() {}

    public Island(Location location, UUID owner, int protectionRange) {
        setOwner(owner);
        createdDate = System.currentTimeMillis();
        updatedDate = System.currentTimeMillis();
        world = location.getWorld();
        // Make a copy of the location
        center = new Location(location.getWorld(), location.getX(), location.getY(), location.getZ());
        range = BentoBox.getInstance().getIWM().getIslandDistance(world);
        this.protectionRange = protectionRange;
    }

    /**
     * Adds a team member. If player is on banned list, they will be removed from it.
     * @param playerUUID - the player's UUID
     */
    public void addMember(UUID playerUUID) {
        if (playerUUID != null) {
            members.put(playerUUID, RanksManager.MEMBER_RANK);
        }
    }
    /**
     * Adds target to a list of banned players for this island. May be blocked by the event being cancelled.
     * If the player is a member, coop or trustee, they will be removed from those lists.
     * @param targetUUID - the target's UUID
     * @return true if successfully added
     */
    public boolean addToBanList(UUID targetUUID) {
        if (targetUUID != null) {
            members.put(targetUUID, RanksManager.BANNED_RANK);
            return true;
        }
        return false;
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
     * Gets the Island Guard flag's setting. If this is a protection flag, the this will be the
     * rank needed to bypass this flag. If it is a Settings flag, any non-zero value means the
     * setting is allowed.
     * @param flag - flag
     * @return flag value
     */
    public int getFlag(Flag flag) {
        flags.putIfAbsent(flag, flag.getDefaultRank());
        return flags.get(flag);
    }

    /**
     * @return the flags
     */
    public Map<Flag, Integer> getFlags() {
        return flags;
    }

    /**
     * @return the levelHandicap
     */
    public int getLevelHandicap() {
        return levelHandicap;
    }

    /**
     * Get the team members of the island. If this is empty or cleared, there is no team.
     * @return the members - key is the UUID, value is the RanksManager enum, e.g. RanksManager.MEMBER_RANK
     */
    public Map<UUID, Integer> getMembers() {
        return members;
    }

    /**
     * Members > MEMBER_RANK
     * @return the members of the island (owner included)
     */
    public ImmutableSet<UUID> getMemberSet(){
        Builder<UUID> result = new ImmutableSet.Builder<>();

        for (Entry<UUID, Integer> member: members.entrySet()) {
            if (member.getValue() >= RanksManager.MEMBER_RANK) {
                result.add(member.getKey());
            }
        }
        return result.build();
    }

    /**
     * @return the minProtectedX
     */
    public int getMinProtectedX() {
        return center.getBlockX() - protectionRange;
    }

    /**
     * @return the minProtectedZ
     */
    public int getMinProtectedZ() {
        return center.getBlockZ() - protectionRange;
    }

    /**
     * @return the minX
     */
    public int getMinX() {
        return center.getBlockX() - range;
    }

    /**
     * @return the minZ
     */
    public int getMinZ() {
        return center.getBlockZ() - range;
    }

    /**
     * @return the island display name. Might be {@code null} if none is set.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the owner
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
     * Returns the island range.
     * It is a convenience method that returns the exact same value than island range, although it has been saved into the Island object for easier access.
     * @return the island range
     * @see #getProtectionRange()
     */
    public int getRange(){
        return range;
    }

    /**
     * Get the rank of user for this island
     * @param user - the User
     * @return rank integer
     */
    public int getRank(User user) {
        return members.getOrDefault(user.getUniqueId(), RanksManager.VISITOR_RANK);
    }

    @Override
    public String getUniqueId() {
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
     * @param x - x coordinate
     * @param z - z coordinate
     * @return true if in the island space
     */
    public boolean inIslandSpace(int x, int z) {
        return x >= getMinX() && x < getMinX() + range*2 && z >= getMinZ() && z < getMinZ() + range*2;
    }

    /**
     * Checks if location is in full island space, not just protected space
     * @param location - location
     * @return true if in island space
     */
    public boolean inIslandSpace(Location location) {
        return Util.sameWorld(world, location.getWorld()) && inIslandSpace(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Checks if the coords are in island space
     * @param blockCoord - Pair(x,z) coords of block
     * @return true or false
     */
    public boolean inIslandSpace(Pair<Integer, Integer> blockCoord) {
        return inIslandSpace(blockCoord.x, blockCoord.z);
    }

    /**
     * Check if the flag is allowed or not
     * For flags that are for the island in general and not related to rank.
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(Flag flag) {
        // A negative value means not allowed
        return getFlag(flag) >= 0;
    }

    /**
     * Check if a user is allowed to bypass the flag or not
     * @param user - the User - user
     * @param flag - flag
     * @return true if allowed, false if not
     */
    public boolean isAllowed(User user, Flag flag) {
        return getRank(user) >= getFlag(flag);
    }

    /**
     * Check if banned
     * @param targetUUID - the target's UUID
     * @return Returns true if target is banned on this island
     */
    public boolean isBanned(UUID targetUUID) {
        return members.containsKey(targetUUID) && members.get(targetUUID).equals(RanksManager.BANNED_RANK);
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
     * @param target - target location
     * @return true if it is, false if not
     */
    public boolean onIsland(Location target) {
        return Util.sameWorld(world, target.getWorld()) && target.getBlockX() >= getMinProtectedX() && target.getBlockX() < (getMinProtectedX() + protectionRange * 2) && target.getBlockZ() >= getMinProtectedZ() && target.getBlockZ() < (getMinProtectedZ() + protectionRange * 2);
    }

    /**
     * Removes target from the banned list. May be cancelled by unban event.
     * @param targetUUID - the target's UUID
     * @return true if successful, otherwise false.
     */
    public boolean removeFromBanList(UUID targetUUID) {
        return (members.remove(targetUUID) != null);
    }

    /**
     * Removes a player from the team member map. Generally, you should
     * use {@link world.bentobox.bentobox.managers.IslandsManager#removePlayer(World, UUID)}
     * @param playerUUID - uuid of player
     */
    public void removeMember(UUID playerUUID) {
        members.remove(playerUUID);
    }

    /**
     * @param center the center to set
     */
    public void setCenter(Location center) {
        if (center != null) {
            this.world = center.getWorld();
        }
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
     * @param flag - flag
     * @param value - Use RanksManager settings, e.g. RanksManager.MEMBER
     */
    public void setFlag(Flag flag, int value){
        flags.put(flag, value);
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(Map<Flag, Integer> flags) {
        this.flags = flags;
    }

    /**
     * Resets the flags to their default as set in config.yml for this island.
     * If flags are missing from the config, the default hard-coded value is used and set
     */
    public void setFlagsDefaults() {
        Map<Flag, Integer> result = new HashMap<>();
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.PROTECTION))
        .forEach(f -> result.put(f, BentoBox.getInstance().getIWM().getDefaultIslandFlags(world).getOrDefault(f, f.getDefaultRank())));
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.SETTING))
        .forEach(f -> result.put(f, BentoBox.getInstance().getIWM().getDefaultIslandSettings(world).getOrDefault(f, f.getDefaultRank())));
        this.setFlags(result);
    }

    /**
     * @param levelHandicap the levelHandicap to set
     */
    public void setLevelHandicap(int levelHandicap) {
        this.levelHandicap = levelHandicap;
    }

    /**
     * @param members the members to set
     */
    public void setMembers(Map<UUID, Integer> members) {
        this.members = members;
    }

    /**
     * Sets the display name of this Island.
     * <br/><br/>
     * An empty String or {@code null} will remove the display name.
     * @param name The display name to set.
     */
    public void setName(String name){
        this.name = (name != null && !name.equals("")) ? name : null;
    }

    /**
     * Sets the owner of the island.
     * @param owner - the island owner - the owner/team leader to set
     */
    public void setOwner(UUID owner){
        this.owner = owner;
        if (owner == null) {
            return;
        }
        // Defensive code: demote any previous owner
        for (Entry<UUID, Integer> en : members.entrySet()) {
            if (en.getValue().equals(RanksManager.OWNER_RANK)) {
                en.setValue(RanksManager.MEMBER_RANK);
            }
        }
        members.put(owner, RanksManager.OWNER_RANK);
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
     * Sets the island range.
     * This method should <u><strong>NEVER</strong></u> be used except for testing purposes.
     * <br>
     * The range value is a copy of {@link WorldSettings#getIslandDistance()} made when the Island
     * got created in order to allow easier access to this value and must therefore remain
     * <u><strong>AS IS</strong></u>.
     * @param range the range to set
     * @see #setProtectionRange(int)
     */
    public void setRange(int range){
        this.range = range;
    }

    /**
     * Set user's rank to an arbitrary rank value
     * @param user - the User
     * @param rank - rank value
     */
    public void setRank(User user, int rank) {
        if (user.getUniqueId() != null) {
            members.put(user.getUniqueId(), rank);
        }
    }

    /**
     * @param ranks the ranks to set
     */
    public void setRanks(Map<UUID, Integer> ranks) {
        members = ranks;
    }

    /**
     * @param isSpawn - if the island is the spawn
     */
    public void setSpawn(boolean isSpawn){
        spawn = isSpawn;
    }

    /**
     * Get the default spawn location for this island. Note that this may only be valid
     * after the initial pasting because the player can change the island after that point
     * @return the spawnPoint
     */
    public Map<Environment, Location> getSpawnPoint() {
        return spawnPoint;
    }

    /**
     * Set when island is pasted
     * @param spawnPoint the spawnPoint to set
     */
    public void setSpawnPoint(Map<Environment, Location> spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    @Override
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

    /**
     * Show info on the island
     * @param plugin - plugin
     * @param user - the user who is receiving the info
     * @param world - world to check
     * @return true always
     */
    public boolean showInfo(BentoBox plugin, User user, World world) {
        user.sendMessage("commands.admin.info.title");
        if (owner == null) {
            user.sendMessage("commands.admin.info.unowned");
        } else {
            user.sendMessage("commands.admin.info.owner", "[owner]", plugin.getPlayers().getName(owner), "[uuid]", owner.toString());

            // Fixes #getLastPlayed() returning 0 when it is the owner's first connection.
            long lastPlayed = (plugin.getServer().getOfflinePlayer(owner).getLastPlayed() != 0) ?
                    plugin.getServer().getOfflinePlayer(owner).getLastPlayed() : plugin.getServer().getOfflinePlayer(owner).getFirstPlayed();
                    user.sendMessage("commands.admin.info.last-login","[date]", new Date(lastPlayed).toString());

                    user.sendMessage("commands.admin.info.deaths", "[number]", String.valueOf(plugin.getPlayers().getDeaths(world, owner)));
                    String resets = String.valueOf(plugin.getPlayers().getResets(world, owner));
                    String total = plugin.getIWM().getResetLimit(world) < 0 ? "Unlimited" : String.valueOf(plugin.getIWM().getResetLimit(world));
                    user.sendMessage("commands.admin.info.resets-left", "[number]", resets, "[total]", total);
                    // Show team members
                    showMembers(plugin, user, world);
        }
        Vector location = center.toVector();
        user.sendMessage("commands.admin.info.island-location", "[xyz]", Util.xyz(location));
        Vector from = center.toVector().subtract(new Vector(range, 0, range)).setY(0);
        Vector to = center.toVector().add(new Vector(range-1, 0, range-1)).setY(center.getWorld().getMaxHeight());
        user.sendMessage("commands.admin.info.island-coords", "[xz1]", Util.xyz(from), "[xz2]", Util.xyz(to));
        user.sendMessage("commands.admin.info.protection-range", "[range]", String.valueOf(protectionRange));
        Vector pfrom = center.toVector().subtract(new Vector(protectionRange, 0, protectionRange)).setY(0);
        Vector pto = center.toVector().add(new Vector(protectionRange-1, 0, protectionRange-1)).setY(center.getWorld().getMaxHeight());
        user.sendMessage("commands.admin.info.protection-coords", "[xz1]", Util.xyz(pfrom), "[xz2]", Util.xyz(pto));
        if (spawn) {
            user.sendMessage("commands.admin.info.is-spawn");
        }
        Set<UUID> banned = getBanned();
        if (!banned.isEmpty()) {
            user.sendMessage("commands.admin.info.banned-players");
            banned.forEach(u -> user.sendMessage("commands.admin.info.banned-format", TextVariables.NAME, plugin.getPlayers().getName(u)));
        }
        return true;
    }

    /**
     * Shows the members of this island
     * @param plugin - plugin
     * @param user - user who is requesting
     * @param world - world to check
     */
    public void showMembers(BentoBox plugin, User user, World world) {
        user.sendMessage("commands.admin.info.team-members-title");
        members.forEach((u, i) -> {
            if (owner.equals(u)) {
                user.sendMessage("commands.admin.info.team-owner-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            } else if (i > RanksManager.VISITOR_RANK){
                user.sendMessage("commands.admin.info.team-member-format", TextVariables.NAME, plugin.getPlayers().getName(u)
                        , "[rank]", user.getTranslation(plugin.getRanksManager().getRank(i)));
            }
        });
    }

    /**
     * Toggles a settings flag
     * @param flag - flag
     */
    public void toggleFlag(Flag flag) {
        if (flag.getType().equals(Flag.Type.SETTING) || flag.getType().equals(Flag.Type.WORLD_SETTING)) {
            setSettingsFlag(flag, !isAllowed(flag));
        }
    }

    /**
     * Sets the state of a settings flag
     * @param flag - flag
     * @param state - true or false
     */
    public void setSettingsFlag(Flag flag, boolean state) {
        if (flag.getType().equals(Flag.Type.SETTING) || flag.getType().equals(Flag.Type.WORLD_SETTING)) {
            flags.put(flag, state ? 1 : -1);
        }
    }

    /**
     * Set the spawn location for this island type
     * @param islandType - island type
     * @param l - location
     */
    public void setSpawnPoint(Environment islandType, Location l) {
        spawnPoint.put(islandType, l);
    }

    /**
     * Get the spawn point for this island type
     * @param islandType - island type
     * @return - location or null if one does not exist
     */
    public Location getSpawnPoint(Environment islandType) {
        return spawnPoint.get(islandType);
    }


}