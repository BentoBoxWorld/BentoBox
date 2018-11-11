package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

public class CoarseDirtTillingListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTillingCoarseDirt(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getItem() == null) {
            return;
        }

        if (e.getClickedBlock().getType().equals(Material.COARSE_DIRT)) {
            switch (e.getItem().getType()) {
                case WOODEN_HOE:
                case STONE_HOE:
                case IRON_HOE:
                case GOLDEN_HOE:
                case DIAMOND_HOE:
                    if (e.getClickedBlock().getType().equals(Material.COARSE_DIRT)
                            && getIWM().inWorld(e.getClickedBlock().getWorld())
                            && !Flags.COARSE_DIRT_TILLING.isSetForWorld(e.getClickedBlock().getWorld())) {
                        e.setCancelled(true);
                        User user = User.getInstance(e.getPlayer());
                        user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(Flags.COARSE_DIRT_TILLING.getHintReference()));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
