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


/**
 * This method prevents sculk sensor from activation based on protection settings.
 */
public class SculkSensorListener extends FlagListener
{
    /**
     * This listener detects if a visitor activates sculk sensor, and block it, if required.
     * @param event Sculk activation event.
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSculkSensor(BlockReceiveGameEvent event)
    {
        if (!this.getIWM().inWorld(event.getBlock().getWorld()))
        {
            return;
        }

        if (event.getBlock().getType() == Material.SCULK_SENSOR &&
            event.getEntity() != null &&
            event.getEntity() instanceof Player player)
        {
            this.checkIsland(event, player, event.getBlock().getLocation(), Flags.SCULK_SENSOR, true);
        }
    }
}
