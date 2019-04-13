package world.bentobox.bentobox.api.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Poslovitch
 */
public abstract class Hook {

    private @NonNull String pluginName;
    private @NonNull Material icon;

    public Hook(@NonNull String pluginName, @NonNull Material icon) {
        if (pluginName.isEmpty()) {
            throw new IllegalArgumentException("Plugin name cannot be empty.");
        }
        this.pluginName = pluginName;
        this.icon = icon;
    }

    /**
     * Returns the name of the plugin related to this Hook.
     * @return the plugin name.
     */
    @NonNull
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Returns the icon representing this Hook.
     * @return the icon.
     */
    @NonNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Returns the Plugin instance related to this Hook or null if it could not be found.
     * @return the Plugin instance of the plugin this Hook hooks into.
     */
    @Nullable
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

    /**
     * Returns an explanation that will be sent to the user to tell them why the hook process did not succeed.
     * @return the probable causes why the hook process did not succeed.
     */
    public abstract String getFailureCause();
}
