package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link Flags#POTION_THROWING} and {@link Flags#EXPERIENCE_BOTTLE_THROWING}.
 * @author Poslovitch
 * @since 1.1
 */
public class ThrowingListener extends FlagListener {

    /**
     * Prevents players from throwing potions / exp bottles.
     * @param e ProjectileLaunchEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerThrowPotion(ProjectileLaunchEvent e) {
        if (e.getEntity().getShooter() instanceof Player && (e.getEntity() instanceof ThrownPotion)) {
            if (e.getEntity() instanceof ThrownPotion) {
                checkIsland(e, (Player) e.getEntity().getShooter(), e.getEntity().getLocation(), Flags.POTION_THROWING);
            } else if (e.getEntity() instanceof ThrownExpBottle) {
                checkIsland(e, (Player) e.getEntity().getShooter(), e.getEntity().getLocation(), Flags.EXPERIENCE_BOTTLE_THROWING);
            }
        }
    }
}
