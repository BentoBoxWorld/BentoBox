package us.tastybento.bskyblock.generators;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;

public class IslandWorld {

    //private BSkyBlock plugin;
    private static World islandWorld;
    private static World netherWorld;
    private static World endWorld;

    /**
     * Returns the World object for the island world named in config.yml.
     * If the world does not exist then it is created.
     *
     * @return Bukkit World object for the BSkyBlock overworld
     */
    public IslandWorld(BSkyBlock plugin) {
        if (Settings.useOwnGenerator) {
            // Do nothing
            return;
        }
        if (plugin.getServer().getWorld(Settings.worldName) == null) {
            Bukkit.getLogger().info("Creating " + plugin.getName() + "'s Island World...");
        }
        // Create the world if it does not exist
        islandWorld = WorldCreator.name(Settings.worldName).type(WorldType.FLAT).environment(World.Environment.NORMAL).generator(new ChunkGeneratorWorld())
                .createWorld();
        // Make the nether if it does not exist
        if (Settings.netherGenerate) {
            if (plugin.getServer().getWorld(Settings.worldName + "_nether") == null) {
                Bukkit.getLogger().info("Creating " + plugin.getName() + "'s Nether...");
            }
            if (!Settings.netherIslands) {
                netherWorld = WorldCreator.name(Settings.worldName + "_nether").type(WorldType.NORMAL).environment(World.Environment.NETHER).createWorld();
            } else {
                netherWorld = WorldCreator.name(Settings.worldName + "_nether").type(WorldType.FLAT).generator(new ChunkGeneratorWorld())
                        .environment(World.Environment.NETHER).createWorld();
            }
        }
        // Make the end if it does not exist
        if (Settings.endGenerate) {
            if (plugin.getServer().getWorld(Settings.worldName + "_the_end") == null) {
                Bukkit.getLogger().info("Creating " + plugin.getName() + "'s End World...");
            }
            if (!Settings.endIslands) {
                endWorld = WorldCreator.name(Settings.worldName + "_the_end").type(WorldType.NORMAL).environment(World.Environment.THE_END).createWorld();
            } else {
                endWorld = WorldCreator.name(Settings.worldName + "_the_end").type(WorldType.FLAT).generator(new ChunkGeneratorWorld())
                        .environment(World.Environment.THE_END).createWorld();
            }
        }
        fixMultiverse(plugin);
    }

    private void fixMultiverse(BSkyBlock plugin) {
        // Multiverse configuration
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            Bukkit.getLogger().info("Trying to register generator with Multiverse ");
            try {
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                        "mv import " + Settings.worldName + " normal -g " + plugin.getName());
                if (!Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                        "mv modify set generator " + plugin.getName() + " " + Settings.worldName)) {
                    Bukkit.getLogger().severe("Multiverse is out of date! - Upgrade to latest version!");
                }
                if (netherWorld != null && Settings.netherGenerate && Settings.netherIslands) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            "mv import " + Settings.worldName + "_nether nether -g " + plugin.getName());
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            "mv modify set generator " + plugin.getName() + " " + Settings.worldName + "_nether");
                }
                if (endWorld != null && Settings.endGenerate && Settings.endIslands) {
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            "mv import " + Settings.worldName + "_the_end end -g " + plugin.getName());
                    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                            "mv modify set generator " + plugin.getName() + " " + Settings.worldName + "_the_end");
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Not successfull! Disabling " + plugin.getName() + "!");
                e.printStackTrace();
                Bukkit.getServer().getPluginManager().disablePlugin(plugin);
            }
        }

        
    }

    /**
     * @return the islandWorld
     */
    public static World getIslandWorld() {
        if (Settings.useOwnGenerator) {
            return Bukkit.getServer().getWorld(Settings.worldName);
        }
        return islandWorld;
    }

    /**
     * @return the netherWorld
     */
    public static World getNetherWorld() {
        if (Settings.useOwnGenerator) {
            return Bukkit.getServer().getWorld(Settings.worldName + "_nether");
        }
        return netherWorld;
    }

    /**
     * @return the endWorld
     */
    public static World getEndWorld() {
        if (Settings.useOwnGenerator) {
            return Bukkit.getServer().getWorld(Settings.worldName + "_the_end");
        }
        return endWorld;
    }


}
