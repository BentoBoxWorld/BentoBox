package world.bentobox.bentobox.hooks;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.eclipse.jdt.annotation.NonNull;

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
 * Hook to display island markers on Dynmap.
 * @author tastybento
 * @since 3.12.0
 */
public class DynmapHook extends MapHook implements Listener {

    private final BentoBox plugin;
    private MarkerAPI markerAPI;
    /**
     * One marker set per game mode; key is the marker set ID (derived from friendly name).
     */
    private final Map<String, MarkerSet> markerSets = new HashMap<>();

    public DynmapHook() {
        super("dynmap", Material.FILLED_MAP);
        this.plugin = BentoBox.getInstance();
    }

    @Override
    public boolean hook() {
        try {
            DynmapAPI dynmapAPI = (DynmapAPI) getPlugin();
            if (dynmapAPI == null) {
                return false;
            }
            MarkerAPI markers = dynmapAPI.getMarkerAPI();
            if (markers == null) {
                return false;
            }
            markerAPI = markers;
        } catch (Exception e) {
            return false;
        }
        // Listen for island events and BentoBoxReadyEvent to populate island markers
        // after islands are loaded (map hooks register before addons enable, so islands
        // are not yet loaded at hook time)
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    /**
     * Register all islands for a given game mode addon.
     * @param addon the game mode addon
     */
    public void registerGameMode(@NonNull GameModeAddon addon) {
        String friendlyName = addon.getWorldSettings().getFriendlyName();
        String markerSetId = friendlyName.toLowerCase(Locale.ENGLISH) + ".markers";
        plugin.logDebug("Setting markers for Game Mode '" + friendlyName + "'");
        MarkerSet markerSet = markerSets.computeIfAbsent(friendlyName, k -> {
            plugin.logDebug("Making a new marker set for '" + k + "'");
            // Dynmap persists marker sets — check for existing one first
            MarkerSet existing = markerAPI.getMarkerSet(markerSetId);
            if (existing != null) {
                existing.setMarkerSetLabel(friendlyName);
                return existing;
            }
            return markerAPI.createMarkerSet(markerSetId, friendlyName, null, true);
        });
        // Clear stale markers from previous runs
        markerSet.getMarkers().forEach(Marker::deleteMarker);
        markerSet.getAreaMarkers().forEach(AreaMarker::deleteMarker);
        // Create a marker for each owned island in this addon's overworld
        plugin.getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null)
                .forEach(island -> {
                    plugin.logDebug("Creating marker for " + island.getCenter());
                    setMarker(markerSet, island);
                });
    }

    private void setMarker(MarkerSet markerSet, Island island) {
        String label = getIslandLabel(island);
        String id = island.getUniqueId();
        World w = island.getCenter().getWorld();
        if (w == null) {
            return;
        }
        String worldName = w.getName();
        plugin.logDebug("Adding a marker called '" + label + "' for island " + id);
        // Remove existing markers if present
        Marker existingMarker = markerSet.findMarker(id);
        if (existingMarker != null) {
            existingMarker.deleteMarker();
        }
        AreaMarker existingArea = markerSet.findAreaMarker(id + "_area");
        if (existingArea != null) {
            existingArea.deleteMarker();
        }
        // Point marker at island center for the label/icon
        markerSet.createMarker(id, label, worldName,
                island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ(),
                markerAPI.getMarkerIcon("default"), true);
        // Area marker showing the protected island border
        double[] xCorners = { island.getMinProtectedX(), island.getMaxProtectedX(),
                island.getMaxProtectedX(), island.getMinProtectedX() };
        double[] zCorners = { island.getMinProtectedZ(), island.getMinProtectedZ(),
                island.getMaxProtectedZ(), island.getMaxProtectedZ() };
        AreaMarker area = markerSet.createAreaMarker(id + "_area", label, false, worldName,
                xCorners, zCorners, true);
        if (area != null) {
            area.setLineStyle(2, 0.8, 0x3388FF);
            area.setFillStyle(0.15, 0x3388FF);
        }
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
        return "Dynmap is not loaded or its Marker API is unavailable.";
    }

    private void add(Island island, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.get(addon.getWorldSettings().getFriendlyName());
        if (markerSet != null) {
            setMarker(markerSet, island);
        }
    }

    private void remove(String islandUniqueId, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.get(addon.getWorldSettings().getFriendlyName());
        if (markerSet != null) {
            Marker marker = markerSet.findMarker(islandUniqueId);
            if (marker != null) {
                marker.deleteMarker();
            }
            AreaMarker area = markerSet.findAreaMarker(islandUniqueId + "_area");
            if (area != null) {
                area.deleteMarker();
            }
        }
    }

    // --- Native API for direct Dynmap access ---

    /**
     * Returns the Dynmap MarkerAPI for addons to create custom markers directly.
     * @return the MarkerAPI instance
     */
    @NonNull
    public MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    /**
     * Gets the native Dynmap marker set for the given game mode addon.
     * @param addon the game mode addon
     * @return the MarkerSet, or null if not registered
     */
    public MarkerSet getNativeMarkerSet(@NonNull GameModeAddon addon) {
        return markerSets.get(addon.getWorldSettings().getFriendlyName());
    }

    // --- MapHook abstract method implementations ---

    @Override
    public void createMarkerSet(@NonNull String id, @NonNull String label) {
        markerSets.computeIfAbsent(id, k -> {
            MarkerSet existing = markerAPI.getMarkerSet(id);
            if (existing != null) {
                existing.setMarkerSetLabel(label);
                return existing;
            }
            return markerAPI.createMarkerSet(id, label, null, true);
        });
    }

    @Override
    public void removeMarkerSet(@NonNull String id) {
        MarkerSet markerSet = markerSets.remove(id);
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }
    }

    @Override
    public void clearMarkerSet(@NonNull String id) {
        MarkerSet markerSet = markerSets.get(id);
        if (markerSet != null) {
            markerSet.getMarkers().forEach(Marker::deleteMarker);
            markerSet.getAreaMarkers().forEach(AreaMarker::deleteMarker);
        }
    }

    @Override
    public void addPointMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull Location location, @NonNull String iconName) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet == null || location.getWorld() == null) {
            return;
        }
        Marker existing = markerSet.findMarker(markerId);
        if (existing != null) {
            existing.deleteMarker();
        }
        MarkerIcon icon = markerAPI.getMarkerIcon(iconName);
        if (icon == null) {
            icon = markerAPI.getMarkerIcon("default");
        }
        markerSet.createMarker(markerId, label, true, location.getWorld().getName(), location.getX(),
                location.getY(), location.getZ(), icon, true);
    }

    @Override
    public void removePointMarker(@NonNull String markerSetId, @NonNull String markerId) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            Marker marker = markerSet.findMarker(markerId);
            if (marker != null) {
                marker.deleteMarker();
            }
        }
    }

    @Override
    public void addAreaMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, double minX, double minZ, double maxX, double maxZ, @NonNull Color lineColor,
            @NonNull Color fillColor, int lineWidth) {
        double[] xCorners = { minX, maxX, maxX, minX };
        double[] zCorners = { minZ, minZ, maxZ, maxZ };
        addPolygonMarker(markerSetId, markerId, label, world, xCorners, zCorners, lineColor, fillColor, lineWidth);
    }

    @Override
    public void addPolygonMarker(@NonNull String markerSetId, @NonNull String markerId, @NonNull String label,
            @NonNull World world, @NonNull double[] xPoints, @NonNull double[] zPoints, @NonNull Color lineColor,
            @NonNull Color fillColor, int lineWidth) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet == null) {
            return;
        }
        AreaMarker existing = markerSet.findAreaMarker(markerId);
        if (existing != null) {
            existing.deleteMarker();
        }
        AreaMarker area = markerSet.createAreaMarker(markerId, label, false, world.getName(), xPoints, zPoints, true);
        if (area != null) {
            int lineRgb = (lineColor.getRed() << 16) | (lineColor.getGreen() << 8) | lineColor.getBlue();
            int fillRgb = (fillColor.getRed() << 16) | (fillColor.getGreen() << 8) | fillColor.getBlue();
            area.setLineStyle(lineWidth, lineColor.getAlpha() / 255.0, lineRgb);
            area.setFillStyle(fillColor.getAlpha() / 255.0, fillRgb);
        }
    }

    @Override
    public void removeAreaMarker(@NonNull String markerSetId, @NonNull String markerId) {
        MarkerSet markerSet = markerSets.get(markerSetId);
        if (markerSet != null) {
            AreaMarker area = markerSet.findAreaMarker(markerId);
            if (area != null) {
                area.deleteMarker();
            }
        }
    }

    // --- Event handlers ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Now that islands are loaded, populate markers for all game modes
        plugin.getAddonsManager().getGameModeAddons().forEach(this::registerGameMode);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandNewIslandEvent e) {
        plugin.logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> add(e.getIsland(), addon));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent e) {
        plugin.logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld())
                .ifPresent(addon -> remove(e.getIsland().getUniqueId(), addon));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandName(IslandNameEvent e) {
        plugin.logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> {
            remove(e.getIsland().getUniqueId(), addon);
            add(e.getIsland(), addon);
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandReset(IslandResettedEvent e) {
        plugin.logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> {
            remove(e.getOldIsland().getUniqueId(), addon);
            add(e.getIsland(), addon);
        });
    }
}
