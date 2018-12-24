package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.schems.Clipboard;

public class SchemsManager {

    private BentoBox plugin;
    private Map<World, Map<String, Clipboard>> islandSchems;

    /**
     * @param plugin - plugin
     */
    public SchemsManager(BentoBox plugin) {
        this.plugin = plugin;
        islandSchems = new HashMap<>();
    }

    private void copySchems(Addon addon, File schems) {

        if (schems.exists()) {
            // If the folder exists, do not copy anything from the jar
            return;
        }
        if (!schems.exists() && !schems.mkdirs()) {
            plugin.logError("Could not make schems folder!");
            return;
        }
        // Save any schems that
        try (JarFile jar = new JarFile(addon.getFile())) {
            plugin.getAddonsManager().listJarFiles(jar, "schems", ".schem").forEach(name -> {
                addon.saveResource("schems/" + name, false);
            });
        } catch (IOException e) {
            plugin.logError("Could not load schem files from addon jar " + e.getMessage());
        }
    }

    /**
     * Get all the schems for this world
     * @param world world
     * @return map of schems for this world or an empty map if there are none registered
     */
    public Map<String, Clipboard> get(World world) {
        return islandSchems.getOrDefault(world, new HashMap<>());
    }

    /**
     * Load schems for addon. Will try and load nether and end schems too if settings are set.
     * @param world - world
     */
    public void loadIslands(World world) {
        plugin.getIWM().getAddon(world).ifPresent(addon -> {
            File schems = new File(addon.getDataFolder(), "schems");
            // Copy any schems fould in the jar
            copySchems(addon, schems);
            // Load all schems in folder
            // Look through the folder
            FilenameFilter schemFilter = (File dir, String name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(".schem")
                    && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("nether-")
                    && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("end-");
            Arrays.stream(Objects.requireNonNull(schems.list(schemFilter))).map(name -> name.substring(0, name.length() - 6)).forEach(name -> {
                if (!plugin.getSchemsManager().loadSchem(world, schems, name)) {
                    plugin.logError("Could not load " + name + ".schem for " + plugin.getIWM().getFriendlyName(world));
                }
                if (plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world)
                        && !plugin.getSchemsManager().loadSchem(plugin.getIWM().getNetherWorld(world), schems, "nether-" + name)) {
                    plugin.logError("Could not load nether-" + name + ".schem for " + plugin.getIWM().getFriendlyName(world));
                }
                if (plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world)
                        && !plugin.getSchemsManager().loadSchem(plugin.getIWM().getEndWorld(world), schems, "end-" + name)) {
                    plugin.logError("Could not load end-" + name + ".schem for " + plugin.getIWM().getFriendlyName(world));
                }
            });


        });
    }

    private boolean loadSchem(World world, File schems, String name) {
        plugin.log("Loading " + name + ".schem for " + world.getName());
        Map<String, Clipboard> schemList = islandSchems.getOrDefault(world, new HashMap<>());
        try {
            Clipboard cb = new Clipboard(plugin, schems);
            cb.load(name);
            schemList.put(name, cb);
            islandSchems.put(world, schemList);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.logError("Could not load " + name + " schem, skipping!");
            return false;
        }
        return true;
    }

    /**
     * Paste the schem to world for island
     * @param world - world
     * @param island - island
     * @param name - file name of schematic (without the .schem suffix)
     */
    public void paste(World world, Island island, String name) {
        paste(world, island, name, null);

    }

    /**
     * Paste the schem for world to the island center location and run task afterwards
     * @param world - world to paste to
     * @param island - the island who owns this schem
     * @param name - file name of schematic (without the .schem suffix)
     * @param task - task to run after pasting is completed
     */
    public void paste(World world, Island island, String name, Runnable task) {
        if (islandSchems.containsKey(world) && islandSchems.get(world).containsKey(name)) {
            islandSchems.get(world).get(name).pasteIsland(world, island, task);
        } else {
            plugin.logError("Tried to paste schem '" + name + "' for " + world.getName() + " but the schem is not loaded!");
            plugin.log("This might be due to an invalid schem format. Keep in mind that schems are not schematics.");
        }
    }


}
