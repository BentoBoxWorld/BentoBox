package world.bentobox.bentobox.hooks;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides implementation and interfacing to interact with Multiverse.
 *
 * @author Poslovitch
 */
public class MultiverseCoreHook extends Hook {

    private static final String MULTIVERSE_SET_GENERATOR = "mv modify set generator ";
    private static final String MULTIVERSE_IMPORT = "mv import ";

    public MultiverseCoreHook() {
        super("Multiverse-Core", Material.COMPASS);
    }

    /**
     * Register the world with Multiverse
     * @param world - world to register
     * @param islandWorld - if true, then this is an island world
     */
    public void registerWorld(World world, boolean islandWorld) {
        if (islandWorld) {
            // Only register generator if one is defined in the addon (is not null)
            String generator = BentoBox.getInstance().getIWM().getAddon(world).map(gm -> gm.getDefaultWorldGenerator(world.getName(), "") != null).orElse(false) ? " -g " + BentoBox.getInstance().getName() : "";
            String cmd1 = MULTIVERSE_IMPORT + world.getName() + " " + world.getEnvironment().name().toLowerCase(Locale.ENGLISH) + generator;
            String cmd2 = MULTIVERSE_SET_GENERATOR + BentoBox.getInstance().getName() + " " + world.getName();
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd1);
            if (!generator.isEmpty()) {
                // Register the generator
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd2);
            }
        } else {
            // Set the generator to null - this will remove any previous registration
            String cmd1 = MULTIVERSE_IMPORT + world.getName() + " " + world.getEnvironment().name().toLowerCase(Locale.ENGLISH);
            String cmd2 = MULTIVERSE_SET_GENERATOR + "null " + world.getName();
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd1);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd2);
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
