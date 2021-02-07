package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class EggListenerTest extends AbstractCommonSetup {

    private EggListener el;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Listener
        el = new EggListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.EggListener#onEggThrow(org.bukkit.event.player.PlayerEggThrowEvent)}.
     */
    @Test
    public void testOnEggThrowAllowed() {
        Egg egg = mock(Egg.class);
        when(egg.getLocation()).thenReturn(location);
        PlayerEggThrowEvent e = new PlayerEggThrowEvent(player, egg, false, (byte) 0, EntityType.CHICKEN);
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
        PlayerEggThrowEvent e = new PlayerEggThrowEvent(player, egg, false, (byte) 0, EntityType.CHICKEN);
        el.onEggThrow(e);
        assertFalse(e.isHatching());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

}
