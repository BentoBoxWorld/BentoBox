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
 * Protects against dying things.
 * @author tastybento
 * @since 1.5.0
 */
public class DyeListener extends FlagListener {

	/**
	 * Prevent dying signs.
	 * @param e - event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEvent e) {
		if (e.getClickedBlock() == null || e.getItem() == null) {
			return;
		}

		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getType().name().contains("SIGN")
			&& e.getItem().getType().name().contains("DYE")) {
			checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.DYE);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final PlayerInteractEntityEvent e) {
		// We cannot use SheepDyeWoolEvent since it doesn't provide who dyed the sheep
		if (e.getRightClicked().getType().equals(EntityType.SHEEP)
				&& (e.getPlayer().getInventory().getItemInMainHand().getType().name().contains("DYE")
				|| e.getPlayer().getInventory().getItemInOffHand().getType().name().contains("DYE"))) {
			checkIsland(e, e.getPlayer(), e.getRightClicked().getLocation(), Flags.DYE);
		}
	}
}
