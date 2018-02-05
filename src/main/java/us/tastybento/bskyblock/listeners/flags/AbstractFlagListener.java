/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.lang.reflect.Method;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.lists.Flags;

/**
 * Abstract class for flag listeners. Provides common code.
 * @author tastybento
 *
 */
public abstract class AbstractFlagListener implements Listener {

    public BSkyBlock plugin;
    private User user = null;

    public AbstractFlagListener(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
    }

    /**
     * Sets the player associated with this event.
     * If the user is a fake player, they are not counted.
     * @param e - the event
     * @return true if found, otherwise false
     */
    private boolean createEventUser(Event e) {
        try {
            // Use reflection to get the getPlayer method if it exists

            Method getPlayer = e.getClass().getMethod("getPlayer");
            if (getPlayer != null) {
                setUser(User.getInstance((Player)getPlayer.invoke(e)));
                return true;
            }
        } catch (Exception e1) { e1.printStackTrace();}
        return false;
    }

    /**
     * Explicitly set the user for the next {@link #checkIsland(Event, Location, Flag)} or {@link #checkIsland(Event, Location, Flag, boolean)}
     * @param user
     */
    public AbstractFlagListener setUser(User user) {
        if (!plugin.getSettings().getFakePlayers().contains(user.getName())) this.user = user;
        return this;
    }

    /*
     * The following methods cover the cancellable events and enable a simple noGo(e) to be used to cancel and send the error message
     */

    /**
     * Cancels the event and sends the island public message to user
     * @param e Event
     */
    public void noGo(Event e) {
        noGo(e, false);
    }

    /**
     * Cancels the event and sends the island protected message to user unless silent is true
     * @param e Event
     * @param silent - if true, message is not sent
     */
    public void noGo(Event e, boolean silent) {
        if (e instanceof Cancellable)       
            ((Cancellable)e).setCancelled(true);
        if (user != null) {
            if (!silent)
                user.sendMessage("protection.protected");
            user.updateInventory();
        }
    }

    /**
     * Check if loc is in the island worlds
     * @param loc
     * @return true if the location is in the island worlds
     */
    public boolean inWorld(Location loc) {
        return (loc.getWorld().equals(plugin.getIslandWorldManager().getIslandWorld())
                || loc.getWorld().equals(plugin.getIslandWorldManager().getNetherWorld())
                || loc.getWorld().equals(plugin.getIslandWorldManager().getEndWorld())) ? true : false;
    }

    /**
     * Check if the entity is in the island worlds
     * @param entity - the entity
     * @return true if in world
     */
    public boolean inWorld(Entity entity) {
        return inWorld(entity.getLocation());
    }

    /**
     * Check if user is in the island worlds
     * @param user - a user
     * @return true if in world
     */
    public boolean inWorld(User user) {
        return inWorld(user.getLocation());
    }

    /**
     * Generic place blocks checker
     * @param e
     * @param loc
     * @param redstone
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(Event e, Location loc, Flags redstone) { 
        return checkIsland(e, loc, redstone, false);
    }



    /**
     * Check if flag is allowed
     * @param e
     * @param loc
     * @param silent - if true, no attempt is made to tell the user
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(Event e, Location loc, Flags flag, boolean silent) {
        
        // If the user is not set already, try to get it from the event
        if (user == null) {
            // Set the user associated with this event
            if (!createEventUser(e)) {
                // The user is not set, and the event does not hold a getPlayer, so return false
                // TODO: is this the correct handling here?
                Bukkit.getLogger().severe("Check island had no associated user! " + e.getEventName());
                return false;
            }
        }
        // If this is not an Island World, skip
        if (!inWorld(user)) return true;
        
        // Get the island and if present, check the flag, react if required and return
        Optional<Island> island = plugin.getIslands().getIslandAt(loc);
        
        if (island.isPresent()) {
            if (!island.get().isAllowed(user, flag)) {
                noGo(e, silent);
                // Clear the user for the next time
                user = null;
                return false;
            } else {
                user = null;
                return true;
            }
        }

        // The player is in the world, but not on an island, so general world settings apply
        if (!isAllowed(flag)) {
            noGo(e, silent);
            user = null;
            return false;
        } else {
            user = null;
            return true;
        }
    }

    /**
     * Get the flag for this ID
     * @param flag
     * @return Flag denoted by the id
     */
    protected Flag id(Flags flag) {
        return plugin.getFlagsManager().getFlagByID(flag);
    }
    
    protected boolean isAllowed(Flags flag) {
       return plugin.getFlagsManager().isAllowed(flag); 
    }
}
