package world.bentobox.bentobox.api.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * @author Poslovitch
 */
public abstract class BaseHook implements Hook {

    private String pluginName;

    public BaseHook(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin(pluginName);
    }

    @Override
    public boolean isPluginAvailable() {
        return getPlugin() != null && getPlugin().isEnabled();
    }
}
