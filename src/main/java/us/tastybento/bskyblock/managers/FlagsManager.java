package us.tastybento.bskyblock.managers;

import java.util.HashMap;

import org.bukkit.Bukkit;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.lists.Flags;

public class FlagsManager {

    private BSkyBlock plugin;
    private HashMap<String, Flag> flags = new HashMap<>();


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
        //Bukkit.getLogger().info("DEBUG: registering flag " + flag.getID());
        flags.put(flag.getID(), flag);
        // If there is a listener, register it into Bukkit.
        flag.getListener().ifPresent(l -> Bukkit.getServer().getPluginManager().registerEvents(l, plugin));
    }

    public HashMap<String, Flag> getFlags() {
        return flags;
    }

    /**
     * Get flag by string
     * @param key - string name same as the enum
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String id) {
        //Bukkit.getLogger().info("DEBUG: requesting " + id + " flags size = " + flags.size());
        return flags.get(id);
    }

    public Flag getFlagByIcon(PanelItem item) {
        for (Flag flag : flags.values()) {
            if (flag.getIcon().equals(item)) return flag;
        }
        return null;
    }

}
