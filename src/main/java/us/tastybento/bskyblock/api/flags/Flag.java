package us.tastybento.bskyblock.api.flags;

import java.util.Optional;

import org.bukkit.event.Listener;

import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.lists.Flags;

public class Flag  implements Comparable<Flag> {

    public enum FlagType {
        PROTECTION,
        SETTING
    }
    
    private final Flags id;
    private final PanelItem icon;
    private final Listener listener;
    private final FlagType type;
    private boolean defaultSetting;
    
    public Flag(Flags id2, PanelItem icon, Listener listener, boolean defaultSetting, FlagType type) {
        this.id = id2;
        this.icon = icon;
        this.listener = listener;
        this.type = type;
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

    public boolean isDefaultSetting() {
        return defaultSetting;
    }

    public void setDefaultSetting(boolean defaultSetting) {
        this.defaultSetting = defaultSetting;
    }

    /**
     * @return the type
     */
    public FlagType getType() {
        return type;
    }

    @Override
    public int compareTo(Flag o) {
        return id.compareTo(o.getID());
    }
}
