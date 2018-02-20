package us.tastybento.bskyblock.api.flags;

import java.util.Optional;

import org.bukkit.event.Listener;

import us.tastybento.bskyblock.api.panels.PanelItem;

public class FlagType {

    public enum Type {
        PROTECTION,
        SETTING
    }

    private final String id;
    private final PanelItem icon;
    private final Listener listener;
    private final Type type;
    private boolean defaultSetting;

    public FlagType(String id2, PanelItem icon, Listener listener, boolean defaultSetting, Type type) {
        id = id2;
        this.icon = icon;
        this.listener = listener;
        this.type = type;
    }

    public String getID() {
        return id;
    }

    public PanelItem getIcon() {
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
        if (!(obj instanceof FlagType)) {
            return false;
        }
        FlagType other = (FlagType) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        return true;
    }

}
