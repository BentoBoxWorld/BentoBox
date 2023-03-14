package world.bentobox.bentobox.listeners.flags.settings;

import java.util.Optional;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTeleportEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles natural mob teleporting.
 * @author tastybento
 */
public class MobTeleportListener extends FlagListener {

    /**
     * Check teleport of Endermen
     * @param event EntityTeleportEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityTeleportEvent(EntityTeleportEvent event) {
        World w = event.getFrom().getWorld();
        // If not in the right world exit immediately.
        if (!this.getIWM().inWorld(w)) {
            return;
        }
        Optional<Island> island = getIslands().getIslandAt(event.getEntity().getLocation());

        if (Boolean.TRUE.equals(island.map(i -> !i.isAllowed(Flags.ENDERMAN_TELEPORT)).orElseGet(
                () -> !Flags.ENDERMAN_TELEPORT.isSetForWorld(w)))) {
            // Enderman teleport is disabled on island or world. Cancel it.
            event.setCancelled(true);
        }
    }
}