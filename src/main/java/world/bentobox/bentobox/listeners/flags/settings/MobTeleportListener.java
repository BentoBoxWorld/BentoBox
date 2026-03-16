package world.bentobox.bentobox.listeners.flags.settings;

import java.util.Optional;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTeleportEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles shulker or enderman teleporting
 * @author tastybento
 */
public class MobTeleportListener extends FlagListener {

    /**
     * Check teleport of Endermen or Shulker
     * @param event EntityTeleportEvent
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityTeleportEvent(EntityTeleportEvent event) {
        // Only cover shulkers and enderman
        if (!event.getEntityType().equals(EntityType.ENDERMAN) && !event.getEntityType().equals(EntityType.SHULKER)) {
            return;
        }
        World w = event.getFrom().getWorld();
        // If not in the right world exit immediately and check teleportation is within the same world
        if (!this.getIWM().inWorld(w) || event.getTo() == null || !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }
        Optional<Island> island = getIslands().getIslandAt(event.getEntity().getLocation());

        if (event.getEntityType().equals(EntityType.ENDERMAN) && Boolean.TRUE.equals(island.map(i -> !i.isAllowed(Flags.ENDERMAN_TELEPORT)).orElseGet(
                () -> !Flags.ENDERMAN_TELEPORT.isSetForWorld(w)))) {
            // Enderman teleport is disabled on island or world. Cancel it.
            event.setCancelled(true);
        }

        if (event.getEntityType().equals(EntityType.SHULKER) && Boolean.TRUE.equals(island.map(i -> !i.isAllowed(Flags.SHULKER_TELEPORT)).orElseGet(
                () -> !Flags.SHULKER_TELEPORT.isSetForWorld(w)))) {
            // Shulker teleport is disabled on island or world. Cancel it.
            event.setCancelled(true);
        }
    }
}