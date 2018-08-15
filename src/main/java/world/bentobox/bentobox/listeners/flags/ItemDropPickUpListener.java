package world.bentobox.bentobox.listeners.flags;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
public class ItemDropPickUpListener extends AbstractFlagListener {

    /*
     * Handle item drop
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        checkIsland(e, e.getItemDrop().getLocation(), Flags.ITEM_DROP);
    }

    /*
     * Handle item pickup
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            setUser(User.getInstance(e.getEntity())).checkIsland(e, e.getItem().getLocation(), Flags.ITEM_PICKUP);
        }
    }
}
