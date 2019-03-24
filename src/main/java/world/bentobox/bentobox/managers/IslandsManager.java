package world.bentobox.bentobox.managers;

import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeSpecies;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.DeleteIslandChunks;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The job of this class is manage all island related data.
 * It also handles island ownership, including team, trustees, coops, etc.
 * The data object that it uses is Island
 * @author tastybento
 */
public class IslandsManager {

    private BentoBox plugin;

    // Tree species to boat material map
    private static final Map<TreeSpecies, Material> TREE_TO_BOAT = ImmutableMap.<TreeSpecies, Material>builder().
            put(TreeSpecies.ACACIA, Material.ACACIA_BOAT).
            put(TreeSpecies.BIRCH, Material.BIRCH_BOAT).
            put(TreeSpecies.DARK_OAK, Material.DARK_OAK_BOAT).
            put(TreeSpecies.JUNGLE, Material.JUNGLE_BOAT).
            put(TreeSpecies.GENERIC, Material.OAK_BOAT).
            put(TreeSpecies.REDWOOD, Material.SPRUCE_BOAT).build();

    /**
     * One island can be spawn, this is the one - otherwise, this value is null
     */
    @NonNull
    private Map<@NonNull World, @Nullable Island> spawn;

    @NonNull
    private Database<Island> handler;

    /**
     * The last locations where an island were put.
     * This is not stored persistently and resets when the server starts
     */
    private Map<World, Location> last;

    // Island Cache
    @NonNull
    private IslandCache islandCache;
    // Quarantined islands
    @NonNull
    private Map<UUID, List<Island>> quarantineCache;
    // Deleted islands
    @NonNull
    private List<String> deletedIslands;

    /**
     * Islands Manager
     * @param plugin - plugin
     */
    public IslandsManager(BentoBox plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Island classes
        handler = new Database<>(plugin, Island.class);
        islandCache = new IslandCache();
        quarantineCache = new HashMap<>();
        spawn = new HashMap<>();
        last = new HashMap<>();
        // This list should always be empty unless database deletion failed
        // In that case a purge utility may be required in the future
        deletedIslands = new ArrayList<>();
    }

    /**
     * This is a generic scan that can work in the overworld or the nether
     * @param l - location around which to scan
     * @param i - the range to scan for a location less than 0 means the full island.
     * @return - safe location, or null if none can be found
     */
    @Nullable
    public Location bigScan(@NonNull Location l, int i) {
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
     * @param l Location to be checked, not null.
     * @return true if safe, otherwise false
     */
    public boolean isSafeLocation(@NonNull Location l) {
        if (l.getWorld() == null) {
            return false;
        }
        Block ground = l.getBlock().getRelative(BlockFace.DOWN);
        Block space1 = l.getBlock();
        Block space2 = l.getBlock().getRelative(BlockFace.UP);

        // Ground must be solid
        if (!ground.getType().isSolid()) {
            return false;
        }
        // Cannot be submerged or water cannot be dangerous
        if (space1.isLiquid() && (space2.isLiquid() || plugin.getIWM().isWaterNotSafe(l.getWorld()))) {
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
        // The isSolid function is not fully accurate (yet) so we have to check a few other items
        // isSolid thinks that PLATEs and SIGNS are solid, but they are not
        return (!space1.getType().isSolid() || space1.getType().equals(Material.SIGN) || space1.getType().equals(Material.WALL_SIGN)) && (!space2.getType().isSolid() || space2.getType().equals(Material.SIGN) || space2.getType().equals(Material.WALL_SIGN));
    }

    /**
     * Create an island with no owner at location
     * @param location the location, not null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(Location location){
        return createIsland(location, null);
    }

    /**
     * Create an island with owner. Note this does not create the schematic. It just creates the island data object.
     * @param location the location, not null
     * @param owner the island owner UUID, may be null
     * @return Island or null if the island could not be created for some reason
     */
    @Nullable
    public Island createIsland(@NonNull Location location, @Nullable UUID owner) {
        Island island = new Island(location, owner, plugin.getIWM().getIslandProtectionRange(location.getWorld()));
        // Game the gamemode name and prefix the uniqueId
        String gmName = plugin.getIWM().getAddon(location.getWorld()).map(gm -> gm.getDescription().getName()).orElse("");
        island.setUniqueId(gmName + island.getUniqueId());
        while (handler.objectExists(island.getUniqueId())) {
            // This should never happen, so although this is a potential infinite loop I'm going to leave it here because
            // it will be bad if this does occur and the server should crash.
            plugin.logWarning("Duplicate island UUID occurred");
            island.setUniqueId(gmName + UUID.randomUUID().toString());
        }
        if (islandCache.addIsland(island)) {
            return island;
        }
        return null;
    }

    /**
     * Deletes island.
     * @param island island to delete, not null
     * @param removeBlocks whether the island blocks should be removed or not
     */
    public void deleteIsland(@NonNull Island island, boolean removeBlocks) {
        // Fire event
        IslandBaseEvent event = IslandEvent.builder().island(island).reason(Reason.DELETE).build();
        if (event.isCancelled()) {
            return;
        }
        // Set the owner of the island to no one.
        island.setOwner(null);
        island.setFlag(Flags.LOCK, RanksManager.VISITOR_RANK);
        if (removeBlocks) {
            // Remove island from the cache
            islandCache.deleteIslandFromCache(island);
            // Log the deletion (it shouldn't matter but may be useful)
            island.log(new LogEntry.Builder("DELETED").build());
            // Set the delete flag which will prevent it from being loaded even if database deletion fails
            island.setDeleted(true);
            // Save the island
            handler.saveObject(island);
            // Delete the island
            handler.deleteObject(island);
            // Remove players from island
            removePlayersFromIsland(island);
            // Remove blocks from world
            new DeleteIslandChunks(plugin, new IslandDeletion(island));
        }
    }

    public int getIslandCount() {
        return islandCache.size();
    }

    public int getIslandCount(World world) {
        return islandCache.size(world);
    }

    /**
     * Gets the island for this player.
     * If they are in a team, the team island is returned.
     * @param world world to check
     * @param user user
     * @return Island or null
     */
    @Nullable
    public Island getIsland(World world, User user){
        return islandCache.get(world, user.getUniqueId());
    }

    /**
     * Gets the island for this player. If they are in a team, the team island is returned.
     * @param world world to check. Includes nether and end worlds.
     * @param uuid user's uuid
     * @return Island or null
     */
    public Island getIsland(World world, UUID uuid){
        return islandCache.get(world, uuid);
    }

    /**
     * Returns the island at the location or Optional empty if there is none.
     * This includes the full island space, not just the protected area.
     * Use {@link #getProtectedIslandAt(Location)} for only the protected island space.
     *
     * @param location - the location
     * @return Optional Island object
     */
    public Optional<Island> getIslandAt(Location location) {
        // If this is not an Island World or a standard Nether or End, skip
        if (!plugin.getIWM().inWorld(location)
                || (plugin.getIWM().isNether(location.getWorld()) && !plugin.getIWM().isNetherIslands(location.getWorld()))
                || (plugin.getIWM().isEnd(location.getWorld()) && !plugin.getIWM().isEndIslands(location.getWorld()))
                ) {
            return Optional.empty();
        }
        // Do not return an island if there is no nether or end or islands in them
        if ((location.getWorld().getEnvironment().equals(World.Environment.NETHER) &&
                (!plugin.getIWM().isNetherGenerate(location.getWorld()) || !plugin.getIWM().isNetherIslands(location.getWorld())))
                || (location.getWorld().getEnvironment().equals(World.Environment.THE_END) &&
                        (!plugin.getIWM().isEndGenerate(location.getWorld()) || !plugin.getIWM().isEndIslands(location.getWorld())))) {
            return Optional.empty();
        }
        return Optional.ofNullable(islandCache.getIslandAt(location));
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the islands (even those who may be unowned).
     * @return unmodifiable collection containing every island.
     * @since 1.1
     */
    @NonNull
    public Collection<Island> getIslands() {
        return islandCache.getIslands();
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
        Island island = getIsland(world, uuid);
        return island != null ? island.getCenter() : null;
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
                // try owner's home
                Location tlh = plugin.getPlayers().getHomeLocation(world, plugin.getIslands().getOwner(world, user.getUniqueId()));
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
     * Gets the island that is defined as spawn in this world
     * @param world world
     * @return optional island, may be empty
     */
    @NonNull
    public Optional<Island> getSpawn(@NonNull World world){
        return Optional.ofNullable(spawn.get(world));
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
     * Provides UUID of this player's island owner or null if it does not exist
     * @param world world to check
     * @param playerUUID the player's UUID
     * @return island owner's UUID or null if player has no island
     */
    @Nullable
    public UUID getOwner(@NonNull World world, @NonNull UUID playerUUID) {
        return islandCache.getOwner(world, playerUUID);
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
     * Checks if a player has an island in the world and owns it
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
                player.getInventory().addItem(new ItemStack(TREE_TO_BOAT.getOrDefault(((Boat) boat).getWoodType(), Material.OAK_BOAT)));
                player.updateInventory();
            }
        }
        if (home == null) {
            // Try to fix this teleport location and teleport the player if possible
            new SafeSpotTeleport.Builder(plugin)
            .entity(player)
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
                plugin.getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
            }
        }
    }

    /**
     * Teleports the player to the spawn location for this world
     * @param world world
     * @param player player to teleport
     * @since 1.1
     */
    public void spawnTeleport(@NonNull World world, @NonNull Player player) {
        User user = User.getInstance(player);
        // If there's no spawn island or the spawn location is null for some reason, then error
        Location spawnTo = getSpawn(world).map(i -> i.getSpawnPoint(World.Environment.NORMAL) == null ? i.getCenter() : i.getSpawnPoint(World.Environment.NORMAL))
                .orElse(null);
        if (spawnTo == null) {
            // There is no spawn here.
            user.sendMessage("commands.island.spawn.no-spawn");
        } else {
            // Teleport the player to the spawn
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

            user.sendMessage("commands.island.spawn.teleporting");
            // Safe teleport
            new SafeSpotTeleport.Builder(plugin).entity(player).location(spawnTo).build();
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
     * Sets an Island to be the spawn of its World. It will become an unowned Island.
     * <br/>
     * If there was already a spawn set for this World, it will no longer be the spawn but it will remain unowned.
     * @param spawn the Island to set as spawn.
     *              Must not be null.
     */
    public void setSpawn(@NonNull Island spawn) {
        // Checking if there is already a spawn set for this world
        if (this.spawn.containsKey(spawn.getWorld()) && this.spawn.get(spawn.getWorld()) != null) {
            Island oldSpawn = this.spawn.get(spawn.getWorld());
            if (oldSpawn.equals(spawn)) {
                return; // The spawn is already the current spawn - no need to update anything.
            } else {
                oldSpawn.setSpawn(false);
            }
        }
        this.spawn.put(spawn.getWorld(), spawn);
        spawn.setSpawn(true);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if the player is the owner of their island.
     */
    public boolean isOwner(@NonNull World world, @NonNull UUID uniqueId) {
        return hasIsland(world, uniqueId) && uniqueId.equals(getIsland(world, uniqueId).getOwner());
    }

    /**
     * Clear and reload all islands from database
     */
    public void load(){
        islandCache.clear();
        quarantineCache.clear();
        List<Island> toQuarantine = new ArrayList<>();
        // Attempt to load islands
        handler.loadObjects().stream().forEach(island -> {
            if (island.isDeleted()) {
                // These will be deleted later
                deletedIslands.add(island.getUniqueId());
            } else if (island.isDoNotLoad() && island.getWorld() != null && island.getCenter() != null) {
                // Add to quarantine cache
                quarantineCache.computeIfAbsent(island.getOwner(), k -> new ArrayList<>()).add(island);
            } else {
                // Fix island center if it is off
                fixIslandCenter(island);
                if (!islandCache.addIsland(island)) {
                    // Quarantine the offending island
                    toQuarantine.add(island);
                    // Add to quarantine cache
                    island.setDoNotLoad(true);
                    quarantineCache.computeIfAbsent(island.getOwner(), k -> new ArrayList<>()).add(island);
                } else if (island.isSpawn()) {
                    // Success, set spawn if this is the spawn island.
                    this.setSpawn(island);
                }
            }
        });
        if (!toQuarantine.isEmpty()) {
            plugin.logError(toQuarantine.size() + " islands could not be loaded successfully; moving to trash bin.");
            toQuarantine.forEach(handler::saveObject);
        }
    }

    /**
     * Island coordinates should always be a multiple of the island distance x 2. If they are not, this method
     * realigns the grid coordinates.
     * @param island - island
     * @since 1.3.0
     */
    public void fixIslandCenter(Island island) {
        World world = island.getWorld();
        if (world == null || island.getCenter() == null || !plugin.getIWM().inWorld(world)) {
            return;
        }
        int distance = island.getRange() * 2;
        long x = ((long) island.getCenter().getBlockX()) - plugin.getIWM().getIslandXOffset(world);
        long z = ((long) island.getCenter().getBlockZ()) - plugin.getIWM().getIslandZOffset(world);
        if (x % distance != 0 || z % distance != 0) {
            // Island is off grid
            x = Math.round((double) x / distance) * distance + plugin.getIWM().getIslandXOffset(world);
            z = Math.round((double) z / distance) * distance + plugin.getIWM().getIslandZOffset(world);
        }
        island.setCenter(new Location(world, x, island.getCenter().getBlockY(), z));
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
     * Checks if an online player is in the protected area of an island he owns or he is part of. i.e. rank is > VISITOR_RANK
     *
     * @param world the World to check. Typically this is the user's world. Does not check nether or end worlds. If null the method will always return {@code false}.
     * @param user the User to check, if null or if this is not a Player the method will always return {@code false}.
     *
     * @return {@code true} if this User is located within the protected area of an island he owns or he is part of,
     *          {@code false} otherwise or if this User is not located in this World.
     */
    public boolean userIsOnIsland(World world, User user) {
        if (user == null || !user.isPlayer() || world == null) {
            return false;
        }
        return (user.getLocation().getWorld() == world)
                && getProtectedIslandAt(user.getLocation())
                .map(i -> i.getMembers().entrySet().stream()
                        .anyMatch(en -> en.getKey().equals(user.getUniqueId()) && en.getValue() > RanksManager.VISITOR_RANK))
                .orElse(false);
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param user - user
     */
    public void removePlayer(World world, User user) {
        removePlayer(world, user.getUniqueId());
    }

    /**
     * Removes this player from any and all islands in world
     * @param world - world
     * @param uuid - user's uuid
     */
    public void removePlayer(World world, UUID uuid) {
        Island island = islandCache.removePlayer(world, uuid);
        if (island != null) {
            handler.saveObject(island);
        }
    }

    /**
     * This teleports players away from an island - used when reseting or deleting an island
     * @param island to remove players from
     */
    public void removePlayersFromIsland(Island island) {
        World w = island.getWorld();
        Bukkit.getOnlinePlayers().stream()
        .filter(p -> p.getGameMode().equals(plugin.getIWM().getDefaultGameMode(island.getWorld())))
        .filter(p -> island.onIsland(p.getLocation())).forEach(p -> {
            // Teleport island players to their island home
            if (!island.getMemberSet().contains(p.getUniqueId()) && (hasIsland(w, p.getUniqueId()) || inTeam(w, p.getUniqueId()))) {
                homeTeleport(w, p);
            } else {
                // Move player to spawn
                if (spawn.containsKey(w)) {
                    // go to island spawn
                    p.teleport(spawn.get(w).getSpawnPoint(w.getEnvironment()));
                } else {
                    plugin.logWarning("During island deletion player " + p.getName() + " could not be sent home so was placed into spectator mode.");
                    p.setGameMode(GameMode.SPECTATOR);
                    p.setFlying(true);
                }
            }
        });
    }

    /**
     * Save the all the islands to the database
     */
    public void saveAll(){
        Collection<Island> collection = islandCache.getIslands();
        for(Island island : collection){
            try {
                handler.saveObject(island);
            } catch (Exception e) {
                plugin.logError("Could not save island to database when running sync! " + e.getMessage());
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
        // Save the island
        handler.saveObject(teamIsland);
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
        // Remove all coop associations
        islandCache.getIslands().stream().forEach(i -> i.getMembers().values().removeIf(p -> p == RanksManager.COOP_RANK));
        saveAll();
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
     * Sets this target as the owner for this island
     * @param world world
     * @param user the user who is issuing the command
     * @param targetUUID the current island member who is going to become the new owner
     */
    public void setOwner(World world, User user, UUID targetUUID) {
        setOwner(user, targetUUID, getIsland(world, targetUUID));
    }

    /**
     * Sets this target as the owner for this island
     * @param user requester
     * @param targetUUID new owner
     * @param island island to register
     */
    public void setOwner(User user, UUID targetUUID, Island island) {
        islandCache.setOwner(island, targetUUID);
        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]", plugin.getPlayers().getName(targetUUID));
        plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon -> {
            User target = User.getInstance(targetUUID);
            // Tell target. If they are offline, then they may receive a message when they login
            target.sendMessage("commands.island.team.setowner.you-are-the-owner");
            // Permission checks for range changes only work when the target is online
            if (target.isOnline()) {
                // Check if new owner has a different range permission than the island size
                int range = target.getPermissionValue(
                        addon.getPermissionPrefix() + "island.range",
                        plugin.getIWM().getIslandProtectionRange(Util.getWorld(island.getWorld())));
                // Range can go up or down
                if (range != island.getProtectionRange()) {
                    user.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                    target.sendMessage("commands.admin.setrange.range-updated", TextVariables.NUMBER, String.valueOf(range));
                    plugin.log("Setowner: Island protection range changed from " + island.getProtectionRange() + " to "
                            + range + " for " + user.getName() + " due to permission.");
                }
                island.setProtectionRange(range);
            }
        });
    }

    /**
     * Clear an area of mobs as per world rules. Radius is 5 blocks in every direction.
     * @param loc - location to clear
     */
    public void clearArea(Location loc) {
        loc.getWorld().getNearbyEntities(loc, 5D, 5D, 5D).stream()
        .filter(en -> Util.isHostileEntity(en)
                && !plugin.getIWM().getRemoveMobsWhitelist(loc.getWorld()).contains(en.getType())
                && !(en instanceof PufferFish))
        .forEach(Entity::remove);
    }

    /**
     * Removes a player from any island where they hold the indicated rank.
     * Typically this is to remove temporary ranks such as coop.
     * Removal is done in all worlds.
     * @param rank - rank to clear
     * @param uniqueId - UUID of player
     */
    public void clearRank(int rank, UUID uniqueId) {
        islandCache.getIslands().stream().forEach(i -> i.getMembers().entrySet().removeIf(e -> e.getKey().equals(uniqueId) && e.getValue() == rank));
    }

    /**
     * Save the island to the database
     * @param island - island
     */
    public void save(Island island) {
        handler.saveObject(island);
    }

    /**
     * Try to get an island by its unique id
     * @param uniqueId - unique id string
     * @return optional island
     * @since 1.3.0
     */
    @NonNull
    public Optional<Island> getIslandById(String uniqueId) {
        return Optional.ofNullable(islandCache.getIslandById(uniqueId));
    }

    /**
     * Try to get a list of quarantined islands owned by uuid in this world
     *
     * @param world - world
     * @param uuid - target player's UUID, or <tt>null</tt> = unowned islands
     * @return list of islands; may be empty
     * @since 1.3.0
     */
    @NonNull
    public List<Island> getQuarantinedIslandByUser(@NonNull World world, @Nullable UUID uuid) {
        return quarantineCache.getOrDefault(uuid, Collections.emptyList()).stream()
                .filter(i -> i.getWorld().equals(world)).collect(Collectors.toList());
    }

    /**
     * Delete quarantined islands owned by uuid in this world
     *
     * @param world - world
     * @param uuid - target player's UUID, or <tt>null</tt> = unowned islands
     * @since 1.3.0
     */
    public void deleteQuarantinedIslandByUser(World world, @Nullable UUID uuid) {
        if (quarantineCache.containsKey(uuid)) {
            quarantineCache.get(uuid).stream().filter(i -> i.getWorld().equals(world))
            .forEach(i -> handler.deleteObject(i));
            quarantineCache.get(uuid).removeIf(i -> i.getWorld().equals(world));
        }
    }

    /**
     * @return the quarantineCache
     * @since 1.3.0
     */
    @NonNull
    public Map<UUID, List<Island>> getQuarantineCache() {
        return quarantineCache;
    }

    /**
     * Remove a quarantined island and delete it from the database completely.
     * This is NOT recoverable unless you have database backups.
     * @param island island
     * @return {@code true} if island is quarantined and removed
     * @since 1.3.0
     */
    public boolean purgeQuarantinedIsland(Island island) {
        if (quarantineCache.containsKey(island.getOwner()) && quarantineCache.get(island.getOwner()).remove(island)) {
            handler.deleteObject(island);
            return true;
        }
        return false;
    }

    /**
     * Switches active island and island in trash
     * @param world  - game world
     * @param target - target player's UUID
     * @param island - island in trash
     * @return <tt>true</tt> if successful, otherwise <tt>false</tt>
     * @since 1.3.0
     */
    public boolean switchIsland(World world, UUID target, Island island) {
        // Remove trashed island from trash
        if (!quarantineCache.containsKey(island.getOwner()) || !quarantineCache.get(island.getOwner()).remove(island)) {
            plugin.logError("Could not remove island from trash");
            return false;
        }
        // Remove old island from cache if it exists
        if (this.hasIsland(world, target)) {
            Island oldIsland = islandCache.get(world, target);
            islandCache.removeIsland(oldIsland);

            // Set old island to trash
            oldIsland.setDoNotLoad(true);

            // Put old island into trash
            quarantineCache.computeIfAbsent(target, k -> new ArrayList<>()).add(oldIsland);
            // Save old island
            if (!handler.saveObject(oldIsland)) {
                plugin.logError("Could not save trashed island in database");
                return false;
            }
        }
        // Restore island from trash
        island.setDoNotLoad(false);
        // Add new island to cache
        if (!islandCache.addIsland(island)) {
            plugin.logError("Could not add recovered island to cache");
            return false;
        }
        // Save new island
        if (!handler.saveObject(island)) {
            plugin.logError("Could not save recovered island to database");
            return false;
        }
        return true;
    }

    /**
     * Resets all flags to gamemode config.yml default
     * @param world - world
     * @since 1.3.0
     */
    public void resetAllFlags(World world) {
        islandCache.resetAllFlags(world);
        this.saveAll();
    }
}
