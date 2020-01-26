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

        // Check if island exists and members are online, or mods or ops are on the island - ignores spawn
        getIslands().getProtectedIslandAt(e.getBlock().getLocation())
        .filter(i -> !i.isSpawn())
        .ifPresent(i -> {
            // Check team members
            for (UUID uuid : i.getMemberSet(RanksManager.COOP_RANK)) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            // Check mods or Ops on island
            if (Bukkit.getOnlinePlayers().parallelStream()
                    .filter(p -> p.isOp() || p.hasPermission(getIWM().getPermissionPrefix(i.getWorld()) + "mod.bypassprotect"))
                    .anyMatch(p -> i.onIsland(p.getLocation()))) {
                return;
            }
            // No one there...
            e.setNewCurrent(0);
        });
    }
}
