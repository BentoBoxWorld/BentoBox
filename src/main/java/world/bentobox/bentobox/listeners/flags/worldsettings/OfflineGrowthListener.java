package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles {@link Flags#OFFLINE_GROWTH} flag.
 * @author Poslovitch, tastybento
 * @since 1.4.0
 */
public class OfflineGrowthListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getBlock().getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }
        // Check if island exists and members are online
        getIslands().getProtectedIslandAt(e.getBlock().getLocation()).ifPresent(i -> {
            for (UUID uuid : i.getMemberSet(RanksManager.COOP_RANK)) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            e.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (!getIWM().inWorld(e.getBlock().getWorld()) || Flags.OFFLINE_GROWTH.isSetForWorld(e.getBlock().getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }
        // Check what is spreading - disallow Bamboo and Kelp growth
        Material m = e.getBlock().getType();
        if (!m.equals(Material.KELP) && !m.equals(Material.BAMBOO) && !m.equals(Material.BAMBOO_SAPLING)) {
            return;
        }
        // Check if island exists and members are online
        getIslands().getProtectedIslandAt(e.getBlock().getLocation()).ifPresent(i -> {
            for (UUID uuid : i.getMemberSet(RanksManager.COOP_RANK)) {
                if (Bukkit.getPlayer(uuid) != null) {
                    return;
                }
            }
            e.setCancelled(true);
        });
    }
}
