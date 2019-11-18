package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockRedstoneEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

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

        // Check if island exists and members are online - excludes spawn
        getIslands().getProtectedIslandAt(e.getBlock().getLocation())
        .filter(i -> !i.isSpawn())
        .ifPresent(i -> {
            for (UUID uuid : i.getMemberSet(RanksManager.COOP_RANK)) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            e.setNewCurrent(0);
        });
    }
}
