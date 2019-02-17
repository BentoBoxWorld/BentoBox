package world.bentobox.bentobox.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author Poslovitch
 * @author tastybento
 */
public class FlagsManager {

    private @NonNull BentoBox plugin;
    private List<@NonNull Flag> flags = new ArrayList<>();

    /**
     * Stores the flag listeners that have already been registered into Bukkit's API to avoid duplicates.
     * Value is true if the listener has been registered already.
     * This helps to make sure each flag listener is loaded correctly.
     * @see #registerListeners()
     */
    private Map<@NonNull Listener, @NonNull Boolean> registeredListeners = new HashMap<>();

    public FlagsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;

        // Register default flags
        Flags.values().forEach(this::registerFlag);
    }

    /**
     * Register a new flag
     * @param flag flag to be registered
     * @return true if successfully registered, false if not, e.g., because one with the same ID already exists
     */
    public boolean registerFlag(@NonNull Flag flag) {
        // Check in case the flag id or icon already exists
        for (Flag fl : flags) {
            if (fl.getID().equals(flag.getID())) {
                return false;
            }
        }
        flags.add(flag);
        // If there is a listener which is not already registered, register it into Bukkit if the plugin is fully loaded
        flag.getListener().ifPresent(this::registerListener);
        return true;
    }

    /**
     * Register any unregistered listeners.
     * This helps to make sure each flag listener is correctly loaded.
     */
    public void registerListeners() {
        registeredListeners.entrySet().stream().filter(e -> !e.getValue()).map(Map.Entry::getKey).forEach(this::registerListener);
    }

    /**
     * Tries to register a listener
     * @param l - listener
     */
    private void registerListener(@NonNull Listener l) {
        registeredListeners.putIfAbsent(l, false);
        if (!registeredListeners.get(l)) {
            Bukkit.getServer().getPluginManager().registerEvents(l, plugin);
            registeredListeners.put(l, true);
        }
    }

    /**
     * @return list of all flags
     */
    @NonNull
    public List<Flag> getFlags() {
        return flags;
    }

    /**
     * Gets a Flag by providing an ID.
     * @param id Unique ID for this Flag.
     * @return Optional containing the Flag instance or empty.
     * @since 1.1
     */
    @NonNull
    public Optional<Flag> getFlag(@NonNull String id) {
        return flags.stream().filter(flag -> flag.getID().equals(id)).findFirst();
    }
}
