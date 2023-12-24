package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Hook to enable slimefun blocks to be deleted when islands are deleted.
 */
public class SlimefunHook extends Hook {

    public SlimefunHook() {
        super("Slimefun", Material.SLIME_BLOCK);
    }

    @Override
    public boolean hook() {
        // See if Slimefun is around
        return Bukkit.getPluginManager().getPlugin("SlimeFun") != null;
    }

    @Override
    public String getFailureCause() {
        return ""; // No errors
    }

    public void clearBlockInfo(Location location, boolean destroy) {
        BlockStorage.clearBlockInfo(location, destroy);
    }


}
