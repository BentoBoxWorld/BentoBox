package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
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
            checkIsland(e, player, player.getLocation(), Flags.ELYTRA);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void wearingElytra(PlayerTeleportEvent e) {
        if (!getIWM().inWorld(e.getTo())) {
            return;
        }
        User user = User.getInstance(e.getPlayer());
        // Check user's inventory
        if (user.getInventory().all(Material.ELYTRA).isEmpty()) {
            return;
        }
        if (getIslands().getProtectedIslandAt(e.getTo()).filter(i-> !i.isAllowed(user, Flags.ELYTRA)).isPresent()) {
            user.notify("protection.flags.ELYTRA.hint");
        }
    }

}