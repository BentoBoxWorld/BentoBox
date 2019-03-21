package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

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

    /**
     * If podzol is mined when coarse dirt tilling is not allowed, then it'll just drop podzol and not dirt
     * This prevents an exploit where growing big spruce trees can turn gravel into podzol.
     * https://github.com/BentoBoxWorld/BentoBox/issues/613
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreakingPodzol(BlockBreakEvent e) {
        if (!e.getPlayer().getGameMode().equals(GameMode.CREATIVE)
                && e.getBlock().getType().equals(Material.PODZOL)
                && getIWM().inWorld(e.getBlock().getWorld())
                && !Flags.COARSE_DIRT_TILLING.isSetForWorld(e.getBlock().getWorld())) {
            e.getBlock().setType(Material.AIR);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.PODZOL));
        }
    }
}
