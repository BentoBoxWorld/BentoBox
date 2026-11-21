package world.bentobox.bentobox.hooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.util.Pair;

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

    /**
     * Removes the Slimefun data for one block, if it has any.
     * @param location block location
     * @param destroy true to also remove the block from Slimefun's saved storage
     */
    public void clearBlockInfo(Location location, boolean destroy) {
        if (BlockStorage.hasBlockInfo(location)) {
            BlockStorage.clearBlockInfo(location, destroy);
        }
    }

    /**
     * Removes the Slimefun data for every Slimefun block inside the given regions of a world.
     *
     * <p>Slimefun keeps its block data in its own files, outside the world's region
     * files, so deleting a region file leaves that data behind. A machine would then
     * reappear, and tick, at the same coordinates once the area is used again (#2051).
     *
     * <p>Only the blocks Slimefun knows about are visited, so the cost depends on how
     * many Slimefun blocks the world holds, not on the size of the regions. The chunks
     * are not loaded.
     *
     * @param world the world the regions were deleted from
     * @param regions region coordinates {@code (regionX, regionZ)} that were deleted
     * @return the number of Slimefun blocks cleared
     * @since 3.23.8
     */
    public int clearRegions(@NonNull World world, @NonNull Collection<Pair<Integer, Integer>> regions) {
        BlockStorage storage = BlockStorage.getStorage(world);
        if (storage == null || regions.isEmpty()) {
            return 0;
        }
        Set<Pair<Integer, Integer>> wanted = new HashSet<>(regions);
        int cleared = 0;
        // Copy the keys: clearing a block removes it from the storage being walked
        for (Location location : new ArrayList<>(storage.getRawStorage().keySet())) {
            if (isInRegions(location.getBlockX(), location.getBlockZ(), wanted)) {
                BlockStorage.clearBlockInfo(location, true);
                cleared++;
            }
        }
        return cleared;
    }

    /**
     * @param blockX block x coordinate
     * @param blockZ block z coordinate
     * @param regions region coordinates {@code (regionX, regionZ)}
     * @return true if the block lies in one of the regions
     */
    static boolean isInRegions(int blockX, int blockZ, Set<Pair<Integer, Integer>> regions) {
        // A region file covers 512 x 512 blocks
        return regions.contains(new Pair<>(blockX >> 9, blockZ >> 9));
    }

}
