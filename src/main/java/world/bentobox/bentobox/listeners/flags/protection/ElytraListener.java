package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

public class ElytraListener extends FlagListener {


    /**
     * Handle visitors using elytra
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGlide(EntityToggleGlideEvent e) {
        if (e.getEntity() instanceof Player) {
            Player player = (Player) e.getEntity();
            if (!checkIsland(e, player, player.getLocation(), Flags.ELYTRA)) {
                player.setGliding(false);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGliding(PlayerTeleportEvent e) {
        if (getIWM().inWorld(e.getTo())&& e.getPlayer().isGliding()) {
            checkIsland(e, e.getPlayer(), e.getTo(), Flags.ELYTRA);
        }
    }

}