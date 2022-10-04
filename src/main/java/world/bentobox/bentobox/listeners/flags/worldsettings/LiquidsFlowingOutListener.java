package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles {@link world.bentobox.bentobox.lists.Flags#LIQUIDS_FLOWING_OUT}.
 * @author Poslovitch
 * @since 1.3.0
 */
public class LiquidsFlowingOutListener extends FlagListener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent e) {
        Block from = e.getBlock();
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

        // Only prevent if it is flowing into the area between islands or into another island.
        Optional<Island> fromIsland = getIslands().getProtectedIslandAt(from.getLocation());
        Optional<Island> toIsland = getIslands().getProtectedIslandAt(to.getLocation());
        if (toIsland.isEmpty() || (fromIsland.isPresent() && !fromIsland.equals(toIsland))) {
            e.setCancelled(true);
        }
    }


    /**
     * Prevents players from dispensing water, lava and powdered snow from dispenser outside island
     * if Flags.LIQUIDS_FLOWING_OUT is disabled.
     * @param event BlockDispenseEvent
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispenserLiquid(BlockDispenseEvent event)
    {
        Location from = event.getBlock().getLocation();

        if (!this.getIWM().inWorld(from) || Flags.LIQUIDS_FLOWING_OUT.isSetForWorld(from.getWorld())) {
            // We do not want to run any check if this is not the right world or if it is allowed.
            return;
        }

        Location to = event.getVelocity().toLocation(event.getBlock().getWorld());

        if (!event.getItem().getType().equals(Material.WATER_BUCKET) &&
            !event.getItem().getType().equals(Material.LAVA_BUCKET) &&
            !event.getItem().getType().equals(Material.POWDER_SNOW_BUCKET))
        {
            return;
        }

        // Only prevent if it is flowing into the area between islands or into another island.
        Optional<Island> fromIsland = this.getIslandsManager().getProtectedIslandAt(from);
        Optional<Island> toIsland = this.getIslandsManager().getProtectedIslandAt(to);

        if (toIsland.isEmpty() || (fromIsland.isPresent() && !fromIsland.equals(toIsland)))
        {
            event.setCancelled(true);
        }
    }
}
