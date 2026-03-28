package world.bentobox.bentobox.api.hooks;

import java.awt.Color;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Abstract hook for web-map plugins (BlueMap, Dynmap, etc.).
 * Concrete implementations translate these calls into the native map API.
 * <p>
 * Addons should not use this class directly. Instead, use
 * {@link world.bentobox.bentobox.managers.MapManager} which fans out calls
 * to all active map hooks and silently no-ops when none are installed.
 *
 * @author tastybento
 * @since 3.12.0
 */
public abstract class MapHook extends Hook {

    protected MapHook(@NonNull String pluginName, @NonNull Material icon) {
        super(pluginName, icon);
    }

    // --- Marker Set operations ---

    /**
     * Creates a marker set with the given ID and label. If a marker set with the
     * same ID already exists, it is returned unchanged.
     *
     * @param id    unique identifier for the marker set
     * @param label display label for the marker set
     */
    public abstract void createMarkerSet(@NonNull String id, @NonNull String label);

    /**
     * Removes a marker set and all its markers.
     *
     * @param id the marker set ID
     */
    public abstract void removeMarkerSet(@NonNull String id);

    /**
     * Removes all markers from a marker set without removing the set itself.
     *
     * @param id the marker set ID
     */
    public abstract void clearMarkerSet(@NonNull String id);

    // --- Point Marker operations ---

    /**
     * Adds or updates a point marker (icon + label) at a location using the default icon.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker within the set
     * @param label       display label
     * @param location    the location in the world
     */
    public void addPointMarker(@NonNull String markerSetId, @NonNull String markerId,
            @NonNull String label, @NonNull Location location) {
        addPointMarker(markerSetId, markerId, label, location, "default");
    }

    /**
     * Adds or updates a point marker at a location with a specific icon.
     * <p>
     * The {@code iconName} is interpreted by each map plugin:
     * <ul>
     *   <li><b>Dynmap</b> — maps to a registered Dynmap marker icon name
     *       (e.g. {@code "sign"}, {@code "diamond"}, {@code "house"}).
     *       Falls back to {@code "default"} if the name is not found.</li>
     *   <li><b>BlueMap</b> — currently uses the default POI icon regardless
     *       of the name (BlueMap uses URL-based icons, not named icons).</li>
     * </ul>
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker within the set
     * @param label       display label
     * @param location    the location in the world
     * @param iconName    icon identifier (interpretation is map-plugin-specific)
     */
    public abstract void addPointMarker(@NonNull String markerSetId, @NonNull String markerId,
            @NonNull String label, @NonNull Location location, @NonNull String iconName);

    /**
     * Removes a point marker.
     *
     * @param markerSetId the marker set ID
     * @param markerId    the marker ID
     */
    public abstract void removePointMarker(@NonNull String markerSetId, @NonNull String markerId);

    // --- Area Marker operations ---

    /**
     * Adds or updates a rectangular area marker with border and fill styling.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param world       the world
     * @param minX        minimum X coordinate
     * @param minZ        minimum Z coordinate
     * @param maxX        maximum X coordinate
     * @param maxZ        maximum Z coordinate
     * @param lineColor   line/border color (alpha channel is used for opacity)
     * @param fillColor   fill color (alpha channel is used for opacity)
     * @param lineWidth   line width in pixels
     */
    public abstract void addAreaMarker(@NonNull String markerSetId, @NonNull String markerId,
            @NonNull String label, @NonNull World world, double minX, double minZ, double maxX, double maxZ,
            @NonNull Color lineColor, @NonNull Color fillColor, int lineWidth);

    /**
     * Adds or updates a polygonal area marker with border and fill styling.
     *
     * @param markerSetId the marker set ID
     * @param markerId    unique ID for this marker
     * @param label       display label
     * @param world       the world
     * @param xPoints     X coordinates of polygon vertices
     * @param zPoints     Z coordinates of polygon vertices
     * @param lineColor   line/border color (alpha channel is used for opacity)
     * @param fillColor   fill color (alpha channel is used for opacity)
     * @param lineWidth   line width in pixels
     */
    public abstract void addPolygonMarker(@NonNull String markerSetId, @NonNull String markerId,
            @NonNull String label, @NonNull World world, @NonNull double[] xPoints, @NonNull double[] zPoints,
            @NonNull Color lineColor, @NonNull Color fillColor, int lineWidth);

    /**
     * Removes an area or polygon marker.
     *
     * @param markerSetId the marker set ID
     * @param markerId    the marker ID
     */
    public abstract void removeAreaMarker(@NonNull String markerSetId, @NonNull String markerId);
}
