package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for {@link GeoLimitMobsListener}
 */
class GeoLimitMobsListenerTest extends CommonTestSetup {

    private List<String> geoLimitList = new ArrayList<>();
    private GeoLimitMobsListener listener;

    @Mock
    private Snowball snowball;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        geoLimitList.add("SNOWBALL");
        when(iwm.getGeoLimitSettings(world)).thenReturn(geoLimitList);
        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.inWorld(location)).thenReturn(true);
        when(location.getWorld()).thenReturn(world);
        when(snowball.getType()).thenReturn(EntityType.SNOWBALL);
        when(snowball.getLocation()).thenReturn(location);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        listener = new GeoLimitMobsListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that a projectile in the geo-limit list is tracked when launched.
     */
    @Test
    void testOnProjectileLaunchTracked() {
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(snowball);
        listener.onProjectileLaunch(e);
        // Verify that getIslandAt was called to track the projectile
        verify(im).getIslandAt(location);
    }

    /**
     * Test that a projectile NOT in the geo-limit list is NOT tracked.
     */
    @Test
    void testOnProjectileLaunchNotInList() {
        when(snowball.getType()).thenReturn(EntityType.ARROW);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(snowball);
        listener.onProjectileLaunch(e);
        // Verify that getIslandAt was NOT called (not tracked)
        verify(im, never()).getIslandAt(any());
    }

    /**
     * Test that a projectile launched outside a BentoBox world is NOT tracked.
     */
    @Test
    void testOnProjectileLaunchNotInWorld() {
        when(iwm.inWorld(location)).thenReturn(false);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(snowball);
        listener.onProjectileLaunch(e);
        // Verify that getIslandAt was NOT called (not tracked)
        verify(im, never()).getIslandAt(any());
    }

    /**
     * Test that a projectile launched outside any island is NOT tracked.
     */
    @Test
    void testOnProjectileLaunchNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(snowball);
        listener.onProjectileLaunch(e);
        // getIslandAt is called, but no island found so nothing is added to tracker
        verify(im).getIslandAt(location);
    }

    /**
     * Test that a projectile launched in a different world is NOT tracked.
     */
    @Test
    void testOnProjectileLaunchDifferentWorld() {
        when(location.getWorld()).thenReturn(org.mockito.Mockito.mock(World.class));
        when(iwm.inWorld(location)).thenReturn(false);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(snowball);
        listener.onProjectileLaunch(e);
        verify(im, never()).getIslandAt(any());
    }

}
