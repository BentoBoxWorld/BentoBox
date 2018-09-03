package world.bentobox.bentobox.managers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.DeleteIslandChunks;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeTeleportBuilder;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 */
public class IslandsManager {

    private BentoBox plugin;

    /**
     * One island can be spawn, this is the one - otherwise, this value is null
     */
    private Map<World, Island> spawn;

    private Database<Island> handler;

    /**
     * The last locations where an island were put.
     * This is not stored persistently and resets when the server starts
     */
    private Map<World,Location> last;

    // Island Cache
    private IslandCache islandCache;

    // Async database saving semaphore
    private boolean midSave;

    /**
     * Islands Manager
     * @param plugin - plugin
     */
    public IslandsManager(BentoBox plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        handler = new Database<>(plugin, Island.class);
        islandCache = new IslandCache();
        spawn = new HashMap<>();
        last = new HashMap<>();
    }

    /**
     * This is a generic scan that can work in the overworld or the nether
     * @param l - location around which to scan
     * @param i - the range to scan for a location less than 0 means the full island.
     * @return - safe location, or null if none can be found
     */
    public Location bigScan(Location l, int i) {
        if (l == null) {
            return null;
        }
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
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     *
     * @param l
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public boolean isSafeLocation(Location l) {
        if (l == null) {
            return false;
        }
        Block ground = l.getBlock().getRelative(BlockFace.DOWN);
        Block space1 = l.getBlock();
        Block space2 = l.getBlock().getRelative(BlockFace.UP);

        // Ground must be solid
        if (!ground.getType().isSolid()) {
            return false;
        }
        // Cannot be submerged
        if (space1.isLiquid() && space2.isLiquid()) {
            return false;
        }
        // Check if water is safe in this world
        if (space1.isLiquid() && plugin.getIWM().isWaterNotSafe(l.getWorld())) {
            return false;
        }

        // Portals are not "safe"
        if (space1.getType() == Material.NETHER_PORTAL || ground.getType() == Material.NETHER_PORTAL || space2.getType() == Material.NETHER_PORTAL
                || space1.getType() == Material.END_PORTAL || ground.getType() == Material.END_PORTAL || space2.getType() == Material.END_PORTAL) {
            return false;
        }
        if (ground.getType().equals(Material.LAVA)
                || space1.getType().equals(Material.LAVA)
                || space2.getType().equals(Material.LAVA)) {
            return false;
        }

        // Check for trapdoors
        BlockData bd = ground.getBlockData();
        if (bd instanceof Openable) {
            return !((Openable)bd).isOpen();
        }

        if (ground.getType().equals(Material.CACTUS) || ground.getType().toString().contains("BOAT") || ground.getType().toString().contains("FENCE")
                || ground.getType().equals(Material.SIGN) || ground.getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        // Check that the space is not solid
        // The isSolid function is not fully accurate (yet) so we have to
        // check
        // a few other items
        // isSolid thinks that PLATEs and SIGNS are solid, but they are not
        return (!space1.getType().isSolid() || space1.getType().equals(Material.SIGN) || space1.getType().equals(Material.WALL_SIGN)) && (!space2.getType().isSolid() || space2.getType().equals(Material.SIGN) || space2.getType().equals(Material.WALL_SIGN));
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
     * @return Island or null if the island could not be created for some reason
     */
    public Island createIsland(Location location, UUID owner){
        Island island = new Island(location, owner, plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        if (islandCache.addIsland(island)) {
            return island;
        }
        return null;
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
        island.setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
        if (removeBlocks) {
            // Remove island from the cache
            islandCache.deleteIslandFromCache(island);
            // Remove the island from the database
            handler.deleteObject(island);
            // Remove players from island
            removePlayersFromIsland(island);
            // Remove blocks from world
            new DeleteIslandChunks(plugin, island);
        }
    }

    public int getCount(){
        return islandCache.size();
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned
     * @param world - world to check
     * @param user - user
     * @return Island or null
     */
    public Island getIsland(World world, User user){
        return islandCache.get(world, user.getUniqueId());
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned
     * @param world - world to check
     * @param uuid - user's uuid
     * @return Island or null
     */
    public Island getIsland(World world, UUID uuid){
        return islandCache.get(world, uuid);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes the full island space, not just the protected area.
     * Use {@link #getProtectedIslandAt(Location)} for only the protected
     * island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getIslandAt(Location location) {
        return Optional.ofNullable(islandCache.getIslandAt(location));
    }

    /**
     * Used for testing only to inject the islandCache mock object
     * @param islandCache - island cache
     */
    public void setIslandCache(IslandCache islandCache) {
        this.islandCache = islandCache;
    }

    /**
     * Returns the player's island location in World
     * Returns an island location OR a team island location
     *
     * @param world - world to check
     * @param uuid - the player's UUID
     * @return Location of player's island or null if one does not exist
     */
    public Location getIslandLocation(World world, UUID uuid) {
        if (hasIsland(world, uuid)) {
            return getIsland(world, uuid).getCenter();
        }
        return null;
    }

    public Location getLast(World world) {
        return last.get(world);
    }

    /**
     * Returns a set of island member UUID's for the island of playerUUID
     * This includes the owner of the island. If there is no island, this set will be empty.
     *
     * @param world - world to check
     * @param playerUUID - the player's UUID
     * @return Set of team UUIDs
     */
    public Set<UUID> getMembers(World world, UUID playerUUID) {
        return islandCache.getMembers(world, playerUUID);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes only the protected area. Use {@link #getIslandAt(Location)}
     * for the full island space.
     *
     * @param location - the location
     * @return Optional Island object
     */

    public Optional<Island> getProtectedIslandAt(Location location) {
        return getIslandAt(location).filter(i -> i.onIsland(location));
    }

    /**
     * Determines a safe teleport spot on player's island or the team island
     * they belong to.
     *
     * @param world - world to check
     * @param user - the player
     * @param number - a number - starting home location e.g., 1
     * @return Location of a safe teleport spot or null if one cannot be found
     */
    public Location getSafeHomeLocation(World world, User user, int number) {
        // Try the numbered home location first
        Location l = plugin.getPlayers().getHomeLocation(world, user, number);

        if (l == null) {
            // Get the default home, which may be null too, but that's okay
            number = 1;
            l = plugin.getPlayers().getHomeLocation(world, user, number);
        }
        // Check if it is safe
        if (l != null) {
            if (isSafeLocation(l)) {
                return l;
            }
            // To cover slabs, stairs and other half blocks, try one block above
            Location lPlusOne = l.clone();
            lPlusOne.add(new Vector(0, 1, 0));
            if (isSafeLocation(lPlusOne)) {
                // Adjust the home location accordingly
                plugin.getPlayers().setHomeLocation(user, lPlusOne, number);
                return lPlusOne;
            }
        }
        // Home location either isn't safe, or does not exist so try the island
        // location
        if (plugin.getIslands().inTeam(world, user.getUniqueId())) {
            l = plugin.getIslands().getIslandLocation(world, user.getUniqueId());
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(user, l, number);
                return l;
            } else {
                // try team leader's home
                Location tlh = plugin.getPlayers().getHomeLocation(world, plugin.getIslands().getTeamLeader(world, user.getUniqueId()));
                if (tlh != null && isSafeLocation(tlh)) {
                    plugin.getPlayers().setHomeLocation(user, tlh, number);
                    return tlh;
                }
            }
        } else {
            l = plugin.getIslands().getIslandLocation(world, user.getUniqueId());
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(user, l, number);
                return l.clone().add(new Vector(0.5D,0,0.5D));
            }
        }
        if (l == null) {
            plugin.logWarning(user.getName() + " player has no island in world " + world.getName() + "!");
            return null;
        }
        // If these island locations are not safe, then we need to get creative
        // Try the default location
        Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(user, dl, number);
            return dl;
        }
        // Try just above the bedrock
        dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(user, dl, number);
            return dl;
        }
        // Try all the way up to the sky
        for (int y = l.getBlockY(); y < 255; y++) {
            final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
            if (isSafeLocation(n)) {
                plugin.getPlayers().setHomeLocation(user, n, number);
                return n;
            }
        }
        // Unsuccessful
        return null;
    }

    /**
     * Get the island that is defined as spawn in this world
     * @param world - world
     * @return island or null
     */
    public Island getSpawn(World world){
        return spawn.get(world);
    }

    /**
     * Get the spawn point on the spawn island if it exists
     * @param world - world
     * @return the spawnPoint or null if spawn does not exist
     */
    public Location getSpawnPoint(World world) {
        return spawn.containsKey(world) ? spawn.get(world).getSpawnPoint(world.getEnvironment()) : null;
    }

    /**
     * Provides UUID of this player's team leader or null if it does not exist
     * @param world - world to check
     * @param leaderUUID - the leader's UUID
     * @return UUID of leader or null if player has no island
     */
    public UUID getTeamLeader(World world, UUID leaderUUID) {
        return islandCache.getTeamLeader(world, leaderUUID);
    }

    /**
     * Checks if a player has an island in the world
     * @param world - world to check
     * @param user - the user
     * @return true if player has island and owns it
     */
    public boolean hasIsland(World world, User user) {
        return islandCache.hasIsland(world, user.getUniqueId());
    }

    /**
     * Checks if a player has an island in the world
     * @param world - world to check
     * @param uuid - the user's uuid
     * @return true if player has island and owns it
     */
    public boolean hasIsland(World world, UUID uuid) {
        return islandCache.hasIsland(world, uuid);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     */
    public void homeTeleport(World world, Player player) {
        homeTeleport(world, player, 1, false);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     *
     * @param world - world to check
     * @param player - the player
     * @param number - a number - home location to do to
     */
    public void homeTeleport(World world, Player player, int number) {
        homeTeleport(world, player, number, false);
    }

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     *
     * @param world - world to check
     * @param player - the player
     * @param newIsland - true if this is a new island teleport
     */
    public void homeTeleport(World world, Player player, boolean newIsland) {
        homeTeleport(world, player, 1, newIsland);
    }

    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     *
     * @param world - world to check
     * @param player - the player
     * @param number - a number - home location to do to
     * @param newIsland - true if this is a new island teleport
     */
    public void homeTeleport(World world, Player player, int number, boolean newIsland) {
        User user = User.getInstance(player);
        Location home = getSafeHomeLocation(world, user, number);
        // Stop any gliding
        player.setGliding(false);
        // Check if the player is a passenger in a boat
        if (player.isInsideVehicle()) {
            Entity boat = player.getVehicle();
            if (boat instanceof Boat) {
                player.leaveVehicle();
                // Remove the boat so they don't lie around everywhere
                boat.remove();
                player.getInventory().addItem(new ItemStack(Material.getMaterial(((Boat) boat).getWoodType().toString() + "_BOAT"), 1));
                player.updateInventory();
            }
        }
        if (home == null) {
            // Try to fix this teleport location and teleport the player if possible
            new SafeTeleportBuilder(plugin).entity(player)
            .island(plugin.getIslands().getIsland(world, user))
            .homeNumber(number)
            .build();
            return;
        }
        player.teleport(home);
        if (number == 1) {
            user.sendMessage("commands.island.go.teleport");
        } else {
            user.sendMessage("commands.island.go.teleported", TextVariables.NUMBER, String.valueOf(number));
        }
        // Exit spectator mode if in it
        if (player.getGameMode().equals(GameMode.SPECTATOR)) {
            player.setGameMode(plugin.getIWM().getDefaultGameMode(world));
        }
        // If this is a new island, then run commands and do resets
        if (newIsland) {
            // TODO add command running

            // Remove money inventory etc.
            if (plugin.getIWM().isOnJoinResetEnderChest(world)) {
                user.getPlayer().getEnderChest().clear();
            }
            if (plugin.getIWM().isOnJoinResetInventory(world)) {
                user.getPlayer().getInventory().clear();
            }
            if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnJoinResetMoney(world)) {
                // TODO: needs Vault
            }
        }
    }

    /**
     * Indicates whether a player is at an island spawn or not
     *
     * @param playerLoc - player's location
     * @return true if they are, false if they are not, or spawn does not exist
     */
    public boolean isAtSpawn(Location playerLoc) {
        return spawn.containsKey(playerLoc.getWorld()) && spawn.get(playerLoc.getWorld()).onIsland(playerLoc);
    }

    /**
     * @param spawn the spawn to set
     */
    public void setSpawn(Island spawn) {
        this.spawn.put(spawn.getWorld(), spawn);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if the player is the owner of their island, i.e., owner or team leader
     */
    public boolean isOwner(World world, UUID uniqueId) {
        return hasIsland(world, uniqueId) && getIsland(world, uniqueId).getOwner().equals(uniqueId);
    }

    /**
     * Clear and reload all islands from database
     */
    public void load(){
        islandCache.clear();
        handler.loadObjects().forEach(islandCache::addIsland);
    }

    /**
     * Checks if a specific location is within the protected range of an island
     * that the player is a member of (owner or member)
     *
     * @param player - the player
     * @param loc - location
     * @return true if location is on island of player
     */
    public boolean locationIsOnIsland(Player player, Location loc) {
        if (player == null) {
            return false;
        }
        // Get the player's island
        return getIslandAt(loc).filter(i -> i.onIsland(loc)).map(i -> i.getMemberSet().contains(player.getUniqueId())).orElse(false);
    }

    /**
     * Checks if an online player is in the protected area of their island, a team island or a
     * coop island in the specific world in the arguments.
     *
     * @param world - the world to check
     * @param user - the user
     * @return true if on their island in world, false if not
     */
    public boolean userIsOnIsland(World world, User user) {
        if (user == null) {
            return false;
        }
        return getProtectedIslandAt(user.getLocation())
                .map(i -> i.getMembers().entrySet().stream()
                        .map(en -> en.getKey().equals(user.getUniqueId()) && en.getValue() > RanksManager.VISITOR_RANK)
                        .findAny().orElse(false))
                .orElse(false);
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param user - user
     */
    public void removePlayer(World world, User user) {
        islandCache.removePlayer(world, user.getUniqueId());
        save(true);
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param uuid - user's uuid
     */
    public void removePlayer(World world, UUID uuid) {
        islandCache.removePlayer(world, uuid);
        save(true);
    }

    /**
     * This removes players from an island overworld and nether - used when reseting or deleting an island
     * Mobs are killed when the chunks are refreshed.
     * @param island to remove players from
     */
    public void removePlayersFromIsland(Island island) {
        // Teleport players away
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (island.inIslandSpace(player.getLocation().getBlockX(), player.getLocation().getBlockZ())) {
                // Teleport island players to their island home
                if (hasIsland(island.getWorld(), player.getUniqueId()) || plugin.getIslands().inTeam(island.getWorld(), player.getUniqueId())) {
                    homeTeleport(island.getWorld(), player);
                } else {
                    // Move player to spawn
                    if (spawn.containsKey(island.getWorld())) {
                        // go to island spawn
                        player.teleport(spawn.get(island.getWorld()).getSpawnPoint(island.getWorld().getEnvironment()));
                    } else {
                        plugin.logWarning("During island deletion player " + player.getName() + " could not be sent home so was placed into spectator mode.");
                        player.setGameMode(GameMode.SPECTATOR);
                        player.getPlayer().setFlying(true);
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
        if (midSave) {
            // If it's already saving, then do nothing
            return;
        }
        Collection<Island> collection = islandCache.getIslands();
        if(async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                midSave = true;
                for(Island island : collection){
                    try {
                        handler.saveObject(island);
                    } catch (Exception e) {
                        plugin.logError("Could not save island to database when running async! " + e.getMessage());
                    }
                }
                midSave = false;
            });
        } else {
            for(Island island : collection){
                try {
                    handler.saveObject(island);
                } catch (Exception e) {
                    plugin.logError("Could not save island to database when running sync! " + e.getMessage());
                }
            }
        }
    }

    /**
     * Puts a player in a team. Removes them from their old island if required.
     * @param teamIsland - team island
     * @param playerUUID - the player's UUID
     */
    public void setJoinTeam(Island teamIsland, UUID playerUUID) {
        // Add player to new island
        teamIsland.addMember(playerUUID);
        islandCache.addPlayer(playerUUID, teamIsland);
        // Save the database
        save(false);

    }

    public void setLast(Location last) {
        this.last.put(last.getWorld(), last);
    }

    /**
     * Called when a player leaves a team
     * @param world - world
     * @param uuid - the player's UUID
     */
    public void setLeaveTeam(World world, UUID uuid) {
        plugin.getPlayers().clearHomeLocations(world, uuid);
        removePlayer(world, uuid);
    }

    public void shutdown(){
        save(false);
        islandCache.clear();
        handler.close();
    }

    /**
     * Checks if a player is in a team
     * @param world - world
     * @param playerUUID - player's UUID
     * @return true if in team, false if not
     */
    public boolean inTeam(World world, UUID playerUUID) {
        return getMembers(world, playerUUID).size() > 1;
    }

    /**
     * Makes a new leader for an island
     * @param world - world
     * @param user - the user who is issuing the command
     * @param targetUUID - the current island member who is going to become the leader
     */
    public void makeLeader(World world, User user, UUID targetUUID, String permPrefix) {
        makeLeader(user, targetUUID, getIsland(world, targetUUID), permPrefix);
    }

    /**
     * Makes a new leader for an island
     * @param user - requester
     * @param targetUUID - new leader
     * @param island - island to register
     */
    public void makeLeader(User user, UUID targetUUID, Island island, String permPrefix) {
        islandCache.setOwner(island, targetUUID);

        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]", plugin.getPlayers().getName(targetUUID));

        // Check if online
        User target = User.getInstance(targetUUID);
        target.sendMessage("commands.island.team.setowner.you-are-the-owner");
        if (target.isOnline()) {
            // Check if new leader has a different range permission than the island size
            int range = Util.getPermValue(target.getPlayer(), permPrefix + "island.range.", plugin.getIWM().getIslandProtectionRange(Util.getWorld(island.getWorld())));
            // Range can go up or down
            if (range != island.getProtectionRange()) {
                user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                target.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                plugin.log("Makeleader: Island protection range changed from " + island.getProtectionRange() + " to "
                        + range + " for " + user.getName() + " due to permission.");
            }
            island.setProtectionRange(range);

        }
    }

    /**
     * Clear an area of mobs as per world rules. Radius is 5 blocks in every direction.
     * @param loc - location to clear
     */
    public void clearArea(Location loc) {
        loc.getWorld().getNearbyEntities(loc, 5D, 5D, 5D).stream()
        .filter(en -> (en instanceof Monster))
        .filter(en -> !plugin.getIWM().getRemoveMobsWhitelist(loc.getWorld()).contains(en.getType()))
        .forEach(Entity::remove);
    }

}
