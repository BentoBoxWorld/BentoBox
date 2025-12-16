package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
public class ElytraListenerTest extends CommonTestSetup {

    private ElytraListener el;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Player
        when(mockPlayer.isGliding()).thenReturn(true);

        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Class under test
        el = new ElytraListener();
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGlide(org.bukkit.event.entity.EntityToggleGlideEvent)}.
     */
    @Test
    public void testOnGlideAllowed() {
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(mockPlayer, false);
        el.onGlide(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGlide(org.bukkit.event.entity.EntityToggleGlideEvent)}.
     */
    @Test
    public void testOnGlideNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(mockPlayer, false);
        el.onGlide(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingAllowed() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location);
        el.onGliding(e);
        verify(notifier, never()).notify(any(), anyString());
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location);
        el.onGliding(e);
        verify(notifier).notify(any(), eq("protection.protected"));
        assertTrue(e.isCancelled());
    }
    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingNotGliding() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        when(mockPlayer.isGliding()).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location);
        el.onGliding(e);
        verify(notifier, never()).notify(any(), anyString());
        assertFalse(e.isCancelled());
    }

}
