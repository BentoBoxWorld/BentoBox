package world.bentobox.bentobox.listeners;

import org.bukkit.plugin.PluginManager;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.teleports.EntityTeleportListener;
import world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener;
import world.bentobox.bentobox.managers.ChunkPregenManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;

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
        // Register the plugin itself for any listeners it implements (e.g. MV unregister)
        manager.registerEvents(plugin, plugin);
        islandDeletionManager = new IslandDeletionManager(plugin);
        manager.registerEvents(islandDeletionManager, plugin);
        chunkPregenManager = new ChunkPregenManager(plugin);
        manager.registerEvents(chunkPregenManager, plugin);
        manager.registerEvents(new PrimaryIslandListener(plugin), plugin);
    }

    /**
     * @return the {@link IslandDeletionManager} created during registration.
     */
    public IslandDeletionManager getIslandDeletionManager() {
        return islandDeletionManager;
    }

    /**
     * @return the {@link ChunkPregenManager} created during registration.
     * @since 3.14.0
     */
    public ChunkPregenManager getChunkPregenManager() {
        return chunkPregenManager;
    }
}
