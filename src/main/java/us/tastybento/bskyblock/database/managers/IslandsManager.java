package us.tastybento.bskyblock.database.managers;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
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
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.schematics.Schematic;
import us.tastybento.bskyblock.schematics.Schematic.PasteReason;
import us.tastybento.bskyblock.util.SafeSpotTeleport;
import us.tastybento.bskyblock.util.Util;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 *
 */
public class IslandsManager {

    private BSkyBlock plugin;
    private BSBDatabase database;

    private WeakHashMap<Location, Island> islandsByLocation;
    private WeakHashMap<UUID, Island> islandsByUUID;
    // 2D islandGrid of islands, x,z
    private TreeMap<Integer, TreeMap<Integer, Island>> islandGrid = new TreeMap<Integer, TreeMap<Integer, Island>>();

    /**
     * One island can be spawn, this is the one - otherwise, this value is null
     */
    private Island spawn;

    // Metrics data
    private int metrics_createdcount = 0;
    private AbstractDatabaseHandler<Island> handler;
    private Location last;

    @SuppressWarnings("unchecked")
    public IslandsManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Island classes
        handler = (AbstractDatabaseHandler<Island>) database.getHandler(plugin, Island.class);
        islandsByLocation = new WeakHashMap<Location, Island>();
        islandsByUUID = new WeakHashMap<UUID, Island>();
        spawn = null;
    }

    /**
     * Clear and reload all islands from database
     */
    public void load(){
        islandsByLocation.clear();
        islandsByUUID.clear();
        spawn = null;
        try {
            for (Island island : handler.loadObjects()) {
                islandsByLocation.put(island.getCenter(), island);
                islandsByUUID.put(island.getOwner(), island);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void save(boolean async){
        if(async){
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                @Override
                public void run() {
                    for(Island island : islandsByLocation.values()){
                        try {
                            handler.saveObject(island);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } 
                    }
                }
            });
        } else {
            for(Island island : islandsByLocation.values()){
                try {
                    handler.saveObject(island);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } 
            }
        }
    }

    public void shutdown(){
        save(false);
        islandsByLocation.clear();
        islandsByUUID.clear();
    }

    public int getCount(){
        return islandsByLocation.size();
    }

    public boolean isIsland(Location location){
        return islandsByLocation.get(location) != null;
    }

    public Island getIsland(Location location){
        return islandsByLocation.get(location);
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
     * Create an island with owner. Note this does not create the schematic. It just reates the island data object.
     * @param location
     * @param owner UUID
     */
    public Island createIsland(Location location, UUID owner){
        Island island = new Island(location, owner, Settings.islandProtectionRange);
        islandsByLocation.put(location, island);
        if (owner != null)
            islandsByUUID.put(owner, island);
        return island;
    }

    public void deleteIsland(Location location){
        if (islandsByLocation.containsKey(location)) {
            Island island = islandsByLocation.get(location);
            if (island.getOwner() != null) {
                islandsByUUID.remove(island.getOwner());
            }
            islandsByLocation.remove(location);
        }
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

    /**
     * This teleports player to their island. If not safe place can be found
     * then the player is sent to spawn via /spawn command
     * 
     * @param player
     * @return true if the home teleport is successful
     */
    public boolean homeTeleport(final Player player) {
        return homeTeleport(player, 1);
    }
    /**
     * Teleport player to a home location. If one cannot be found a search is done to
     * find a safe place.
     * @param player
     * @param number - home location to do to
     * @return true if successful, false if not
     */
    public boolean homeTeleport(final Player player, int number) {
        Location home = null;
        //plugin.getLogger().info("home teleport called for #" + number);
        home = getSafeHomeLocation(player.getUniqueId(), number);
        //plugin.getLogger().info("home get safe loc = " + home);
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
            //plugin.getLogger().info("Fixing home location using safe spot teleport");
            // Try to fix this teleport location and teleport the player if possible
            new SafeSpotTeleport(plugin, player, plugin.getPlayers().getHomeLocation(player.getUniqueId(), number), number);
            return true;
        }
        //plugin.getLogger().info("DEBUG: home loc = " + home + " teleporting");
        //home.getChunk().load();
        player.teleport(home);
        //player.sendBlockChange(home, Material.GLOWSTONE, (byte)0);
        if (number ==1 ) {
            Util.sendMessage(player, ChatColor.GREEN + plugin.getLocale(player.getUniqueId()).get("island.teleport").replace("[label]", Settings.ISLANDCOMMAND));
        } else {
            Util.sendMessage(player, ChatColor.GREEN + "teleported to #" + number);
        }
        return true;

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
        //plugin.getLogger().info("DEBUG: Home location " + l);
        if (l != null) {
            // Homes are stored as integers and need correcting to be more central
            if (isSafeLocation(l)) {
                return l.clone().add(new Vector(0.5D,0,0.5D));
            }
            // To cover slabs, stairs and other half blocks, try one block above
            Location lPlusOne = l.clone();
            lPlusOne.add(new Vector(0, 1, 0));
            if (lPlusOne != null) {
                if (isSafeLocation(lPlusOne)) {
                    // Adjust the home location accordingly
                    plugin.getPlayers().setHomeLocation(playerUUID, lPlusOne, number);
                    return lPlusOne.clone().add(new Vector(0.5D,0,0.5D));
                }
            }
        }

        //plugin.getLogger().info("DEBUG: Home location either isn't safe, or does not exist so try the island");
        // Home location either isn't safe, or does not exist so try the island
        // location
        if (plugin.getPlayers().inTeam(playerUUID)) {
            l = plugin.getIslands().getIslandLocation(playerUUID);
            if (isSafeLocation(l)) {
                plugin.getPlayers().setHomeLocation(playerUUID, l, number);
                return l.clone().add(new Vector(0.5D,0,0.5D));
            } else {
                // try team leader's home
                Location tlh = plugin.getPlayers().getHomeLocation(plugin.getIslands().getTeamLeader(playerUUID));
                if (tlh != null) {
                    if (isSafeLocation(tlh)) {
                        plugin.getPlayers().setHomeLocation(playerUUID, tlh, number);
                        return tlh.clone().add(new Vector(0.5D,0,0.5D));
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
            plugin.getLogger().warning(plugin.getPlayers().getName(playerUUID) + " player has no island!");
            return null;
        }
        //plugin.getLogger().info("DEBUG: If these island locations are not safe, then we need to get creative");
        // If these island locations are not safe, then we need to get creative
        // Try the default location
        //plugin.getLogger().info("DEBUG: default");
        Location dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 2.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try just above the bedrock
        //plugin.getLogger().info("DEBUG: above bedrock");
        dl = new Location(l.getWorld(), l.getX() + 0.5D, l.getY() + 5D, l.getZ() + 0.5D, 0F, 30F);
        if (isSafeLocation(dl)) {
            plugin.getPlayers().setHomeLocation(playerUUID, dl, number);
            return dl;
        }
        // Try all the way up to the sky
        //plugin.getLogger().info("DEBUG: try all the way to the sky");
        for (int y = l.getBlockY(); y < 255; y++) {
            final Location n = new Location(l.getWorld(), l.getX() + 0.5D, y, l.getZ() + 0.5D);
            if (isSafeLocation(n)) {
                plugin.getPlayers().setHomeLocation(playerUUID, n, number);
                return n;
            }
        }
        //plugin.getLogger().info("DEBUG: unsuccessful");
        // Unsuccessful
        return null;
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
            Island island = getIslandAt(l);
            if (island == null) {
                return null;
            }
            i = island.getProtectionRange();
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
     * Checks if this location is safe for a player to teleport to. Used by
     * warps and boat exits Unsafe is any liquid or air and also if there's no
     * space
     * 
     * @param l
     *            - Location to be checked
     * @return true if safe, otherwise false
     */
    public static boolean isSafeLocation(final Location l) {
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
        if (ground.isLiquid() || space1.isLiquid() || space2.isLiquid()) {
            // Check if acid has no damage
            if (Settings.acidDamage > 0D) {
                // Bukkit.getLogger().info("DEBUG: acid");
                return false;
            } else if (ground.getType().equals(Material.STATIONARY_LAVA) || ground.getType().equals(Material.LAVA)
                    || space1.getType().equals(Material.STATIONARY_LAVA) || space1.getType().equals(Material.LAVA)
                    || space2.getType().equals(Material.STATIONARY_LAVA) || space2.getType().equals(Material.LAVA)) {
                // Lava check only
                // Bukkit.getLogger().info("DEBUG: lava");
                return false;
            }
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
    
    /**
     * Makes an island using schematic. No permission checks are made. They have to be decided
     * before this method is called.
     * @param player
     * @param schematic
     */
    public void newIsland(final Player player, final Schematic schematic) {
        //long time = System.nanoTime();
        final UUID playerUUID = player.getUniqueId();
        boolean firstTime = false;
        if (!plugin.getPlayers().hasIsland(playerUUID)) {
            firstTime = true;
        }
        //plugin.getLogger().info("DEBUG: finding island location");
        Location next = getNextIsland(player.getUniqueId());
        //plugin.getLogger().info("DEBUG: found " + next);
        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(playerUUID);

        // Set the biome
        //BiomesPanel.setIslandBiome(next, schematic.getBiome());
        // Teleport to the new home
        if (schematic.isPlayerSpawn()) {
            // Set home and teleport
            plugin.getPlayers().setHomeLocation(playerUUID, schematic.getPlayerSpawn(next), 1);
        }

        // Create island based on schematic
        if (schematic != null) {
            //plugin.getLogger().info("DEBUG: pasting schematic " + schematic.getName() + " " + schematic.getPerm());
            //plugin.getLogger().info("DEBUG: nether world is " + BSkyBlock.getNetherWorld());
            // Paste the starting island. If it is a HELL biome, then we start in the Nether
            if (Settings.createNether && schematic.isInNether() && Settings.islandNether && IslandWorld.getNetherWorld() != null) {
                // Nether start
                // Paste the overworld if it exists
                if (!schematic.getPartnerName().isEmpty()) {
                    // A partner schematic is available
                    pastePartner(plugin.getSchematics().getSchematic(schematic.getPartnerName()),next, player);
                }
                // Switch home location to the Nether
                next = next.toVector().toLocation(IslandWorld.getNetherWorld());
                // Set the player's island location to this new spot
                //plugin.getPlayers().setIslandLocation(playerUUID, next);
                schematic.pasteSchematic(next, player, true, firstTime ? PasteReason.NEW_ISLAND: PasteReason.RESET);
            } else {
                // Over world start
                //plugin.getLogger().info("DEBUG: pasting");
                //long timer = System.nanoTime();
                // Paste the island and teleport the player home
                schematic.pasteSchematic(next, player, true, firstTime ? PasteReason.NEW_ISLAND: PasteReason.RESET);
                //double diff = (System.nanoTime() - timer)/1000000;
                //plugin.getLogger().info("DEBUG: nano time = " + diff + " ms");
                //plugin.getLogger().info("DEBUG: pasted overworld");
                if (Settings.createNether && Settings.islandNether && IslandWorld.getNetherWorld() != null) {
                    // Paste the other world schematic
                    final Location netherLoc = next.toVector().toLocation(IslandWorld.getNetherWorld());
                    if (schematic.getPartnerName().isEmpty()) {
                        // This will paste the over world schematic again
                        //plugin.getLogger().info("DEBUG: pasting nether");
                        pastePartner(schematic, netherLoc, player);
                        //plugin.getLogger().info("DEBUG: pasted nether");
                    } else {
                        if (plugin.getSchematics().getAll().containsKey(schematic.getPartnerName())) {
                            //plugin.getLogger().info("DEBUG: pasting partner");
                            // A partner schematic is available
                            pastePartner(plugin.getSchematics().getAll().get(schematic.getPartnerName()),netherLoc, player);
                        } else {
                            plugin.getLogger().severe("Partner schematic heading '" + schematic.getPartnerName() + "' does not exist");
                        }
                    }
                }
            }
        } 
        // Add to the grid
        Island myIsland = plugin.getIslands().createIsland(next, playerUUID);
        myIsland.setLevelHandicap(schematic.getLevelHandicap());
        // Save the player so that if the server is reset weird things won't happen
        plugin.getPlayers().save(true);

        // Start the reset cooldown
        //if (!firstTime) {
        //    setResetWaitTime(player);
        //}
        // Set the custom protection range if appropriate
        // Dynamic island range sizes with permissions
        int range = Settings.islandProtectionRange;        
        for (PermissionAttachmentInfo perms : player.getEffectivePermissions()) {
            if (perms.getPermission().startsWith(Settings.PERMPREFIX + "island.range.")) {
                if (perms.getPermission().contains(Settings.PERMPREFIX + "island.range.*")) {
                    range = Settings.islandProtectionRange;
                    break;
                } else {
                    String[] spl = perms.getPermission().split(Settings.PERMPREFIX + "island.range.");
                    if (spl.length > 1) {
                        if (!NumberUtils.isDigits(spl[1])) {
                            plugin.getLogger().severe("Player " + player.getName() + " has permission: " + perms.getPermission() + " <-- the last part MUST be a number! Ignoring...");
                        } else {
                            range = Math.max(range, Integer.valueOf(spl[1]));
                        }
                    }
                }
            }
        }
        // Do some sanity checking
        if (range % 2 != 0) {
            range--;
            plugin.getLogger().warning("Protection range must be even, using " + range + " for " + player.getName());
        }
        if (range > Settings.islandDistance) {
            plugin.getLogger().warning("Player has " + Settings.PERMPREFIX + "island.range." + range);
            range = Settings.islandDistance;
            plugin.getLogger().warning(
                    "Island protection range must be " + Settings.islandDistance + " or less. Setting to: " + range);
        }
        myIsland.setProtectionRange(range);

        // Save grid just in case there's a crash
        plugin.getIslands().save(true);
        // Done - fire event
        //final IslandNewEvent event = new IslandNewEvent(player,schematic, myIsland);
        //plugin.getServer().getPluginManager().callEvent(event);
        //plugin.getLogger().info("DEBUG: Done! " + (System.nanoTime()- time) * 0.000001);
    }

    /**
     * Does a delayed pasting of the partner island
     * @param schematic
     * @param player
     */
    private void pastePartner(final Schematic schematic, final Location loc, final Player player) {
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {

            @Override
            public void run() {
                schematic.pasteSchematic(loc, player, false, PasteReason.PARTNER);

            }}, 60L);

    }

    /**
     * Get the location of next free island spot
     * @param playerUUID
     * @return Location of island spot
     */
    private Location getNextIsland(UUID playerUUID) {
        // Find the next free spot
        if (last == null) {
            last = new Location(IslandWorld.getIslandWorld(), Settings.islandXOffset + Settings.islandStartX, Settings.islandHeight, Settings.islandZOffset + Settings.islandStartZ);
        }
        Location next = last.clone();

        while (plugin.getIslands().isIsland(next)) {
            next = nextGridLocation(next);
        }
        // Make the last next, last
        last = next.clone();
        return next;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     * 
     * @param lastIsland
     * @return Location of next free island
     */
    private Location nextGridLocation(final Location lastIsland) {
        // plugin.getLogger().info("DEBUG nextIslandLocation");
        final int x = lastIsland.getBlockX();
        final int z = lastIsland.getBlockZ();
        final Location nextPos = lastIsland;
        if (x < z) {
            if (-1 * x < z) {
                nextPos.setX(nextPos.getX() + Settings.islandDistance);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
            return nextPos;
        }
        if (x > z) {
            if (-1 * x >= z) {
                nextPos.setX(nextPos.getX() - Settings.islandDistance);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
            return nextPos;
        }
        if (x <= 0) {
            nextPos.setZ(nextPos.getZ() + Settings.islandDistance);
            return nextPos;
        }
        nextPos.setZ(nextPos.getZ() - Settings.islandDistance);
        return nextPos;
    }

}
