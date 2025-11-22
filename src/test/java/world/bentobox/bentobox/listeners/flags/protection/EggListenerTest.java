package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
public class EggListenerTest extends CommonTestSetup {

    private EggListener el;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Listener
        el = new EggListener();
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EggListener#onEggThrow(org.bukkit.event.player.PlayerEggThrowEvent)}.
     */
    @Test
    public void testOnEggThrowAllowed() {
        Egg egg = mock(Egg.class);
        when(egg.getLocation()).thenReturn(location);
        PlayerEggThrowEvent e = new PlayerEggThrowEvent(mockPlayer, egg, false, (byte) 0, EntityType.CHICKEN);
        el.onEggThrow(e);
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EggListener#onEggThrow(org.bukkit.event.player.PlayerEggThrowEvent)}.
     */
    @Test
    public void testOnEggThrowNotAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Egg egg = mock(Egg.class);
        when(egg.getLocation()).thenReturn(location);
        PlayerEggThrowEvent e = new PlayerEggThrowEvent(mockPlayer, egg, false, (byte) 0, EntityType.CHICKEN);
        el.onEggThrow(e);
        assertFalse(e.isHatching());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

}
