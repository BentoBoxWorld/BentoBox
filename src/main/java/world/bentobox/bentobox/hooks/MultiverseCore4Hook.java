package world.bentobox.bentobox.hooks;

import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

public class MultiverseCore4Hook extends Hook implements WorldManagementHook {

    public MultiverseCore4Hook() {
        super("Multiverse-Core", Material.COMPASS);
    }

    /**
     * Register the world with Multiverse
     * @param world - world to register
     * @param islandWorld - if true, then this is an island world
     */
    @Override
    public void registerWorld(World world, boolean islandWorld) {
        MultiverseCore core = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (core == null) {
            return;
        }
        String generator = islandWorld ? getGenerator(world) : null;
        core.getMVWorldManager().addWorld(
                world.getName(),
                world.getEnvironment(),
                String.valueOf(world.getSeed()),
                world.getWorldType(),
                world.canGenerateStructures(),
                generator
        );
        core.getMVWorldManager().getMVWorld(world.getName()).setAutoLoad(false);
    }

    private String getGenerator(World world) {
        return BentoBox.getInstance().getIWM().getAddon(world)
                .map(gm -> gm.getDefaultWorldGenerator(world.getName(), "") != null).orElse(false)
                ? BentoBox.getInstance().getName()
                : null;
    }

    @Override
    public void unregisterWorld(World world) {
        MultiverseCore core = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (core == null) {
            return;
        }
        core.getMVWorldManager().removeWorldFromConfig(world.getName());
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
