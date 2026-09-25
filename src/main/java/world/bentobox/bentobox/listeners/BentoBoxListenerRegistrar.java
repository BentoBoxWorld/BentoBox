package world.bentobox.bentobox.listeners;

import org.bukkit.plugin.PluginManager;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.protection.CushionListener;
import world.bentobox.bentobox.listeners.teleports.EntityTeleportListener;
import world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener;
import world.bentobox.bentobox.managers.ChunkPregenManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.suggestions.DidYouMeanListener;

/**
 * Registers all BentoBox event listeners with the Bukkit plugin manager.
 * Extracted from BentoBox to keep the main class focused on lifecycle coordination.
 */
public class BentoBoxListenerRegistrar {

    private final BentoBox plugin;
    private IslandDeletionManager islandDeletionManager;
    private ChunkPregenManager chunkPregenManager;

    public BentoBoxListenerRegistrar(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers all listeners. Must be called after worlds and managers are initialised.
     */
    public void register() {
        PluginManager manager = plugin.getServer().getPluginManager();
        manager.registerEvents(new JoinLeaveListener(plugin), plugin);
        manager.registerEvents(new PanelListenerManager(), plugin);
        manager.registerEvents(new StandardSpawnProtectionListener(plugin), plugin);
        manager.registerEvents(new PlayerTeleportListener(plugin), plugin);
        manager.registerEvents(new EntityTeleportListener(plugin), plugin);
        manager.registerEvents(new BlockEndDragon(plugin), plugin);
        manager.registerEvents(new BannedCommands(plugin), plugin);
        manager.registerEvents(new DeathListener(plugin), plugin);
        manager.registerEvents(new DidYouMeanListener(plugin), plugin);
        // Register the plugin itself for any listeners it implements (e.g. MV unregister)
        manager.registerEvents(plugin, plugin);
        islandDeletionManager = new IslandDeletionManager(plugin);
        chunkPregenManager = new ChunkPregenManager(plugin);
        manager.registerEvents(chunkPregenManager, plugin);
        manager.registerEvents(new PrimaryIslandListener(plugin), plugin);
        // Cushions (26.3) - the listener's method signatures name 26.3-only classes, so the JVM
        // cannot even verify it on an older server. Only touch the class when the API is there.
        if (hasCushionApi()) {
            manager.registerEvents(new CushionListener(), plugin);
        }
    }

    /**
     * @return true if the running server API has the Minecraft 26.3 cushion entity and Paper's
     *         entity-break event, i.e. {@link CushionListener} can be loaded.
     * @since 3.23.1
     */
    static boolean hasCushionApi() {
        return classExists("org.bukkit.entity.Cushion")
                && classExists("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, BentoBoxListenerRegistrar.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * @return the {@link IslandDeletionManager} created during registration.
     */
    public IslandDeletionManager getIslandDeletionManager() {
        return islandDeletionManager;
    }

    /**
     * @return the {@link ChunkPregenManager} created during registration.
     * @since 3.15.0
     */
    public ChunkPregenManager getChunkPregenManager() {
        return chunkPregenManager;
    }
}
