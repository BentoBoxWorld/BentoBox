/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.listeners.FlagListener;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author ben
 *
 */
public class AnvilListener extends FlagListener {

    public AnvilListener() {
        super(BSkyBlock.getInstance());
    }

    /**
     * Handle placing of Anvils
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (e.getClickedBlock().getType().equals(Material.ANVIL)) {
            checkIsland(e, getUser().getLocation(), Flags.ANVIL);
        }
    }
}
