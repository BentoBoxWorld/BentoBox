package world.bentobox.bentobox.managers;

import java.awt.Color;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.MapHook;

/**
 * Facade for web-map integrations. Delegates calls to all active {@link MapHook}
 * implementations (BlueMap, Dynmap, etc.). If no map plugin is installed, all
 * methods silently do nothing.
 * <p>
 * Addons use this via {@code BentoBox.getInstance().getMapManager()}.
 *
 * @author tastybento
 * @since 3.12.0
 */
public class MapManager {

    private final BentoBox plugin;

    public MapManager(BentoBox plugin) {
        this.plugin = plugin;
    }

    private List<MapHook> getMapHooks() {
        return plugin.getHooks().getHooks().stream()
                .filter(MapHook.class::isInstance)
                .map(MapHook.class::cast)
                .toList();
    }

    /**
     * Returns true if at least one map plugin is hooked.
     *
     * @return true if a map hook is available
     */
    public boolean hasMapHook() {
        return !getMapHooks().isEmpty();
    }

    /**
     * Creates a marker set on all active map plugins.
     *
     * @param id    unique identifier for the marker set
     * @param label display label
     */
    public void createMarkerSet(@NonNull String id, @NonNull String label) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.createMarkerSet(id, label);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed createMarkerSet: " + e.getMessage());
            }
        }
    }

    /**
     * Removes a marker set and all its markers from all active map plugins.
     *
     * @param id the marker set ID
     */
    public void removeMarkerSet(@NonNull String id) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.removeMarkerSet(id);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed removeMarkerSet: " + e.getMessage());
            }
        }
    }

    /**
     * Removes all markers from a marker set without removing the set itself.
     *
     * @param id the marker set ID
     */
    public void clearMarkerSet(@NonNull String id) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.clearMarkerSet(id);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed clearMarkerSet: " + e.getMessage());
            }
        }
    }

    /**
     * Adds or updates a point marker on all active map plugins using the default icon.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param location    the location in the world
     */
    public void addPointMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull Location location) {
        addPointMarker(markerSetId, markerId, label, location, "default");
    }

    /**
     * Adds or updates a point marker on all active map plugins with a specific icon.
     * <p>
     * For <b>Dynmap</b>, the {@code iconName} maps to a built-in marker icon.
     * If the name is not recognized, {@code "default"} is used as a fallback.
     * Built-in Dynmap icon names:
     * {@code anchor}, {@code bank}, {@code basket}, {@code bed}, {@code beer},
     * {@code bighouse}, {@code blueflag}, {@code bomb}, {@code bookshelf},
     * {@code bricks}, {@code bronzemedal}, {@code bronzestar}, {@code building},
     * {@code cake}, {@code camera}, {@code cart}, {@code caution}, {@code chest},
     * {@code church}, {@code coins}, {@code comment}, {@code compass},
     * {@code construction}, {@code cross}, {@code cup}, {@code cutlery},
     * {@code default}, {@code diamond}, {@code dog}, {@code door}, {@code down},
     * {@code drink}, {@code exclamation}, {@code factory}, {@code fire},
     * {@code flower}, {@code gear}, {@code goldmedal}, {@code goldstar},
     * {@code greenflag}, {@code hammer}, {@code heart}, {@code house},
     * {@code key}, {@code king}, {@code left}, {@code lightbulb},
     * {@code lighthouse}, {@code lock}, {@code minecart}, {@code offlineuser},
     * {@code orangeflag}, {@code pin}, {@code pinkflag}, {@code pirateflag},
     * {@code pointdown}, {@code pointleft}, {@code pointright}, {@code pointup},
     * {@code portal}, {@code purpleflag}, {@code queen}, {@code redflag},
     * {@code right}, {@code ruby}, {@code scales}, {@code shield}, {@code sign},
     * {@code silvermedal}, {@code silverstar}, {@code skull}, {@code star},
     * {@code sun}, {@code temple}, {@code theater}, {@code tornado},
     * {@code tower}, {@code tree}, {@code truck}, {@code up}, {@code walk},
     * {@code warning}, {@code world}, {@code wrench}, {@code yellowflag}.
     * <p>
     * For <b>BlueMap</b>, the icon name is currently ignored (the default POI
     * icon is always used).
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param location    the location in the world
     * @param iconName    icon identifier (interpretation is map-plugin-specific)
     */
    public void addPointMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull Location location, @NonNull String iconName) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.addPointMarker(markerSetId, markerId, label, location, iconName);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed addPointMarker: " + e.getMessage());
            }
        }
    }

    /**
     * Removes a point marker from all active map plugins.
     *
     * @param markerSetId the marker set ID
     * @param markerId    the marker ID
     */
    public void removePointMarker(@NonNull String markerSetId, @NonNull String markerId) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.removePointMarker(markerSetId, markerId);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed removePointMarker: " + e.getMessage());
            }
        }
    }

    /**
     * Adds or updates a rectangular area marker on all active map plugins.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param world       the world
     * @param minX        minimum X coordinate
     * @param minZ        minimum Z coordinate
     * @param maxX        maximum X coordinate
     * @param maxZ        maximum Z coordinate
     * @param lineColor   line/border color (alpha channel for opacity)
     * @param fillColor   fill color (alpha channel for opacity)
     * @param lineWidth   line width in pixels
     */
    public void addAreaMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, double minX, double minZ, double maxX, double maxZ, @NonNull Color lineColor,
            @NonNull Color fillColor, int lineWidth) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.addAreaMarker(markerSetId, markerId, label, world, minX, minZ, maxX, maxZ, lineColor, fillColor,
                        lineWidth);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed addAreaMarker: " + e.getMessage());
            }
        }
    }

    /**
     * Adds or updates a polygonal area marker on all active map plugins.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param world       the world
     * @param xPoints     X coordinates of polygon vertices
     * @param zPoints     Z coordinates of polygon vertices
     * @param lineColor   line/border color (alpha channel for opacity)
     * @param fillColor   fill color (alpha channel for opacity)
     * @param lineWidth   line width in pixels
     */
    public void addPolygonMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, @NonNull double[] xPoints, @NonNull double[] zPoints, @NonNull Color lineColor,
            @NonNull Color fillColor, int lineWidth) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.addPolygonMarker(markerSetId, markerId, label, world, xPoints, zPoints, lineColor, fillColor,
                        lineWidth);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed addPolygonMarker: " + e.getMessage());
            }
        }
    }

    /**
     * Removes an area or polygon marker from all active map plugins.
     *
     * @param markerSetId the marker set ID
     * @param markerId    the marker ID
     */
    public void removeAreaMarker(@NonNull String markerSetId, @NonNull String markerId) {
        for (MapHook hook : getMapHooks()) {
            try {
                hook.removeAreaMarker(markerSetId, markerId);
            } catch (Exception e) {
                plugin.logError("Map hook " + hook.getPluginName() + " failed removeAreaMarker: " + e.getMessage());
            }
        }
    }
}
