package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

class PortalListenerTest extends CommonTestSetup {

    private PortalListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);

        listener = new PortalListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnNetherPortalAllowed() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL);
        listener.onPlayerPortal(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnNetherPortalNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL);
        listener.onPlayerPortal(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnEndPortalAllowed() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL);
        listener.onPlayerPortal(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnEndPortalNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL);
        listener.onPlayerPortal(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnOtherPortalCauseNotAffected() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.COMMAND);
        listener.onPlayerPortal(e);
        assertFalse(e.isCancelled());
    }
}
