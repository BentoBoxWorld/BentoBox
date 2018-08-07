package world.bentobox.bentobox.api.flags;

import java.lang.reflect.Method;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * Abstract class for flag listeners. Provides common code.
 * @author tastybento
 *
 */
public abstract class AbstractFlagListener implements Listener {

    private BentoBox plugin = BentoBox.getInstance();
    private User user = null;

    /**
     * @return the plugin
     */
    public BentoBox getPlugin() {
        return plugin;
    }

    /**
     * Used for unit testing only to set the plugin
     * @param plugin - plugin object
     */
    public void setPlugin(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets the player associated with this event.
     * If the user is a fake player, they are not counted.
     * @param e - event
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
        } catch (Exception e1) {  // Do nothing
        }
        return false;
    }

    /**
     * Explicitly set the user for the next {@link #checkIsland(Event, Location, Flag)} or {@link #checkIsland(Event, Location, Flag, boolean)}
     * @param user - the User
     */
    public AbstractFlagListener setUser(User user) {
        if (!plugin.getSettings().getFakePlayers().contains(user.getName())) {
            this.user = user;
        }
        return this;
    }

    /*
     * The following methods cover the cancellable events and enable a simple noGo(e) to be used to cancel and send the error message
     */

    /**
     * Cancels the event and sends the island public message to user
     * @param e - event
     * @param flag - the flag that has been checked
     */
    public void noGo(Event e, Flag flag) {
        noGo(e, flag, false);
    }

    /**
     * Cancels the event and sends the island protected message to user unless silent is true
     * @param e - event
     * @param flag - the flag that has been checked
     * @param silent - if true, message is not sent
     */
    public void noGo(Event e, Flag flag, boolean silent) {
        if (e instanceof Cancellable) {
            ((Cancellable)e).setCancelled(true);
        }
        if (user != null) {
            if (!silent) {
                user.notify("protection.protected", TextVariables.DESCRIPTION, user.getTranslation(flag.getHintReference()));
            }
            user.updateInventory();
        }
    }

    /**
     * Check if flag is allowed at location
     * @param e - event
     * @param loc - location
     * @param flag - flag {@link world.bentobox.bentobox.lists.Flags}
     * @return true if allowed, false if not
     */
    public boolean checkIsland(Event e, Location loc, Flag flag) {
        return checkIsland(e, loc, flag, false);
    }

    /**
     * Check if flag is allowed at location
     * @param e - event
     * @param loc - location
     * @param flag - flag {@link world.bentobox.bentobox.lists.Flags}
     * @param silent - if true, no attempt is made to tell the user
     * @return true if the check is okay, false if it was disallowed
     */
    public boolean checkIsland(Event e, Location loc, Flag flag, boolean silent) {
        // If this is not an Island World, skip
        if (!plugin.getIWM().inWorld(loc)) {
            return true;
        }
        // Get the island and if present
        Optional<Island> island = getIslands().getProtectedIslandAt(loc);
        // Handle Settings Flag
        if (flag.getType().equals(Flag.Type.SETTING)) {
            // If the island exists, return the setting, otherwise return the default setting for this flag
            return island.map(x -> x.isAllowed(flag)).orElse(flag.isSetForWorld(loc.getWorld()));
        }

        // Protection flag
        // If the user is not set already, try to get it from the event
        // Set the user associated with this event
        // The user is not set, and the event does not hold a getPlayer, so return false
        // TODO: is this the correct handling here?
        if (user == null && !createEventUser(e)) {
            plugin.logError("Check island had no associated user! " + e.getEventName());
            return false;
        }

        // Ops or "bypass everywhere" moderators can do anything
        if (user.isOp() || user.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + ".mod.bypass." + flag.getID() + ".everywhere")) {
            user = null;
            return true;
        }

        // Check if the plugin is set in User (required for testing)
        User.setPlugin(plugin);

        if (island.isPresent()) {
            // If it is not allowed on the island, "bypass island" moderators can do anything
            if (!island.get().isAllowed(user, flag) && !user.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + ".mod.bypass." + flag.getID() + ".island")) {
                noGo(e, flag, silent);
                // Clear the user for the next time
                user = null;
                return false;
            } else {
                user = null;
                return true;
            }
        }
        // The player is in the world, but not on an island, so general world settings apply
        if (!flag.isSetForWorld(loc.getWorld())) {
            noGo(e, flag, silent);
            user = null;
            return false;
        } else {
            user = null;
            return true;
        }
    }

    /**
     * Get the flag for this ID
     * @param id - the flag ID
     * @return Flag denoted by the id
     */
    protected Flag id(String id) {
        return plugin.getFlagsManager().getFlagByID(id);
    }

    /**
     * Get the island database manager
     * @return the island database manager
     */
    protected IslandsManager getIslands() {
        return plugin.getIslands();
    }

    /**
     * Get the island world manager
     * @return Island World Manager
     */
    protected IslandWorldManager getIWM() {
        return plugin.getIWM();
    }
}
