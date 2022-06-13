//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.listeners.flags.worldsettings;


import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.raid.RaidTriggerEvent;
import java.util.Optional;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;


/**
 * This listener checks for island visitors that want to start a new raid.
 */
public class VisitorsStartingRaidListener extends FlagListener
{
    /**
     * This method process raid allowance from visitors.
     * @param event RaidTriggerEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent event)
    {
        World world = Util.getWorld(event.getWorld());

        if (!this.getIWM().inWorld(world) || Flags.VISITOR_TRIGGER_RAID.isSetForWorld(world))
        {
            // If the player triggers raid non-protected world or VISITOR_TRIGGER_RAID is disabled then do nothing.
            this.report(User.getInstance(event.getPlayer()),
                event,
                event.getPlayer().getLocation(),
                Flags.VISITOR_TRIGGER_RAID,
                Why.SETTING_ALLOWED_IN_WORLD);

            return;
        }

        Optional<Island> island = this.getIslands().getProtectedIslandAt(event.getPlayer().getLocation());

        if (island.isPresent() && !island.get().getMemberSet().contains(event.getPlayer().getUniqueId()))
        {
            event.setCancelled(true);
            this.report(User.getInstance(event.getPlayer()),
                event,
                event.getPlayer().getLocation(),
                Flags.VISITOR_TRIGGER_RAID,
                Why.SETTING_NOT_ALLOWED_IN_WORLD);
        }
    }
}
