package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockRedstoneEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link Flags#OFFLINE_REDSTONE} flag.
 * @author tastybento
 */
public class OfflineRedstoneListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockRedstone(BlockRedstoneEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_REDSTONE.isSetForWorld(e.getBlock().getWorld())) {
            // Do not do anything if it is not in the right world or if it is disabled.
            return;
        }

        // Check if island exists and members are online
        getIslands().getProtectedIslandAt(e.getBlock().getLocation()).ifPresent(i -> {
            for (UUID uuid : i.getMemberSet()) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            e.setNewCurrent(0);
        });
    }
}
