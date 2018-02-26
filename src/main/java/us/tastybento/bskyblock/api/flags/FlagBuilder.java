package us.tastybento.bskyblock.api.flags;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.flags.Flag.Type;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;

public class FlagBuilder {

    private String id;
    private Material icon;
    private Listener listener;
    private boolean defaultSetting;
    private Type type = Type.PROTECTION;

    public FlagBuilder id(String string) {
        id = string;
        return this;
    }

    public FlagBuilder icon(Material icon) {
        this.icon = icon;
        return this;
    }

    public FlagBuilder listener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public Flag build() {
        return new Flag(id, icon, listener, defaultSetting, type);
    }

    /**
     * Sets the default setting for this flag in the world
     * @param setting
     * @return FlagBuilder
     */
    public FlagBuilder allowedByDefault(boolean setting) {
        defaultSetting = setting;
        return this;
    }

    /**
     * Set the type of this flag
     * @param type {@link Type}
     * @return FlagBuilder
     */
    public FlagBuilder type(Type type) {
        this.type = type;
        return this;
    }

    /**
     * Set the id of this flag to the name of this enum value
     * @param flag
     * @return FlagBuilder
     */
    public FlagBuilder id(Enum<?> flag) {
        id = flag.name();
        return this;
    }
}
