package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
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
 * Hook to display island markers on BlueMap.
 * @author tastybento
 * @since 2.1.0
 */
public class BlueMapHook extends Hook implements Listener {

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
        // Register markers for all game mode addons known at hook time
        plugin.getAddonsManager().getGameModeAddons().forEach(this::registerGameMode);
        // Listen for future island events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    /**
     * Register all islands for a given game mode addon and attach the marker set to BlueMap worlds.
     * @param addon the game mode addon
     */
    public void registerGameMode(@NonNull GameModeAddon addon) {
        String friendlyName = addon.getWorldSettings().getFriendlyName();
        plugin.logDebug("Setting markers for Game Mode '" + friendlyName + "'");
        MarkerSet markerSet = markerSets.computeIfAbsent(friendlyName, k -> {
            plugin.logDebug("Making a new marker set for '" + k + "'");
            return MarkerSet.builder().toggleable(true).defaultHidden(false).label(k).build();
        });
        // Create a marker for each owned island in this addon's overworld
        plugin.getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null)
                .forEach(island -> {
                    plugin.logDebug("Creating marker for " + island.getCenter());
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
            plugin.logDebug("BlueMap knows about " + bmWorld.getId());
            for (BlueMapMap map : bmWorld.getMaps()) {
                plugin.logDebug("Adding markerSet to " + map.getName() + " map");
                map.getMarkerSets().put(markerSetId, markerSet);
            }
        });
    }

    private void setMarker(MarkerSet markerSet, Island island) {
        String label = getIslandLabel(island);
        plugin.logDebug("Adding a marker called '" + label + "' for island " + island.getUniqueId());
        POIMarker marker = POIMarker.builder().label(label).listed(true).defaultIcon()
                .position(island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ())
                .build();
        markerSet.put(island.getUniqueId(), marker);
    }

    private String getIslandLabel(Island island) {
        if (island.getName() != null && !island.getName().isBlank()) {
            return island.getName();
        } else if (island.getOwner() != null) {
            return User.getInstance(island.getOwner()).getName();
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
        }
    }

    // Listeners

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
