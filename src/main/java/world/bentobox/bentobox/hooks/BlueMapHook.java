package world.bentobox.bentobox.hooks;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 * @since 2.1.0
 */
public class BlueMapHook extends Hook implements Listener {

    private BlueMapAPI api;

    public BlueMapHook() {
        super("BlueMap", Material.MAP);
    }

    @Override
    public boolean hook() {
        if (BlueMapAPI.getInstance().isPresent()) {
            api = BlueMapAPI.getInstance().get();
        } else {
            return false;
        }
        // Register the islands known at hook time
        BentoBox.getInstance().getAddonsManager().getGameModeAddons().forEach(this::getMarkerSet);
        return true;
    }

    public void getMarkerSet(@NonNull GameModeAddon addon) {
        MarkerSet markerSet = MarkerSet.builder().label(addon.getWorldSettings().getFriendlyName()).build();
        // Register the island name for each island in this addon
        BentoBox.getInstance().getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null).forEach(island -> {
                    String name = getIslandName(island);
                    POIMarker marker = POIMarker.builder().label(name)
                    .position(island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ())
                    .maxDistance(1000).build();
            markerSet.getMarkers().put(addon.getWorldSettings().getFriendlyName(), marker);
        });
        // Over world
        api.getWorld(addon.getOverWorld()).ifPresent(world -> {
            for (BlueMapMap map : world.getMaps()) {
                map.getMarkerSets().put(addon.getWorldSettings().getFriendlyName(), markerSet);
            }
        });
        // Nether
        if (addon.getWorldSettings().isNetherGenerate() && addon.getWorldSettings().isNetherIslands()) {
            api.getWorld(addon.getNetherWorld()).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    map.getMarkerSets().put(addon.getWorldSettings().getFriendlyName(), markerSet);
                }
            });
        }
        // End
        if (addon.getWorldSettings().isEndGenerate() && addon.getWorldSettings().isEndIslands()) {
            api.getWorld(addon.getEndWorld()).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    map.getMarkerSets().put(addon.getWorldSettings().getFriendlyName(), markerSet);
                }
            });
        }
    }

    private String getIslandName(Island island) {
        if (island.getName() != null && !island.getName().isBlank()) {
            // Name has been set
            return island.getName();
        } else if (island.getOwner() != null) {
            return User.getInstance(island.getOwner()).getDisplayName();
        }
        return "";
    }

    @Override
    public String getFailureCause() {
        return "the version of BlueMap is incompatible with this hook. Use a newer version.";
    }
}