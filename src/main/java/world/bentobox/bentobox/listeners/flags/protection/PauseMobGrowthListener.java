package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Prevents visitors from using a golden dandelion to pause or unpause baby mob growth.
 * This feature was introduced in Minecraft 26.1.1.
 * @author tastybento
 * @since 3.12.2
 */
public class PauseMobGrowthListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        var item = e.getHand() == EquipmentSlot.OFF_HAND
                ? p.getInventory().getItemInOffHand()
                : p.getInventory().getItemInMainHand();
        if (e.getRightClicked() instanceof Ageable ageable && !ageable.isAdult()
                && item.getType().name().equals("GOLDEN_DANDELION")) {
            checkIsland(e, p, e.getRightClicked().getLocation(), Flags.PAUSE_MOB_GROWTH);
        }
    }
}
