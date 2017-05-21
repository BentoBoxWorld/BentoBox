package us.tastybento.bskyblock.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Location;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;

public class IslandsManager {

    private BSkyBlock plugin;
    private BSBDatabase database;
    
    private HashMap<Location, Island> islands;
    private HashMap<UUID, Island> islandsByUUID;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<Integer, TreeMap<Integer, Island>>();
    
    private Island spawn;

    // Metrics data
    private int metrics_createdcount = 0;

    public IslandsManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        islands = new HashMap<Location, Island>();
        islandsByUUID = new HashMap<UUID, Island>();
        spawn = null;
    }

    public void load(){
        //TODO
    }

    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    for(Entry<Location, Island> entry : islands.entrySet()){
                        database.saveIslandData(entry.getValue());
                    }
                }
            });
        } else {
            for(Entry<Location, Island> entry : islands.entrySet()){
                database.saveIslandData(entry.getValue());
            }
        }
    }

    public void shutdown(){
        save(false);
        islands.clear();
    }

    public int getCount(){
        return islands.size();
    }

    public boolean isIsland(Location location){
        return islands.get(location) != null;
    }

    public Island getIsland(Location location){
        return islands.get(location);
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned
     * @param uuid
     * @return
     */
    public Island getIsland(UUID uuid){
        return islandsByUUID.get(uuid);
    }

    /**
     * Create an island with no owner at location
     * @param location
     */
    public Island createIsland(Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner
     * @param location
     * @param owner UUID
     */
    public Island createIsland(Location location, UUID owner){
        Island island = new Island(location, owner, Settings.protectionRange);
        islands.put(location, island);
        if (owner != null)
            islandsByUUID.put(owner, island);
        return island;
    }
    
    public void deleteIsland(Location location){
        //TODO
    }

    public Island getSpawn(){
        return spawn;
    }

    // Metrics-related methods //

    public int metrics_getCreatedCount(){
        return metrics_createdcount;
    }

    public void metrics_setCreatedCount(int count){
        this.metrics_createdcount = count;
    }

    /**
     * Removes this player from any and all islands
     * @param playerUUID
     */
    public void removePlayer(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null) {
            if (island.getOwner().equals(playerUUID)) {
                // Clear ownership and members
                island.getMembers().clear();
                island.setOwner(null);
            }
            island.getMembers().remove(playerUUID);
        }
    }

    /**
     * Get the island level
     * @param playerUUID
     * @return Level of island, or null if unknown
     */
    public Integer getIslandLevel(UUID playerUUID) {
        if (islandsByUUID.containsKey(playerUUID))
            return islandsByUUID.get(playerUUID).getLevel();
        return null;
    }

    /**
     * Set the island level for this player
     * @param playerUUID
     * @param islandLevel
     * @return true if successful, false if not
     */
    public boolean setIslandLevel(UUID playerUUID, int islandLevel) {
        if (islandsByUUID.containsKey(playerUUID)) {
            islandsByUUID.get(playerUUID).setLevel(islandLevel);
            // TODO
            //plugin.getChatListener().setPlayerLevel(playerUUID, islandLevel);
            return true;
        }
        return false;
    }

    /**
     * Puts a player in a team. Removes them from their old island if required.
     * @param playerUUID
     * @param teamLeader
     * @param islandLocation
     * @return true if successful, false if not
     */
    public boolean setJoinTeam(UUID playerUUID, UUID teamLeader) {
        Island teamIsland = islandsByUUID.get(teamLeader);
        if (teamIsland == null) {
            // Something odd here, team leader does not have an island!
            plugin.getLogger().severe("Team leader does not have an island!");
            return false;
        }
        if (teamIsland.getMembers().contains(playerUUID)) {
            // Player already on island
            return true;
        }
        
        // TODO: Fire a join team event. If canceled, return false
        
        if (!setLeaveTeam(playerUUID)) {
            // Player not allowed to leave team
            return false;
        }
        // Add player to new island
        teamIsland.addMember(playerUUID);
        return true;
    }

    /**
     * Called when a player leaves a team
    * @param playerUUID
     * @return true if successful, false if not
     */
    public boolean setLeaveTeam(UUID playerUUID) {
        // Try to remove player from old island
        // TODO: Fire an event, if not cancelled, zero the player data
        plugin.getPlayers().zeroPlayerData(playerUUID);
        return true;
    }

    /**
     * Returns a set of island member UUID's for the island of playerUUID
     * 
     * @param playerUUID
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(UUID playerUUID) {
        Island island = islandsByUUID.get(playerUUID);
        if (island != null)
            return island.getMembers();
        return null;
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param playerUUID
     * @return UUID of leader or null if player has no island
     */
    public UUID getTeamLeader(UUID playerUUID) {
        if (islandsByUUID.containsKey(playerUUID))
            return islandsByUUID.get(playerUUID).getOwner();
        return null;
    }

    /**
     * Returns the island at the location or null if there is none.
     * This includes the full island space, not just the protected area
     * 
     * @param location
     * @return PlayerIsland object
     */
    public Island getIslandAt(Location location) {
        if (location == null) {
            return null;
        }
        // World check
        if (!inWorld(location)) {
            return null;
        }
        // Check if it is spawn
        if (spawn != null && spawn.onIsland(location)) {
            return spawn;
        }
        return getIslandAt(location.getBlockX(), location.getBlockZ());
    }

    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     * 
     * @param x
     * @param z
     * @return PlayerIsland or null
     */
    public Island getIslandAt(int x, int z) {
        Entry<Integer, TreeMap<Integer, Island>> en = islandGrid.floorEntry(x);
        if (en != null) {
            Entry<Integer, Island> ent = en.getValue().floorEntry(z);
            if (ent != null) {
                // Check if in the island range
                Island island = ent.getValue();
                if (island.inIslandSpace(x, z)) {
                    // plugin.getLogger().info("DEBUG: In island space");
                    return island;
                }
                //plugin.getLogger().info("DEBUG: not in island space");
            }
        }
        return null;
    }

    
    /**
     * Determines if a location is in the island world or not or
     * in the new nether if it is activated
     * @param loc
     * @return true if in the island world
     */
    protected boolean inWorld(Location loc) {
        // TODO: determine if the world is correct
        return true;
    }

    /**
     * @param playerUUID
     * @return ture if player has island
     */
    public boolean hasIsland(UUID playerUUID) {
        return islandsByUUID.containsKey(playerUUID);
    }

    /**
     * Returns the player's island location.
     * Returns an island location OR a team island location
     * 
     * @param playerUUID
     * @return Location of player's island or null if one does not exist
     */
    public Location getIslandLocation(UUID playerUUID) {
        if (hasIsland(playerUUID))
            return getIsland(playerUUID).getCenter();
        return null;
    }
    
    /**
     * @param playerUUID
     * @return ban list for player
     */
    public Set<UUID> getBanList(UUID playerUUID) {
        // Get player's island
        Island island = getIsland(playerUUID);
        return island == null ? new HashSet<UUID>(): island.getBanned();
    }

    /**
     * @param uniqueId
     * @return true if the player is the owner of their island, i.e., owner or team leader
     */
    public boolean isOwner(UUID uniqueId) {
        if (hasIsland(uniqueId)) {
            return getIsland(uniqueId).getOwner().equals(uniqueId) ? true : false;
        }
        return false;
    }
}
