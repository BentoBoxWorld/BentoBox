package us.tastybento.bskyblock.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import us.tastybento.bskyblock.BSkyBlock;

public class BlockEndDragon implements Listener {
    private BSkyBlock plugin;

    public BlockEndDragon(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * This handles end dragon spawning prevention
     * 
     * @param event
     * @return true if dragon can spawn, false if not
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onDragonSpawn(CreatureSpawnEvent event) {
        if (!event.getEntityType().equals(EntityType.ENDER_DRAGON) || plugin.getIWM().isDragonSpawn(event.getEntity().getWorld())) {
            return true;
        }
        event.getEntity().setHealth(0);
        event.getEntity().remove();
        event.setCancelled(true);
        return false;
    }


}
