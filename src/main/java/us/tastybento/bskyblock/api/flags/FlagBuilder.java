package us.tastybento.bskyblock.api.flags;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.flags.Flag.FlagType;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;

public class FlagBuilder {

    private String id;
    private PanelItem icon;
    private Listener listener;
    private boolean defaultSetting;
    private FlagType type = FlagType.PROTECTION;

    public FlagBuilder id(String string) {
        id = string;
        return this;
    }

    public FlagBuilder icon(Material icon) {
        icon(new PanelItemBuilder().icon(new ItemStack(icon)).build());
        return this;
    }

    public FlagBuilder icon(PanelItem icon) {
        this.icon = icon;
        //TODO: if icon don't have a clickhandler, add the default one
        //TODO: if icon don't have a display name, set it to the default reference
        //TODO: if icon don't have a lore, set it to the default one
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
     * @return
     */
    public FlagBuilder allowedByDefault(boolean setting) {
        defaultSetting = setting;
        return this;
    }

    /**
     * Set the type of this flag
     * @param type {@link FlagType}
     * @return FlagBuilder
     */
    public FlagBuilder type(FlagType type) {
        this.type = type;
        return this;
    }

    /**
     * Set the id of this flag to the name of this enum value
     * @param flag
     * @return
     */
    public FlagBuilder id(Enum<?> flag) {
        id = flag.name();
        return this;
    }
}
