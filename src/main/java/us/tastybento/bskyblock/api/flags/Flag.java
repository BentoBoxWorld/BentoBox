package us.tastybento.bskyblock.api.flags;

import java.util.Optional;

import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.lists.Flags;

public class Flag  implements Comparable<Flag> {

    private final Flags id;
    private final PanelItem icon;
    private final Listener listener;
    private boolean defaultSetting;
    
    public Flag(BSkyBlock plugin, Flags id2, PanelItem icon, Listener listener, boolean defaultSetting) {
        this.id = id2;
        this.icon = icon;
        this.listener = listener;
        //System.out.println("DEBUG: " + plugin);
        //System.out.println("DEBUG: " + plugin.getFlagsManager());
        plugin.getFlagsManager().registerFlag(this);
    }

    public Flags getID() {
        return id;
    }

    public PanelItem getIcon() {
        return icon;
    }

    public Optional<Listener> getListener() {
        return Optional.ofNullable(listener);
    }

    public boolean isAllowed() {
        return defaultSetting;
    }

    public void setDefaultSetting(boolean defaultSetting) {
        this.defaultSetting = defaultSetting;
    }

    @Override
    public int compareTo(Flag o) {
        return id.compareTo(o.getID());
    }
}
