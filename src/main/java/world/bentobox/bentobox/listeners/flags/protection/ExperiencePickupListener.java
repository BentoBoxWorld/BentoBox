package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles the {@link world.bentobox.bentobox.lists.Flags#EXPERIENCE_PICKUP} flag.
 *
 * @author Poslovitch
 */
public class ExperiencePickupListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExperienceOrbTargetPlayer(EntityTargetLivingEntityEvent e) {
        // Make sure the target is a Player and the entity is an experience orb
        if (e.getTarget() instanceof Player && e.getEntity() instanceof ExperienceOrb) {
            setUser(User.getInstance((Player) e.getTarget()));
            if (!checkIsland(e, e.getEntity().getLocation(), Flags.EXPERIENCE_PICKUP)) {
                // Cancelling the event won't be enough, we need to explicitly set the target to null
                e.setTarget(null);
            }
        }
    }
}
