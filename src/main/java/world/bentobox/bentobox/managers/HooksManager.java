package world.bentobox.bentobox.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * @author Poslovitch
 */
public class HooksManager {

    private final BentoBox plugin;
    /**
     * List of successfully registered hooks.
     */
    private final Map<String, Hook> hooks;

    public HooksManager(BentoBox plugin) {
        this.plugin = plugin;
        this.hooks = new HashMap<>();
    }

    public void registerHook(@NonNull Hook hook) {
        if (hook.isPluginAvailable()) {
            plugin.log("Hooking with " + hook.getPluginName() + "...");
            if (hook.hook()) {
                hooks.put(hook.getPluginName(), hook);
            } else {
                plugin.logError("Could not hook with " + hook.getPluginName() + ((hook.getFailureCause() != null) ? " because: " + hook.getFailureCause() : "") + ". Skipping...");
            }
        }
        // Do not tell the user if we couldn't hook with a plugin which is not available.
        // We may have in the near future almost ~25 hooks, which would basically spam the console and make users nervous.
    }

    /**
     * Returns the list of successfully registered hooks.
     * @return list of successfully registered hooks.
     */
    public List<Hook> getHooks() {
        return List.copyOf(hooks.values());
    }

    public Optional<Hook> getHook(String pluginName) {
        return Optional.ofNullable(hooks.get(pluginName));
    }
}
