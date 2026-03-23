package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandNameEvent;
import world.bentobox.bentobox.api.events.island.IslandNewIslandEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;

class DynmapHookTest extends CommonTestSetup {

    /**
     * Combined interface so a single mock satisfies both Plugin (returned by
     * Bukkit's PluginManager) and DynmapAPI (cast target in DynmapHook).
     */
    private interface DynmapPlugin extends Plugin, DynmapAPI {}

    @Mock
    private DynmapPlugin dynmapPlugin;
    @Mock
    private MarkerAPI markerAPI;
    @Mock
    private MarkerSet markerSet;
    @Mock
    private MarkerIcon defaultIcon;
    @Mock
    private Marker marker;
    @Mock
    private AreaMarker areaMarker;
    @Mock
    private GameModeAddon addon;
    @Mock
    private WorldSettings worldSettings;
    @Mock
    private World overWorld;
    @Mock
    private AddonsManager addonsManager;

    private DynmapHook hook;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Dynmap plugin mock — DynmapAPI extends Plugin
        when(pim.getPlugin("dynmap")).thenReturn(dynmapPlugin);
        when(dynmapPlugin.getMarkerAPI()).thenReturn(markerAPI);
        when(markerAPI.getMarkerIcon("default")).thenReturn(defaultIcon);
        when(markerAPI.getMarkerSet(anyString())).thenReturn(null);
        when(markerAPI.createMarkerSet(anyString(), anyString(), isNull(), eq(true)))
                .thenReturn(markerSet);
        when(markerSet.getMarkers()).thenReturn(Set.of());
        when(markerSet.getAreaMarkers()).thenReturn(Set.of());
        when(markerSet.createMarker(anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean()))
                .thenReturn(marker);
        when(markerSet.createAreaMarker(anyString(), anyString(), anyBoolean(), anyString(),
                any(double[].class), any(double[].class), anyBoolean()))
                .thenReturn(areaMarker);
        when(markerSet.findMarker(anyString())).thenReturn(null);
        when(markerSet.findAreaMarker(anyString())).thenReturn(null);

        // Addon + world settings
        when(addon.getWorldSettings()).thenReturn(worldSettings);
        when(worldSettings.getFriendlyName()).thenReturn("BSkyBlock");
        when(addon.getOverWorld()).thenReturn(overWorld);

        // Islands manager: no islands by default
        when(im.getIslands(any(World.class))).thenReturn(Collections.emptyList());

        // AddonsManager
        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(addonsManager.getGameModeAddons()).thenReturn(List.of(addon));

        // Island basic setup
        when(island.getOwner()).thenReturn(uuid);
        when(island.getUniqueId()).thenReturn(uuid.toString());
        Location center = mock(Location.class);
        when(center.getX()).thenReturn(0.0);
        when(center.getY()).thenReturn(64.0);
        when(center.getZ()).thenReturn(0.0);
        when(center.getWorld()).thenReturn(overWorld);
        when(island.getCenter()).thenReturn(center);
        when(island.getWorld()).thenReturn(overWorld);
        when(island.getName()).thenReturn(null);
        when(overWorld.getName()).thenReturn("bskyblock_world");

        // IWM: return addon for overWorld
        when(iwm.getAddon(overWorld)).thenReturn(Optional.of(addon));

        hook = new DynmapHook();
    }

    /**
     * Calls hook() then fires BentoBoxReadyEvent to trigger island marker population,
     * mirroring the real startup sequence.
     */
    private void hookAndReady() {
        hook.hook();
        hook.onBentoBoxReady(mock(BentoBoxReadyEvent.class));
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---- hook() ----

    @Test
    void testHookSucceeds() {
        assertTrue(hook.hook());
    }

    @Test
    void testHookFailsWhenDynmapNotPresent() {
        when(pim.getPlugin("dynmap")).thenReturn(null);
        assertFalse(hook.hook());
    }

    @Test
    void testHookFailsWhenMarkerAPINull() {
        when(dynmapPlugin.getMarkerAPI()).thenReturn(null);
        assertFalse(hook.hook());
    }

    @Test
    void testHookRegistersEvents() {
        hook.hook();
        verify(pim).registerEvents(hook, plugin);
    }

    @Test
    void testBentoBoxReadyCallsRegisterGameMode() {
        hookAndReady();
        verify(im).getIslands(overWorld);
    }

    // ---- getPluginName() / getFailureCause() ----

    @Test
    void testGetPluginName() {
        assertEquals("dynmap", hook.getPluginName());
    }

    @Test
    void testGetFailureCause() {
        assertNotNull(hook.getFailureCause());
    }

    // ---- registerGameMode() ----

    @Test
    void testRegisterGameModeNoIslands() {
        hookAndReady();
        verify(markerAPI).createMarkerSet("bskyblock.markers", "BSkyBlock", null, true);
    }

    @Test
    void testRegisterGameModeWithIsland() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        verify(markerSet).createMarker(eq(uuid.toString()), eq("tastybento"),
                eq("bskyblock_world"), eq(0.0), eq(64.0), eq(0.0), eq(defaultIcon), eq(true));
    }

    @Test
    void testRegisterGameModeUnownedIslandIgnored() {
        Island unowned = mock(Island.class);
        when(unowned.getOwner()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(unowned));
        hookAndReady();
        verify(markerSet, never()).createMarker(anyString(), anyString(), anyString(),
                anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean());
    }

    @Test
    void testRegisterGameModeReusesExistingMarkerSet() {
        MarkerSet existingSet = mock(MarkerSet.class);
        when(existingSet.getMarkers()).thenReturn(Set.of());
        when(existingSet.getAreaMarkers()).thenReturn(Set.of());
        when(markerAPI.getMarkerSet("bskyblock.markers")).thenReturn(existingSet);
        hookAndReady();
        // Should not create a new one
        verify(markerAPI, never()).createMarkerSet(anyString(), anyString(), isNull(), eq(true));
        // Should update the label
        verify(existingSet).setMarkerSetLabel("BSkyBlock");
    }

    // ---- Island label logic ----

    @Test
    void testIslandLabelUsesCustomName() {
        when(island.getName()).thenReturn("My Island");
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        verify(markerSet).createMarker(eq(uuid.toString()), eq("My Island"),
                anyString(), anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean());
    }

    @Test
    void testIslandLabelUsesOwnerName() {
        when(island.getName()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        // player name from CommonTestSetup is "tastybento"
        verify(markerSet).createMarker(eq(uuid.toString()), eq("tastybento"),
                anyString(), anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean());
    }

    @Test
    void testIslandLabelFallsBackToUUID() {
        // Test UUID fallback via the event path with no-owner island
        hookAndReady();
        Island noOwnerIsland = mock(Island.class);
        UUID noOwnerUuid = UUID.randomUUID();
        when(noOwnerIsland.getName()).thenReturn(null);
        when(noOwnerIsland.getOwner()).thenReturn(null);
        when(noOwnerIsland.getUniqueId()).thenReturn(noOwnerUuid.toString());
        Location c = mock(Location.class);
        when(c.getX()).thenReturn(0.0);
        when(c.getY()).thenReturn(64.0);
        when(c.getZ()).thenReturn(0.0);
        when(c.getWorld()).thenReturn(overWorld);
        when(noOwnerIsland.getCenter()).thenReturn(c);
        when(noOwnerIsland.getWorld()).thenReturn(overWorld);

        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(noOwnerIsland);
        hook.onNewIsland(event);

        verify(markerSet).createMarker(eq(noOwnerUuid.toString()), eq(noOwnerUuid.toString()),
                anyString(), anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean());
    }

    // ---- Event handlers ----

    @Test
    void testOnNewIsland() {
        hookAndReady();
        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onNewIsland(event);

        verify(markerSet).createMarker(eq(uuid.toString()), eq("tastybento"),
                eq("bskyblock_world"), eq(0.0), eq(64.0), eq(0.0), eq(defaultIcon), eq(true));
    }

    @Test
    void testOnNewIslandNoAddon() {
        hookAndReady();
        when(iwm.getAddon(overWorld)).thenReturn(Optional.empty());
        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        // Should not throw
        hook.onNewIsland(event);
    }

    @Test
    void testOnIslandDelete() {
        hookAndReady();
        Marker existingMarker = mock(Marker.class);
        AreaMarker existingArea = mock(AreaMarker.class);
        when(markerSet.findMarker(uuid.toString())).thenReturn(existingMarker);
        when(markerSet.findAreaMarker(uuid.toString() + "_area")).thenReturn(existingArea);

        IslandDeleteEvent event = mock(IslandDeleteEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onIslandDelete(event);

        verify(existingMarker).deleteMarker();
        verify(existingArea).deleteMarker();
    }

    @Test
    void testOnIslandDeleteMarkerNotFound() {
        hookAndReady();
        when(markerSet.findMarker(uuid.toString())).thenReturn(null);

        IslandDeleteEvent event = mock(IslandDeleteEvent.class);
        when(event.getIsland()).thenReturn(island);
        // Should not throw
        hook.onIslandDelete(event);
    }

    @Test
    void testOnIslandName() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        when(island.getName()).thenReturn("Old Name");
        hookAndReady();

        // Now rename
        when(island.getName()).thenReturn("New Name");
        Marker existingMarker = mock(Marker.class);
        when(markerSet.findMarker(uuid.toString())).thenReturn(existingMarker);

        IslandNameEvent event = mock(IslandNameEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onIslandName(event);

        // deleteMarker called twice: once by remove(), once by setMarker() which
        // finds the same stub before re-creating
        verify(existingMarker, times(2)).deleteMarker();
        verify(markerSet).createMarker(eq(uuid.toString()), eq("New Name"),
                anyString(), anyDouble(), anyDouble(), anyDouble(), any(MarkerIcon.class), anyBoolean());
    }

    @Test
    void testOnIslandReset() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();

        Island newIsland = mock(Island.class);
        UUID newUuid = UUID.randomUUID();
        when(newIsland.getOwner()).thenReturn(newUuid);
        when(newIsland.getUniqueId()).thenReturn(newUuid.toString());
        when(newIsland.getName()).thenReturn("Reset Island");
        Location newCenter = mock(Location.class);
        when(newCenter.getX()).thenReturn(100.0);
        when(newCenter.getY()).thenReturn(64.0);
        when(newCenter.getZ()).thenReturn(100.0);
        when(newCenter.getWorld()).thenReturn(overWorld);
        when(newIsland.getCenter()).thenReturn(newCenter);
        when(newIsland.getWorld()).thenReturn(overWorld);

        Marker oldMarker = mock(Marker.class);
        when(markerSet.findMarker(uuid.toString())).thenReturn(oldMarker);

        IslandResettedEvent event = mock(IslandResettedEvent.class);
        when(event.getIsland()).thenReturn(newIsland);
        when(event.getOldIsland()).thenReturn(island);
        hook.onIslandReset(event);

        // Old island marker removed
        verify(oldMarker).deleteMarker();
        // New island marker added
        verify(markerSet).createMarker(eq(newUuid.toString()), eq("Reset Island"),
                eq("bskyblock_world"), eq(100.0), eq(64.0), eq(100.0), eq(defaultIcon), eq(true));
    }

    // ---- Public addon API ----

    @Test
    void testGetMarkerAPI() {
        hook.hook();
        assertEquals(markerAPI, hook.getMarkerAPI());
    }

    @Test
    void testGetNativeMarkerSet() {
        hookAndReady();
        assertEquals(markerSet, hook.getNativeMarkerSet(addon));
    }

    @Test
    void testGetNativeMarkerSetNotRegistered() {
        hookAndReady();
        GameModeAddon unknownAddon = mock(GameModeAddon.class);
        WorldSettings unknownSettings = mock(WorldSettings.class);
        when(unknownAddon.getWorldSettings()).thenReturn(unknownSettings);
        when(unknownSettings.getFriendlyName()).thenReturn("UnknownGame");
        assertNull(hook.getNativeMarkerSet(unknownAddon));
    }

    @Test
    void testCreateMarkerSetNew() {
        hook.hook();
        MarkerSet customSet = mock(MarkerSet.class);
        when(markerAPI.getMarkerSet("warps.markers")).thenReturn(null);
        when(markerAPI.createMarkerSet("warps.markers", "Warps", null, true)).thenReturn(customSet);

        hook.createMarkerSet("warps.markers", "Warps");
        verify(markerAPI).createMarkerSet("warps.markers", "Warps", null, true);
    }

    @Test
    void testCreateMarkerSetExisting() {
        hook.hook();
        MarkerSet existingSet = mock(MarkerSet.class);
        when(markerAPI.getMarkerSet("warps.markers")).thenReturn(existingSet);

        hook.createMarkerSet("warps.markers", "Warps");
        // Should not create a new one
        verify(markerAPI, never()).createMarkerSet(eq("warps.markers"), eq("Warps"), isNull(), eq(true));
    }

    // ---- addPointMarker with icon ----

    @Test
    void testAddPointMarkerUsesCustomIcon() {
        hook.hook();
        hook.createMarkerSet("warps", "Warps");
        MarkerIcon signIcon = mock(MarkerIcon.class);
        when(markerAPI.getMarkerIcon("sign")).thenReturn(signIcon);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(overWorld);
        when(loc.getX()).thenReturn(10.0);
        when(loc.getY()).thenReturn(64.0);
        when(loc.getZ()).thenReturn(20.0);

        hook.addPointMarker("warps", "marker1", "My Warp", loc, "sign");
        verify(markerSet).createMarker("marker1", "My Warp", true, "bskyblock_world",
                10.0, 64.0, 20.0, signIcon, true);
    }

    @Test
    void testAddPointMarkerFallsBackToDefaultIcon() {
        hook.hook();
        hook.createMarkerSet("warps", "Warps");
        when(markerAPI.getMarkerIcon("nonexistent")).thenReturn(null);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(overWorld);
        when(loc.getX()).thenReturn(10.0);
        when(loc.getY()).thenReturn(64.0);
        when(loc.getZ()).thenReturn(20.0);

        hook.addPointMarker("warps", "marker1", "My Warp", loc, "nonexistent");
        verify(markerSet).createMarker("marker1", "My Warp", true, "bskyblock_world",
                10.0, 64.0, 20.0, defaultIcon, true);
    }
}
