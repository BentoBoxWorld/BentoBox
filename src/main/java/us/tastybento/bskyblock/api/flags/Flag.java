package us.tastybento.bskyblock.api.flags;

import org.bukkit.event.Listener;
import us.tastybento.bskyblock.api.panels.PanelItem;

import java.util.Optional;

public class Flag {

    private String id;
    private PanelItem icon;
    private Optional<Listener> listener;

    public Flag(String id, PanelItem icon, Optional<Listener> listener) {
        this.id = id;
        this.icon = icon;
        this.listener = listener;
    }

    public String getID() {
        return id;
    }

    public PanelItem getIcon() {
        return icon;
    }

    public Optional<Listener> getListener() {
        return listener;
    }
}
