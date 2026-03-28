package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandNameEvent;
import world.bentobox.bentobox.api.events.island.IslandNewIslandEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.api.hooks.MapHook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Hook to display island markers on BlueMap.
 * @author tastybento
 * @since 2.1.0
 */
public class BlueMapHook extends MapHook implements Listener {

    private final BentoBox plugin;
    private BlueMapAPI api;
    /**
     * One marker set per game mode; key is the friendly name of the game mode.
     */
    private final Map<String, MarkerSet> markerSets = new HashMap<>();

    public BlueMapHook() {
        super("BlueMap", Material.MAP);
        this.plugin = BentoBox.getInstance();
    }

    @Override
    public boolean hook() {
        if (BlueMapAPI.getInstance().isPresent()) {
            api = BlueMapAPI.getInstance().get();
        } else {
            return false;
        }
        // Listen for island events and BentoBoxReadyEvent to populate island markers
        // after islands are loaded (map hooks register before addons enable, so islands
        // are not yet loaded at hook time)
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    /**
     * Register all islands for a given game mode addon and attach the marker set to BlueMap worlds.
     * @param addon the game mode addon
     */
    public void registerGameMode(@NonNull GameModeAddon addon) {
        String friendlyName = addon.getWorldSettings().getFriendlyName();

        MarkerSet markerSet = markerSets.computeIfAbsent(friendlyName, k -> {

            return MarkerSet.builder().toggleable(true).defaultHidden(false).label(k).build();
        });
        // Create a marker for each owned island in this addon's overworld
        plugin.getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null)
                .forEach(island -> {

                    setMarker(markerSet, island);
                });
        // Overworld
        addMarkerSetToWorld(addon.getOverWorld(), friendlyName, markerSet);
        // Nether
        if (addon.getWorldSettings().isNetherGenerate() && addon.getWorldSettings().isNetherIslands()
                && addon.getNetherWorld() != null) {
            addMarkerSetToWorld(addon.getNetherWorld(), friendlyName, markerSet);
        }
        // End
        if (addon.getWorldSettings().isEndGenerate() && addon.getWorldSettings().isEndIslands()
                && addon.getEndWorld() != null) {
            addMarkerSetToWorld(addon.getEndWorld(), friendlyName, markerSet);
        }
    }

    private void addMarkerSetToWorld(World world, String markerSetId, MarkerSet markerSet) {
        api.getWorld(world).ifPresent(bmWorld -> {

            for (BlueMapMap map : bmWorld.getMaps()) {

                map.getMarkerSets().put(markerSetId, markerSet);
            }
        });
    }

    private void setMarker(MarkerSet markerSet, Island island) {
        String label = getIslandLabel(island);
        String id = island.getUniqueId();

        // Point marker at island center for the label/icon
        POIMarker marker = POIMarker.builder().label(label).listed(true).defaultIcon()
                .position(island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ())
                .build();
        markerSet.put(id, marker);
        // Shape marker showing the protected island border
        ShapeMarker area = ShapeMarker.builder()
                .label(label)
                .shape(Shape.createRect(island.getMinProtectedX(), island.getMinProtectedZ(),
                        island.getMaxProtectedX(), island.getMaxProtectedZ()),
                        (float) island.getCenter().getY())
                .lineColor(new de.bluecolored.bluemap.api.math.Color(51, 136, 255))
                .fillColor(new de.bluecolored.bluemap.api.math.Color(51, 136, 255, 0.15f))
                .lineWidth(2)
                .build();
        markerSet.put(id + "_area", area);
    }

    private String getIslandLabel(Island island) {
        if (island.getName() != null && !island.getName().isBlank()) {
            return island.getName();
        } else if (island.getOwner() != null) {
            User owner = User.getInstance(island.getOwner());
            if (owner != null) {
                return owner.getName();
            }
        }
        return island.getUniqueId();
    }

    @Override
    public String getFailureCause() {
        return "BlueMap is not loaded or the API version is incompatible.";
    }

    private void add(Island island, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.computeIfAbsent(addon.getWorldSettings().getFriendlyName(),
                k -> MarkerSet.builder().label(k).build());
        setMarker(markerSet, island);
    }

    private void remove(String islandUniqueId, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.get(addon.getWorldSettings().getFriendlyName());
        if (markerSet != null) {
            markerSet.remove(islandUniqueId);
            markerSet.remove(islandUniqueId + "_area");
        }
    }

    // --- Native API for direct BlueMap access ---

    /**
     * Returns the BlueMapAPI instance for addons to create custom markers directly.
     * @return the BlueMapAPI instance
     */
    @NonNull
    public BlueMapAPI getBlueMapAPI() {
        return api;
    }

    /**
     * Gets the native BlueMap marker set for the given game mode addon.
     * @param addon the game mode addon
     * @return the MarkerSet, or null if not registered
     */
    public MarkerSet getMarkerSet(@NonNull GameModeAddon addon) {
        return markerSets.get(addon.getWorldSettings().getFriendlyName());
    }

    // --- MapHook abstract method implementations ---

    @Override
    public void createMarkerSet(@NonNull String id, @NonNull String label) {
        MarkerSet markerSet = markerSets.computeIfAbsent(id,
                k -> MarkerSet.builder().label(label).toggleable(true).defaultHidden(false).build());
        api.getMaps().forEach(map -> map.getMarkerSets().put(id, markerSet));
    }

    @Override
    public void removeMarkerSet(@NonNull String id) {
        markerSets.remove(id);
        api.getMaps().forEach(map -> map.getMarkerSets().remove(id));
    }

    @Override
    public void clearMarkerSet(@NonNull String id) {
        MarkerSet markerSet = markerSets.get(id);
        if (markerSet != null) {
            markerSet.getMarkers().clear();
        }
    }

    @Override
    public void addPointMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull Location location, @NonNull String iconName) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            // BlueMap uses URL-based icons; named icons are not supported yet, so use default
            POIMarker marker = POIMarker.builder().label(label).listed(true).defaultIcon()
                    .position(location.getX(), location.getY(), location.getZ()).build();
            markerSet.put(markerId, marker);
        }
    }

    @Override
    public void removePointMarker(@NonNull String markerSetId, @NonNull String markerId) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            markerSet.remove(markerId);
        }
    }

    @Override
    public void addAreaMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, double minX, double minZ, double maxX, double maxZ,
            java.awt.Color lineColor, java.awt.Color fillColor, int lineWidth) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            ShapeMarker area = ShapeMarker.builder().label(label)
                    .shape(Shape.createRect(minX, minZ, maxX, maxZ), 64)
                    .lineColor(toBlueMapColor(lineColor)).fillColor(toBlueMapColor(fillColor)).lineWidth(lineWidth)
                    .build();
            markerSet.put(markerId, area);
        }
    }

    @Override
    public void addPolygonMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, @NonNull double[] xPoints, @NonNull double[] zPoints,
            java.awt.Color lineColor, java.awt.Color fillColor, int lineWidth) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null && xPoints.length == zPoints.length && xPoints.length >= 3) {
            com.flowpowered.math.vector.Vector2d[] points = new com.flowpowered.math.vector.Vector2d[xPoints.length];
            for (int i = 0; i < xPoints.length; i++) {
                points[i] = new com.flowpowered.math.vector.Vector2d(xPoints[i], zPoints[i]);
            }
            Shape shape = new Shape(points);
            ShapeMarker area = ShapeMarker.builder().label(label).shape(shape, 64)
                    .lineColor(toBlueMapColor(lineColor)).fillColor(toBlueMapColor(fillColor)).lineWidth(lineWidth)
                    .build();
            markerSet.put(markerId, area);
        }
    }

    @Override
    public void removeAreaMarker(@NonNull String markerSetId, @NonNull String markerId) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            markerSet.remove(markerId);
        }
    }

    private static de.bluecolored.bluemap.api.math.Color toBlueMapColor(java.awt.Color c) {
        return new de.bluecolored.bluemap.api.math.Color(c.getRed(), c.getGreen(), c.getBlue(),
                c.getAlpha() / 255.0f);
    }

    // --- Event handlers ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Now that islands are loaded, populate markers for all game modes
        plugin.getAddonsManager().getGameModeAddons().forEach(this::registerGameMode);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandNewIslandEvent e) {

        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> add(e.getIsland(), addon));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent e) {

        plugin.getIWM().getAddon(e.getIsland().getWorld())
                .ifPresent(addon -> remove(e.getIsland().getUniqueId(), addon));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandName(IslandNameEvent e) {

        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> {
            remove(e.getIsland().getUniqueId(), addon);
            add(e.getIsland(), addon);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandReset(IslandResettedEvent e) {

        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> {
            remove(e.getOldIsland().getUniqueId(), addon);
            add(e.getIsland(), addon);
        });
    }
}
