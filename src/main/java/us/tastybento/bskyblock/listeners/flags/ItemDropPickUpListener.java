/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author tastybento
 *
 */
public class ItemDropPickUpListener extends AbstractFlagListener {

    public ItemDropPickUpListener(BSkyBlock plugin) {
        super(plugin);
    }

    /*
     * Handle item drop by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDrop(PlayerDropItemEvent e) {
        checkIsland(e, e.getItemDrop().getLocation(), Flags.ITEM_DROP);
    }

    /*
     * Handle item pickup by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDrop(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            setUser(User.getInstance(e.getEntity()));
            // Disallow, but don't tell the player an error
            checkIsland(e, e.getItem().getLocation(), Flags.ITEM_PICKUP, false);
        }
    }
}
