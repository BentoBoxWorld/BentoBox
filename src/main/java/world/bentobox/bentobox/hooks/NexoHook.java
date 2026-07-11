package world.bentobox.bentobox.hooks;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;

import world.bentobox.bentobox.api.hooks.Hook;

/**
 * Provides Nexo API access without requiring addons to depend on Nexo directly.
 */
public class NexoHook extends Hook {

    public NexoHook() {
        super("Nexo", Material.NOTE_BLOCK);
    }

    @Override
    public boolean hook() {
        // See if Nexo is around
        return Bukkit.getPluginManager().getPlugin("Nexo") != null;
    }

    @Override
    public String getFailureCause() {
        return "Nexo is not installed";
    }

    /**
     * Returns {@code true} if a Nexo custom block with the given ID exists in the registry.
     *
     * @param id the Nexo custom block ID
     * @return {@code true} if the block ID is registered
     */
    public static boolean exists(String id) {
        return NexoBlocks.isCustomBlock(id);
    }

    /**
     * Places a Nexo custom block at the given location.
     *
     * @param location the target location
     * @param blockId  the Nexo custom block ID
     * @return {@code true} if the block was placed successfully
     */
    public static boolean placeBlock(Location location, String blockId) {
        return NexoBlocks.place(blockId, location);
    }

    /**
     * Returns the ID of the Nexo custom block represented by the given block, or {@code null}
     * if it is not a Nexo custom block.
     *
     * @param block the block to check
     * @return the Nexo custom block ID or {@code null}
     */
    public static String getBlockId(Block block) {
        CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(block);
        return mechanic == null ? null : mechanic.getItemID();
    }

    /**
     * Returns an {@link ItemStack} for the Nexo item with the given ID.
     * Useful for rendering the correct icon in panels and GUIs.
     *
     * @param id the Nexo item ID
     * @return an Optional containing the registered item's ItemStack, or empty if not registered
     */
    public static Optional<ItemStack> getItemStack(String id) {
        if (id == null) {
            return Optional.empty();
        }
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(builder.build());
    }
}
