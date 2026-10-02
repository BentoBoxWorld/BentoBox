package world.bentobox.bentobox.hooks;

import org.bukkit.Material;
import org.bukkit.World;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides implementation and interfacing to interact with Multiverse.
 *
 * @author Poslovitch
 */
public class MultiverseCore5Hook extends Hook implements WorldManagementHook {

    public MultiverseCore5Hook() {
        super("Multiverse-Core", Material.COMPASS);
    }

    /**
     * Register the world with Multiverse
     * @param world - world to register
     * @param islandWorld - if true, then this is an island world
     */
    @Override
    public void registerWorld(World world, boolean islandWorld) {
        if (world == null) {
            return;
        }
        MultiverseCoreApi api = MultiverseCoreApi.get();
        String generator = islandWorld ? getGenerator(world) : null;
        // Let BentoBox handle loading on startup by default. Auto-load is only turned off when the world is
        // first imported. If Multiverse already knows the world the import fails and the entry is left alone,
        // so an admin who sets auto-load back to true (e.g. so other plugins can use the world during their
        // own startup) keeps that setting across restarts.
        api.getWorldManager().importWorld(ImportWorldOptions.worldName(world.getName())
                .environment(world.getEnvironment())
                .generator(generator))
                .peek(mvWorld -> {
                    mvWorld.setAutoLoad(false);
                    api.getWorldManager().saveWorldsConfig();
                });
    }

    private String getGenerator(World world) {
        return BentoBox.getInstance().getIWM().getAddon(world)
                .map(gm -> gm.getDefaultWorldGenerator(world.getName(), "") != null).orElse(false)
                ? BentoBox.getInstance().getName()
                : null;
    }

    @Override
    public void unregisterWorld(World world) {
        MultiverseCoreApi api = MultiverseCoreApi.get();
        api.getWorldManager().getWorld(world.getName()).peek(api.getWorldManager()::removeWorld);
    }

    @Override
    public boolean hook() {
        return true; // The hook process shouldn't fail
    }

    @Override
    public String getFailureCause() {
        return null; // The hook process shouldn't fail
    }
}
