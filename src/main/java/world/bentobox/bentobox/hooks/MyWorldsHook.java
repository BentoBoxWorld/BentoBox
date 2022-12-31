package world.bentobox.bentobox.hooks;

import org.bukkit.Material;
import org.bukkit.World;

import com.bergerkiller.bukkit.mw.WorldConfigStore;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides implementation and interfacing to interact with MyWorlds.
 *
 * @author bergerkiller (Irmo van den Berge)
 */
public class MyWorldsHook extends Hook implements WorldManagementHook {

    public MyWorldsHook() {
        super("My_Worlds", Material.FILLED_MAP);
    }

    /**
     * Register the world with MyWorlds
     *
     * @param world - world to register
     * @param islandWorld - if true, then this is an island world
     */
    @Override
    public void registerWorld(World world, boolean islandWorld) {
        if (islandWorld) {
            // Only register generator if one is defined in the addon (is not null)
            boolean hasGenerator = BentoBox.getInstance().getIWM().getAddon(world).map(gm -> gm.getDefaultWorldGenerator(world.getName(), "") != null).orElse(false);
            setUseBentoboxGenerator(world, hasGenerator);
        } else {
            // Set the generator to null - this will remove any previous registration
            setUseBentoboxGenerator(world, false);
        }
    }

    private void setUseBentoboxGenerator(World world, boolean hasGenerator) {
        String name = hasGenerator ? BentoBox.getInstance().getName() : null;

        try {
            WorldConfigStore.get(world).setChunkGeneratorName(name);

            // Alternative Reflection way to do it, if a MyWorlds dependency isn't available at
            // compile time.
            /*
            // WorldConfigStore -> public static WorldConfig get(World world);
            Object worldConfig = Class.forName("com.bergerkiller.bukkit.mw.WorldConfigStore")
                    .getMethod("get", World.class)
                    .invoke(null, world);

            // WorldConfig -> public void setChunkGeneratorName(String name);
            Class.forName("com.bergerkiller.bukkit.mw.WorldConfig")
                    .getMethod("setChunkGeneratorName", String.class)
                    .invoke(worldConfig, name);
             */
        } catch (Exception t) {
            BentoBox.getInstance().logError("Failed to register world " + world.getName() + " with MyWorlds " + t.getMessage());
        }
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
