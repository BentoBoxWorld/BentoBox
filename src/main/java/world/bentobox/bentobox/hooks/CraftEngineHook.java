package world.bentobox.bentobox.hooks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.util.Key;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides CraftEngine API access without requiring addons to depend on CraftEngine directly.
 */
public class CraftEngineHook extends Hook {

    public CraftEngineHook() {
        super("CraftEngine", Material.NOTE_BLOCK);
    }

    @Override
    public boolean hook() {
        return true;
    }

    @Override
    public String getFailureCause() {
        return "CraftEngine is not installed";
    }

    /**
     * Returns the namespaced ID (e.g. {@code "mynamespace:my_block"}) of the CraftEngine custom block
     * at the given location, or {@code null} if no custom block is present.
     *
     * @param loc the location to check
     * @return namespaced block ID or {@code null}
     */
    public static String getBlockId(Location loc) {
        return getBlockId(loc.getBlock());
    }

    /**
     * Returns the namespaced ID (e.g. {@code "mynamespace:my_block"}) of the CraftEngine custom block
     * represented by the given block, or {@code null} if it is not a CraftEngine custom block.
     *
     * @param block the block to check
     * @return namespaced block ID or {@code null}
     */
    public static String getBlockId(Block block) {
        if (!CraftEngineBlocks.isCustomBlock(block)) {
            return null;
        }
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(block);
        if (state == null) {
            return null;
        }
        return state.owner().value().id().asString();
    }

    /**
     * Returns {@code true} if a CraftEngine custom block with the given namespaced ID exists in the registry.
     *
     * @param id namespaced block ID (e.g. {@code "mynamespace:my_block"})
     * @return {@code true} if the block ID is registered
     */
    public static boolean exists(String id) {
        return CraftEngineBlocks.byId(Key.of(id)) != null;
    }

    /**
     * Places a CraftEngine custom block at the given location.
     *
     * @param location the target location
     * @param blockId  namespaced block ID (e.g. {@code "mynamespace:my_block"})
     * @return {@code true} if the block was placed successfully
     */
    public static boolean placeBlock(Location location, String blockId) {
        return CraftEngineBlocks.place(location, Key.of(blockId), false);
    }
}
