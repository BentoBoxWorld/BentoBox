package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects islands from withers blowing things up
 * @author tastybento
 * @since 1.6.0
 */
public class WitherListener extends FlagListener {

    /**
     * Prevents Wither explosion from breaking blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (!getIWM().inWorld(e.getLocation())) return;
        // Remove  blocks from the explosion list if required
        if((e.getEntityType().equals(EntityType.WITHER_SKULL) || e.getEntityType().equals(EntityType.WITHER))
                && !Flags.WITHER_DAMAGE.isSetForWorld(e.getLocation().getWorld())) {
            e.blockList().clear();
        }
    }

    /**
     * Withers change blocks to air after they are hit (don't know why)
     * This prevents this when the wither has been spawned
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onWitherChangeBlocks(final EntityChangeBlockEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld())) return;
        e.setCancelled(e.getEntityType().equals(EntityType.WITHER) && !Flags.WITHER_DAMAGE.isSetForWorld(e.getBlock().getWorld()));
    }
}
