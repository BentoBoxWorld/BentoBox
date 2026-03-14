package world.bentobox.bentobox.hooks;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.hooks.placeholders.PlaceholderAPIHook;
import world.bentobox.bentobox.managers.HooksManager;

/**
 * Registers all built-in BentoBox hooks with the {@link HooksManager}.
 * Extracted from BentoBox to keep the main class focused on lifecycle coordination.
 */
public class BentoBoxHookRegistrar {

    private static final String MV5_CLASS = "org.mvplugins.multiverse.core.MultiverseCore";
    private static final String MV4_CLASS = "com.onarandombox.MultiverseCore.MultiverseCore";

    private final BentoBox plugin;
    private final HooksManager hooksManager;

    public BentoBoxHookRegistrar(BentoBox plugin) {
        this.plugin = plugin;
        this.hooksManager = plugin.getHooks();
    }

    /**
     * Registers hooks that must be available early in the enable sequence,
     * before addons are enabled and before PlaceholdersManager is created.
     */
    public void registerEarlyHooks() {
        hooksManager.registerHook(new MultipaperHook());
        hooksManager.registerHook(new VaultHook());
        hooksManager.registerHook(new FancyNpcsHook());
        hooksManager.registerHook(new ZNPCsPlusHook());
        hooksManager.registerHook(new MythicMobsHook());
        hooksManager.registerHook(new PlaceholderAPIHook());
    }

    /**
     * Registers world-manager hooks (Multiverse variants) that load after BentoBox worlds.
     * Must be called after {@code islandWorldManager.registerWorldsToMultiverse()} is ready.
     */
    public void registerWorldHooks() {
        if (hasClass(MV5_CLASS)) {
            hooksManager.registerHook(new MultiverseCore5Hook());
        } else if (hasClass(MV4_CLASS)) {
            hooksManager.registerHook(new MultiverseCore4Hook());
        }
        hooksManager.registerHook(new MyWorldsHook());
    }

    /**
     * Registers hooks for plugins that load after BentoBox (Slimefun, ItemsAdder, Oraxen, BlueMap).
     */
    public void registerLateHooks() {
        hooksManager.registerHook(new SlimefunHook());
        hooksManager.registerHook(new ItemsAdderHook(plugin));
        hooksManager.registerHook(new OraxenHook(plugin));
        hooksManager.registerHook(new BlueMapHook());
    }

    private boolean hasClass(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
