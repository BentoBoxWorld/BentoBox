package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;

import world.bentobox.bentobox.lists.Flags;

/**
 * Handles the {@link Flags#EXPERIENCE_PICKUP} flag with a Paper specific pickup event.
 * @since 1.13.0
 * @author KennyTV
 */
public class PaperExperiencePickupListener extends ExperiencePickupListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExperiencePickup(PlayerPickupExperienceEvent e) {
        if (!checkIsland(e, e.getPlayer(), e.getExperienceOrb().getLocation(), Flags.EXPERIENCE_PICKUP)) {
            e.setCancelled(true);
        }
    }
}
