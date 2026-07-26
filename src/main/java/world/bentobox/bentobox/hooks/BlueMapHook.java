package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
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
    /**
     * Worlds each addon-created marker set (see {@link #createMarkerSet(String, String)}) has
     * markers in, so a set is only attached to the maps of those worlds rather than to every
     * map on the server. Populated lazily as markers are added, because
     * {@link MapHook#createMarkerSet(String, String)} carries no world. Game mode marker sets
     * are not tracked here - they are attached directly in {@link #registerGameMode}.
     */
    private final Map<String, Set<World>> markerSetWorlds = new HashMap<>();

    public BlueMapHook() {
        super("BlueMap", Material.MAP);
        this.plugin = BentoBox.getInstance();
    }

    @Override
    public boolean hook() {
        // Register with BlueMap's own lifecycle instead of grabbing the API once. BlueMap
        // invokes the onEnable callback immediately if its API is already loaded, and again
        // every time BlueMap (re)loads - crucially, after a "/bluemap reload", which discards
        // BlueMap's maps and their marker sets. Re-populating from the callback means our
        // island markers survive a reload instead of vanishing until the next server restart.
        // A one-shot getInstance() in hook() was also racy at startup: if BlueMap had not
        // finished loading yet, the hook silently produced no markers.
        BlueMapAPI.onEnable(loadedApi -> {
            this.api = loadedApi;
            populateAll();
        });
        BlueMapAPI.onDisable(disabledApi -> this.api = null);
        // Listen for island events and BentoBoxReadyEvent to populate island markers
        // after islands are loaded (map hooks register before addons enable, so islands
        // are not yet loaded at hook time)
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    /**
     * (Re)creates and attaches every known marker set to BlueMap. Called whenever BlueMap's
     * API becomes available (including after a reload) and once islands have loaded. Safe to
     * call repeatedly - marker creation is idempotent.
     */
    private void populateAll() {
        if (api == null) {
            return;
        }
        // Rebuild island markers for every game mode and re-attach the sets to their worlds
        Set<String> gameModeNames = new HashSet<>();
        plugin.getAddonsManager().getGameModeAddons().forEach(addon -> {
            gameModeNames.add(addon.getWorldSettings().getFriendlyName());
            registerGameMode(addon);
        });
        // Re-attach any addon-created marker sets (created via createMarkerSet), since a BlueMap
        // reload drops them from the freshly built maps too. Only to the worlds they actually
        // have markers in - attaching them to every map put unrelated sets on every world's map.
        markerSets.forEach((id, markerSet) -> {
            if (!gameModeNames.contains(id)) {
                markerSetWorlds.getOrDefault(id, Set.of())
                        .forEach(world -> addMarkerSetToWorld(world, id, markerSet));
            }
        });
    }

    /**
     * Register all islands for a given game mode addon and attach the marker set to BlueMap worlds.
     * @param addon the game mode addon
     */
    public void registerGameMode(@NonNull GameModeAddon addon) {
        String friendlyName = addon.getWorldSettings().getFriendlyName();

        MarkerSet markerSet = markerSets.computeIfAbsent(friendlyName, k -> MarkerSet.builder().toggleable(true).defaultHidden(false).label(k).build());
        // Create a marker for each owned island in this addon's overworld
        plugin.getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null)
                .forEach(island -> setMarker(markerSet, island));
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
        if (api == null || world == null) {
            return;
        }
        api.getWorld(world).ifPresent(bmWorld -> {

            for (BlueMapMap map : bmWorld.getMaps()) {

                map.getMarkerSets().put(markerSetId, markerSet);
            }
        });
    }

    /**
     * Records that an addon-created marker set has markers in the given world and attaches the
     * set to that world's maps. Called as markers are added, since
     * {@link MapHook#createMarkerSet(String, String)} has no world parameter to scope by.
     * No-op for unknown marker set IDs or a null world.
     * @param markerSetId the marker set ID
     * @param world the world a marker was just added in
     */
    private void trackAndAttach(String markerSetId, World world) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet == null || world == null) {
            return;
        }
        // Track regardless of whether BlueMap is loaded, so populateAll() can attach it later
        markerSetWorlds.computeIfAbsent(markerSetId, k -> new HashSet<>()).add(world);
        // Attaching is an idempotent put, so this is safe to repeat
        addMarkerSetToWorld(world, markerSetId, markerSet);
    }

    private void setMarker(MarkerSet markerSet, Island island) {
        Settings settings = plugin.getSettings();
        String label = getIslandLabel(island);
        String id = island.getUniqueId();

        // Point marker at island center for the label/icon
        if (settings.isBluemapIslandMarkers()) {
            POIMarker.Builder builder = POIMarker.builder().label(label).listed(true)
                    .position(island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ())
                    .minDistance(settings.getBluemapMarkerMinDistance())
                    .maxDistance(settings.getBluemapMarkerMaxDistance());
            String icon = settings.getBluemapMarkerIcon();
            if (icon != null && !icon.isBlank()) {
                builder.icon(icon, settings.getBluemapMarkerIconAnchorX(), settings.getBluemapMarkerIconAnchorY());
            } else {
                builder.defaultIcon();
            }
            markerSet.put(id, builder.build());
        }
        // Shape marker showing the protected island border
        if (settings.isBluemapIslandAreas()) {
            ShapeMarker area = ShapeMarker.builder()
                    .label(label)
                    .shape(Shape.createRect(island.getMinProtectedX(), island.getMinProtectedZ(),
                            island.getMaxProtectedX(), island.getMaxProtectedZ()),
                            (float) island.getCenter().getY())
                    .lineColor(parseHexColor(settings.getBluemapAreaLineColor(), 1.0f))
                    .fillColor(parseHexColor(settings.getBluemapAreaFillColor(),
                            (float) settings.getBluemapAreaFillOpacity()))
                    .lineWidth(settings.getBluemapAreaLineWidth())
                    .build();
            markerSet.put(id + "_area", area);
        }
    }

    /**
     * Parses a {@code #RRGGBB} (or {@code RRGGBB}) hex string into a BlueMap colour with the
     * given alpha. Falls back to the default island blue (51, 136, 255) if the string is null
     * or malformed, so a bad config value never stops markers from rendering.
     * @param hex hex colour string
     * @param alpha alpha channel, 0.0 to 1.0
     * @return the parsed colour
     */
    private de.bluecolored.bluemap.api.math.Color parseHexColor(String hex, float alpha) {
        if (hex != null) {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() == 6) {
                try {
                    int r = Integer.parseInt(h.substring(0, 2), 16);
                    int g = Integer.parseInt(h.substring(2, 4), 16);
                    int b = Integer.parseInt(h.substring(4, 6), 16);
                    return new de.bluecolored.bluemap.api.math.Color(r, g, b, alpha);
                } catch (NumberFormatException e) {
                    plugin.logError("Invalid BlueMap colour in config: '" + hex + "'. Using default.");
                }
            } else {
                plugin.logError("Invalid BlueMap colour in config: '" + hex + "'. Expected #RRGGBB. Using default.");
            }
        }
        return new de.bluecolored.bluemap.api.math.Color(51, 136, 255, alpha);
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
     * <p>
     * This is {@code null} until BlueMap has finished loading, and again while BlueMap is
     * reloading, so callers must null-check it. Prefer the {@link MapHook} methods, which
     * handle the API being unavailable and re-attach marker sets after a BlueMap reload.
     * @return the BlueMapAPI instance, or {@code null} if BlueMap is not currently loaded
     */
    @Nullable
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
        // The set is only created here. It is attached to a world's maps when the first marker
        // for that world is added - MapHook#createMarkerSet has no world to scope by, and
        // attaching to every map put e.g. a warps set on unrelated game modes' maps.
        markerSets.computeIfAbsent(id,
                k -> MarkerSet.builder().label(label).toggleable(true).defaultHidden(false).build());
    }

    @Override
    public void removeMarkerSet(@NonNull String id) {
        markerSets.remove(id);
        Set<World> worlds = markerSetWorlds.remove(id);
        if (api == null || worlds == null) {
            return;
        }
        worlds.forEach(world -> api.getWorld(world)
                .ifPresent(bmWorld -> bmWorld.getMaps().forEach(map -> map.getMarkerSets().remove(id))));
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
            trackAndAttach(markerSetId, location.getWorld());
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
            trackAndAttach(markerSetId, world);
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
            trackAndAttach(markerSetId, world);
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
        // Islands are now loaded. If BlueMap enabled before this point its onEnable callback
        // ran against an empty island cache, so populate again now. No-op if BlueMap's API is
        // not yet available - its onEnable callback will populate once it is.
        populateAll();
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
