package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.block.Beacon;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Animals;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import world.bentobox.bentobox.api.flags.FlagListener;
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
        Player player = (Player)e.getWhoClicked();

        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (inventoryHolder == null || !(e.getWhoClicked() instanceof Player)) {
            return;
        }
        if (inventoryHolder instanceof Animals) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (inventoryHolder instanceof Dispenser) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.DISPENSER);
        }
        else if (inventoryHolder instanceof Dropper) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.DROPPER);
        }
        else if (inventoryHolder instanceof Hopper
                || inventoryHolder instanceof HopperMinecart) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.HOPPER);
        }
        else if (inventoryHolder instanceof Furnace) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.FURNACE);
        }
        else if (inventoryHolder instanceof BrewingStand) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.BREWING);
        }
        else if (inventoryHolder instanceof Beacon) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.BEACON);
        }
        else if (inventoryHolder instanceof NPC) {
            checkIsland(e, player, e.getInventory().getLocation(), Flags.TRADING);
        }
        else if (!(inventoryHolder instanceof Player)) {
            // All other containers
            checkIsland(e, player, e.getInventory().getLocation(), Flags.CONTAINER);
        }
    }
}
