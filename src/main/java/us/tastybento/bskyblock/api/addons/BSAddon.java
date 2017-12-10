package us.tastybento.bskyblock.api.addons;

public abstract class BSAddon {

    private AddonDescription description;
    private boolean enabled;

    public abstract void enable();
    public abstract void disable();
    public abstract void load();

    public AddonDescription getDescription() {
        return description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
