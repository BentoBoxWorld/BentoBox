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

import org.bukkit.entity.Sheep;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

class ShearingListenerTest extends CommonTestSetup {

    private ShearingListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);

        listener = new ShearingListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnShearAllowed() {
        Sheep sheep = mock(Sheep.class);
        when(sheep.getLocation()).thenReturn(location);
        PlayerShearEntityEvent e = new PlayerShearEntityEvent(mockPlayer, sheep);
        listener.onShear(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), anyString());
    }

    @Test
    void testOnShearNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Sheep sheep = mock(Sheep.class);
        when(sheep.getLocation()).thenReturn(location);
        PlayerShearEntityEvent e = new PlayerShearEntityEvent(mockPlayer, sheep);
        listener.onShear(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }
}
