package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.WindCharge;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 */
class WindChargeListenerTest extends CommonTestSetup {

    private WindChargeListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        listener = new WindChargeListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that a player is allowed to launch wind charges when the flag is enabled.
     */
    @Test
    void testOnWindChargeLaunchAllowed() {
        WindCharge entity = mock(WindCharge.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(mockPlayer);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        listener.onWindChargeLaunch(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test that a player is NOT allowed to launch wind charges when the flag is disabled.
     */
    @Test
    void testOnWindChargeLaunchNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        WindCharge entity = mock(WindCharge.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(mockPlayer);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        listener.onWindChargeLaunch(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test that a non-player entity (e.g. Blaze) shooting a wind charge is not blocked.
     */
    @Test
    void testOnWindChargeLaunchNonPlayer() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        WindCharge entity = mock(WindCharge.class);
        when(entity.getLocation()).thenReturn(location);
        Blaze blaze = mock(Blaze.class);
        when(entity.getShooter()).thenReturn(blaze);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        listener.onWindChargeLaunch(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test that a non-wind-charge projectile is not affected by this listener.
     */
    @Test
    void testOnWindChargeLaunchNonWindCharge() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        org.bukkit.entity.Arrow arrow = mock(org.bukkit.entity.Arrow.class);
        when(arrow.getLocation()).thenReturn(location);
        when(arrow.getShooter()).thenReturn(mockPlayer);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(arrow);
        listener.onWindChargeLaunch(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test that AbstractWindCharge (e.g. BreezeWindCharge) fired by a player is also blocked.
     */
    @Test
    void testOnAbstractWindChargeLaunchNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        AbstractWindCharge entity = mock(AbstractWindCharge.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(mockPlayer);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        listener.onWindChargeLaunch(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }
}
