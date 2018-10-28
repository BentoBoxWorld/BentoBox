package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.schems.Clipboard;

public class SchemsManager {

    private BentoBox plugin;
    private Map<World, Clipboard> islandSchems;

    private static final String SCHEM = ".schem";

    /**
     * @param plugin - plugin
     */
    public SchemsManager(BentoBox plugin) {
        this.plugin = plugin;
        islandSchems = new HashMap<>();
    }

    /**
     * @param schems - schems folder for either the addon or the plugin
     * @param world - world
     * @param name - name of the schem to save (excluding .schem)
     */
    private void copySchems(File schems, World world, String subFolder, String name) {
        if (!schems.exists() && !schems.mkdirs()) {
            plugin.logError("Could not make schems folder!");
            return;
        }
        File schem = new File(schems, name + SCHEM);
        if (schem.exists()) {
            // No overwriting
            return;
        }
        Optional<Addon> addon = plugin.getIWM().getAddon(world);
        if (addon.isPresent()) {
            addon.get().saveResource("schems/" + subFolder + "/" + name + SCHEM, false);
        } else {
            plugin.saveResource("schems/" + subFolder + "/"  + name + SCHEM, false);
        }
    }

    public Clipboard get(World world) {
        return islandSchems.get(world);
    }

    /**
     * Load schems for world. Will try and load nether and end schems too if settings are set.
     * @param world - world
     */
    public void loadIslands(World world, String subFolder) {
        if (!loadSchem(world, subFolder, "island")) {
            plugin.logError("Could not load island.schem for " + plugin.getIWM().getFriendlyName(world));
        }
        if (plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world)
                && !loadSchem(plugin.getIWM().getNetherWorld(world), subFolder, "nether-island")) {
            plugin.logError("Could not load nether-island.schem for " + plugin.getIWM().getFriendlyName(world));
        }
        if (plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world)
                && !loadSchem(plugin.getIWM().getEndWorld(world), subFolder, "end-island")) {
            plugin.logError("Could not load end-island.schem for " + plugin.getIWM().getFriendlyName(world));
        }
    }

    private boolean loadSchem(World world, String subFolder, String name) {
        plugin.log("Loading " + subFolder + "/" + name + ".schem for " + world.getName());
        File schems = new File(plugin.getIWM().getDataFolder(world), "schems");
        File subSchems = new File(schems, subFolder);
        copySchems(subSchems, world, subFolder, name);
        try {
            Clipboard cb = new Clipboard(plugin, subSchems);
            cb.load(name);
            islandSchems.put(world, cb);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.logError("Could not load " + name + " schem in folder " + subFolder);
            return false;
        }
        return true;
    }

    /**
     * Paste the schem for world to the island center location and run task afterwards
     * @param world - world to paste to
     * @param island - the island who owns this schem
     * @param task - task to run after pasting is completed
     */
    public void paste(World world, Island island, Runnable task) {
        if (islandSchems.containsKey(world)) {
            islandSchems.get(world).pasteIsland(world, island, task);
        } else {
            plugin.logError("Tried to paste schem for " + world.getName() + " but it is not loaded!");
        }
    }

    /**
     * Paste the schem to world for island
     * @param world - world
     * @param island - island
     */
    public void paste(World world, Island island) {
        paste(world, island, null);

    }

}
