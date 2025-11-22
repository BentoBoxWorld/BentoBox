package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Witch;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
public class ThrowingListenerTest extends CommonTestSetup {

    private ThrowingListener tl;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        // Thrown listener
        tl = new ThrowingListener();
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link ThrowingListener#onPlayerThrowPotion(org.bukkit.event.entity.ProjectileLaunchEvent)}.
     */
    @Test
    public void testOnPlayerThrowPotion() {
        ThrownPotion entity = mock(ThrownPotion.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getShooter()).thenReturn(mockPlayer);
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
        when(entity.getShooter()).thenReturn(mockPlayer);
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
