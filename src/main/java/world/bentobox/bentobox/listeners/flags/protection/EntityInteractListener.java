package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.ChestBoat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.versions.ServerCompatibility;


/**
 * Handles interaction with entities like armor stands
 * Note - armor stand protection from breaking or placing is done elsewhere.
 * @author tastybento
 *
 */
public class EntityInteractListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand) {
            checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.ARMOR_STAND);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
    {
        Player p = e.getPlayer();
        Location l = e.getRightClicked().getLocation();

        if (e.getRightClicked() instanceof Vehicle)
        {
            if (e.getRightClicked() instanceof Animals)
            {
                // Animal riding
                this.checkIsland(e, p, l, Flags.RIDING);
            }
            else if (e.getRightClicked() instanceof RideableMinecart)
            {
                // Minecart riding
                this.checkIsland(e, p, l, Flags.MINECART);
            }
            else if (e.getRightClicked() instanceof StorageMinecart)
            {
                this.checkIsland(e, p, l, Flags.CHEST);
            }
            else if (e.getRightClicked() instanceof HopperMinecart)
            {
                this.checkIsland(e, p, l, Flags.HOPPER);
            }
            else if (e.getRightClicked() instanceof PoweredMinecart)
            {
                this.checkIsland(e, p, l, Flags.FURNACE);
            }
            else if (e.getPlayer().isSneaking() && e.getRightClicked() instanceof ChestBoat)
            {
                // Access to chest boat since 1.19
                this.checkIsland(e, p, l, Flags.CHEST);
            }
            else if (e.getRightClicked() instanceof Boat)
            {
                // Boat riding
                this.checkIsland(e, p, l, Flags.BOAT);
            }
        }
        else if (e.getRightClicked() instanceof Villager || e.getRightClicked() instanceof WanderingTrader)
        {
            // Villager trading
            // Check naming and check trading
            this.checkIsland(e, p, l, Flags.TRADING);

            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG))
            {
                this.checkIsland(e, p, l, Flags.NAME_TAG);
            }
        }
        else if (e.getRightClicked() instanceof Allay)
        {
            // Allay item giving/taking
            this.checkIsland(e, p, l, Flags.ALLAY);

            // Check naming
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG))
            {
                this.checkIsland(e, p, l, Flags.NAME_TAG);
            }
        }
        else if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG))
        {
            // Name tags
            this.checkIsland(e, p, l, Flags.NAME_TAG);
        }
    }
}
