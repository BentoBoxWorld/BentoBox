package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Protects against dying
 * @author tastybento
 */
public class DyeListener extends FlagListener {

	/**
	 * Prevent dying
	 * @param e - event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent e) {
		
		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getType().name().contains("SIGN")) {
			checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.DYE);
			return;
		}
		
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEntityEvent e) {

		if (e.getRightClicked().getType().equals(EntityType.SHEEP)) {
			checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.DYE);
			return;
		}
	}


}
