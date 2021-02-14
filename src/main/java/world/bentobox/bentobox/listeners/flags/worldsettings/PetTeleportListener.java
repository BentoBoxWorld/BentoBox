package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTeleportEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents pets from teleporting to islands unless
 * the owner is a member of the island.
 * @author tastybento
 * @since 1.16.0
 */
public class PetTeleportListener extends FlagListener {

    /**
     * Prevents pets teleporting
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPetTeleport(final EntityTeleportEvent e) {
        if (e.getTo() == null
                || !getIWM().inWorld(e.getFrom())
                || !Flags.PETS_STAY_AT_HOME.isSetForWorld(e.getFrom().getWorld())
                || !(e.getEntity() instanceof Tameable)
                ) return;
        Tameable t = (Tameable)e.getEntity();
        if (t.isTamed() && t.getOwner() != null) {
            // Get where the pet is going
            e.setCancelled(getIslands().getProtectedIslandAt(e.getTo())
                    // Not home island
                    .map(i -> !i.getMemberSet().contains(t.getOwner().getUniqueId()))
                    // Not any island
                    .orElse(true));
        }
    }

}
