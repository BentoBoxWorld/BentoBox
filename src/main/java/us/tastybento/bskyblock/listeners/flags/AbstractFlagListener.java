/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Abstract class for flag listeners. Provides common code.
 * @author tastybento
 *
 */
public abstract class AbstractFlagListener implements Listener {

    public BSkyBlock plugin;
    private User user = null;

    public AbstractFlagListener() {
        super();
        this.plugin = BSkyBlock.getInstance();
    }

    /**
     * Sets the player associated with this event.
     * If the user is a fake player, they are not counted.
     * @param e - the event
     * @return user or empty
     */
    private Optional<User> createEventUser(Event e) {
        // Set the user
        if (e instanceof PlayerEvent) {
            user = User.getInstance(((PlayerEvent)e).getPlayer());
            // Handle fake players
            if (plugin.getSettings().getFakePlayers().contains(user.getName())) user = null;
        }       
        return Optional.ofNullable(user);
    }

    /**
     * Explicitly set the user
     * @param user
     */
    public void setUser(User user) {
        this.user = user;
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
     * Cancels the event and sends the island public message to user unless silent is true
     * @param e Event
     * @param silent - if true, message is not sent
     */
    public void noGo(Event e, boolean silent) {
        if (e instanceof Cancellable)       
            ((Cancellable)e).setCancelled(true);
        if (user != null) {
            if (!silent)
                user.sendMessage("protection.public");
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
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(Event e, Location loc, Flag flag) { 
        return checkIsland(e, loc, flag, false);
    }



    /**
     * Generic place blocks checker
     * @param e
     * @param loc
     * @param silent - if true, no attempt is made to tell the user
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(Event e, Location loc, Flag flag, boolean silent) {
        // If the user is not set, try to get it from the event
        if (user == null) {
            // Set the user associated with this event
            if (!createEventUser(e).isPresent()) return true;
        }
        // If this is not an Island World, skip
        if (!inWorld(user)) return true;

        // Get the island and if present, check the flag, react if required and return
        Optional<Island> island = plugin.getIslands().getIslandAt(loc);
        
        if (island.isPresent()) {
            if (!island.get().isAllowed(user, flag)) {
                noGo(e, silent);
                return false;
            } else {
                return true;
            }
        }

        // The player is in the world, but not on an island, so general world settings apply
        if (!flag.isAllowed()) {
            noGo(e, silent);
            return false;
        } else {
            return true;
        }
        
    }

}
