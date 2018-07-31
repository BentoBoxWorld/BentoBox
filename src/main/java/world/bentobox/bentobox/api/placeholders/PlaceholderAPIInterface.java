package world.bentobox.bentobox.api.placeholders;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

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
     * @param plugin - plugin object
     * @return true if successfully registered
     */
    boolean register(BentoBox plugin);

    /**
     * Unregisters the placeholder API
     * @param plugin - plugin object
     */
    void unregister(BentoBox plugin);

    /**
     * Replace placeholders in the message according to the receiver
     * @param receiver - user who will receive the message
     * @param message - message
     * @return updated message
     */
    String replacePlaceholders(User receiver, String message);
}