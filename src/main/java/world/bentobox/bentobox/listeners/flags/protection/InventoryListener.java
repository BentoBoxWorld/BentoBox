package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Beacon;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Dropper;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;


/**
 * Handles inventory protection
 * @author tastybento
 */
public class InventoryListener extends FlagListener
{
    /**
     * Prevents players opening inventories
     * @param event - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        InventoryHolder inventoryHolder = event.getInventory().getHolder();

        if (inventoryHolder == null || !(event.getPlayer() instanceof Player player))
        {
            return;
        }

        if (inventoryHolder instanceof Animals)
        {
            // Prevent opening animal inventories.
            this.checkIsland(event, player, event.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (inventoryHolder instanceof ChestBoat)
        {
            // Prevent opening chest inventories
            this.checkIsland(event, player, event.getInventory().getLocation(), Flags.CHEST);
        }
    }


    /**
     * Prevents players picking items from inventories
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent e)
    {
        Player player = (Player) e.getWhoClicked();
        InventoryHolder inventoryHolder = e.getInventory().getHolder();

        if (inventoryHolder == null || !(e.getWhoClicked() instanceof Player))
        {
            return;
        }

        if (inventoryHolder instanceof Animals)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.MOUNT_INVENTORY);
        }
        else if (inventoryHolder instanceof Dispenser)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.DISPENSER);
        }
        else if (inventoryHolder instanceof Dropper)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.DROPPER);
        }
        else if (inventoryHolder instanceof Hopper || inventoryHolder instanceof HopperMinecart)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.HOPPER);
        }
        else if (inventoryHolder instanceof Furnace)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.FURNACE);
        }
        else if (inventoryHolder instanceof BrewingStand)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.BREWING);
        }
        else if (inventoryHolder instanceof Beacon)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.BEACON);
        }
        else if (inventoryHolder instanceof NPC)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.TRADING);
        }
        else if (inventoryHolder instanceof Barrel)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.BARREL);
        }
        else if (inventoryHolder instanceof ShulkerBox)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.SHULKER_BOX);
        }
        else if (inventoryHolder instanceof Chest c)
        {
            this.checkInvHolder(c.getLocation(), e, player);
        }
        else if (inventoryHolder instanceof DoubleChest dc)
        {
            this.checkInvHolder(dc.getLocation(), e, player);
        }
        else if (inventoryHolder instanceof StorageMinecart)
        {
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.CHEST);
        }
        else if (inventoryHolder instanceof ChestBoat)
        {
            // TODO: 1.19 added chest boat. Remove compatibility check when 1.18 is dropped.
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.CHEST);
        }
        else if (!(inventoryHolder instanceof Player))
        {
            // All other containers
            this.checkIsland(e, player, e.getInventory().getLocation(), Flags.CONTAINER);
        }
    }


    /**
     * This method runs check based on clicked chest type.
     * @param l location of chest.
     * @param e click event.
     * @param player player who clicked.
     */
    private void checkInvHolder(Location l, InventoryClickEvent e, Player player)
    {
        if (l.getBlock().getType().equals(Material.TRAPPED_CHEST))
        {
            this.checkIsland(e, player, l, Flags.TRAPPED_CHEST);
        }
        else
        {
            this.checkIsland(e, player, l, Flags.CHEST);
        }
    }
}
