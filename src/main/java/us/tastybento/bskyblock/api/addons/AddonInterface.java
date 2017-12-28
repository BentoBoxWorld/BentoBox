package us.tastybento.bskyblock.api.addons;

public interface AddonInterface {
    void onEnable();
    void onDisable();
    default void onLoad() {}
}
