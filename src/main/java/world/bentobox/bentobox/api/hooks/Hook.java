package world.bentobox.bentobox.api.hooks;

import org.bukkit.plugin.Plugin;

/**
 * @author Poslovitch
 */
public interface Hook {

    /**
     * Returns the Plugin instance related to this Hook or null if it could not be found.
     * @return the Plugin instance of the plugin this Hook hooks into.
     */
    Plugin getPlugin();

    /**
     * Returns whether the plugin is available or not.
     * @return true if the plugin is available, false otherwise.
     */
    boolean isPluginAvailable();

    /**
     * Tries to hook into the plugin and returns whether it succeeded or not.
     * @return true if it successfully hooked into the plugin, false otherwise.
     */
    boolean hook();
}
