package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
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

class BlueMapHookTest extends CommonTestSetup {

    @Mock
    private BlueMapAPI blueMapAPI;
    @Mock
    private BlueMapWorld blueMapWorld;
    @Mock
    private BlueMapMap blueMapMap;
    @Mock
    private GameModeAddon addon;
    @Mock
    private WorldSettings worldSettings;
    @Mock
    private World overWorld;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
    @Mock
    private AddonsManager addonsManager;

    private MockedStatic<BlueMapAPI> mockedBlueMapAPI;
    private BlueMapHook hook;
    private Map<String, MarkerSet> mapMarkerSets;
    private Consumer<BlueMapAPI> onEnableConsumer;
    private Consumer<BlueMapAPI> onDisableConsumer;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // BlueMapAPI static mock. hook() registers onEnable/onDisable callbacks rather than
        // calling getInstance(); capture them so tests can simulate BlueMap (re)loading.
        mockedBlueMapAPI = Mockito.mockStatic(BlueMapAPI.class);
        mockedBlueMapAPI.when(() -> BlueMapAPI.onEnable(any())).thenAnswer(inv -> {
            onEnableConsumer = inv.getArgument(0);
            return null;
        });
        mockedBlueMapAPI.when(() -> BlueMapAPI.onDisable(any())).thenAnswer(inv -> {
            onDisableConsumer = inv.getArgument(0);
            return null;
        });

        // BlueMap world/map chain
        mapMarkerSets = new HashMap<>();
        when(blueMapAPI.getWorld(any(World.class))).thenReturn(Optional.of(blueMapWorld));
        when(blueMapWorld.getMaps()).thenReturn(List.of(blueMapMap));
        when(blueMapWorld.getId()).thenReturn("overworld");
        when(blueMapMap.getName()).thenReturn("Overworld");
        when(blueMapMap.getMarkerSets()).thenReturn(mapMarkerSets);

        // Addon + world settings
        when(addon.getWorldSettings()).thenReturn(worldSettings);
        when(worldSettings.getFriendlyName()).thenReturn("BSkyBlock");
        when(addon.getOverWorld()).thenReturn(overWorld);
        when(addon.getNetherWorld()).thenReturn(netherWorld);
        when(addon.getEndWorld()).thenReturn(endWorld);
        when(worldSettings.isNetherGenerate()).thenReturn(false);
        when(worldSettings.isNetherIslands()).thenReturn(false);
        when(worldSettings.isEndGenerate()).thenReturn(false);
        when(worldSettings.isEndIslands()).thenReturn(false);

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
        when(island.getCenter()).thenReturn(center);
        when(island.getWorld()).thenReturn(overWorld);
        when(island.getName()).thenReturn(null);

        // IWM: return addon for overWorld
        when(iwm.getAddon(overWorld)).thenReturn(Optional.of(addon));

        hook = new BlueMapHook();
    }

    /**
     * Calls hook(), simulates BlueMap becoming available, then fires BentoBoxReadyEvent,
     * mirroring the real startup sequence.
     */
    private void hookAndReady() {
        hook.hook();
        simulateBlueMapEnable();
        hook.onBentoBoxReady(mock(BentoBoxReadyEvent.class));
    }

    /** Simulates BlueMap's API becoming available (initial load or after a reload). */
    private void simulateBlueMapEnable() {
        onEnableConsumer.accept(blueMapAPI);
    }

    /** Simulates BlueMap tearing down its API (as happens at the start of a reload). */
    private void simulateBlueMapDisable() {
        if (onDisableConsumer != null) {
            onDisableConsumer.accept(blueMapAPI);
        }
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        mockedBlueMapAPI.close();
        super.tearDown();
    }

    // ---- hook() ----

    @Test
    void testHookSucceeds() {
        assertTrue(hook.hook());
    }

    @Test
    void testHookRegistersLifecycleCallbacks() {
        // hook() succeeds by registering with BlueMap's lifecycle; it no longer fails just
        // because BlueMap's API is not loaded at hook time (that was the startup race).
        assertTrue(hook.hook());
        mockedBlueMapAPI.verify(() -> BlueMapAPI.onEnable(any()));
        mockedBlueMapAPI.verify(() -> BlueMapAPI.onDisable(any()));
    }

    @Test
    void testHookRegistersEvents() {
        hook.hook();
        verify(pim).registerEvents(hook, plugin);
    }

    @Test
    void testBentoBoxReadyCallsRegisterGameMode() {
        hookAndReady();
        verify(im, atLeastOnce()).getIslands(overWorld);
    }

    /**
     * Regression test for the reported bug: island markers disappeared after "/bluemap reload"
     * and did not return until a full server restart. A reload discards BlueMap's maps (and
     * their marker sets) and re-enables the API with fresh, empty maps. The hook must re-attach
     * its markers via the onEnable callback.
     */
    @Test
    void testMarkersSurviveBlueMapReload() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        assertNotNull(mapMarkerSets.get("BSkyBlock"));

        // Simulate /bluemap reload: BlueMap disables, then re-enables with a brand new,
        // empty marker-set map on the freshly rebuilt map.
        Map<String, MarkerSet> freshMapSets = new HashMap<>();
        when(blueMapMap.getMarkerSets()).thenReturn(freshMapSets);
        simulateBlueMapDisable();
        simulateBlueMapEnable();

        // The island marker set and its markers must be re-attached to the new map.
        assertTrue(freshMapSets.containsKey("BSkyBlock"));
        assertNotNull(freshMapSets.get("BSkyBlock").get(uuid.toString()));
        assertNotNull(freshMapSets.get("BSkyBlock").get(uuid.toString() + "_area"));
    }

    // ---- getPluginName() / getFailureCause() ----

    @Test
    void testGetPluginName() {
        assertEquals("BlueMap", hook.getPluginName());
    }

    @Test
    void testGetFailureCause() {
        assertNotNull(hook.getFailureCause());
    }

    // ---- registerGameMode() ----

    @Test
    void testRegisterGameModeNoIslands() {
        hookAndReady();
        // marker set should be attached to the map
        assertTrue(mapMarkerSets.containsKey("BSkyBlock"));
    }

    @Test
    void testRegisterGameModeWithIsland() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms);
        assertNotNull(ms.get(uuid.toString()));
        // Area marker should also be created
        assertNotNull(ms.get(uuid.toString() + "_area"));
        assertInstanceOf(ShapeMarker.class, ms.get(uuid.toString() + "_area"));
    }

    @Test
    void testIslandMarkersDisabledNoPointMarker() {
        plugin.getSettings().setBluemapIslandMarkers(false);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms);
        // No POI pin, but the area shape is still drawn
        assertNull(ms.get(uuid.toString()));
        assertNotNull(ms.get(uuid.toString() + "_area"));
    }

    @Test
    void testIslandAreasDisabledNoAreaMarker() {
        plugin.getSettings().setBluemapIslandAreas(false);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms);
        // POI pin still placed, but no area shape
        assertNotNull(ms.get(uuid.toString()));
        assertNull(ms.get(uuid.toString() + "_area"));
    }

    @Test
    void testCustomMarkerIconApplied() {
        plugin.getSettings().setBluemapMarkerIcon("assets/island.png");
        plugin.getSettings().setBluemapMarkerIconAnchorX(30);
        plugin.getSettings().setBluemapMarkerIconAnchorY(60);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        POIMarker pin = (POIMarker) mapMarkerSets.get("BSkyBlock").get(uuid.toString());
        assertEquals("assets/island.png", pin.getIconAddress());
        assertEquals(30, pin.getAnchor().getX());
        assertEquals(60, pin.getAnchor().getY());
    }

    @Test
    void testMarkerDistanceRangeApplied() {
        plugin.getSettings().setBluemapMarkerMinDistance(15.0);
        plugin.getSettings().setBluemapMarkerMaxDistance(5000.0);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        POIMarker pin = (POIMarker) mapMarkerSets.get("BSkyBlock").get(uuid.toString());
        assertEquals(15.0, pin.getMinDistance());
        assertEquals(5000.0, pin.getMaxDistance());
    }

    @Test
    void testAreaStyleCustomised() {
        plugin.getSettings().setBluemapAreaLineColor("#FF0000");
        plugin.getSettings().setBluemapAreaFillColor("#00FF00");
        plugin.getSettings().setBluemapAreaFillOpacity(0.5);
        plugin.getSettings().setBluemapAreaLineWidth(4);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        ShapeMarker area = (ShapeMarker) mapMarkerSets.get("BSkyBlock").get(uuid.toString() + "_area");
        assertEquals(255, area.getLineColor().getRed());
        assertEquals(0, area.getLineColor().getGreen());
        assertEquals(0, area.getLineColor().getBlue());
        assertEquals(0, area.getFillColor().getRed());
        assertEquals(255, area.getFillColor().getGreen());
        assertEquals(0.5f, area.getFillColor().getAlpha());
        assertEquals(4, area.getLineWidth());
    }

    @Test
    void testMalformedAreaColorFallsBackToDefault() {
        plugin.getSettings().setBluemapAreaLineColor("not-a-colour");
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        ShapeMarker area = (ShapeMarker) mapMarkerSets.get("BSkyBlock").get(uuid.toString() + "_area");
        // Falls back to the default island blue (51, 136, 255) instead of throwing
        assertEquals(51, area.getLineColor().getRed());
        assertEquals(136, area.getLineColor().getGreen());
        assertEquals(255, area.getLineColor().getBlue());
    }

    @Test
    void testRegisterGameModeUnownedIslandIgnored() {
        Island unowned = mock(Island.class);
        when(unowned.getOwner()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(unowned));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms);
        assertTrue(ms.getMarkers().isEmpty());
    }

    @Test
    void testRegisterGameModeWithNetherWorld() {
        when(worldSettings.isNetherGenerate()).thenReturn(true);
        when(worldSettings.isNetherIslands()).thenReturn(true);
        Map<String, MarkerSet> netherMapSets = new HashMap<>();
        BlueMapMap netherMap = mock(BlueMapMap.class);
        BlueMapWorld netherBMWorld = mock(BlueMapWorld.class);
        when(blueMapAPI.getWorld(netherWorld)).thenReturn(Optional.of(netherBMWorld));
        when(netherBMWorld.getMaps()).thenReturn(List.of(netherMap));
        when(netherBMWorld.getId()).thenReturn("nether");
        when(netherMap.getName()).thenReturn("Nether");
        when(netherMap.getMarkerSets()).thenReturn(netherMapSets);

        hookAndReady();
        assertTrue(netherMapSets.containsKey("BSkyBlock"));
    }

    @Test
    void testRegisterGameModeWithEndWorld() {
        when(worldSettings.isEndGenerate()).thenReturn(true);
        when(worldSettings.isEndIslands()).thenReturn(true);
        Map<String, MarkerSet> endMapSets = new HashMap<>();
        BlueMapMap endMap = mock(BlueMapMap.class);
        BlueMapWorld endBMWorld = mock(BlueMapWorld.class);
        when(blueMapAPI.getWorld(endWorld)).thenReturn(Optional.of(endBMWorld));
        when(endBMWorld.getMaps()).thenReturn(List.of(endMap));
        when(endBMWorld.getId()).thenReturn("end");
        when(endMap.getName()).thenReturn("End");
        when(endMap.getMarkerSets()).thenReturn(endMapSets);

        hookAndReady();
        assertTrue(endMapSets.containsKey("BSkyBlock"));
    }

    @Test
    void testRegisterGameModeNetherWorldNull() {
        when(worldSettings.isNetherGenerate()).thenReturn(true);
        when(worldSettings.isNetherIslands()).thenReturn(true);
        when(addon.getNetherWorld()).thenReturn(null);
        // should not throw
        hookAndReady();
        // nether map should not have been touched
        verify(blueMapAPI, never()).getWorld(netherWorld);
    }

    @Test
    void testRegisterGameModeBlueMapDoesNotKnowWorld() {
        when(blueMapAPI.getWorld(overWorld)).thenReturn(Optional.empty());
        hookAndReady();
        // No marker sets added since BlueMap doesn't know the world
        assertFalse(mapMarkerSets.containsKey("BSkyBlock"));
    }

    // ---- Island label logic ----

    @Test
    void testIslandLabelUsesCustomName() {
        when(island.getName()).thenReturn("My Island");
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms.get(uuid.toString()));
        assertEquals("My Island", ms.get(uuid.toString()).getLabel());
    }

    @Test
    void testIslandLabelUsesOwnerName() {
        when(island.getName()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        // player name from CommonTestSetup is "tastybento"
        assertEquals("tastybento", ms.get(uuid.toString()).getLabel());
    }

    @Test
    void testIslandLabelFallsBackToUUID() {
        // The registerGameMode filter skips null-owner islands; test UUID fallback via the event path
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
        when(noOwnerIsland.getCenter()).thenReturn(c);
        when(noOwnerIsland.getWorld()).thenReturn(overWorld);

        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(noOwnerIsland);
        hook.onNewIsland(event);

        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms.get(noOwnerUuid.toString()));
        assertEquals(noOwnerUuid.toString(), ms.get(noOwnerUuid.toString()).getLabel());
    }

    // ---- Event handlers ----

    @Test
    void testOnNewIsland() {
        hookAndReady();
        // Clear existing markers so we can detect the add
        mapMarkerSets.get("BSkyBlock").remove(uuid.toString());

        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onNewIsland(event);

        assertNotNull(mapMarkerSets.get("BSkyBlock").get(uuid.toString()));
    }

    @Test
    void testOnNewIslandNoAddon() {
        hookAndReady();
        when(iwm.getAddon(overWorld)).thenReturn(Optional.empty());
        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        // Should not throw; marker set unchanged
        int sizeBefore = mapMarkerSets.get("BSkyBlock").getMarkers().size();
        hook.onNewIsland(event);
        assertEquals(sizeBefore, mapMarkerSets.get("BSkyBlock").getMarkers().size());
    }

    @Test
    void testOnIslandDelete() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();
        // island should be in the marker set after hook()
        IslandDeleteEvent event = mock(IslandDeleteEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onIslandDelete(event);
        // both markers should be removed
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertFalse(ms.getMarkers().containsKey(uuid.toString()));
        assertFalse(ms.getMarkers().containsKey(uuid.toString() + "_area"));
    }

    @Test
    void testOnIslandName() {
        when(island.getName()).thenReturn("Old Name");
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hookAndReady();

        // Now rename
        when(island.getName()).thenReturn("New Name");
        IslandNameEvent event = mock(IslandNameEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onIslandName(event);

        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms.get(uuid.toString()));
        assertEquals("New Name", ms.get(uuid.toString()).getLabel());
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
        when(newIsland.getCenter()).thenReturn(newCenter);
        when(newIsland.getWorld()).thenReturn(overWorld);

        IslandResettedEvent event = mock(IslandResettedEvent.class);
        when(event.getIsland()).thenReturn(newIsland);
        when(event.getOldIsland()).thenReturn(island);
        hook.onIslandReset(event);

        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        // Old island marker removed
        assertFalse(ms.getMarkers().containsKey(uuid.toString()));
        // New island marker added
        assertNotNull(ms.get(newUuid.toString()));
        assertNotNull(ms.get(newUuid.toString() + "_area"));
    }

    // ---- Public addon API ----

    @Test
    void testGetBlueMapAPI() {
        hook.hook();
        simulateBlueMapEnable();
        assertEquals(blueMapAPI, hook.getBlueMapAPI());
    }

    @Test
    void testGetMarkerSet() {
        hookAndReady();
        assertEquals(mapMarkerSets.get("BSkyBlock"), hook.getMarkerSet(addon));
    }

    @Test
    void testGetMarkerSetNotRegistered() {
        hookAndReady();
        GameModeAddon unknownAddon = mock(GameModeAddon.class);
        WorldSettings unknownSettings = mock(WorldSettings.class);
        when(unknownAddon.getWorldSettings()).thenReturn(unknownSettings);
        when(unknownSettings.getFriendlyName()).thenReturn("UnknownGame");
        assertNull(hook.getMarkerSet(unknownAddon));
    }

    @Test
    void testCreateMarkerSet() {
        when(blueMapAPI.getMaps()).thenReturn(List.of(blueMapMap));
        hook.hook();
        simulateBlueMapEnable();
        hook.createMarkerSet("warps.markers", "Warps");
        // Should be attached to the BlueMap map
        assertTrue(mapMarkerSets.containsKey("warps.markers"));
        assertEquals("Warps", mapMarkerSets.get("warps.markers").getLabel());
    }
}
