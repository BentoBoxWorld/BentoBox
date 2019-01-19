package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
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
        super("Multiverse-Core");
    }

    public void registerWorld(World world) {
        String cmd1 = MULTIVERSE_IMPORT + world.getName() + " " + world.getEnvironment().name().toLowerCase() + " -g " + BentoBox.getInstance().getName();
        String cmd2 = MULTIVERSE_SET_GENERATOR + BentoBox.getInstance().getName() + " " + world.getName();
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd1);
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), cmd2);
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
