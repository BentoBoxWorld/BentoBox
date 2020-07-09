package world.bentobox.bentobox.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author Poslovitch
 * @author tastybento
 */
public class FlagsManager {

    private @NonNull BentoBox plugin;
    private Map<@NonNull Flag, @Nullable Addon> flags = new HashMap<>();

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
        Flags.values().forEach(f -> registerFlag(null, f));
    }

    /**
     * Registers a new flag.
     * Consider using {@link #registerFlag(Addon, Flag)} instead if your flag declares a listener.
     * @param flag flag to be registered
     * @return true if successfully registered, false if not, e.g., because one with the same ID already exists
     * @see #registerFlag(Addon, Flag)
     */
    public boolean registerFlag(@NonNull Flag flag) {
        return registerFlag(null, flag);
    }

    /**
     * Registers a new flag.
     * @param addon - addon that is registering this flag
     * @param flag flag to be registered
     * @return true if successfully registered, false if not, e.g., because one with the same ID already exists
     * @since 1.5.0
     */
    public boolean registerFlag(@Nullable Addon addon, @NonNull Flag flag) {
        // Check in case the flag id or icon already exists
        for (Flag fl : flags.keySet()) {
            if (fl.getID().equals(flag.getID())) {
                return false;
            }
        }
        flags.put(flag, addon);
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
        if (!registeredListeners.computeIfAbsent(l, k -> false)) {
            Bukkit.getPluginManager().registerEvents(l, plugin);
            registeredListeners.put(l, true);
        }
    }

    /**
     * @return list of all flags
     */
    @NonNull
    public List<Flag> getFlags() {
        return new ArrayList<>(flags.keySet());
    }

    /**
     * Gets a Flag by providing an ID.
     * @param id Unique ID for this Flag.
     * @return Optional containing the Flag instance or empty.
     * @since 1.1
     */
    @NonNull
    public Optional<Flag> getFlag(@NonNull String id) {
        return flags.keySet().stream().filter(flag -> flag.getID().equals(id)).findFirst();
    }

    /**
     * Unregister flags for addon
     * @param addon - addon
     * @since 1.5.0
     */
    public void unregister(@NonNull Addon addon) {
        // Unregister listeners
        flags.entrySet().stream().filter(e -> addon.equals(e.getValue())).map(Map.Entry::getKey)
        .forEach(f -> f.getListener().ifPresent(HandlerList::unregisterAll));
        // Remove flags
        flags.values().removeIf(addon::equals);
    }

    /**
     * Unregister a specific flag
     * @param flag - flag
     * @since 1.14.0
     */
    public void unregister(@NonNull Flag flag) {
        // Unregister any listener
        flag.getListener().ifPresent(HandlerList::unregisterAll);
        // Remove flag
        flags.remove(flag);
    }
}
