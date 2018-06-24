/*

 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerTeleportEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * Removes mobs when teleporting to an island
 * @author tastybento
 *
 */
public class RemoveMobsListener extends AbstractFlagListener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUserTeleport(PlayerTeleportEvent e) {
        // Only process if flag is active
        if (getIslands().locationIsOnIsland(e.getPlayer(), e.getTo()) && Flags.REMOVE_MOBS.isSetForWorld(e.getTo().getWorld())) {
            clearArea(e.getTo());
        }
    }

    public static void clearArea(Location loc) {
        Set<EntityType> keepers = new HashSet<>();
        keepers.add(EntityType.ZOMBIE_VILLAGER);
        keepers.add(EntityType.PIG_ZOMBIE);
        keepers.add(EntityType.WITHER);
        keepers.add(EntityType.ENDERMAN);
        keepers.add(EntityType.GHAST);

        loc.getWorld().getNearbyEntities(loc, 5D, 5D, 5D).stream()
        .filter(en -> (en instanceof Monster))
        .filter(en -> !keepers.contains(en.getType()))
        .forEach(Entity::remove);
    }
}
