/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles interaction with entities like armor stands
 * Note - armor stand protection from breaking or placing is done elsewhere.
 * @author tastybento
 *
 */
public class EntityInteractListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand) {
            checkIsland(e, e.getRightClicked().getLocation(), Flags.ARMOR_STAND);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
        // Animal riding
        if (e.getRightClicked() instanceof Vehicle && e.getRightClicked() instanceof Animals) {
            checkIsland(e, e.getRightClicked().getLocation(), Flags.RIDING);
        }
        // Villager trading
        if (e.getRightClicked().getType().equals(EntityType.VILLAGER)) {
            checkIsland(e, e.getRightClicked().getLocation(), Flags.TRADING);
        }
    }
}
