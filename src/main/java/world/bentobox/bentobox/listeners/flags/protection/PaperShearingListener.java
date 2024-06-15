package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import io.papermc.paper.event.block.PlayerShearBlockEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

public class PaperShearingListener extends FlagListener {

    // Block shearing - paper only
    @EventHandler(priority = EventPriority.LOW)
    public void onShearBlock(final PlayerShearBlockEvent e) {
        checkIsland(e, e.getPlayer(), e.getBlock().getLocation(), Flags.SHEARING);
    }

}
