package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.hooks.MapHook;

class MapManagerTest extends CommonTestSetup {

    @Mock
    private MapHook mapHook1;
    @Mock
    private MapHook mapHook2;
    @Mock
    private Hook nonMapHook;

    private MapManager mapManager;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getHooks()).thenReturn(hooksManager);
        mapManager = new MapManager(plugin);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // --- hasMapHook ---

    @Test
    void testHasMapHookFalseWhenNoHooks() {
        when(hooksManager.getHooks()).thenReturn(List.of());
        assertFalse(mapManager.hasMapHook());
    }

    @Test
    void testHasMapHookFalseWhenOnlyNonMapHooks() {
        when(hooksManager.getHooks()).thenReturn(List.of(nonMapHook));
        assertFalse(mapManager.hasMapHook());
    }

    @Test
    void testHasMapHookTrue() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1));
        assertTrue(mapManager.hasMapHook());
    }

    // --- Fan-out to multiple hooks ---

    @Test
    void testCreateMarkerSetFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        mapManager.createMarkerSet("test", "Test");
        verify(mapHook1).createMarkerSet("test", "Test");
        verify(mapHook2).createMarkerSet("test", "Test");
    }

    @Test
    void testRemoveMarkerSetFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        mapManager.removeMarkerSet("test");
        verify(mapHook1).removeMarkerSet("test");
        verify(mapHook2).removeMarkerSet("test");
    }

    @Test
    void testClearMarkerSetFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        mapManager.clearMarkerSet("test");
        verify(mapHook1).clearMarkerSet("test");
        verify(mapHook2).clearMarkerSet("test");
    }

    @Test
    void testAddPointMarkerFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        Location loc = mock(Location.class);
        mapManager.addPointMarker("set", "id", "label", loc);
        verify(mapHook1).addPointMarker("set", "id", "label", loc, "default");
        verify(mapHook2).addPointMarker("set", "id", "label", loc, "default");
    }

    @Test
    void testAddPointMarkerWithIconFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        Location loc = mock(Location.class);
        mapManager.addPointMarker("set", "id", "label", loc, "sign");
        verify(mapHook1).addPointMarker("set", "id", "label", loc, "sign");
        verify(mapHook2).addPointMarker("set", "id", "label", loc, "sign");
    }

    @Test
    void testRemovePointMarkerFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        mapManager.removePointMarker("set", "id");
        verify(mapHook1).removePointMarker("set", "id");
        verify(mapHook2).removePointMarker("set", "id");
    }

    @Test
    void testAddAreaMarkerFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        World w = mock(World.class);
        Color line = new Color(255, 0, 0);
        Color fill = new Color(0, 0, 255, 38);
        mapManager.addAreaMarker("set", "id", "label", w, 0, 0, 100, 100, line, fill, 2);
        verify(mapHook1).addAreaMarker("set", "id", "label", w, 0, 0, 100, 100, line, fill, 2);
        verify(mapHook2).addAreaMarker("set", "id", "label", w, 0, 0, 100, 100, line, fill, 2);
    }

    @Test
    void testAddPolygonMarkerFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        World w = mock(World.class);
        double[] x = { 0, 100, 100 };
        double[] z = { 0, 0, 100 };
        Color line = Color.RED;
        Color fill = Color.BLUE;
        mapManager.addPolygonMarker("set", "id", "label", w, x, z, line, fill, 1);
        verify(mapHook1).addPolygonMarker("set", "id", "label", w, x, z, line, fill, 1);
        verify(mapHook2).addPolygonMarker("set", "id", "label", w, x, z, line, fill, 1);
    }

    @Test
    void testRemoveAreaMarkerFansOut() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        mapManager.removeAreaMarker("set", "id");
        verify(mapHook1).removeAreaMarker("set", "id");
        verify(mapHook2).removeAreaMarker("set", "id");
    }

    // --- Silent no-op when no map hooks ---

    @Test
    void testNoOpWhenNoHooks() {
        when(hooksManager.getHooks()).thenReturn(List.of());
        Location loc = mock(Location.class);
        // These should all silently do nothing
        mapManager.createMarkerSet("test", "Test");
        mapManager.addPointMarker("test", "id", "label", loc);
        mapManager.removePointMarker("test", "id");
        mapManager.addAreaMarker("test", "id", "label", mock(World.class), 0, 0, 100, 100, Color.RED, Color.BLUE, 2);
        mapManager.removeAreaMarker("test", "id");
        mapManager.clearMarkerSet("test");
        mapManager.removeMarkerSet("test");
    }

    // --- Exception isolation ---

    @Test
    void testExceptionInOneHookDoesNotBlockOther() {
        when(hooksManager.getHooks()).thenReturn(List.of(mapHook1, mapHook2));
        doThrow(new RuntimeException("boom")).when(mapHook1).createMarkerSet(anyString(), anyString());
        mapManager.createMarkerSet("test", "Test");
        // mapHook2 should still be called despite mapHook1 throwing
        verify(mapHook2).createMarkerSet("test", "Test");
    }

    // --- Non-map hooks are filtered out ---

    @Test
    void testNonMapHooksIgnored() {
        when(hooksManager.getHooks()).thenReturn(List.of(nonMapHook, mapHook1));
        mapManager.createMarkerSet("test", "Test");
        verify(mapHook1).createMarkerSet("test", "Test");
    }
}
