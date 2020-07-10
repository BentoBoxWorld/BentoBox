package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents players from taking books out of a lectern.
 * @author Poslovitch
 * @since 1.10.0
 */
public class LecternListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerTakeBookFromLectern(PlayerTakeLecternBookEvent e) {
        checkIsland(e, e.getPlayer(), e.getLectern().getLocation(), Flags.LECTERN);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlaceBooksOnLectern(BlockPlaceEvent e) {
        if (e.getItemInHand().getType().equals(Material.WRITABLE_BOOK)
                || e.getItemInHand().getType().equals(Material.WRITTEN_BOOK)) {
            // Books can only be placed on lecterns and as such are protected by the LECTERN flag.
            checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.LECTERN);
        }
    }
}
