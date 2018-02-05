package us.tastybento.bskyblock.api.flags;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.lists.Flags;

public class FlagBuilder {

    private Flags id;
    private PanelItem icon;
    private Listener listener;
    private boolean defaultSetting;

    public FlagBuilder id(Flags flag) {
        this.id = flag;
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

    public Flag build(BSkyBlock plugin) {
        return new Flag(plugin, id, icon, listener, defaultSetting);
    }
    
    /**
     * Sets the default setting for this flag in the world
     * @param setting
     * @return
     */
    public FlagBuilder allowedByDefault(boolean setting) {
        this.defaultSetting = setting;
        return this;
    }
}
