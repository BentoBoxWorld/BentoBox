package us.tastybento.bskyblock.database.managers.island;

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
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.DeleteIslandChunks;
import us.tastybento.bskyblock.util.SafeTeleportBuilder;
import us.tastybento.bskyblock.util.Util;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 *
 */
public class IslandsManager {

    private static final boolean DEBUG = false;
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
        // TODO: improve the safe location finding.
        //Bukkit.getLogger().info("DEBUG: " + l.toString());
        final Block ground = l.getBlock().getRelative(BlockFace.DOWN);
        final Block space1 = l.getBlock();
        final Block space2 = l.getBlock().getRelative(BlockFace.UP);
        //Bukkit.getLogger().info("DEBUG: ground = " + ground.getType());
        //Bukkit.getLogger().info("DEBUG: space 1 = " + space1.getType());
        //Bukkit.getLogger().info("DEBUG: space 2 = " + space2.getType());
        // Portals are not "safe"
        if (space1.getType() == Material.PORTAL || ground.getType() == Material.PORTAL || space2.getType() == Material.PORTAL
                || space1.getType() == Material.ENDER_PORTAL || ground.getType() == Material.ENDER_PORTAL || space2.getType() == Material.ENDER_PORTAL) {
            return false;
        }
        // If ground is AIR, then this is either not good, or they are on slab,
        // stair, etc.
        if (ground.getType() == Material.AIR) {
            // Bukkit.getLogger().info("DEBUG: air");
            return false;
        }
        // In BSkyBlock, liquid may be unsafe
        // Check if acid has no damage
        if (plugin.getSettings().getAcidDamage() > 0D && (ground.isLiquid() || space1.isLiquid() || space2.isLiquid())) {
            // Bukkit.getLogger().info("DEBUG: acid");
            return false;
        }
        if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
                || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
                || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
            // Lava check only
            // Bukkit.getLogger().info("DEBUG: lava");
            return false;
        }

        MaterialData md = ground.getState().getData();
        if (md instanceof SimpleAttachableMaterialData) {
            //Bukkit.getLogger().info("DEBUG: trapdoor/button/tripwire hook etc.");
            if (md instanceof TrapDoor) {
                TrapDoor trapDoor = (TrapDoor)md;
                if (trapDoor.isOpen()) {
                    //Bukkit.getLogger().info("DEBUG: trapdoor open");
                    return false;
                }
            } else {
                return false;
            }
            //Bukkit.getLogger().info("DEBUG: trapdoor closed");
        }
        if (ground.getType().equals(Material.CACTUS) || ground.getType().equals(Material.BOAT) || ground.getType().equals(Material.FENCE)
                || ground.getType().equals(Material.NETHER_FENCE) || ground.getType().equals(Material.SIGN_POST) || ground.getType().equals(Material.WALL_SIGN)) {
            // Bukkit.getLogger().info("DEBUG: cactus");
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
        //Bukkit.getLogger().info("DEBUG: safe!");
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
     * @param i - the range to scan for a location < 0 means the full island.
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


        //plugin.getLogger().info("DEBUG: ranges i = " + i);
        //plugin.getLogger().info(" " + minX + "," + minZ + " " + maxX + " " + maxZ);
        //plugin.getLogger().info("DEBUG: height = " + height);
        //plugin.getLogger().info("DEBUG: depth = " + depth);
        //plugin.getLogger().info("DEBUG: trying to find a safe spot at " + l.toString());

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
                            //plugin.getLogger().info("DEBUG: checking " + x + "," + y + "," + z);
                            Location ultimate = new Location(l.getWorld(), x + 0.5D, y, z + 0.5D);
                            if (isSafeLocation(ultimate)) {
                                //plugin.getLogger().info("DEBUG: Found! " + ultimate);
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
            //plugin.getLogger().info("DEBUG: Radii " + minXradius + "," + minYradius + "," + minZradius +
            //    "," + maxXradius + "," + maxYradius + "," + maxZradius);
        } while (minXradius < i || maxXradius < i || minZradius < i || maxZradius < i || minYradius < depth
                || maxYradius < height);
        // Nothing worked
        return null;
    }

    /**
     * Create an island with no owner at location
     * @param location
     */
    public Island createIsland(Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not create the schematic. It just creates the island data object.
     * @param location
     * @param owner UUID
     */
    public Island createIsland(Location location, UUID owner){
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: adding island for " + owner + " at " + location);
        }
        return islandCache.createIsland(new Island(location, owner, plugin.getSettings().getIslandProtectionRange()));
    }

    /**
     * Deletes island. If island is null, it does nothing
     * @param island
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
                plugin.getLogger().severe("Could not delete island from database! " + e.getMessage());
            }
            // Remove blocks from world
            new DeleteIslandChunks(plugin, island);
        }
        //getServer().getPluginManager().callEvent(new IslandDeleteEvent(player, island.getCenter()));

    }

    /**
     * Delete Island
     * Called when an island is restarted or reset
     *
     * @param player
     *            - player name String
     * @param removeBlocks
     *            - true to remove the island blocks
     */
    public void deleteIsland(final UUID player, boolean removeBlocks) {
        // Removes the island
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: deleting player island");
        }
        //CoopPlay.getInstance().clearAllIslandCoops(player);
        //getWarpSignsListener().removeWarp(player);
        final Island island = getIsland(player);
        if (island != null) {
            deleteIsland(island, removeBlocks);
        } else {
            plugin.getLogger().severe("Could not delete player: " + player.toString() + " island!");
            //plugin.getServer().getPluginManager().callEvent(new IslandDeleteEvent(player, null));
        }
        //players.zeroPlayerData(player);
    }

    /**
     * @param playerUUID
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
     * @param uuid
     * @return
     */
    public Island getIsland(UUID uuid){
        return islandCache.get(uuid);
    }

    /**
     * Returns the island at the x,z location or null if there is none.
     * This includes the full island space, not just the protected area.
     *
     * @param x
     * @param z
     * @return Island or null
     */
    public Island getIslandAt(int x, int z) {
        return islandCache.getIslandAt(x,z);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes the full island space, not just the protected area
     *
     * @param location
     * @return Island object
     */
    public Optional<Island> getIslandAt(Location location) {
        if (location == null) {
            //plugin.getLogger().info("DEBUG: location is null");
            return Optional.empty();
        }
        // World check
        if (!Util.inWorld(location)) {
            //plugin.getLogger().info("DEBUG: not in right world");
            return Optional.empty();
        }
        // Check if it is spawn
        if (spawn != null && spawn.onIsland(location)) {
            //plugin.getLogger().info("DEBUG: spawn");
            return Optional.of(spawn);
        }
        return Optional.ofNullable(getIslandAt(location.getBlockX(), location.getBlockZ()));
    }

    /**
     * Returns the player's island location.
     * Returns an island location OR a team island location
     *
     * @param playerUUID
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
     * @param owner
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
     * @param playerUUID
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(UUID playerUUID) {
        return islandCache.getMembers(playerUUID);
    }

    /**
     * Returns the island being public at the location or Optional Empty if there is none
     *
     * @param location
     * @return Optional Island object
     */

    public Optional<Island> getProtectedIslandAt(Location location) {
        //plugin.getLogger().info("DEBUG: getProtectedIslandAt " + location);
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
     * @param playerUUID UUID of player
     * @param number - starting home location e.g., 1
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
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Home location " + l);
        }
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
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Home location either isn't safe, or does not exist so try the island");
        }
        // Home location either isn't safe, or does not exist so try the island
        // location
        if (plugin.getPlayers().inTeam(playerUUID)) {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG:player is in team");
            }
            l = plugin.getIslands().getIslandLocation(playerUUID);
            if (isSafeLocation(l)) {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG:island loc is safe");
                }
                plugin.getPlayers().setHomeLocation(playerUUID, l, number);
                return l;
            } else {
                // try team leader's home
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: trying leader's home");
                }
                Location tlh = plugin.getPlayers().getHomeLocation(plugin.getIslands().getTeamLeader(playerUUID));
                if (tlh != null) {
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: leader has a home");
                    }
                    if (isSafeLocation(tlh)) {
                        if (DEBUG) {
                            plugin.getLogger().info("DEBUG: team leader's home is safe");
                        }
                        plugin.getPlayers().setHomeLocation(playerUUID, tlh, number);
                        return tlh;
                    }
                }
            }
        } else {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: player is not in team - trying island location");
            }
            l = plugin.getIslands().getIslandLocation(playerUUID);
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(playerUUID, l, number);
                return l.clone().add(new Vector(0.5D,0,0.5D));
            }
        }
        if (l == null) {
            plugin.getLogger().warning(plugin.getPlayers().getName(playerUUID) + " player has no island!");
            return null;
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: If these island locations are not safe, then we need to get creative");
        }
        // If these island locations are not safe, then we need to get creative
        // Try the default location
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: try default location");
        }
        Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try just above the bedrock
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: above bedrock");
        }
        dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try all the way up to the sky
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: try all the way to the sky");
        }
        for (int y = l.getBlockY(); y < 255; y++) {
            final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
            if (isSafeLocation(n)) {
                plugin.getPlayers().setHomeLocation(playerUUID, n, number);
                return n;
            }
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: unsuccessful");
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
        //plugin.getLogger().info("DEBUG: getting spawn point : " + spawn.getSpawnPoint());
        if (spawn == null) {
            return null;
        }
        return spawn.getSpawnPoint();
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param playerUUID
     * @return UUID of leader or null if player has no island
     */
    public UUID getTeamLeader(UUID playerUUID) {
        return islandCache.getTeamLeader(playerUUID);
    }

    /**
     * @param playerUUID
     * @return true if player has island and owns it
     */
    public boolean hasIsland(UUID playerUUID) {
        return islandCache.hasIsland(playerUUID);
    }
    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param player
     * @return true if the home teleport is successful
     */
    public void homeTeleport(final Player player) {
        homeTeleport(player, 1);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     * @param player
     * @param number - home location to do to
     * @return true if successful, false if not
     */
    @SuppressWarnings("deprecation")
    public void homeTeleport(final Player player, int number) {
        Location home;
        if (DEBUG) {
            plugin.getLogger().info("home teleport called for #" + number);
        }
        home = getSafeHomeLocation(player.getUniqueId(), number);
        if (DEBUG) {
            plugin.getLogger().info("home get safe loc = " + home);
        }
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
            if (DEBUG) {
                plugin.getLogger().info("Fixing home location using safe spot teleport");
            }
            // Try to fix this teleport location and teleport the player if possible
            new SafeTeleportBuilder(plugin).entity(player)
            .location(plugin.getPlayers().getHomeLocation(player.getUniqueId(), number))
            .setHome(true)
            .homeNumber(number)
            .build();
            return;
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: home loc = " + home + " teleporting");
        }
        //home.getChunk().load();
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
     * @param location
     * @return
     */
    public boolean isIsland(Location location){
        if (location == null) {
            return true;
        }
        location = getClosestIsland(location);
        if (islandCache.contains(location)) {
            return true;
        }

        if (!plugin.getSettings().isUseOwnGenerator()) {
            // Block check
            if (!location.getBlock().isEmpty() && !location.getBlock().isLiquid()) {
                plugin.getLogger().info("Found solid block at island height - adding");
                createIsland(location);
                return true;
            }
            // Look around

            for (int x = -5; x <= 5; x++) {
                for (int y = 10; y <= 255; y++) {
                    for (int z = -5; z <= 5; z++) {
                        if (!location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).isEmpty()
                                && !location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).isLiquid()) {
                            plugin.getLogger().info("Solid block found during long search - adding ");
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
     * @param location location to query
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
     * @param uniqueId
     * @return true if the player is the owner of their island, i.e., owner or team leader
     */
    public boolean isOwner(UUID uniqueId) {
        if (hasIsland(uniqueId)) {
            return getIsland(uniqueId).getOwner().equals(uniqueId);
        }
        return false;
    }

    /**
     * Clear and reload all islands from database
     */
    public void load(){
        islandCache.clear();
        spawn = null;
        try {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: loading grid");
            }
            for (Island island : handler.loadObjects()) {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: adding island at "+ island.getCenter());
                }
                islandCache.addIsland(island);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load islands to cache! " + e.getMessage());
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: islands loaded");
        }
    }

    /**
     * Checks if a location is within the home boundaries of a player. If coop is true, this check includes coop players.
     * @param uuid
     * @param coop
     * @param loc
     * @return
     */
    public boolean locationIsAtHome(UUID uuid, boolean coop, Location loc) {
        // Make a list of test locations and test them
        Set<Location> islandTestLocations = new HashSet<>();
        if (plugin.getPlayers().hasIsland(uuid) || plugin.getPlayers().inTeam(uuid)) {
            islandTestLocations.add(plugin.getIslands().getIslandLocation(uuid));
            // If new Nether
            if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands() && plugin.getIslandWorldManager().getNetherWorld() != null) {
                islandTestLocations.add(netherIsland(plugin.getIslands().getIslandLocation(uuid)));
            }
        }
        // TODO: Check coop locations
        /*
        if (coop) {
            islandTestLocations.addAll(CoopPlay.getInstance().getCoopIslands(player));
        }*/
        if (islandTestLocations.isEmpty()) {
            return false;
        }
        // Run through all the locations
        for (Location islandTestLocation : islandTestLocations) {
            // Must be in the same world as the locations being checked
            // Note that getWorld can return null if a world has been deleted on the server
            if (islandTestLocation != null && islandTestLocation.getWorld() != null && islandTestLocation.getWorld().equals(loc.getWorld())) {
                int protectionRange = getIslandAt(islandTestLocation).map(x->x.getProtectionRange())
                        .orElse(plugin.getSettings().getIslandProtectionRange());
                if (loc.getX() > islandTestLocation.getX() - protectionRange
                        && loc.getX() < islandTestLocation.getX() + protectionRange
                        && loc.getZ() > islandTestLocation.getZ() - protectionRange
                        && loc.getZ() < islandTestLocation.getZ() + protectionRange) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * owned by the player
     *
     * @param player
     * @param loc
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(final Player player, final Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island from the grid if it exists
        Optional<Island> island = getIslandAt(loc);
        if (island.isPresent()) {
            //plugin.getLogger().info("DEBUG: island here is " + island.getCenter());
            // On an island in the grid
            //plugin.getLogger().info("DEBUG: onIsland = " + island.onIsland(loc));
            //plugin.getLogger().info("DEBUG: members = " + island.getMembers());
            //plugin.getLogger().info("DEBUG: player UUID = " + player.getUniqueId());

            if (island.get().onIsland(loc) && island.get().getMemberSet().contains(player.getUniqueId())) {
                //plugin.getLogger().info("DEBUG: allowed");
                // In a protected zone but is on the list of acceptable players
                return true;
            } else {
                // Not allowed
                //plugin.getLogger().info("DEBUG: not allowed");
                return false;
            }
        } else {
            //plugin.getLogger().info("DEBUG: no island at this location");
        }
        // Not in the grid, so do it the old way
        // Make a list of test locations and test them
        Set<Location> islandTestLocations = new HashSet<>();
        if (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId())) {
            islandTestLocations.add(getIslandLocation(player.getUniqueId()));
        }
        // TODO: Check any coop locations
        /*
        islandTestLocations.addAll(CoopPlay.getInstance().getCoopIslands(player));
        if (islandTestLocations.isEmpty()) {
            return false;
        }*/
        // Run through all the locations
        for (Location islandTestLocation : islandTestLocations) {
            if (loc.getWorld().equals(islandTestLocation.getWorld())) {
                if (loc.getX() >= islandTestLocation.getX() - plugin.getSettings().getIslandProtectionRange()
                        && loc.getX() < islandTestLocation.getX() + plugin.getSettings().getIslandProtectionRange()
                        && loc.getZ() >= islandTestLocation.getZ() - plugin.getSettings().getIslandProtectionRange()
                        && loc.getZ() < islandTestLocation.getZ() + plugin.getSettings().getIslandProtectionRange()) {
                    return true;
                }
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
     * Generates a Nether version of the locations
     * @param islandLocation
     * @return
     */
    private Location netherIsland(Location islandLocation) {
        //plugin.getLogger().info("DEBUG: netherworld = " + ASkyBlock.getNetherWorld());
        return islandLocation.toVector().toLocation(plugin.getIslandWorldManager().getNetherWorld());
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island
     *
     * @param player
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(User user) {
        return playerIsOnIsland(user, true);
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island
     * @param user
     * @param coop - if true, coop islands are included
     * @return true if on valid island, false if not
     */
    public boolean playerIsOnIsland(User user, boolean coop) {
        return locationIsAtHome(user.getUniqueId(), coop, user.getLocation());
    }

    /**
     * @param location
     */
    public void removeMobs(Location location) {
        // TODO Auto-generated method stub

    }

    /**
     * Removes this player from any and all islands
     * @param playerUUID
     */
    public void removePlayer(UUID playerUUID) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: removing player");
        }
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
                //plugin.getLogger().info("DEBUG: in island space");
                // Teleport island players to their island home
                if (plugin.getPlayers().hasIsland(player.getUniqueId()) || plugin.getPlayers().inTeam(player.getUniqueId())) {
                    //plugin.getLogger().info("DEBUG: home teleport");
                    homeTeleport(player);
                } else {
                    //plugin.getLogger().info("DEBUG: move player to spawn");
                    // Move player to spawn
                    Island spawn = getSpawn();
                    if (spawn != null) {
                        // go to island spawn
                        player.teleport(plugin.getIslandWorldManager().getIslandWorld().getSpawnLocation());
                        //plugin.getLogger().warning("During island deletion player " + player.getName() + " sent to spawn.");
                    } else {
                        if (!player.performCommand(Constants.SPAWNCOMMAND)) {
                            plugin.getLogger().warning(
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
                int index = 1;
                for(Island island : collection){
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: saving island async " + index++);
                    }
                    try {
                        handler.saveObject(island);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not save island to datavase when running async! " + e.getMessage());
                    }
                }
            };
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, save);
        } else {
            int index = 1;
            for(Island island : collection){
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: saving island " + index++);
                }
                try {
                    handler.saveObject(island);
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not save island to datavase when running sync! " + e.getMessage());
                }
            }
        }
    }

    // Metrics-related methods //

    /**
     * Set the island name
     * @param owner
     * @param name
     */
    public void setIslandName(UUID owner, String name) {
        islandCache.setIslandName(owner, name);
    }

    /**
     * Puts a player in a team. Removes them from their old island if required.
     * @param playerUUID
     * @return true if successful, false if not
     */
    public boolean setJoinTeam(Island teamIsland, UUID playerUUID) {
        // Add player to new island
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Adding player to new island");
        }
        teamIsland.addMember(playerUUID);
        islandCache.addPlayer(playerUUID, teamIsland);
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: new team member list:");
            plugin.getLogger().info(teamIsland.getMemberSet().toString());
        }
        // Save the database
        save(false);

        return true;
    }

    public void setLast(Location last) {
        this.last = last;
    }

    /**
     * Called when a player leaves a team
     * @param playerUUID
     */
    public void setLeaveTeam(UUID playerUUID) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: leaving team");
        }
        plugin.getPlayers().clearPlayerHomes(playerUUID);
        removePlayer(playerUUID);
    }

    public void shutdown(){
        save(false);
        islandCache.clear();
    }

}
