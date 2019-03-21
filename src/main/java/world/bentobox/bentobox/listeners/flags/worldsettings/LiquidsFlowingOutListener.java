package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockFromToEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link world.bentobox.bentobox.lists.Flags#LIQUIDS_FLOWING_OUT}.
 * @author Poslovitch
 * @since 1.3.0
 */
public class LiquidsFlowingOutListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLiquidFlow(BlockFromToEvent e) {
        Block from = e.getBlock();
        if (!from.isLiquid()) {
            return;
        }

        Block to = e.getToBlock();
        if (!getIWM().inWorld(from.getLocation()) || Flags.LIQUIDS_FLOWING_OUT.isSetForWorld(from.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        // https://github.com/BentoBoxWorld/BentoBox/issues/511#issuecomment-460040287
        // Time to do some maths! We've got the vector FromTo, let's check if its y coordinate is different from zero.
        if (to.getLocation().toVector().subtract(from.getLocation().toVector()).getY() != 0) {
            // We do not run any checks if this is a vertical flow - would be too much resource consuming.
            return;
        }

        // Only prevent if it is flowing into the area between islands.
        if (!getIslands().getProtectedIslandAt(to.getLocation()).isPresent()) {
            e.setCancelled(true);
        }
    }

}
