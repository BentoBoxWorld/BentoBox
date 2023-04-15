package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
public class ThrowingListenerTest extends AbstractCommonSetup {

    private ThrowingListener tl;

    /**
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        // Thrown listener
        tl = new ThrowingListener();
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotion() {
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(player);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNotAllowed() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(player);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertTrue(e.isCancelled());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNonHuman() {
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        Witch witch = mock(Witch.class);
        when(witch.getLocation()).thenReturn(location);
        when(witch.getUniqueId()).thenReturn(UUID.randomUUID());
        when(witch.getName()).thenReturn("witch");
        when(witch.getWorld()).thenReturn(world);
        when(entity.getShooter()).thenReturn(witch);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotionNotAllowedNonHuman() {
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        Witch witch = mock(Witch.class);
        when(witch.getLocation()).thenReturn(location);
        when(witch.getUniqueId()).thenReturn(UUID.randomUUID());
        when(witch.getName()).thenReturn("witch");
        when(witch.getWorld()).thenReturn(world);
        when(entity.getShooter()).thenReturn(witch);
        ProjectileLaunchEvent e = new ProjectileLaunchEvent(entity);
        tl.onPlayerThrowPotion(e);
        assertFalse(e.isCancelled());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

}
