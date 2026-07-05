package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.FishHook;
import org.bukkit.event.player.PlayerFishEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 */
class FishingListenerTest extends CommonTestSetup {

    private FishingListener fl;
    private FishHook hook;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        hook = mock(FishHook.class);
        when(hook.getLocation()).thenReturn(location);

        // Listener
        fl = new FishingListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.FishingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnCastAllowed() {
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, null, hook, PlayerFishEvent.State.FISHING);
        fl.onFishing(e);
        assertFalse(e.isCancelled());
        verify(hook, never()).remove();
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.FishingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnCastNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, null, hook, PlayerFishEvent.State.FISHING);
        fl.onFishing(e);
        assertTrue(e.isCancelled());
        verify(hook).remove();
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.FishingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnCaughtFishAllowed() {
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, null, hook, PlayerFishEvent.State.CAUGHT_FISH);
        fl.onFishing(e);
        assertFalse(e.isCancelled());
        verify(hook, never()).remove();
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.FishingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    void testOnCaughtFishNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, null, hook, PlayerFishEvent.State.CAUGHT_FISH);
        fl.onFishing(e);
        assertTrue(e.isCancelled());
        verify(hook).remove();
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Reeling in with no catch should never be blocked, even when fishing is not allowed.
     */
    @Test
    void testOnReelInNotAllowedIgnored() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, null, hook, PlayerFishEvent.State.REEL_IN);
        fl.onFishing(e);
        assertFalse(e.isCancelled());
        verify(hook, never()).remove();
        verify(notifier, never()).notify(any(), anyString());
    }
}
