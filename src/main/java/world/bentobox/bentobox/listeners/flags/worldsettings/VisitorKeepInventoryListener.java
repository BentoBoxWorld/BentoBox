package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

import java.util.Optional;

/**
 * Prevents visitors from losing their items if they
 * die on an island in which they are a visitor.
 * Handles {@link world.bentobox.bentobox.lists.Flags#VISITOR_KEEP_INVENTORY}.
 * @author jstnf
 * @since 1.17.0
 */
public class VisitorKeepInventoryListener extends FlagListener {

    @EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDeath(PlayerDeathEvent e) {
        World world = Util.getWorld(e.getEntity().getWorld());
        if (!getIWM().inWorld(world) || !Flags.VISITOR_KEEP_INVENTORY.isSetForWorld(world)) {
            // If the player dies outside of the island world, don't do anything
            return;
        }

        Optional<Island> island = getIslands().getProtectedIslandAt(e.getEntity().getLocation());
        if (island.isPresent() && !island.get().getMemberSet().contains(e.getEntity().getUniqueId())) {
            e.setKeepInventory(true);
            e.setKeepLevel(true);
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }
}