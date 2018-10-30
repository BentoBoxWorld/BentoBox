package world.bentobox.bentobox.managers;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

import java.util.List;
import java.util.Optional;

/**
 * @author Poslovitch
 */
public class HooksManager {

    private BentoBox plugin;
    private List<Hook> hooks;

    public HooksManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    public void registerHook(Hook hook) {
        if (hook.isPluginAvailable()) {
            plugin.log("Hooking with " + hook.getPluginName() + "...");
            if (hook.hook()) {
                hooks.add(hook);
            } else {
                plugin.log("Could not hook with " + hook.getPluginName() + ". Skipping...");
            }
        }
    }

    public List<Hook> getHooks() {
        return hooks;
    }

    public Optional<Hook> getHook(String pluginName) {
        return hooks.stream().filter(hook -> hook.getPluginName().equals(pluginName)).findFirst();
    }
}
