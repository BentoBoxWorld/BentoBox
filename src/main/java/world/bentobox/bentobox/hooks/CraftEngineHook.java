package world.bentobox.bentobox.hooks;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.CustomItem;
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

    /**
     * Returns an {@link ItemStack} for the CraftEngine custom item with the given namespaced ID.
     * Useful for rendering the correct icon (texture / model data) and display name in panels and GUIs.
     *
     * @param id namespaced ID (e.g. {@code "mynamespace:my_block"})
     * @return an Optional containing the registered custom item's ItemStack, or empty if not registered
     */
    public static Optional<ItemStack> getItemStack(String id) {
        if (id == null) {
            return Optional.empty();
        }
        CustomItem<ItemStack> customItem = CraftEngineItems.byId(Key.of(id));
        if (customItem == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customItem.buildItemStack());
    }
}
