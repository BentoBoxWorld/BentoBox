package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.raid.RaidTriggerEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles rank-based protection for raid triggers on islands.
 * Players must meet the island's minimum rank requirement to trigger a raid.
 * @since 1.24.1
 */
public class RaidTriggerListener extends FlagListener {

    /**
     * Checks if the player is allowed to trigger a raid at their current location.
     * @param event RaidTriggerEvent
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onRaidTrigger(RaidTriggerEvent event) {
        checkIsland(event, event.getPlayer(), event.getPlayer().getLocation(), Flags.RAID_TRIGGER);
    }
}
