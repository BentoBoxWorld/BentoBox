package world.bentobox.bentobox.api.addons;

public interface AddonInterface {
    void onEnable();
    void onDisable();
    default void onLoad() {}
}
