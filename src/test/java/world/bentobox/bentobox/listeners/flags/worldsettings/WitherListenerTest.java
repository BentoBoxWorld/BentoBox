package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
class WitherListenerTest extends CommonTestSetup {

    private WitherListener wl;
    @Mock
    private Location location2;
    @Mock
    private World world2;

    private List<Block> blocks;
    @Mock
    private @Nullable WorldSettings ws;
    private Map<String, Boolean> map;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.inWorld(world2)).thenReturn(false);
        when(iwm.inWorld(location)).thenReturn(true);
        when(iwm.inWorld(location2)).thenReturn(false);
        map = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(map);
        when(iwm.getWorldSettings(any())).thenReturn(ws);

        when(location2.getWorld()).thenReturn(world2);
        when(location2.getBlockX()).thenReturn(0);
        when(location2.getBlockY()).thenReturn(0);
        when(location2.getBlockZ()).thenReturn(0);
        when(location2.clone()).thenReturn(location2); // Paper

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

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    void testOnExplosionWither() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        when(location.clone()).thenReturn(location);
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        wl.onExplosion(e);
        assertTrue(blocks.isEmpty());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    void testOnExplosionWitherWrongWorld() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location2);
        when(entity.getWorld()).thenReturn(world2);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        EntityExplodeEvent e = getExplodeEvent(entity, location2, blocks);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    void testOnExplosionWitherAllowed() {
        Flags.WITHER_DAMAGE.setSetting(world, true);
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER);
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    void testOnExplosionWitherSkull() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.WITHER_SKULL);
        when(location.clone()).thenReturn(location);
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        wl.onExplosion(e);
        assertTrue(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    void testOnExplosionNotWither() {
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);
        when(entity.getType()).thenReturn(EntityType.DRAGON_FIREBALL);
        EntityExplodeEvent e = getExplodeEvent(entity, location, blocks);
        wl.onExplosion(e);
        assertFalse(blocks.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener#WitherChangeBlocks(org.bukkit.event.entity.EntityChangeBlockEvent)}.
     */
    @Test
    void testWitherChangeBlocks() {
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
    void testWitherChangeBlocksWrongWorld() {
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
    void testWitherChangeBlocksAllowed() {
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
