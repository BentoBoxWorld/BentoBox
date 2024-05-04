package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class LeashListener extends FlagListener {

    /**
     * Prevents leashing
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent e) {
        checkIsland(e, e.getPlayer(), e.getEntity().getLocation(), Flags.LEASH);
    }

    /**
     * Prevents unleashing
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUnleash(PlayerUnleashEntityEvent e) {
        checkIsland(e, e.getPlayer(), e.getEntity().getLocation(), Flags.LEASH);
    }

    /**
     * Prevents hitching
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerLeashHitch(final HangingPlaceEvent e) {
        EntityType LEASH_HITCH = Util.findFirstMatchingEnum(EntityType.class, "LEASH_HITCH", "LEASH_KNOT");
        if (e.getEntity().getType().equals(LEASH_HITCH)) {
            checkIsland(e, e.getPlayer(), e.getEntity().getLocation(), Flags.LEASH);
        }
    }

}
