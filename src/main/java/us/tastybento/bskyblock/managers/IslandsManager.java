package us.tastybento.bskyblock.managers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.material.SimpleAttachableMaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.island.IslandCache;
import us.tastybento.bskyblock.util.DeleteIslandChunks;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.teleport.SafeTeleportBuilder;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 *
 */
public class IslandsManager {

    /**
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     *
     * @param l
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public boolean isSafeLocation(final Location l) {
        if (l == null) {
            return false;
        }
        final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
        final Block space1 = l.getBlock();
        final Block space2 = l.getBlock().getRelative(BlockFace.UP);

        // Ground must be solid
        if (!ground.getType().isSolid()) {
            return false;
        } 
        // Cannot be submerged
        if (space1.isLiquid() && space2.isLiquid()) {
            return false;
        }
        
        // Portals are not "safe"
        if (space1.getType() == Material.PORTAL || ground.getType() == Material.PORTAL || space2.getType() == Material.PORTAL
                || space1.getType() == Material.ENDER_PORTAL || ground.getType() == Material.ENDER_PORTAL || space2.getType() == Material.ENDER_PORTAL) {
            return false;
        }
        // In BSkyBlock, liquid may be unsafe
        // Check if acid has no damage
        if (plugin.getSettings().getAcidDamage() > 0D && (ground.isLiquid() || space1.isLiquid() || space2.isLiquid())) {
            return false;
        }
        if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
                || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
                || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
            return false;
        }

        MaterialData md = ground.getState().getData();
        if (md instanceof SimpleAttachableMaterialData) {
            if (md instanceof TrapDoor) {
                TrapDoor trapDoor = (TrapDoor)md;
                if (trapDoor.isOpen()) {
                    return false;
                }
            } else {
                return false;
            }
        }
        if (ground.getType().equals(Material.CACTUS) || ground.getType().equals(Material.BOAT) || ground.getType().equals(Material.FENCE)
                || ground.getType().equals(Material.NETHER_FENCE) || ground.getType().equals(Material.SIGN_POST) || ground.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        // Check that the space is not solid
        // The isSolid function is not fully accurate (yet) so we have to
        // check
        // a few other items
        // isSolid thinks that PLATEs and SIGNS are solid, but they are not
        if (space1.getType().isSolid() && !space1.getType().equals(Material.SIGN_POST) && !space1.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        if (space2.getType().isSolid()&& !space2.getType().equals(Material.SIGN_POST) && !space2.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        // Safe
        return true;
    }

    private BSkyBlock plugin;

    private BSBDatabase database;

    /**
     * One island can be spawn, this is the one - otherwise, this value is null
     */
    private Island spawn;
    private AbstractDatabaseHandler<Island> handler;

    private Location last;
    // Metrics data
    private int metrics_createdcount = 0;

    // Island Cache
    private IslandCache islandCache;

    @SuppressWarnings("unchecked")
    public IslandsManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        handler = (AbstractDatabaseHandler<Island>) database.getHandler(Island.class);
        islandCache = new IslandCache();
        spawn = null;
    }

    /**
     * This is a generic scan that can work in the overworld or the nether
     * @param l - location around which to scan
     * @param i - the range to scan for a location less than 0 means the full island.
     * @return - safe location, or null if none can be found
     */
    public Location bigScan(Location l, int i) {
        final int height;
        final int depth;
        if (i > 0) {
            height = i;
            depth = i;
        } else {

            Optional<Island> island = getIslandAt(l);
            if (!island.isPresent()) {
                return null;
            }
            i = island.get().getProtectionRange();
            height = l.getWorld().getMaxHeight() - l.getBlockY();
            depth = l.getBlockY();
        }

        // Work outwards from l until the closest safe location is found.
        int minXradius = 0;
        int maxXradius = 0;
        int minZradius = 0;
        int maxZradius = 0;
        int minYradius = 0;
        int maxYradius = 0;

        do {
            int minX = l.getBlockX()-minXradius;
            int minZ = l.getBlockZ()-minZradius;
            int minY = l.getBlockY()-minYradius;
            int maxX = l.getBlockX()+maxXradius;
            int maxZ = l.getBlockZ()+maxZradius;
            int maxY = l.getBlockY()+maxYradius;
            for (int x = minX; x<= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        if (!((x > minX && x < maxX) && (z > minZ && z < maxZ) && (y > minY && y < maxY))) {
                            Location ultimate = new Location(l.getWorld(), x + 0.5D, y, z + 0.5D);
                            if (isSafeLocation(ultimate)) {
                                return ultimate;
                            }
                        }
                    }
                }
            }
            if (minXradius < i) {
                minXradius++;
            }
            if (maxXradius < i) {
                maxXradius++;
            }
            if (minZradius < i) {
                minZradius++;
            }
            if (maxZradius < i) {
                maxZradius++;
            }
            if (minYradius < depth) {
                minYradius++;
            }
            if (maxYradius < height) {
                maxYradius++;
            }
        } while (minXradius < i || maxXradius < i || minZradius < i || maxZradius < i || minYradius < depth
                || maxYradius < height);
        // Nothing worked
        return null;
    }

    /**
     * Create an island with no owner at location
     * @param location - the location - location
     * @return Island
     */
    public Island createIsland(Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not create the schematic. It just creates the island data object.
     * @param location - the location - location
     * @param owner - the island owner UUID
     * @return Island
     */
    public Island createIsland(Location location, UUID owner){
        return islandCache.createIsland(new Island(location, owner, plugin.getSettings().getIslandProtectionRange()));
    }

    /**
     * Deletes island. If island is null, it does nothing
     * @param island - island
     * @param removeBlocks - if the island blocks should be removed or not
     */
    public void deleteIsland(Island island, boolean removeBlocks) {
        if (island == null) {
            return;
        }
        // Set the owner of the island to no one.
        island.setOwner(null);
        island.setLocked(false);
        if (removeBlocks) {
            // Remove players from island
            removePlayersFromIsland(island);
            // Remove island from the cache
            islandCache.deleteIslandFromCache(island);
            // Remove the island from the database
            try {
                handler.deleteObject(island);
            } catch (Exception e) {
                plugin.getLogger().severe(()->"Could not delete island from database! " + e.getMessage());
            }
            // Remove blocks from world
            new DeleteIslandChunks(plugin, island);
        }
    }

    /**
     * Delete Island
     * Called when an island is restarted or reset
     *
     * @param player - the player
     *            - player name String
     * @param removeBlocks
     *            - true to remove the island blocks
     */
    public void deleteIsland(final UUID player, boolean removeBlocks) {
        // Removes the island
        final Island island = getIsland(player);
        if (island != null) {
            deleteIsland(island, removeBlocks);
        } else {
            plugin.getLogger().severe(()->"Could not delete player: " + player.toString() + " island!");
        }
    }

    /**
     * @param playerUUID - the player's UUID
     * @return ban list for player
     */
    public Set<UUID> getBanList(UUID playerUUID) {
        // Get player's island
        Island island = getIsland(playerUUID);
        return island == null ? new HashSet<>(): island.getBanned();
    }

    public int getCount(){
        return islandCache.size();
    }

    public AbstractDatabaseHandler<Island> getHandler() {
        return handler;
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned
     * @param uuid - UUID
     * @return Island or null
     */
    public Island getIsland(UUID uuid){
        return islandCache.get(uuid);
    }

    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x - x coordinate
     * @param z - z coordinate
     * @return Island or null
     */
    public Island getIslandAt(int x, int z) {
        return islandCache.getIslandAt(x,z);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes the full island space, not just the protected area
     *
     * @param location - the location
     * @return Island object
     */
    public Optional<Island> getIslandAt(Location location) {
        if (location == null) {
            return Optional.empty();
        }
        // World check
        if (!Util.inWorld(location)) {
            return Optional.empty();
        }
        // Check if it is spawn
        if (spawn != null && spawn.onIsland(location)) {
            return Optional.of(spawn);
        }
        return Optional.ofNullable(getIslandAt(location.getBlockX(), location.getBlockZ()));
    }

    /**
     * Returns the player's island location.
     * Returns an island location OR a team island location
     *
     * @param playerUUID - the player's UUID
     * @return Location of player's island or null if one does not exist
     */
    public Location getIslandLocation(UUID playerUUID) {
        if (hasIsland(playerUUID)) {
            return getIsland(playerUUID).getCenter();
        }
        return null;
    }

    /**
     * Get name of the island owned by owner
     * @param owner - the island owner
     * @return Returns the name of owner's island, or the owner's name if there is none.
     */
    public String getIslandName(UUID owner) {
        return islandCache.getIslandName(owner);
    }

    public Location getLast() {
        return last;
    }

    /**
     * Returns a set of island member UUID's for the island of playerUUID
     *
     * @param playerUUID - the player's UUID
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(UUID playerUUID) {
        return islandCache.getMembers(playerUUID);
    }

    /**
     * Returns the island being public at the location or Optional Empty if there is none
     *
     * @param location - the location
     * @return Optional Island object
     */

    public Optional<Island> getProtectedIslandAt(Location location) {
        // Try spawn
        if (spawn != null && spawn.onIsland(location)) {
            return Optional.of(spawn);
        }
        Optional<Island> island = getIslandAt(location);
        return island.map(x->x.onIsland(location) ? island.get() : null);
    }

    /**
     * Determines a safe teleport spot on player's island or the team island
     * they belong to.
     *
     * @param playerUUID - the player's UUID UUID of player
     * @param number - a number - starting home location e.g., 1
     * @return Location of a safe teleport spot or null if one cannot be found
     */
    public Location getSafeHomeLocation(final UUID playerUUID, int number) {
        // Try the numbered home location first
        Location l = plugin.getPlayers().getHomeLocation(playerUUID, number);
        if (l == null) {
            // Get the default home, which may be null too, but that's okay
            number = 1;
            l = plugin.getPlayers().getHomeLocation(playerUUID, number);
        }
        // Check if it is safe
        if (l != null) {
            if (isSafeLocation(l)) {
                return l;
            }
            // To cover slabs, stairs and other half blocks, try one block above
            Location lPlusOne = l.clone();
            lPlusOne.add(new Vector(0, 1, 0));
            if (lPlusOne != null) {
                if (isSafeLocation(lPlusOne)) {
                    // Adjust the home location accordingly
                    plugin.getPlayers().setHomeLocation(playerUUID, lPlusOne, number);
                    return lPlusOne;
                }
            }
        }
        // Home location either isn't safe, or does not exist so try the island
        // location
        if (plugin.getPlayers().inTeam(playerUUID)) {
            l = plugin.getIslands().getIslandLocation(playerUUID);
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(playerUUID, l, number);
                return l;
            } else {
                // try team leader's home
                Location tlh = plugin.getPlayers().getHomeLocation(plugin.getIslands().getTeamLeader(playerUUID));
                if (tlh != null) {
                    if (isSafeLocation(tlh)) {
                        plugin.getPlayers().setHomeLocation(playerUUID, tlh, number);
                        return tlh;
                    }
                }
            }
        } else {
            l = plugin.getIslands().getIslandLocation(playerUUID);
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(playerUUID, l, number);
                return l.clone().add(new Vector(0.5D,0,0.5D));
            }
        }
        if (l == null) {
            plugin.getLogger().warning(()-> plugin.getPlayers().getName(playerUUID) + " player has no island!");
            return null;
        }
        // If these island locations are not safe, then we need to get creative
        // Try the default location
        Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try just above the bedrock
        dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try all the way up to the sky
        for (int y = l.getBlockY(); y < 255; y++) {
            final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
            if (isSafeLocation(n)) {
                plugin.getPlayers().setHomeLocation(playerUUID, n, number);
                return n;
            }
        }
        // Unsuccessful
        return null;
    }

    public Island getSpawn(){
        return spawn;
    }

    /**
     * @return the spawnPoint or null if spawn does not exist
     */
    public Location getSpawnPoint() {
        return spawn == null ? null : spawn.getSpawnPoint();
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param playerUUID - the player's UUID
     * @return UUID of leader or null if player has no island
     */
    public UUID getTeamLeader(UUID playerUUID) {
        return islandCache.getTeamLeader(playerUUID);
    }

    /**
     * @param playerUUID - the player's UUID
     * @return true if player has island and owns it
     */
    public boolean hasIsland(UUID playerUUID) {
        return islandCache.hasIsland(playerUUID);
    }
    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param player - the player
     */
    public void homeTeleport(final Player player) {
        homeTeleport(player, 1);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     * @param player - the player
     * @param number - a number - home location to do to
     */
    @SuppressWarnings("deprecation")
    public void homeTeleport(final Player player, int number) {
        Location home = getSafeHomeLocation(player.getUniqueId(), number);
        // Check if the player is a passenger in a boat
        if (player.isInsideVehicle()) {
            Entity boat = player.getVehicle();
            if (boat instanceof Boat) {
                player.leaveVehicle();
                // Remove the boat so they don't lie around everywhere
                boat.remove();
                player.getInventory().addItem(new ItemStack(Material.BOAT, 1));
                player.updateInventory();
            }
        }
        if (home == null) {
            // Try to fix this teleport location and teleport the player if possible
            new SafeTeleportBuilder(plugin).entity(player)
            .island(plugin.getIslands().getIsland(player.getUniqueId()))
            .homeNumber(number)
            .build();
            return;
        }
        player.teleport(home);
        User user = User.getInstance(player);
        if (number == 1) {
            user.sendMessage("commands.island.go.teleport", "[label]", Constants.ISLANDCOMMAND);
        } else {
            user.sendMessage("commands.island.go.island.go.teleported", "[number]", String.valueOf(number));
        }
        // Exit spectator mode if in it
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        return;
    }

    /**
     * Indicates whether a player is at the island spawn or not
     *
     * @param playerLoc
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
        if (spawn == null) {
            return false;
        }
        return spawn.onIsland(playerLoc);
    }

    /**
     * Checks if there is an island or blocks at this location
     * @param location - the location
     * @return true if island found
     */
    public boolean isIsland(Location location){
        if (location == null) {
            return true;
        }
        location = getClosestIsland(location);
        if (islandCache.getIslandAt(location) != null) {
            return true;
        }

        if (!plugin.getSettings().isUseOwnGenerator()) {
            // Block check
            if (!location.getBlock().isEmpty() && !location.getBlock().isLiquid()) {
                createIsland(location);
                return true;
            }
            // Look around

            for (int x = -5; x <= 5; x++) {
                for (int y = 10; y <= 255; y++) {
                    for (int z = -5; z <= 5; z++) {
                        if (!location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).isEmpty()
                                && !location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).isLiquid()) {
                            createIsland(location);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This returns the coordinate of where an island should be on the grid.
     *
     * @param location - the location location to query
     * @return Location of closest island
     */
    public Location getClosestIsland(Location location) {
        long x = Math.round((double) location.getBlockX() / plugin.getSettings().getIslandDistance())
                * plugin.getSettings().getIslandDistance() + plugin.getSettings().getIslandXOffset();
        long z = Math.round((double) location.getBlockZ() / plugin.getSettings().getIslandDistance())
                * plugin.getSettings().getIslandDistance() + plugin.getSettings().getIslandZOffset();
        long y = plugin.getSettings().getIslandHeight();
        return new Location(location.getWorld(), x, y, z);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if the player is the owner of their island, i.e., owner or team leader
     */
    public boolean isOwner(UUID uniqueId) {
        return hasIsland(uniqueId) ? getIsland(uniqueId).getOwner().equals(uniqueId) : false;
    }

    /**
     * Clear and reload all islands from database
     */
    public void load(){
        islandCache.clear();
        spawn = null;
        try {
            for (Island island : handler.loadObjects()) {
                islandCache.addIsland(island);
            }
        } catch (Exception e) {
            plugin.getLogger().severe(()->"Could not load islands to cache! " + e.getMessage());
        }
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * owned by the player
     *
     * @param player - the player
     * @param loc - location
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island from the grid if it exists
        Optional<Island> island = getIslandAt(loc);
        if (island.isPresent()) {
            // Return whether the location is within the protected zone and the player is on the list of acceptable players
            return island.get().onIsland(loc) && island.get().getMemberSet().contains(player.getUniqueId());
        }
        // Not in the grid, so do it the old way
        // Make a list of test locations and test them
        Set<Location> islandTestLocations = new HashSet<>();
        if (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId())) {
            islandTestLocations.add(getIslandLocation(player.getUniqueId()));
        }
        // TODO: Check any coop locations
        // Run through all the locations
        for (Location islandTestLocation : islandTestLocations) {
            if (loc.getWorld().equals(islandTestLocation.getWorld())) {
                return loc.getX() >= islandTestLocation.getX() - plugin.getSettings().getIslandProtectionRange()
                        && loc.getX() < islandTestLocation.getX() + plugin.getSettings().getIslandProtectionRange()
                        && loc.getZ() >= islandTestLocation.getZ() - plugin.getSettings().getIslandProtectionRange()
                        && loc.getZ() < islandTestLocation.getZ() + plugin.getSettings().getIslandProtectionRange();
            }
        }
        return false;
    }

    public int metrics_getCreatedCount(){
        return metrics_createdcount;
    }

    public void metrics_setCreatedCount(int count){
        metrics_createdcount = count;
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island
     *
     * @param user - the User
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(User user) {
        return getIslandAt(user.getLocation()).map(x -> x.onIsland(user.getLocation())).orElse(false);
    }


    /**
     * @param location - the location
     */
    public void removeMobs(Location location) {
        // TODO Auto-generated method stub

    }

    /**
     * Removes this player from any and all islands
     * @param playerUUID - the player's UUID
     */
    public void removePlayer(UUID playerUUID) {
        islandCache.removePlayer(playerUUID);
    }

    /**
     * This removes players from an island overworld and nether - used when reseting or deleting an island
     * Mobs are killed when the chunks are refreshed.
     * @param island to remove players from
     */
    public void removePlayersFromIsland(final Island island) {
        // Teleport players away
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (island.inIslandSpace(player.getLocation().getBlockX(), player.getLocation().getBlockZ())) {
                // Teleport island players to their island home
                if (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId())) {
                    homeTeleport(player);
                } else {
                    // Move player to spawn
                    Island spawn = getSpawn();
                    if (spawn != null) {
                        // go to island spawn
                        player.teleport(plugin.getIslandWorldManager().getIslandWorld().getSpawnLocation());
                    } else {
                        if (!player.performCommand(Constants.SPAWNCOMMAND)) {
                            plugin.getLogger().warning(()->
                            "During island deletion player " + player.getName() + " could not be sent to spawn so was dropped, sorry.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Save the islands to the database
     * @param async - if true, saving will be done async
     */
    public void save(boolean async){
        Collection<Island> collection = islandCache.getIslands();
        if(async){
            Runnable save = () -> {
                for(Island island : collection){
                    try {
                        handler.saveObject(island);
                    } catch (Exception e) {
                        plugin.getLogger().severe(()->"Could not save island to datavase when running async! " + e.getMessage());
                    }
                }
            };
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, save);
        } else {
            for(Island island : collection){
                try {
                    handler.saveObject(island);
                } catch (Exception e) {
                    plugin.getLogger().severe(()->"Could not save island to datavase when running sync! " + e.getMessage());
                }
            }
        }
    }

    /**
     * Set the island name
     * @param owner - the island owner
     * @param name
     */
    public void setIslandName(UUID owner, String name) {
        islandCache.setIslandName(owner, name);
    }

    /**
     * Puts a player in a team. Removes them from their old island if required.
     * @param playerUUID - the player's UUID
     * @return true if successful, false if not
     */
    public boolean setJoinTeam(Island teamIsland, UUID playerUUID) {
        // Add player to new island
        teamIsland.addMember(playerUUID);
        islandCache.addPlayer(playerUUID, teamIsland);
        // Save the database
        save(false);

        return true;
    }

    public void setLast(Location last) {
        this.last = last;
    }

    /**
     * Called when a player leaves a team
     * @param playerUUID - the player's UUID
     */
    public void setLeaveTeam(UUID playerUUID) {
        plugin.getPlayers().clearHomeLocations(playerUUID);
        removePlayer(playerUUID);
    }

    public void shutdown(){
        save(false);
        islandCache.clear();
    }

}
