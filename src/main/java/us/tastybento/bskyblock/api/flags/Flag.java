package us.tastybento.bskyblock.api.flags;

import java.util.Optional;

import org.bukkit.event.Listener;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.PanelItem;

public class Flag {

    private String id;
    private PanelItem icon;
    private Listener listener;
    private boolean defaultSetting;

    public Flag(String id, PanelItem icon, Listener listener, boolean defaultSetting) {
        this.id = id;
        this.icon = icon;
        this.listener = listener;
        BSkyBlock.getInstance().getFlagsManager().registerFlag(this);
    }

    public String getID() {
        return id;
    }

    public PanelItem getIcon() {
        return icon;
    }

    public Optional<Listener> getListener() {
        return Optional.of(listener);
    }

    public boolean isAllowed() {
        return defaultSetting;
    }

    public void setDefaultSetting(boolean defaultSetting) {
        this.defaultSetting = defaultSetting;
    }
}
