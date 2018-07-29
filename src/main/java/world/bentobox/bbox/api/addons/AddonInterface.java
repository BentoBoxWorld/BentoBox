package world.bentobox.bbox.api.addons;

public interface AddonInterface {
    void onEnable();
    void onDisable();
    default void onLoad() {}
}
