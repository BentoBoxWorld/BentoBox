/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEggThrowEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * Handles throwing regular eggs (not spawn eggs)
 * @author tastybento
 *
 */
public class EggListener extends AbstractFlagListener {

    /**
     * Handle visitor chicken egg throwing
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        if (!checkIsland(e, e.getEgg().getLocation(), Flags.EGGS)) {
            e.setHatching(false);
        }
    }

}
