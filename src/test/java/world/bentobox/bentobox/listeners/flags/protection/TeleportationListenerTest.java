package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

class TeleportationListenerTest extends CommonTestSetup {

    private TeleportationListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);

        listener = new TeleportationListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnEnderPearlAllowed() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location, TeleportCause.ENDER_PEARL);
        listener.onPlayerTeleport(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnEnderPearlNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location, TeleportCause.ENDER_PEARL);
        listener.onPlayerTeleport(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnChorusFruitAllowed() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location, TeleportCause.CHORUS_FRUIT);
        listener.onPlayerTeleport(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnChorusFruitNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location, TeleportCause.CHORUS_FRUIT);
        listener.onPlayerTeleport(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnOtherTeleportCauseNotAffected() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, location, location, TeleportCause.COMMAND);
        listener.onPlayerTeleport(e);
        assertFalse(e.isCancelled());
    }
}
