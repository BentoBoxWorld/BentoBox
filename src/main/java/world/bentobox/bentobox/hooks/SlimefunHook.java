/**
 * 
 */
package world.bentobox.bentobox.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Hook to enable slimefun blocks to be deleted when islands are deleted.
 */
public class SlimefunHook extends Hook {

    private Slimefun sfPlugin;

    public SlimefunHook() {
        super("Slimefun", Material.SLIME_BLOCK);
    }

    @Override
    public boolean hook() {
        // See if Slimefun is around
        sfPlugin = (Slimefun) Bukkit.getPluginManager().getPlugin("SlimeFun");
        return sfPlugin != null;
    }

    @Override
    public String getFailureCause() {
        return ""; // No errors
    }

    public void clearAllBlockInfoAtChunk(World world, int x, int z, boolean destroy) {
        if (!BlockStorage.isWorldLoaded(world)) {
            return; // Not sure if this is needed.
        }
        BlockStorage.clearAllBlockInfoAtChunk(world, x, z, destroy);
    }

    public void clearBlockInfo(Block b, boolean destroy) {
        BlockStorage.clearBlockInfo(b, destroy);
    }

    public void clearBlockInfo(Location location, boolean destroy) {
        BlockStorage.clearBlockInfo(location, destroy);
    }


}
