/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author tastybento
 *
 */
public class LeashListener extends AbstractFlagListener {

    public LeashListener(BSkyBlock plugin) {
        super(plugin);
    }


    /**
     * Prevents leashing
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeashUse(PlayerLeashEntityEvent e) {
        checkIsland(e, e.getEntity().getLocation(),Flags.LEASH);
    }


    /**
     * Prevents unleashing
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeashUse(PlayerUnleashEntityEvent e) {
        checkIsland(e, e.getEntity().getLocation(),Flags.LEASH);
    }

    /**
     * Prevents hitching
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerLeashHitch(final HangingPlaceEvent e) {
        if (e.getEntity() != null && e.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
            checkIsland(e, e.getEntity().getLocation(),Flags.LEASH);
        }
    }

}
