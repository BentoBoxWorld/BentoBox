/*

 */
package world.bentobox.bentobox.listeners.flags;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handles respawning back on island
 * @author tastybento
 *
 */
public class IslandRespawnListener extends AbstractFlagListener {
    
    Map<UUID, World> respawn = new HashMap<>();

    /**
     * Tag players who die in island space and have an island
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (getIWM().inWorld(e.getEntity().getLocation()) && Flags.ISLAND_RESPAWN.isSetForWorld(e.getEntity().getWorld()) 
                && getIslands().hasIsland(e.getEntity().getWorld(), e.getEntity().getUniqueId())) {
            respawn.put(e.getEntity().getUniqueId(), e.getEntity().getWorld());
        }
    }
    
    /**
     * Place players back on their island if respawn on island is true and active
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (Flags.ISLAND_RESPAWN.isSetForWorld(e.getPlayer().getWorld()) && respawn.containsKey(e.getPlayer().getUniqueId())) {
            World world = respawn.get(e.getPlayer().getUniqueId()); 
            respawn.remove(e.getPlayer().getUniqueId());
            Location respawnLocation = getIslands().getSafeHomeLocation(world, User.getInstance(e.getPlayer().getUniqueId()), 1);
            if (respawnLocation != null) {
                e.setRespawnLocation(respawnLocation);
            }
        }
    }
}
