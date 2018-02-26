package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.PanelItem;
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
        for (Flag flag : Flags.values()) {
            registerFlag(flag);
        }
    }

    /**
     * Register a new flag with BSkyBlock
     * @param flag
     */
    public void registerFlag(Flag flag) {
        //TODO throw an exception in case someone registers a flag with an existing id?
        flags.add(flag);
        // If there is a listener which is not already registered, register it into Bukkit.
        flag.getListener().ifPresent(l -> {
            if (!registeredListeners.contains(l)) {
                Bukkit.getServer().getPluginManager().registerEvents(l, plugin);
                registeredListeners.add(l);
            }
        });

        // Sorts the list
    }

    public List<Flag> getFlags() {
        return flags;
    }

    /**
     * Get flag by ID
     * @param id
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String id) {
        for (Flag flag : flags) {
            if (flag.getID().equals(id)) {
                return flag;
            }
        }
        return null;
    }

    public Flag getFlagByIcon(Material icon) {
        for (Flag flag : flags) {
            if (flag.getIcon().equals(icon)) {
                return flag;
            }
        }
        return null;
    }

}
