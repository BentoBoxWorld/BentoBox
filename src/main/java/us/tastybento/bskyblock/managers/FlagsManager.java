package us.tastybento.bskyblock.managers;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.FlagType;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.lists.Flag;

/**
 * @author Poslovitch
 * @author tastybento
 */
public class FlagsManager {

    private BSkyBlock plugin;
    private HashMap<String, FlagType> flags = new HashMap<>();

    /**
     * Stores the flag listeners that have already been registered into Bukkit's API to avoid duplicates.
     */
    private ArrayList<Listener> registeredListeners = new ArrayList<>();

    public FlagsManager(BSkyBlock plugin) {
        this.plugin = plugin;

        // Register default flags
        for (FlagType flag : Flag.values()) {
            registerFlag(flag);
        }
    }

    /**
     * Register a new flag with BSkyBlock
     * @param flag
     */
    public void registerFlag(FlagType flag) {
        //Bukkit.getLogger().info("DEBUG: registering flag " + flag.getID());
        flags.put(flag.getID(), flag);
        // If there is a listener which is not already registered, register it into Bukkit.
        flag.getListener().ifPresent(l -> {
            if (!registeredListeners.contains(l)) {
                Bukkit.getServer().getPluginManager().registerEvents(l, plugin);
                registeredListeners.add(l);
            }
        });
    }

    public HashMap<String, FlagType> getFlags() {
        return flags;
    }

    /**
     * Get flag by ID
     * @param id
     * @return Flag or null if not known
     */
    public FlagType getFlagByID(String id) {
        //Bukkit.getLogger().info("DEBUG: requesting " + id + " flags size = " + flags.size());
        return flags.get(id);
    }

    public FlagType getFlagByIcon(PanelItem item) {
        for (FlagType flag : flags.values()) {
            if (flag.getIcon().equals(item)) {
                return flag;
            }
        }
        return null;
    }

}
