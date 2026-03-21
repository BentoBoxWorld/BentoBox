package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link Flags#WIND_CHARGE}.
 * Prevents visitors from launching wind charges on protected islands.
 * @author tastybento
 * @since 2.6.0
 */
public class WindChargeListener extends FlagListener {

    /**
     * Prevents players from launching wind charges on protected islands.
     * @param e ProjectileLaunchEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onWindChargeLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof AbstractWindCharge && e.getEntity().getShooter() instanceof Player player) {
            checkIsland(e, player, e.getEntity().getLocation(), Flags.WIND_CHARGE);
        }
    }
}
