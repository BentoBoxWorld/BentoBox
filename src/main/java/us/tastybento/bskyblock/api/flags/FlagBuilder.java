package us.tastybento.bskyblock.api.flags;

import org.bukkit.event.Listener;
import us.tastybento.bskyblock.api.panels.PanelItem;

import java.util.Optional;

public class FlagBuilder {

    private String id;
    private PanelItem icon;
    private Optional<Listener> listener = Optional.empty();

    public FlagBuilder id(String id) {
        this.id = id;
        return this;
    }

    public FlagBuilder icon(PanelItem icon) {
        this.icon = icon;
        return this;
    }

    public FlagBuilder listener(Listener listener) {
        this.listener = Optional.of(listener);
        return this;
    }

    public void build() {
        new Flag(id, icon, listener);
    }
}
