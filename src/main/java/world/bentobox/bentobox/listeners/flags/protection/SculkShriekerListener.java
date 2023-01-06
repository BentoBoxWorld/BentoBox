//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.flags.protection;


import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockReceiveGameEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.versions.ServerCompatibility;


/**
 * This method prevents sculk shrieker from activation based on protection settings.
 */
public class SculkShriekerListener extends FlagListener
{
    /**
     * This listener detects if a visitor activates sculk sensor, and block it, if required.
     * @param event Sculk activation event.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSculkShrieker(BlockReceiveGameEvent event)
    {
        if (!this.getIWM().inWorld(event.getBlock().getWorld()))
        {
            return;
        }

        if (event.getBlock().getType() == Material.SCULK_SHRIEKER &&
            event.getEntity() != null &&
            event.getEntity() instanceof Player player)
        {
            this.checkIsland(event, player, event.getBlock().getLocation(), Flags.SCULK_SHRIEKER, true);
        }
    }
}
