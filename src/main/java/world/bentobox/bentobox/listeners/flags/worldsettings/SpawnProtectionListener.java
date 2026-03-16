package world.bentobox.bentobox.listeners.flags.worldsettings;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Prevents visitors at the spawn island from dying in the void.
 * When the {@link Flags#SPAWN_PROTECTION} world setting is enabled,
 * any player that falls into the void while at the spawn island will
 * be teleported back to the spawn point instead of dying.
 * Handles {@link Flags#SPAWN_PROTECTION}.
 * @author tastybento
 * @since 2.6.0
 */
public class SpawnProtectionListener extends FlagListener {

    /**
     * Prevents players at the spawn island from dying in the void.
     * @param e - entity damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerVoidDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)
                || !e.getCause().equals(DamageCause.VOID)) {
            return;
        }
        World world = Util.getWorld(p.getWorld());
        if (world == null || !getIWM().inWorld(world) || !Flags.SPAWN_PROTECTION.isSetForWorld(world)) {
            return;
        }
        // Check if the player is at the spawn island (X/Z within spawn bounds)
        if (!getIslands().isAtSpawn(p.getLocation())) {
            return;
        }
        // Cancel void damage and return the player to spawn to prevent death
        e.setCancelled(true);
        getIslands().spawnTeleport(world, p);
    }
}
