package us.tastybento.bskyblock.util.placeholders;

import org.bukkit.command.CommandSender;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Simple interface for every Placeholder API.
 * 
 * @author Poslovitch
 */
public interface PlaceholderInterface {
    
    /**
     * Get the name of the Placeholder API
     * @return name of the placeholder plugin
     */
    String getName();
    
    /**
     * Register the placeholder API
     * @param plugin
     * @return true if registered
     */
    boolean register(BSkyBlock plugin);
    
    /**
     * Unregister the placeholder API
     * @param plugin
     */
    void unregister(BSkyBlock plugin);
    
    /**
     * Replace placeholders in the message according to the receiver
     * @param sender
     * @param message
     * @return updated message
     */
    String replacePlaceholders(CommandSender receiver, String message);
}