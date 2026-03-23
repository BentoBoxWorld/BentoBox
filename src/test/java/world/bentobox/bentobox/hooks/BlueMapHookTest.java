package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
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

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // BlueMapAPI static mock
        mockedBlueMapAPI = Mockito.mockStatic(BlueMapAPI.class);
        mockedBlueMapAPI.when(BlueMapAPI::getInstance).thenReturn(Optional.of(blueMapAPI));

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
    void testHookFailsWhenBlueMapNotPresent() {
        mockedBlueMapAPI.when(BlueMapAPI::getInstance).thenReturn(Optional.empty());
        assertFalse(hook.hook());
    }

    @Test
    void testHookRegistersEvents() {
        hook.hook();
        verify(pim).registerEvents(hook, plugin);
    }

    @Test
    void testHookCallsRegisterGameMode() {
        hook.hook();
        verify(im).getIslands(overWorld);
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
        hook.hook();
        // marker set should be attached to the map
        assertTrue(mapMarkerSets.containsKey("BSkyBlock"));
    }

    @Test
    void testRegisterGameModeWithIsland() {
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hook.hook();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms);
        assertNotNull(ms.get(uuid.toString()));
        // Area marker should also be created
        assertNotNull(ms.get(uuid.toString() + "_area"));
        assertInstanceOf(ShapeMarker.class, ms.get(uuid.toString() + "_area"));
    }

    @Test
    void testRegisterGameModeUnownedIslandIgnored() {
        Island unowned = mock(Island.class);
        when(unowned.getOwner()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(unowned));
        hook.hook();
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

        hook.hook();
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

        hook.hook();
        assertTrue(endMapSets.containsKey("BSkyBlock"));
    }

    @Test
    void testRegisterGameModeNetherWorldNull() {
        when(worldSettings.isNetherGenerate()).thenReturn(true);
        when(worldSettings.isNetherIslands()).thenReturn(true);
        when(addon.getNetherWorld()).thenReturn(null);
        // should not throw
        hook.hook();
        // nether map should not have been touched
        verify(blueMapAPI, never()).getWorld(netherWorld);
    }

    @Test
    void testRegisterGameModeBlueMapDoesNotKnowWorld() {
        when(blueMapAPI.getWorld(overWorld)).thenReturn(Optional.empty());
        hook.hook();
        // No marker sets added since BlueMap doesn't know the world
        assertFalse(mapMarkerSets.containsKey("BSkyBlock"));
    }

    // ---- Island label logic ----

    @Test
    void testIslandLabelUsesCustomName() {
        when(island.getName()).thenReturn("My Island");
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hook.hook();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        assertNotNull(ms.get(uuid.toString()));
        assertEquals("My Island", ms.get(uuid.toString()).getLabel());
    }

    @Test
    void testIslandLabelUsesOwnerName() {
        when(island.getName()).thenReturn(null);
        when(im.getIslands(overWorld)).thenReturn(List.of(island));
        hook.hook();
        MarkerSet ms = mapMarkerSets.get("BSkyBlock");
        // player name from CommonTestSetup is "tastybento"
        assertEquals("tastybento", ms.get(uuid.toString()).getLabel());
    }

    @Test
    void testIslandLabelFallsBackToUUID() {
        // The registerGameMode filter skips null-owner islands; test UUID fallback via the event path
        hook.hook();
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
        hook.hook();
        // Clear existing markers so we can detect the add
        mapMarkerSets.get("BSkyBlock").remove(uuid.toString());

        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        hook.onNewIsland(event);

        assertNotNull(mapMarkerSets.get("BSkyBlock").get(uuid.toString()));
    }

    @Test
    void testOnNewIslandNoAddon() {
        hook.hook();
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
        hook.hook();
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
        hook.hook();

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
        hook.hook();

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
        assertEquals(blueMapAPI, hook.getBlueMapAPI());
    }

    @Test
    void testGetMarkerSet() {
        hook.hook();
        assertEquals(mapMarkerSets.get("BSkyBlock"), hook.getMarkerSet(addon));
    }

    @Test
    void testGetMarkerSetNotRegistered() {
        hook.hook();
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
        MarkerSet result = hook.createMarkerSet("warps.markers", "Warps");
        assertNotNull(result);
        assertEquals("Warps", result.getLabel());
        // Should be attached to the BlueMap map
        assertTrue(mapMarkerSets.containsKey("warps.markers"));
    }
}
