package world.bentobox.bentobox.api.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * @author Poslovitch
 */
public abstract class Hook {

    private String pluginName;

    public Hook(String pluginName) {
        if (pluginName == null || pluginName.isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be null nor empty.");
        }
        this.pluginName = pluginName;
    }

    /**
     * Returns the name of the plugin related to this Hook.
     * Cannot be null.
     * @return the plugin name.
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Returns the Plugin instance related to this Hook or null if it could not be found.
     * @return the Plugin instance of the plugin this Hook hooks into.
     */
    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin(pluginName);
    }

    /**
     * Returns whether the plugin is available or not.
     * @return true if the plugin is available, false otherwise.
     */
    public boolean isPluginAvailable() {
        return getPlugin() != null && getPlugin().isEnabled();
    }

    /**
     * Tries to hook into the plugin and returns whether it succeeded or not.
     * @return true if it successfully hooked into the plugin, false otherwise.
     */
    public abstract boolean hook();
}
