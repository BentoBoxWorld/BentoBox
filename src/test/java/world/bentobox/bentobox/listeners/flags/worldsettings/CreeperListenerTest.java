package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, Flags.class, Util.class })
public class CreeperListenerTest extends AbstractCommonSetup {

    private CreeperListener cl;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        cl = new CreeperListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotCreeper() {
        List<Block> list = new ArrayList<>();
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.TNT);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotInWorld() {
        List<Block> list = new ArrayList<>();
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(false);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionCreeperInWorldDamageOK() {
        List<Block> list = new ArrayList<>();
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        Creeper entity = mock(Creeper.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
        assertFalse(event.blockList().isEmpty()); // No clearing of block list
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionCreeperInWorldDamageNOK() {
        Flags.CREEPER_DAMAGE.setSetting(world, false);
        List<Block> list = new ArrayList<>();
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        Creeper entity = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
        assertTrue(event.blockList().isEmpty()); // No clearing of block list
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntity() {
        //TODO
    }

}
