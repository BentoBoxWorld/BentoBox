package us.tastybento.bskyblock.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.generators.ChunkGeneratorWorld;

public class IslandWorldManager {

    private static final String MULTIVERSE_SET_GENERATOR = "mv modify set generator ";
    private static final String MULTIVERSE_IMPORT = "mv import ";
    private static final String NETHER = "_nether";
    private static final String THE_END = "_the_end";
    private static final String CREATING = "Creating ";

    private BSkyBlock plugin;
    private World islandWorld;
    private World netherWorld;
    private World endWorld;
    private Map<World, String> worlds;

    /**
     * Generates the Skyblock worlds.
     */
    public IslandWorldManager(BSkyBlock plugin) {
        this.plugin = plugin;
        worlds = new HashMap<>();
        if (plugin.getSettings().isUseOwnGenerator()) {
            // Do nothing
            return;
        }
        if (plugin.getServer().getWorld(plugin.getSettings().getWorldName()) == null) {
            plugin.log(CREATING + plugin.getName() + "'s Island World...");
        }
        // Create the world if it does not exist
        islandWorld = WorldCreator.name(plugin.getSettings().getWorldName()).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(new ChunkGeneratorWorld(plugin))
                .createWorld();
        addWorld("bsb", islandWorld);
        // Make the nether if it does not exist
        if (plugin.getSettings().isNetherGenerate()) {
            if (plugin.getServer().getWorld(plugin.getSettings().getWorldName() + NETHER) == null) {
                plugin.log(CREATING + plugin.getName() + "'s Nether...");
            }
            if (!plugin.getSettings().isNetherIslands()) {
                netherWorld = WorldCreator.name(plugin.getSettings().getWorldName() + NETHER).type(WorldType.NORMAL).environment(World.Environment.NETHER).createWorld();
            } else {
                netherWorld = WorldCreator.name(plugin.getSettings().getWorldName() + NETHER).type(WorldType.FLAT).generator(new ChunkGeneratorWorld(plugin))
                        .environment(World.Environment.NETHER).createWorld();
            }
            addWorld("bsb_nether", netherWorld);
        }
        // Make the end if it does not exist
        if (plugin.getSettings().isEndGenerate()) {
            if (plugin.getServer().getWorld(plugin.getSettings().getWorldName() + THE_END) == null) {
                plugin.log(CREATING + plugin.getName() + "'s End World...");
            }
            if (!plugin.getSettings().isEndIslands()) {
                endWorld = WorldCreator.name(plugin.getSettings().getWorldName() + THE_END).type(WorldType.NORMAL).environment(World.Environment.THE_END).createWorld();
            } else {
                endWorld = WorldCreator.name(plugin.getSettings().getWorldName() + THE_END).type(WorldType.FLAT).generator(new ChunkGeneratorWorld(plugin))
                        .environment(World.Environment.THE_END).createWorld();
            }
            addWorld("bsb_end", endWorld);
        }
    }

    private void multiverseReg(World world) {
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
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
     * @return the islandWorld
     */
    public World getIslandWorld() {
        if (plugin.getSettings().isUseOwnGenerator()) {
            return Bukkit.getServer().getWorld(plugin.getSettings().getWorldName());
        }
        return islandWorld;
    }

    /**
     * @return the netherWorld
     */
    public World getNetherWorld() {
        if (plugin.getSettings().isUseOwnGenerator()) {
            return Bukkit.getServer().getWorld(plugin.getSettings().getWorldName() + NETHER);
        }
        return netherWorld;
    }

    /**
     * @return the endWorld
     */
    public World getEndWorld() {
        if (plugin.getSettings().isUseOwnGenerator()) {
            return Bukkit.getServer().getWorld(plugin.getSettings().getWorldName() + THE_END);
        }
        return endWorld;
    }

    /**
     * Checks if a location is in any of the island worlds
     * @param loc - location
     * @return true if in a world or false if not
     */
    public boolean inWorld(Location loc) {
        return worlds.containsKey(loc.getWorld());
    }

    /**
     * @return Set of over worlds
     */
    public Set<World> getOverWorlds() {
        return worlds.keySet().stream().filter(w -> w.getEnvironment().equals(Environment.NORMAL)).collect(Collectors.toSet());
    }

    /**
     * Get friendly names of all the over worlds
     * @return Set of world names
     */
    public Set<String> getOverWorldNames() {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL)).map(w -> w.getValue()).collect(Collectors.toSet());
    }

    /**
     * Get a set of world names that user does not already have an island in
     * @param user - user
     * @return set of world names, or empty set
     */
    public Set<String> getFreeOverWorldNames(User user) {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL))
                .filter(w -> !plugin.getIslands().hasIsland(w.getKey(), user))
                .map(w -> w.getValue()).collect(Collectors.toSet());
    }
    
    /**
     * Check if a name is a known friendly world name, ignores case
     * @param name - world name
     * @return true if name is a known world name
     */
    public boolean isOverWorld(String name) {
        return worlds.entrySet().stream().filter(e -> e.getKey().getEnvironment().equals(Environment.NORMAL)).anyMatch(w -> w.getValue().equalsIgnoreCase(name));
    }

    /**
     * Add world to the list of known worlds along with a friendly name that will be used in commands
     * @param friendlyName - string
     * @param world - world
     */
    public void addWorld(String friendlyName, World world) {
        plugin.log("Adding world " + friendlyName);
        worlds.put(world, friendlyName);
        multiverseReg(world);
    }

    /**
     * Get the world based on friendly name.
     * @param friendlyName - friendly name of world
     * @return world, or null if it does not exist
     */
    public World getWorld(String friendlyName) {
        return worlds.entrySet().stream().filter(n -> n.getValue().equalsIgnoreCase(friendlyName)).map(e -> e.getKey()).findFirst().orElse(null);
    }

}
