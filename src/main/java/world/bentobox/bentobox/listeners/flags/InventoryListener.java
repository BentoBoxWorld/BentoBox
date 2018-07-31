package world.bentobox.bentobox.listeners.flags;

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

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles inventory protection
 * @author tastybento
 *
 */
public class InventoryListener extends AbstractFlagListener {

    /**
     * Prevents visitors picking items from inventories
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onMountInventoryClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() == null) {
            return;
        }
        if (e.getInventory().getHolder() instanceof Animals) {
            checkIsland(e, e.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (e.getInventory().getHolder() instanceof Chest
                || e.getInventory().getHolder() instanceof Dispenser
                || e.getInventory().getHolder() instanceof Hopper
                || e.getInventory().getHolder() instanceof Dropper
                || e.getInventory().getHolder() instanceof ShulkerBox) {
            setUser(User.getInstance(e.getWhoClicked())).checkIsland(e, e.getInventory().getLocation(), Flags.CHEST);
        }
        else if (e.getInventory().getHolder() instanceof Furnace) {
            setUser(User.getInstance(e.getWhoClicked())).checkIsland(e, e.getInventory().getLocation(), Flags.FURNACE);
        }
        else if (e.getInventory().getHolder() instanceof BrewingStand) {
            setUser(User.getInstance(e.getWhoClicked())).checkIsland(e, e.getInventory().getLocation(), Flags.BREWING);
        }
        else if (e.getInventory().getHolder() instanceof Beacon) {
            setUser(User.getInstance(e.getWhoClicked())).checkIsland(e, e.getInventory().getLocation(), Flags.BEACON);
        }
    }


}
