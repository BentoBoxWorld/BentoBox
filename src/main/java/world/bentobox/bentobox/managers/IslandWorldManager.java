package world.bentobox.bentobox.managers;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.EntityType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Handles registration and management of worlds
 *
 * @author tastybento
 *
 */
public class IslandWorldManager {

    private static final String MULTIVERSE_SET_GENERATOR = "mv modify set generator ";
    private static final String MULTIVERSE_IMPORT = "mv import ";
    private static final String NETHER = "_nether";
    private static final String THE_END = "_the_end";

    private BentoBox plugin;
    private Map<World, String> worlds;
    private Map<World, WorldSettings> worldSettings;

    /**
     * Generates the Skyblock worlds.
     */
    public IslandWorldManager(BentoBox plugin) {
        this.plugin = plugin;
        worlds = new HashMap<>();
        worldSettings = new HashMap<>();
    }

    /**
     * Registers a world with Multiverse if the plugin exists
     *
     * @param world
     *            - world
     */
    private void multiverseReg(World world) {
        if (!isUseOwnGenerator(world) && Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Bukkit.getScheduler().runTask(plugin,
                    () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            MULTIVERSE_IMPORT + world.getName() + " normal -g " + plugin.getName()));
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                        MULTIVERSE_SET_GENERATOR + plugin.getName() + " " + world.getName())) {
                    plugin.logError("Multiverse is out of date! - Upgrade to latest version!");
                }
            });
        }
    }

    /**
     * Checks if a location is in any of the island worlds
     *
     * @param loc
     *            - location
     * @return true if in a world or false if not
     */
    public boolean inWorld(Location loc) {
        return worlds.containsKey(Util.getWorld(loc.getWorld()));
    }

    /**
     * @return List of over worlds
     */
    public List<World> getOverWorlds() {
        return worlds.keySet().stream().filter(w -> w.getEnvironment().equals(Environment.NORMAL))
                .collect(Collectors.toList());
    }

    /**
     * Get friendly names of all the over worlds
     *
     * @return Set of world names
     */
    public Set<String> getOverWorldNames() {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL))
                .map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    /**
     * Get a set of world names that user does not already have an island in
     *
     * @param user
     *            - user
     * @return set of world names, or empty set
     */
    public Set<String> getFreeOverWorldNames(User user) {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL))
                .filter(w -> !plugin.getIslands().hasIsland(w.getKey(), user)).map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a name is a known friendly world name, ignores case
     *
     * @param name
     *            - world name
     * @return true if name is a known world name
     */
    public boolean isOverWorld(String name) {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL))
                .anyMatch(w -> w.getValue().equalsIgnoreCase(name));
    }

    /**
     * Add world to the list of known worlds along with a friendly name that will be
     * used in commands
     *
     * @param world - world
     */
    public void addWorld(World world, WorldSettings settings) {
        String friendlyName = settings.getFriendlyName().isEmpty() ? world.getName() : settings.getFriendlyName();
        worlds.put(world, friendlyName);
        worldSettings.put(world, settings);
        // Call Multiverse
        multiverseReg(world);
        // Set default island settings
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.PROTECTION))
        .forEach(f -> settings.getDefaultIslandFlags().putIfAbsent(f, f.getDefaultRank()));
        Flags.values().stream().filter(f -> f.getType().equals(Flag.Type.SETTING))
        .forEach(f -> settings.getDefaultIslandSettings().putIfAbsent(f, f.getDefaultRank()));
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Set world difficulty
            Difficulty diff = settings.getDifficulty();
            if (diff == null) {
                diff = Difficulty.NORMAL;
                settings.setDifficulty(diff);
            }
            world.setDifficulty(diff);

            // Handle nether and end difficulty levels
            if (settings.isNetherGenerate()) {
                this.getNetherWorld(world).setDifficulty(diff);
            }
            if (settings.isEndGenerate()) {
                this.getEndWorld(world).setDifficulty(diff);
            }
            plugin.log("Added world " + friendlyName + " (" + world.getDifficulty() + ")");
        });

    }

    /**
     * Get the settings for this world or sub-worlds (nether, end)
     *
     * @param world
     *            - world
     * @return world settings, or null if world is unknown
     */
    public WorldSettings getWorldSettings(World world) {
        return worldSettings.get(Util.getWorld(world));
    }

    /**
     * Get the world based on friendly name.
     *
     * @param friendlyName
     *            - friendly name of world
     * @return world, or null if it does not exist
     */
    public World getWorld(String friendlyName) {
        return worlds.entrySet().stream().filter(n -> n.getValue().equalsIgnoreCase(friendlyName))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    /**
     * @return the islandDistance
     */
    public int getIslandDistance(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandDistance();
    }

    /**
     * @return the islandHeight
     */
    public int getIslandHeight(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandHeight();
    }

    /**
     * @return the islandProtectionRange
     */
    public int getIslandProtectionRange(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandProtectionRange();
    }

    /**
     * @return the islandStartX
     */
    public int getIslandStartX(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandStartX();
    }

    /**
     * @return the islandStartZ
     */
    public int getIslandStartZ(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandStartZ();
    }

    /**
     * @return the islandXOffset
     */
    public int getIslandXOffset(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandXOffset();
    }

    /**
     * @return the islandZOffset
     */
    public int getIslandZOffset(World world) {
        return worldSettings.get(Util.getWorld(world)).getIslandZOffset();
    }

    /**
     * @return the maxIslands
     */
    public int getMaxIslands(World world) {
        return worldSettings.get(Util.getWorld(world)).getMaxIslands();
    }

    /**
     * @return the netherSpawnRadius
     */
    public int getNetherSpawnRadius(World world) {
        return worldSettings.get(Util.getWorld(world)).getNetherSpawnRadius();
    }

    /**
     * @return the seaHeight
     */
    public int getSeaHeight(World world) {
        return worldSettings.get(Util.getWorld(world)).getSeaHeight();
    }

    /**
     * @return the worldName
     */
    public String getWorldName(World world) {
        return worldSettings.get(Util.getWorld(world)).getWorldName();
    }

    /**
     * @return the endGenerate
     */
    public boolean isEndGenerate(World world) {
        return worldSettings.get(Util.getWorld(world)).isEndGenerate();
    }

    /**
     * @return the endIslands
     */
    public boolean isEndIslands(World world) {
        return worldSettings.get(Util.getWorld(world)).isEndIslands();
    }

    /**
     * @return the netherGenerate
     */
    public boolean isNetherGenerate(World world) {
        return worldSettings.get(Util.getWorld(world)).isNetherGenerate();
    }

    /**
     * @return the netherIslands
     */
    public boolean isNetherIslands(World world) {
        return worldSettings.get(Util.getWorld(world)).isNetherIslands();
    }

    /**
     * Checks if a world is a known nether world
     *
     * @param world
     *            - world
     * @return true if world is a known and valid nether world
     */
    public boolean isNether(World world) {
        World w = Util.getWorld(world);
        return worldSettings.containsKey(w) && worldSettings.get(w).isNetherGenerate();
    }

    /**
     * Checks if a world is a known island nether world
     *
     * @param world
     *            - world
     * @return true if world is a known and valid nether world
     */
    public boolean isIslandNether(World world) {
        World w = Util.getWorld(world);
        return worldSettings.containsKey(w) && worldSettings.get(w).isNetherGenerate()
                && worldSettings.get(w).isNetherIslands();
    }

    /**
     * Checks if a world is a known end world
     *
     * @param world
     *            - world
     * @return true if world is a known and valid nether world
     */
    public boolean isEnd(World world) {
        World w = Util.getWorld(world);
        return worldSettings.containsKey(w) && worldSettings.get(w).isEndGenerate();
    }

    /**
     * Checks if a world is a known island end world
     *
     * @param world
     *            - world
     * @return true if world is a known and valid nether world
     */
    public boolean isIslandEnd(World world) {
        World w = Util.getWorld(world);
        return worldSettings.containsKey(w) && worldSettings.get(w).isEndGenerate()
                && worldSettings.get(w).isEndIslands();
    }

    /**
     * Get the nether world of this overWorld
     *
     * @param overWorld
     *            - overworld
     * @return nether world, or null if it does not exist
     */
    public World getNetherWorld(World overWorld) {
        return Bukkit.getWorld(overWorld.getName() + NETHER);
    }

    /**
     * Get the end world of this overWorld
     *
     * @param overWorld
     *            - overworld
     * @return end world, or null if it does not exist
     */
    public World getEndWorld(World overWorld) {
        return Bukkit.getWorld(overWorld.getName() + THE_END);
    }

    /**
     * Check if nether trees should be created in the nether or not
     *
     * @param world
     *            - world
     * @return true or false
     */
    public boolean isNetherTrees(World world) {
        World w = Util.getWorld(world);
        return worldSettings.containsKey(w) && worldSettings.get(w).isNetherTrees();
    }

    /**
     * Whether the End Dragon can spawn or not in this world
     *
     * @param world
     *            - world
     * @return true (default) if it can spawn or not
     */
    public boolean isDragonSpawn(World world) {
        World w = Util.getWorld(world);
        return !worldSettings.containsKey(w) || worldSettings.get(w).isDragonSpawn();
    }

    /**
     * @return a comma separated string of friendly world names
     */
    public String getFriendlyNames() {
        StringBuilder r = new StringBuilder();
        worlds.values().forEach(n -> r.append(n).append(", "));
        if (r.length() > 0) {
            r.setLength(r.length() - 2);
        }
        return r.toString();
    }

    /**
     * Gets world from friendly name
     *
     * @param friendlyWorldName
     *            - friendly world name. Used for commands.
     * @return world, or null if not known
     */
    public World getIslandWorld(String friendlyWorldName) {
        return worlds.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(friendlyWorldName)).findFirst()
                .map(Map.Entry::getKey).orElse(null);
    }

    /**
     * Get max team size for this world
     *
     * @param world
     *            - world
     * @return max team size
     */
    public int getMaxTeamSize(World world) {
        return worldSettings.get(Util.getWorld(world)).getMaxTeamSize();
    }

    /**
     * Get max homes for world
     *
     * @param world
     *            - world
     * @return max homes
     */
    public int getMaxHomes(World world) {
        return worldSettings.get(Util.getWorld(world)).getMaxHomes();
    }

    /**
     * Get the friendly name for world or related nether or end
     *
     * @param world
     *            - world
     * @return Friendly name
     */
    public String getFriendlyName(World world) {
        return worldSettings.get(Util.getWorld(world)).getFriendlyName();
    }

    /**
     * Get the permission prefix for this world. No trailing dot included.
     *
     * @param world
     *            - world
     * @return permission prefix for this world
     */
    public String getPermissionPrefix(World world) {
        return worldSettings.get(Util.getWorld(world)).getPermissionPrefix();

    }

    /**
     * Get the invincible visitor settings for this world
     *
     * @param world
     *            - world
     * @return invincible visitor settings
     */
    public List<String> getIvSettings(World world) {
        return worldSettings.get(Util.getWorld(world)).getIvSettings();
    }

    /**
     * Returns whether a world flag is set or not Same result as calling
     * {@link Flag#isSetForWorld(World)}
     *
     * @param world
     *            - world
     * @param flag
     *            - world setting flag
     * @return true or false
     */
    public boolean isWorldFlag(World world, Flag flag) {
        return flag.isSetForWorld(world);
    }

    /**
     * Get the default game mode for this world.
     *
     * @param world
     *            - world
     * @return GameMode: SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
     */
    public GameMode getDefaultGameMode(World world) {
        return worldSettings.get(Util.getWorld(world)).getDefaultGameMode();
    }

    /**
     * Get the set of entity types not to remove when player teleports to island
     *
     * @param world
     *            - world
     * @return - set of entity types
     */
    public Set<EntityType> getRemoveMobsWhitelist(World world) {
        return worldSettings.get(Util.getWorld(world)).getRemoveMobsWhitelist();
    }

    /**
     * @return the onJoinResetMoney
     */
    public boolean isOnJoinResetMoney(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnJoinResetMoney();
    }

    /**
     * @return the onJoinResetInventory
     */
    public boolean isOnJoinResetInventory(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnJoinResetInventory();
    }

    /**
     * @return the onJoinResetEnderChest
     */
    public boolean isOnJoinResetEnderChest(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnJoinResetEnderChest();
    }

    /**
     * @return the onLeaveResetMoney
     */
    public boolean isOnLeaveResetMoney(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnLeaveResetMoney();
    }

    /**
     * @return the onLeaveResetInventory
     */
    public boolean isOnLeaveResetInventory(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnLeaveResetInventory();
    }

    /**
     * @return the onLeaveResetEnderChest
     */
    public boolean isOnLeaveResetEnderChest(World world) {
        return worldSettings.get(Util.getWorld(world)).isOnLeaveResetEnderChest();
    }

    /**
     * Get data folder for the addon that registered this world
     *
     * @return data folder file object or the plugin's data folder if none found
     */
    public File getDataFolder(World world) {
        return worldSettings.get(Util.getWorld(world)).getAddon().map(Addon::getDataFolder)
                .orElse(plugin.getDataFolder());
    }

    /**
     * Get the addon associated with this world set
     *
     * @param world
     *            - world
     * @return Addon, or empty
     */
    public Optional<Addon> getAddon(World world) {
        return worldSettings.get(Util.getWorld(world)).getAddon();
    }

    /**
     * Get default island flag settings for this world.
     *
     * @param world
     *            - world
     * @return default rank settings for new islands.
     */
    public Map<Flag, Integer> getDefaultIslandFlags(World world) {
        return worldSettings.get(Util.getWorld(world)).getDefaultIslandFlags();
    }

    public List<String> getVisibleSettings(World world) {
        return worldSettings.get(Util.getWorld(world)).getVisibleSettings();
    }

    /**
     * Return island setting defaults for world
     *
     * @param world
     *            - world
     * @return default settings for new islands
     */
    public Map<Flag, Integer> getDefaultIslandSettings(World world) {
        return worldSettings.get(Util.getWorld(world)).getDefaultIslandSettings();
    }

    public boolean isUseOwnGenerator(World world) {
        return worldSettings.get(Util.getWorld(world)).isUseOwnGenerator();
    }

    /**
     * Return banned commands for visitors
     * @return the visitorbannedcommands
     */
    public List<String> getVisitorBannedCommands(World world) {
        return worldSettings.get(Util.getWorld(world)).getVisitorBannedCommands();
    }

    /**
     * Check if water is not safe, e.g., it is acid, in the world
     * @param world - world
     * @return true if water is not safe, e.g.for home locations
     */
    public boolean isWaterNotSafe(World world) {
        return worldSettings.get(Util.getWorld(world)).isWaterUnsafe();
    }

    /**
     * Get a list of entity types that should not exit the island limits
     * @param world - world
     * @return list
     */
    public List<String> getGeoLimitSettings(World world) {
        return worldSettings.get(Util.getWorld(world)).getGeoLimitSettings();
    }

    /**
     * Get the reset limit for this world
     * @param world - world
     * @return number of resets allowed. -1 = unlimited
     */
    public int getResetLimit(World world) {
        return worlds.containsKey(Util.getWorld(world)) ? worldSettings.get(Util.getWorld(world)).getResetLimit() : -1;
    }


    /**
     * Gets the time stamp for when all player resets were zeroed
     * @param world - world
     */
    public long getResetEpoch(World world) {
        return worldSettings.get(Util.getWorld(world)).getResetEpoch();
    }

    /**
     * Sets the time stamp for when all player resets were zeroed
     * @param world - world
     */
    public void setResetEpoch(World world) {
        worldSettings.get(Util.getWorld(world)).setResetEpoch(System.currentTimeMillis());
    }

    public boolean isTeamJoinDeathReset(World world) {
        return worldSettings.get(Util.getWorld(world)).isTeamJoinDeathReset();
    }

    /**
     * Get the maximum number of deaths allowed in this world
     * @param world - world
     * @return max deaths
     */
    public int getDeathsMax(World world) {
        return worldSettings.get(Util.getWorld(world)).getDeathsMax();
    }

}
