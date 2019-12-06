package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Bukkit.class} )
public class WitherListenerTest {

    private WitherListener wl;
    @Mock
    private Location location;
    @Mock
    private Location location2;
    @Mock
    private World world;
    @Mock
    private World world2;
    @Mock
    private IslandWorldManager iwm;

    private List<Block> blocks;
    @Mock
    private @Nullable WorldSettings ws;
    private Map<String, Boolean> map;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(eq(world))).thenReturn(true);
        when(iwm.inWorld(eq(location))).thenReturn(true);
        map = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(map);
        when(iwm.getWorldSettings(any())).thenReturn(ws);

        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        when(location2.getWorld()).thenReturn(world2);
        when(location2.getBlockX()).thenReturn(0);
        when(location2.getBlockY()).thenReturn(0);
        when(location2.getBlockZ()).thenReturn(0);

        blocks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Block block = mock(Block.class);
            when(block.getLocation()).thenReturn(location);
            blocks.add(block);
        }



        wl = new WitherListener();

        // Set flag
        Flags.WITHER_DAMAGE.setSetting(world, false);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionWither() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, blocks, 0);
        wl.onExplosion(e);
        assertTrue(blocks.isEmpty());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionWitherWrongWorld() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location2);
        when(entity.getWorld()).thenReturn(world2);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location2, blocks, 0);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionWitherAllowed() {
        Flags.WITHER_DAMAGE.setSetting(world, true);
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, blocks, 0);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionWitherSkull() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER_SKULL);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, blocks, 0);
        wl.onExplosion(e);
        assertTrue(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotWither() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.DRAGON_FIREBALL);
        EntityExplodeEvent e = new EntityExplodeEvent(entity, location, blocks, 0);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#WitherChangeBlocks(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testWitherChangeBlocks() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);
        BlockData blockData = mock(BlockData.class);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(entity, block, blockData);
        wl.onWitherChangeBlocks(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#WitherChangeBlocks(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testWitherChangeBlocksWrongWorld() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location2);
        when(entity.getWorld()).thenReturn(world2);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location2);
        when(block.getWorld()).thenReturn(world2);
        BlockData blockData = mock(BlockData.class);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(entity, block, blockData);
        wl.onWitherChangeBlocks(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#WitherChangeBlocks(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    public void testWitherChangeBlocksAllowed() {
        Flags.WITHER_DAMAGE.setSetting(world, true);
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);
        BlockData blockData = mock(BlockData.class);
        EntityChangeBlockEvent e = new EntityChangeBlockEvent(entity, block, blockData);
        wl.onWitherChangeBlocks(e);
        assertFalse(e.isCancelled());
    }

}
