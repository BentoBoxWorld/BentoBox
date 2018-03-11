package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.lists.Flags;

/**
 * @author Poslovitch
 * @author tastybento
 */
public class FlagsManager {

    private BSkyBlock plugin;
    private List<Flag> flags = new ArrayList<>();

    /**
     * Stores the flag listeners that have already been registered into Bukkit's API to avoid duplicates.
     */
    private ArrayList<Listener> registeredListeners = new ArrayList<>();

    public FlagsManager(BSkyBlock plugin) {
        this.plugin = plugin;

        // Register default flags
        Flags.values().forEach(this::registerFlag);
    }

    /**
     * Register a new flag with BSkyBlock
     * @param flag flag to be registered
     * @return true if successfully registered, false if not, e.g., because one with the same ID already exists
     */
    public boolean registerFlag(Flag flag) {
        // Check in case the flag id or icon already exists
        for (Flag fl : flags) {
            if (fl.getID().equals(flag.getID()) || fl.getIcon().equals(flag.getIcon())) {
                return false;
            }
        }
        flags.add(flag);
        // If there is a listener which is not already registered, register it into Bukkit.
        flag.getListener().ifPresent(l -> {
            if (!registeredListeners.contains(l)) {
                Bukkit.getServer().getPluginManager().registerEvents(l, plugin);
                registeredListeners.add(l);
            }
        });
        return true;
    }

    /**
     * @return list of all flags
     */
    public List<Flag> getFlags() {
        return flags;
    }

    /**
     * Get flag by ID
     * @param id unique id for this flag
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String id) {
        return flags.stream().filter(flag -> flag.getID().equals(id)).findFirst().orElse(null);
    }
}
