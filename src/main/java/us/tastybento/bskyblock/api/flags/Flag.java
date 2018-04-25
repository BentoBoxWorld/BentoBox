package us.tastybento.bskyblock.api.flags;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;

public class Flag implements Comparable<Flag> {

    public enum Type {
        PROTECTION,
        SETTING
    }

    private final String id;
    private final Material icon;
    private final Listener listener;
    private final Type type;
    private boolean defaultSetting;

    Flag(String id, Material icon, Listener listener, boolean defaultSetting, Type type) {
        this.id = id;
        this.icon = icon;
        this.listener = listener;
        this.defaultSetting = defaultSetting;
        this.type = type;
    }

    public String getID() {
        return id;
    }

    public Material getIcon() {
        return icon;
    }

    public Optional<Listener> getListener() {
        return Optional.ofNullable(listener);
    }

    /**
     * @return - true means it is allowed. false means it is not allowed
     */
    public boolean isDefaultSetting() {
        return defaultSetting;
    }

    /**
     * Set the status of this flag for locations outside of island spaces
     * @param defaultSetting - true means it is allowed. false means it is not allowed
     */
    public void setDefaultSetting(boolean defaultSetting) {
        this.defaultSetting = defaultSetting;
    }

    /**
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Flag)) {
            return false;
        }
        Flag other = (Flag) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }

        return type == other.type;
    }

    public PanelItem toPanelItem(User user) {
        return new PanelItemBuilder()
                .icon(new ItemStack(icon))
                .name(user.getTranslation("protection.panel.flag-item.name-layout", "[name]", user.getTranslation("protection.flags." + id + ".name")))
                .description(user.getTranslation("protection.panel.flag-item.description-layout",
                        "[description]", user.getTranslation("protection.flags." + id + ".description"),
                        "[rank]", "Owner"))
                .clickHandler((clicker, click) -> {
                    clicker.sendRawMessage("You clicked on : " + id);
                    return true;
                })
                .build();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Flag [id=" + id + ", icon=" + icon + ", type=" + type + ", defaultSetting=" + defaultSetting + "]";
    }

    @Override
    public int compareTo(Flag o) {
        return getID().compareTo(o.getID());
    }
}
