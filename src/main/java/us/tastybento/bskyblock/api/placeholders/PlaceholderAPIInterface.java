package us.tastybento.bskyblock.api.placeholders;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;

/**
 * Simple interface for every Placeholder API.
 *
 * @author Poslovitch
 */
public interface PlaceholderAPIInterface {

    /**
     * Gets the name of the Placeholder API
     * @return name of the placeholder plugin
     */
    String getName();

    /**
     * Registers the placeholder API
     * @param plugin - BSkyBlock plugin object
     * @return true if successfully registered
     */
    boolean register(BSkyBlock plugin);

    /**
     * Unregisters the placeholder API
     * @param plugin - BSkyBlock plugin object
     */
    void unregister(BSkyBlock plugin);

    /**
     * Replace placeholders in the message according to the receiver
     * @param receiver
     * @param message
     * @return updated message
     */
    String replacePlaceholders(User receiver, String message);
}