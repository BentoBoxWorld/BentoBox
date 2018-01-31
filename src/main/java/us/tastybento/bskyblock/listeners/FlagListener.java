/**
 * 
 */
package us.tastybento.bskyblock.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;

/**
 * Abstract class for flag listeners to inherit. Provides some common code.
 * @author ben
 *
 */
public abstract class FlagListener implements Listener {
    
    private static final boolean DEBUG = false;
    protected BSkyBlock plugin;
    protected User user;
    
    public FlagListener(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEvent(Event e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
    }

    /*
     * The following methods cover the cancellable events and enable a simple noGo(e) to be used to cancel and send the error message
     */
    /**
     * Sends the island protected message to user
     * @param e 
     */
    protected void noGo(BlockBreakEvent e) {
        e.setCancelled(true);
        user.sendMessage("protection.protected");
    }
    
    /**
     * Sends the island protected message to user
     * @param e 
     */
    protected void noGo(BlockPlaceEvent e) {
        e.setCancelled(true);
        user.sendMessage("protection.protected");
    }
    
    /**
     * Sends the island protected message to user
     * @param e 
     */
    protected void noGo(InventoryPickupItemEvent e) {
        e.setCancelled(true);
        user.sendMessage("protection.protected");
    }
    
    /**
     * Sends the island protected message to user
     * @param e 
     */
    protected void noGo(PlayerLeashEntityEvent e) {
        e.setCancelled(true);
        user.sendMessage("protection.protected");
    }
    
    /**
     * Sends the island protected message to user
     * @param e 
     */
    protected void noGo(InventoryMoveItemEvent e) {
        e.setCancelled(true);
        user.sendMessage("protection.protected");
    }
    
    /**
     * Check if loc is in the island worlds
     * @param loc
     * @return true if the location is in the island worlds
     */
    protected boolean inWorld(Location loc) {
        return (loc.getWorld().equals(plugin.getIslandWorldManager().getIslandWorld())
                || loc.getWorld().equals(plugin.getIslandWorldManager().getNetherWorld())
                || loc.getWorld().equals(plugin.getIslandWorldManager().getEndWorld())) ? true : false;
    }
    
    /**
     * Check if the entity is in the island worlds
     * @param entity - the entity
     * @return true if in world
     */
    protected boolean inWorld(Entity entity) {
        return inWorld(entity.getLocation());
    }
    
}
