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

import org.bukkit.inventory.InventoryHolder;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles inventory protection
 * @author tastybento
 */
public class InventoryListener extends FlagListener {

    /**
     * Prevents players picking items from inventories
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent e) {
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder == null) {
            return;
        }
        setUser(User.getInstance(e.getWhoClicked()));
        if (inventoryHolder instanceof Animals) {
            checkIsland(e, e.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (inventoryHolder instanceof Chest
                || inventoryHolder instanceof ShulkerBox) {
            checkIsland(e, e.getInventory().getLocation(), Flags.CHEST);
        }
        else if (inventoryHolder instanceof Dispenser) {
            checkIsland(e, e.getInventory().getLocation(), Flags.DISPENSER);
        }
        else if (inventoryHolder instanceof Dropper) {
            checkIsland(e, e.getInventory().getLocation(), Flags.DROPPER);
        }
        else if (inventoryHolder instanceof Hopper) {
            checkIsland(e, e.getInventory().getLocation(), Flags.HOPPER);
        }
        else if (inventoryHolder instanceof Furnace) {
            checkIsland(e, e.getInventory().getLocation(), Flags.FURNACE);
        }
        else if (inventoryHolder instanceof BrewingStand) {
            checkIsland(e, e.getInventory().getLocation(), Flags.BREWING);
        }
        else if (inventoryHolder instanceof Beacon) {
            checkIsland(e, e.getInventory().getLocation(), Flags.BEACON);
        }
    }
}
