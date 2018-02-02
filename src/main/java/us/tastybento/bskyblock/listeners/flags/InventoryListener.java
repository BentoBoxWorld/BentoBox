/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.block.Beacon;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Animals;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles inventory protection
 * @author tastybento
 *
 */
public class InventoryListener extends AbstractFlagListener {

    /**
     * Prevents visitors picking items from inventories
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onMountInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() == null) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Animals) {
            checkIsland(event, event.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (event.getInventory().getHolder() instanceof Chest 
                || event.getInventory().getHolder() instanceof Dispenser
                || event.getInventory().getHolder() instanceof Hopper
                || event.getInventory().getHolder() instanceof Dropper
                || event.getInventory().getHolder() instanceof ShulkerBox) {
            checkIsland(event, event.getInventory().getLocation(), Flags.CHEST);
        }
        else if (event.getInventory().getHolder() instanceof Furnace) {
            checkIsland(event, event.getInventory().getLocation(), Flags.FURNACE);
        }
        else if (event.getInventory().getHolder() instanceof BrewingStand) {
            checkIsland(event, event.getInventory().getLocation(), Flags.BREWING);
        }
        else if (event.getInventory().getHolder() instanceof Beacon) {
            checkIsland(event, event.getInventory().getLocation(), Flags.BEACON);
        }
    }


}
