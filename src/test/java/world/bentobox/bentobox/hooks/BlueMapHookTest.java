package world.bentobox.bentobox.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.events.island.IslandNameEvent;
import world.bentobox.bentobox.api.events.island.IslandNewIslandEvent;
import world.bentobox.bentobox.api.events.island.IslandResettedEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * Test class for BlueMapHook
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, BlueMapAPI.class })
public class BlueMapHookTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private BlueMapAPI blueMapAPI;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private IslandsManager islandsManager;
    @Mock
    private IslandWorldManager islandWorldManager;
    @Mock
    private AddonsManager addonsManager;
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
    private BlueMapWorld blueMapWorld;
    @Mock
    private BlueMapMap blueMapMap;
    @Mock
    private Island island;

    private BlueMapHook hook;
    private Map<String, MarkerSet> mapMarkerSets;

    private static final String FRIENDLY_NAME = "BSkyBlock";
    private static final String ISLAND_UUID = UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getAddonsManager()).thenReturn(addonsManager);
        when(plugin.getIslands()).thenReturn(islandsManager);
        when(plugin.getIWM()).thenReturn(islandWorldManager);

        // Suppress debug logging
        Mockito.doNothing().when(plugin).logDebug(any());

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);

        // BlueMapAPI static mock
        PowerMockito.mockStatic(BlueMapAPI.class, Mockito.RETURNS_MOCKS);
        when(BlueMapAPI.getInstance()).thenReturn(Optional.of(blueMapAPI));

        // GameMode addon setup
        when(addonsManager.getGameModeAddons()).thenReturn(Collections.singletonList(addon));
        when(addon.getWorldSettings()).thenReturn(worldSettings);
        when(worldSettings.getFriendlyName()).thenReturn(FRIENDLY_NAME);
        when(addon.getOverWorld()).thenReturn(overWorld);
        when(addon.getNetherWorld()).thenReturn(netherWorld);
        when(addon.getEndWorld()).thenReturn(endWorld);
        when(worldSettings.isNetherGenerate()).thenReturn(false);
        when(worldSettings.isNetherIslands()).thenReturn(false);
        when(worldSettings.isEndGenerate()).thenReturn(false);
        when(worldSettings.isEndIslands()).thenReturn(false);

        // Islands manager
        when(islandsManager.getIslands(overWorld)).thenReturn(Collections.emptyList());

        // BlueMap world/map setup
        when(blueMapAPI.getWorld(overWorld)).thenReturn(Optional.of(blueMapWorld));
        when(blueMapAPI.getWorld(netherWorld)).thenReturn(Optional.of(blueMapWorld));
        when(blueMapAPI.getWorld(endWorld)).thenReturn(Optional.of(blueMapWorld));
        when(blueMapWorld.getId()).thenReturn("overworld");
        when(blueMapWorld.getMaps()).thenReturn(Collections.singletonList(blueMapMap));
        when(blueMapMap.getName()).thenReturn("world");
        mapMarkerSets = new HashMap<>();
        when(blueMapMap.getMarkerSets()).thenReturn(mapMarkerSets);

        // Island mock
        when(island.getUniqueId()).thenReturn(ISLAND_UUID);
        when(island.getOwner()).thenReturn(UUID.randomUUID());
        when(island.getWorld()).thenReturn(overWorld);
        Location center = mock(Location.class);
        when(center.getX()).thenReturn(0.0);
        when(center.getY()).thenReturn(64.0);
        when(center.getZ()).thenReturn(0.0);
        when(island.getCenter()).thenReturn(center);
        when(island.getName()).thenReturn(null);

        // IWM
        when(islandWorldManager.getAddon(overWorld)).thenReturn(Optional.of(addon));

        // User for island owner
        User.setPlugin(plugin);

        hook = new BlueMapHook();
    }

    @After
    public void tearDown() {
        User.clearUsers();
    }

    /**
     * Test that hook() returns true when BlueMapAPI is present.
     */
    @Test
    public void testHookSuccess() {
        assertTrue(hook.hook());
        verify(pluginManager).registerEvents(hook, plugin);
    }

    /**
     * Test that hook() returns false when BlueMapAPI is not present.
     */
    @Test
    public void testHookFailure() {
        when(BlueMapAPI.getInstance()).thenReturn(Optional.empty());
        assertFalse(hook.hook());
        verify(pluginManager, never()).registerEvents(any(), any());
    }

    /**
     * Test that the hook has the correct icon.
     */
    @Test
    public void testIcon() {
        assertEquals(Material.MAP, hook.getIcon());
    }

    /**
     * Test that the hook has the correct name.
     */
    @Test
    public void testName() {
        assertNotNull(hook);
        assertEquals("BlueMap", hook.getPluginName());
    }

    /**
     * Test that getFailureCause returns a message.
     */
    @Test
    public void testGetFailureCause() {
        assertNotNull(hook.getFailureCause());
        assertFalse(hook.getFailureCause().isBlank());
    }

    /**
     * Test that registerGameMode sets up the marker set for the over world.
     */
    @Test
    public void testRegisterGameModeOverWorldOnly() {
        assertTrue(hook.hook());
        // The marker set should have been added to the BlueMap map
        assertTrue(mapMarkerSets.containsKey(FRIENDLY_NAME));
    }

    /**
     * Test that registerGameMode adds islands to the marker set.
     */
    @Test
    public void testRegisterGameModeWithIsland() {
        when(islandsManager.getIslands(overWorld)).thenReturn(Collections.singletonList(island));
        // Give the island a name
        when(island.getName()).thenReturn("My Island");

        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        assertNotNull(markerSet);
        // The island should be added as a marker with its unique ID as key
        assertNotNull(markerSet.get(ISLAND_UUID));
    }

    /**
     * Test that nether world markers are set up when nether islands are enabled.
     */
    @Test
    public void testRegisterGameModeWithNetherWorld() {
        when(worldSettings.isNetherGenerate()).thenReturn(true);
        when(worldSettings.isNetherIslands()).thenReturn(true);

        Map<String, MarkerSet> netherMarkerSets = new HashMap<>();
        BlueMapMap netherMap = mock(BlueMapMap.class);
        when(netherMap.getName()).thenReturn("nether");
        when(netherMap.getMarkerSets()).thenReturn(netherMarkerSets);
        BlueMapWorld netherBmWorld = mock(BlueMapWorld.class);
        when(netherBmWorld.getId()).thenReturn("nether");
        when(netherBmWorld.getMaps()).thenReturn(Collections.singletonList(netherMap));
        when(blueMapAPI.getWorld(netherWorld)).thenReturn(Optional.of(netherBmWorld));

        assertTrue(hook.hook());

        // Marker set should be added to nether map
        assertTrue(netherMarkerSets.containsKey(FRIENDLY_NAME));
    }

    /**
     * Test that end world markers are set up when end islands are enabled.
     */
    @Test
    public void testRegisterGameModeWithEndWorld() {
        when(worldSettings.isEndGenerate()).thenReturn(true);
        when(worldSettings.isEndIslands()).thenReturn(true);

        Map<String, MarkerSet> endMarkerSets = new HashMap<>();
        BlueMapMap endMap = mock(BlueMapMap.class);
        when(endMap.getName()).thenReturn("the_end");
        when(endMap.getMarkerSets()).thenReturn(endMarkerSets);
        BlueMapWorld endBmWorld = mock(BlueMapWorld.class);
        when(endBmWorld.getId()).thenReturn("end");
        when(endBmWorld.getMaps()).thenReturn(Collections.singletonList(endMap));
        when(blueMapAPI.getWorld(endWorld)).thenReturn(Optional.of(endBmWorld));

        assertTrue(hook.hook());

        // Marker set should be added to end map
        assertTrue(endMarkerSets.containsKey(FRIENDLY_NAME));
    }

    /**
     * Test that nether world markers are NOT set up when nether world is null.
     */
    @Test
    public void testRegisterGameModeNetherWorldNull() {
        when(worldSettings.isNetherGenerate()).thenReturn(true);
        when(worldSettings.isNetherIslands()).thenReturn(true);
        when(addon.getNetherWorld()).thenReturn(null);

        assertTrue(hook.hook());
        // No exception should be thrown, only over world marker set set up
        assertTrue(mapMarkerSets.containsKey(FRIENDLY_NAME));
    }

    /**
     * Test the onNewIsland event handler adds a marker.
     */
    @Test
    public void testOnNewIsland() {
        // No islands initially
        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        assertNotNull(markerSet);
        assertEquals(0, markerSet.getMarkers().size());

        // Fire the new island event
        when(island.getName()).thenReturn("New Island");
        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        when(event.getEventName()).thenReturn("IslandNewIslandEvent");

        hook.onNewIsland(event);

        // Island marker should have been added with unique ID as key
        assertEquals(1, markerSet.getMarkers().size());
        assertNotNull(markerSet.get(ISLAND_UUID));
        assertEquals("New Island", markerSet.get(ISLAND_UUID).getLabel());
    }

    /**
     * Test the onIslandDelete event handler removes a marker.
     */
    @Test
    public void testOnIslandDelete() {
        // Pre-populate with an island
        when(islandsManager.getIslands(overWorld)).thenReturn(Collections.singletonList(island));
        when(island.getName()).thenReturn("Test Island");
        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        assertNotNull(markerSet.get(ISLAND_UUID));

        IslandDeleteEvent event = mock(IslandDeleteEvent.class);
        when(event.getIsland()).thenReturn(island);
        when(event.getEventName()).thenReturn("IslandDeleteEvent");

        hook.onIslandDelete(event);

        // Island marker should have been removed
        assertFalse(markerSet.getMarkers().containsKey(ISLAND_UUID));
    }

    /**
     * Test the onIslandName event handler updates the marker label.
     */
    @Test
    public void testOnIslandName() {
        // Pre-populate with an island
        when(islandsManager.getIslands(overWorld)).thenReturn(Collections.singletonList(island));
        when(island.getName()).thenReturn("Old Name");
        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        assertNotNull(markerSet.get(ISLAND_UUID));

        // Now rename the island
        when(island.getName()).thenReturn("New Name");
        IslandNameEvent event = mock(IslandNameEvent.class);
        when(event.getIsland()).thenReturn(island);
        when(event.getEventName()).thenReturn("IslandNameEvent");

        hook.onIslandName(event);

        // Marker should still exist (removed old, added new)
        assertNotNull(markerSet.get(ISLAND_UUID));
        assertEquals("New Name", markerSet.get(ISLAND_UUID).getLabel());
    }

    /**
     * Test the onIslandReset event handler removes old island marker and adds new one.
     */
    @Test
    public void testOnIslandReset() {
        // Pre-populate with the old island
        when(islandsManager.getIslands(overWorld)).thenReturn(Collections.singletonList(island));
        when(island.getName()).thenReturn("Old Island");
        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        assertNotNull(markerSet.get(ISLAND_UUID));

        // Create the new island
        String newIslandUuid = UUID.randomUUID().toString();
        Island newIsland = mock(Island.class);
        when(newIsland.getUniqueId()).thenReturn(newIslandUuid);
        when(newIsland.getOwner()).thenReturn(UUID.randomUUID());
        when(newIsland.getWorld()).thenReturn(overWorld);
        when(newIsland.getName()).thenReturn("New Island");
        Location newCenter = mock(Location.class);
        when(newCenter.getX()).thenReturn(100.0);
        when(newCenter.getY()).thenReturn(64.0);
        when(newCenter.getZ()).thenReturn(100.0);
        when(newIsland.getCenter()).thenReturn(newCenter);

        IslandResettedEvent event = mock(IslandResettedEvent.class);
        when(event.getIsland()).thenReturn(newIsland);
        when(event.getOldIsland()).thenReturn(island);
        when(event.getEventName()).thenReturn("IslandResettedEvent");

        hook.onIslandReset(event);

        // Old island marker should be removed
        assertFalse(markerSet.getMarkers().containsKey(ISLAND_UUID));
        // New island marker should be added
        assertNotNull(markerSet.get(newIslandUuid));
    }

    /**
     * Test that onNewIsland does nothing when no addon is found for the world.
     */
    @Test
    public void testOnNewIslandNoAddon() {
        assertTrue(hook.hook());
        when(islandWorldManager.getAddon(overWorld)).thenReturn(Optional.empty());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        int sizeBefore = markerSet.getMarkers().size();

        IslandNewIslandEvent event = mock(IslandNewIslandEvent.class);
        when(event.getIsland()).thenReturn(island);
        when(event.getEventName()).thenReturn("IslandNewIslandEvent");

        hook.onNewIsland(event);

        // No marker should be added
        assertEquals(sizeBefore, markerSet.getMarkers().size());
    }

    /**
     * Test that multiple islands can have the same label (name) but different marker IDs.
     */
    @Test
    public void testMultipleIslandsWithSameName() {
        String island2Uuid = UUID.randomUUID().toString();
        Island island2 = mock(Island.class);
        when(island2.getUniqueId()).thenReturn(island2Uuid);
        when(island2.getOwner()).thenReturn(UUID.randomUUID());
        when(island2.getWorld()).thenReturn(overWorld);
        when(island2.getName()).thenReturn("Shared Name");
        Location center2 = mock(Location.class);
        when(center2.getX()).thenReturn(500.0);
        when(center2.getY()).thenReturn(64.0);
        when(center2.getZ()).thenReturn(500.0);
        when(island2.getCenter()).thenReturn(center2);

        when(island.getName()).thenReturn("Shared Name");
        when(islandsManager.getIslands(overWorld)).thenReturn(List.of(island, island2));

        assertTrue(hook.hook());

        MarkerSet markerSet = mapMarkerSets.get(FRIENDLY_NAME);
        // Both islands should be present with their unique IDs
        assertNotNull(markerSet.get(ISLAND_UUID));
        assertNotNull(markerSet.get(island2Uuid));
        assertEquals(2, markerSet.getMarkers().size());
    }
}
