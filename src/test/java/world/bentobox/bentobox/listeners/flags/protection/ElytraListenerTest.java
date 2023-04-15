package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class ElytraListenerTest extends AbstractCommonSetup {

    private ElytraListener el;

    /**
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Player
        when(player.isGliding()).thenReturn(true);

        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Class under test
        el = new ElytraListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGlide(org.bukkit.event.entity.EntityToggleGlideEvent)}.
     */
    @Test
    public void testOnGlideAllowed() {
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(player, false);
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
        EntityToggleGlideEvent e = new EntityToggleGlideEvent(player, false);
        el.onGlide(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.protection.ElytraListener#onGliding(org.bukkit.event.player.PlayerTeleportEvent)}.
     */
    @Test
    public void testGlidingAllowed() {
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
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
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
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
        when(player.isGliding()).thenReturn(false);
        PlayerTeleportEvent e = new PlayerTeleportEvent(player, location, location);
        el.onGliding(e);
        verify(notifier, never()).notify(any(), anyString());
        assertFalse(e.isCancelled());
    }

}
