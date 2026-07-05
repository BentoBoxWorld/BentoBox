package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles fishing with a fishing rod ({@link Flags#FISHING}).
 * Both casting and catching fish are checked at the hook's location, so fishing
 * in protected water from outside the protected area is also blocked.
 * Hooking entities is not handled here - that is covered by the hurting and PVP flags.
 * @author tastybento
 * @since 3.19.0
 */
public class FishingListener extends FlagListener {

    /**
     * Check when a player casts a rod or catches a fish
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFishing(PlayerFishEvent e) {
        if ((e.getState() == PlayerFishEvent.State.FISHING || e.getState() == PlayerFishEvent.State.CAUGHT_FISH)
                && !checkIsland(e, e.getPlayer(), e.getHook().getLocation(), Flags.FISHING)) {
            e.getHook().remove();
        }
    }
}
