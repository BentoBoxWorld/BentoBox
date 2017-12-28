package us.tastybento.bskyblock.api.addons;

public interface AddOnInterface {
    public abstract void onEnable();
    public abstract void onDisable();
    public default void onLoad() {};
}
