package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.SheepDyeWoolEvent;
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
	public void onPlayerInteract(final PlayerInteractEvent e)
	{
		if (e.getClickedBlock() == null || e.getItem() == null)
		{
			return;
		}

		if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
			e.getClickedBlock().getType().name().contains("SIGN") &&
			(e.getItem().getType().name().contains("DYE") || e.getItem().getType().equals(Material.GLOW_INK_SAC)))
		{
			this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.DYE);
		}
	}


	/**
	 * Prevents from interacting with sheep.
	 * @param e - event
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onPlayerInteract(final SheepDyeWoolEvent e)
	{
		if (e.getPlayer() == null)
		{
			// Sheep is not dyed by the player.
			return;
		}

		this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.DYE);
	}
}
