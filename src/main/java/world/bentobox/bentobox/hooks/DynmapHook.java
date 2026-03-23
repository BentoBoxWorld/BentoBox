package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandNameEvent;
import world.bentobox.bentobox.api.events.island.IslandNewIslandEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Hook to display island markers on Dynmap.
 * @author tastybento
 * @since 3.12.0
 */
public class DynmapHook extends Hook implements Listener {

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
        // Register markers for all game mode addons known at hook time
        plugin.getAddonsManager().getGameModeAddons().forEach(this::registerGameMode);
        // Listen for future island events
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

    // --- Public addon API ---

    /**
     * Returns the Dynmap MarkerAPI for addons to create custom markers.
     * @return the MarkerAPI instance
     */
    @NonNull
    public MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    /**
     * Gets the marker set for the given game mode addon, if one has been registered.
     * @param addon the game mode addon
     * @return the MarkerSet, or null if not registered
     */
    public MarkerSet getMarkerSet(@NonNull GameModeAddon addon) {
        return markerSets.get(addon.getWorldSettings().getFriendlyName());
    }

    /**
     * Creates or retrieves a custom marker set. Useful for addons like Warps
     * that want to display their own markers on Dynmap.
     * @param id unique identifier for the marker set
     * @param label display label for the marker set
     * @return the MarkerSet
     */
    @NonNull
    public MarkerSet createMarkerSet(@NonNull String id, @NonNull String label) {
        MarkerSet existing = markerAPI.getMarkerSet(id);
        if (existing != null) {
            return existing;
        }
        return markerAPI.createMarkerSet(id, label, null, true);
    }

    // --- Event handlers ---

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
