package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.jar.JarFile;

import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.schems.Clipboard;
import world.bentobox.bentobox.util.Util;

public class SchemsManager {

    /**
     * Name of the schem that is expected to be the default one.
     * @since 1.2.0
     */
    public static final @NonNull String DEFAULT_SCHEM_NAME = "island";

    /**
     * File extension of the schems file.
     * @since 1.2.0
     */
    public static final @NonNull String FILE_EXTENSION = ".schem";

    /**
     * Name of the folder containing schems.
     * @since 1.2.0
     */
    public static final @NonNull String FOLDER_NAME = "schems";

    private BentoBox plugin;
    private Map<World, Map<String, Clipboard>> islandSchems;

    /**
     * @param plugin - plugin
     */
    public SchemsManager(BentoBox plugin) {
        this.plugin = plugin;
        islandSchems = new HashMap<>();
    }

    private void copySchems(Addon addon, File folder) {
        if (folder.exists()) {
            // If the folder exists, do not copy anything from the jar
            return;
        }
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.logError("Could not make '" + FOLDER_NAME + "' folder!");
            return;
        }
        // Save any schems that are in the jar
        try (JarFile jar = new JarFile(addon.getFile())) {
            Util.listJarFiles(jar, FOLDER_NAME, FILE_EXTENSION).forEach(name -> addon.saveResource(name, false));
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
        return islandSchems.getOrDefault(world, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Load schems for addon. Will try and load nether and end schems too if settings are set.
     * @param addon - GameModeAddon
     */
    public void loadIslands(GameModeAddon addon) {
        File schems = new File(addon.getDataFolder(), FOLDER_NAME);
        // Copy any schems fould in the jar
        copySchems(addon, schems);
        // Load all schems in folder
        // Look through the folder
        FilenameFilter schemFilter = (File dir, String name) -> name.toLowerCase(java.util.Locale.ENGLISH).endsWith(FILE_EXTENSION)
                && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("nether-")
                && !name.toLowerCase(java.util.Locale.ENGLISH).startsWith("end-");
        Arrays.stream(Objects.requireNonNull(schems.list(schemFilter))).map(name -> name.substring(0, name.length() - 6)).forEach(name -> {
            if (!plugin.getSchemsManager().loadSchem(addon.getOverWorld(), schems, name)) {
                plugin.logError("Could not load " + name + ".schem for " + addon.getWorldSettings().getFriendlyName());
            }
            if (addon.getWorldSettings().isNetherGenerate() && addon.getWorldSettings().isNetherIslands()
                    && !plugin.getSchemsManager().loadSchem(addon.getNetherWorld(), schems, "nether-" + name)) {
                plugin.logError("Could not load nether-" + name + ".schem for " + addon.getWorldSettings().getFriendlyName());
            }
            if (addon.getWorldSettings().isEndGenerate() && addon.getWorldSettings().isEndIslands()
                    && !plugin.getSchemsManager().loadSchem(addon.getEndWorld(), schems, "end-" + name)) {
                plugin.logError("Could not load end-" + name + ".schem for " + addon.getWorldSettings().getFriendlyName());
            }
        });
    }

    private boolean loadSchem(World world, File schems, String name) {
        plugin.log("Loading " + name + ".schem for " + world.getName());
        Map<String, Clipboard> schemList = islandSchems.getOrDefault(world, new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
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
