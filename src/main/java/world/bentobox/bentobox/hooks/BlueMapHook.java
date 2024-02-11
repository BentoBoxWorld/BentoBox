package world.bentobox.bentobox.hooks;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * @author tastybento
 * @since 2.1.0
 */
public class BlueMapHook extends Hook implements Listener {

    private BentoBox plugin = BentoBox.getInstance();
    private BlueMapAPI api;
    /**
     * One marker set per world; key is the friendly name of the Game Mode
     */
    private Map<String, MarkerSet> markerSets = new HashMap<>();
    /**
     * 
     */
    private Map<String, String> islands = new HashMap<>();

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
        // Register this to list for island events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return true;
    }

    public void getMarkerSet(@NonNull GameModeAddon addon) {
        BentoBox.getInstance()
                .logDebug("Settings markers for Game Mode '" + addon.getWorldSettings().getFriendlyName() + "'");
        MarkerSet markerSet = markerSets.computeIfAbsent(addon.getWorldSettings().getFriendlyName(),
                k -> {
                    BentoBox.getInstance().logDebug("Making a new marker set for '" + k + "'");
                    return MarkerSet.builder().toggleable(true).defaultHidden(false).label(k).build();
                });
        // Register the island name for each island in this addon
        BentoBox.getInstance().getIslands().getIslands(addon.getOverWorld()).stream()
                .filter(is -> is.getOwner() != null).forEach(island -> {
                    BentoBox.getInstance().logDebug("Creating marker for " + island.getCenter());
                    setMarker(markerSet, addon.getWorldSettings().getFriendlyName(), island);
                    BentoBox.getInstance().logDebug("There are now " + markerSet.getMarkers().size()
                            + " markers in marketset " + markerSet.getLabel());
        });
        // Over world
        api.getWorld(addon.getOverWorld()).ifPresent(world -> {
            BentoBox.getInstance().logDebug("BlueMap knows about " + world.getId());
            for (BlueMapMap map : world.getMaps()) {
                BentoBox.getInstance().logDebug("Adding markerSet to " + map.getName() + " map");
                map.getMarkerSets().put(addon.getWorldSettings().getFriendlyName(), markerSet);
            }
        });
        /*
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
        */
    }

    private void setMarker(MarkerSet markerSet, String worldName, Island island) {
        String name = getIslandName(island);
        // Check if name is already used
        int index = 0;
        String newName = name;
        while (index++ < Integer.MAX_VALUE && islands.values().contains(newName)) {
            newName = name + String.valueOf(index);
        }
        BentoBox.getInstance().logDebug("Adding a marker called '" + newName + "' to '" + worldName + "'");
        islands.put(island.getUniqueId(), newName);
        // Set marker
        POIMarker marker = POIMarker.builder().label(newName).listed(true).defaultIcon()
                .position(island.getCenter().getX(), island.getCenter().getY(), island.getCenter().getZ())
                .build();
        markerSet.put(worldName, marker);

    }

    private String getIslandName(Island island) {
        if (island.getName() != null && !island.getName().isBlank()) {
            // Name has been set
            return island.getName();
        } else if (island.getOwner() != null) {
            return User.getInstance(island.getOwner()).getName();
        }
        return "";
    }

    @Override
    public String getFailureCause() {
        return "the version of BlueMap is incompatible with this hook. Use a newer version.";
    }

    // Listeners
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNewIsland(IslandNewIslandEvent e) {
        BentoBox.getInstance().logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> add(e.getIsland(), addon));
    }

    private void add(Island island, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.computeIfAbsent(addon.getWorldSettings().getFriendlyName(),
                k -> MarkerSet.builder().label(k).build());
        this.setMarker(markerSet, addon.getWorldSettings().getFriendlyName(), island);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandDelete(IslandDeleteEvent e) {
        BentoBox.getInstance().logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld())
                .ifPresent(addon -> remove(e.getIsland().getUniqueId(), addon));
    }

    private void remove(String island, GameModeAddon addon) {
        MarkerSet markerSet = markerSets.get(addon.getWorldSettings().getFriendlyName());
        if (markerSet != null) {
            markerSet.remove(islands.get(island));
            islands.remove(island);
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onIslandDelete(IslandNameEvent e) {
        BentoBox.getInstance().logDebug(e.getEventName());
        plugin.getIWM().getAddon(e.getIsland().getWorld()).ifPresent(addon -> {
            remove(e.getIsland().getUniqueId(), addon);
            add(e.getIsland(), addon);
        });
    }

}