package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles throwing regular eggs (not spawn eggs)
 * @author tastybento
 *
 */
public class EggListener extends FlagListener {

    /**
     * Handle visitor chicken egg throwing
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        if (!checkIsland(e, e.getPlayer(), e.getEgg().getLocation(), Flags.EGGS)) {
            e.setHatching(false);
        }
    }

    /**
     * Handle visitor chicken egg hitting
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEggHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Egg egg) {
            if (egg.getShooter() instanceof Player player) {
                if (!checkIsland(e, player, egg.getLocation(), Flags.EGGS)) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
