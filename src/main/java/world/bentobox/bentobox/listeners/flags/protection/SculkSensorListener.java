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

import com.google.common.base.Enums;

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
     * @return true if the check is okay, false if it was disallowed
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onSculkSensor(BlockReceiveGameEvent event)
    {
        if (!this.getIWM().inWorld(event.getBlock().getWorld()))
        {
            return true;
        }

        if ((event.getBlock().getType() == Material.SCULK_SENSOR 
                || event.getBlock().getType() == Enums.getIfPresent(Material.class, "CALIBRATED_SCULK_SENSOR").or(Material.SCULK_SENSOR))
            && event.getEntity() != null && event.getEntity() instanceof Player player)
        {
            return this.checkIsland(event, player, event.getBlock().getLocation(), Flags.SCULK_SENSOR, true);
        }
        return true;
    }
}
