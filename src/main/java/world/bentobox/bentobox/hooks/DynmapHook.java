package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.hooks.Hook;

/**
 * @author Poslovitch
 * @since 1.5.0
 */
public class DynmapHook extends Hook {

    private MarkerAPI markerAPI;

    @NonNull
    private Map<@NonNull GameModeAddon, @NonNull MarkerSet> markerSets;

    public DynmapHook() {
        super("dynmap", Material.FILLED_MAP);
        this.markerSets = new HashMap<>();
    }

    @Override
    public boolean hook() {
        DynmapAPI dynmapAPI = (DynmapAPI) getPlugin();
        MarkerAPI markers = dynmapAPI.getMarkerAPI();

        if (markers == null) {
            return false;
        }
        markerAPI = markers;

        BentoBox.getInstance().getAddonsManager().getGameModeAddons().forEach(this::registerMarkerSet);

        return true;
    }

    public void registerMarkerSet(@NonNull GameModeAddon addon) {
        String name = addon.getDescription().getName();
        if (getMarkerSet(addon) == null) {
            // From the javadoc: createMarkerSet(String id, String label, Set<MarkerIcon> allowedIcons, boolean persistent)
            MarkerSet set = markerAPI.createMarkerSet(name.toLowerCase() + ".markers", name, null, true);
            markerSets.put(addon, set);
        }
    }

    @NonNull
    public Map<GameModeAddon, MarkerSet> getMarkerSets() {
        return markerSets;
    }

    @Nullable
    public MarkerSet getMarkerSet(@NonNull GameModeAddon addon) {
        if (markerSets.containsKey(addon)) {
            return markerSets.get(addon);
        } else {
            return markerAPI.getMarkerSet(addon.getDescription().getName().toLowerCase() + ".markers");
        }
    }

    /**
     * Returns the MarkerAPI instance. Not null.
     * @return the MarkerAPI instance.
     */
    @NonNull
    public MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    @Override
    public String getFailureCause() {
        return "the version of dynmap you're using is incompatible with this hook";
    }
}