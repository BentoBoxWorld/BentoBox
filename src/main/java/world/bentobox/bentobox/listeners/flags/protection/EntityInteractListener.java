package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.entity.minecart.RideableMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

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
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        Location l = e.getRightClicked().getLocation();
        if (e.getRightClicked() instanceof Vehicle) {
            // Animal riding
            if (e.getRightClicked() instanceof Animals) {
                checkIsland(e, p, l, Flags.RIDING);
            }
            // Minecart riding
            else if (e.getRightClicked() instanceof RideableMinecart) {
                checkIsland(e, p, l, Flags.MINECART);
            }
            // Boat riding
            else if (e.getRightClicked() instanceof Boat) {
                checkIsland(e, p, l, Flags.BOAT);
            }
        }
        // Villager trading
        else if (e.getRightClicked() instanceof Villager || e.getRightClicked() instanceof WanderingTrader) {
            // Check naming and check trading
            checkIsland(e, p, l, Flags.TRADING);
            if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {
                checkIsland(e, p, l, Flags.NAME_TAG);
            }
        }
        // Name tags
        else if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.NAME_TAG)) {
            checkIsland(e, p, l, Flags.NAME_TAG);
        }
    }
}
