package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Tests for block explosion methods in {@link ExplosionListener}.
 * TNT-related methods are covered by {@link TNTListenerTest}.
 */
class ExplosionListenerTest extends CommonTestSetup {

    private ExplosionListener listener;
    @Mock
    private Block block;
    @Mock
    private BlockState blockState;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);

        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);

        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), anyString())).thenCallRealMethod();

        listener = new ExplosionListener();
        listener.setPlugin(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // --- BlockExplodeEvent tests ---

    @Test
    void testOnBlockExplosionProtected() {
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(false);
        List<Block> list = new ArrayList<>();
        list.add(block);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionAllowed() {
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(true);
        List<Block> list = new ArrayList<>();
        list.add(block);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    void testOnBlockExplosionNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        List<Block> list = new ArrayList<>();
        list.add(block);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    void testOnBlockExplosionOutsideIslandProtected() {
        Flags.WORLD_BLOCK_EXPLODE_DAMAGE.setDefaultSetting(false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<Block> list = new ArrayList<>();
        list.add(block);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionOutsideIslandAllowed() {
        Flags.WORLD_BLOCK_EXPLODE_DAMAGE.setDefaultSetting(true);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        List<Block> list = new ArrayList<>();
        list.add(block);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
        assertFalse(list.isEmpty());
    }

    @Test
    void testOnBlockExplosionPartialProtection() {
        // Main location is allowed, but one block in the list is on a protected island
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(true).thenReturn(false).thenReturn(true);
        Block protectedBlock = mock(Block.class);
        when(protectedBlock.getLocation()).thenReturn(location);
        List<Block> list = new ArrayList<>();
        list.add(block);
        list.add(protectedBlock);
        BlockExplodeEvent e = new BlockExplodeEvent(block, blockState, list, 1.0f, ExplosionResult.DESTROY);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    // --- EntityDamageEvent (block explosion) tests ---

    @Test
    void testOnBlockExplosionDamageEntityProtected() {
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(false);
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.BLOCK_EXPLOSION, 10D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionDamageEntityAllowed() {
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(true);
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.BLOCK_EXPLOSION, 10D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionDamageEntityNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.BLOCK_EXPLOSION, 10D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionDamageEntityNotBlockExplosion() {
        when(island.isAllowed(Flags.BLOCK_EXPLODE_DAMAGE)).thenReturn(false);
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.FIRE, 10D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionDamageEntityOutsideIslandProtected() {
        Flags.WORLD_BLOCK_EXPLODE_DAMAGE.setDefaultSetting(false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.BLOCK_EXPLOSION, 10D);
        listener.onExplosion(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testOnBlockExplosionDamageEntityOutsideIslandAllowed() {
        Flags.WORLD_BLOCK_EXPLODE_DAMAGE.setDefaultSetting(true);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        Entity damagee = mock(Entity.class);
        when(damagee.getLocation()).thenReturn(location);
        when(damagee.getWorld()).thenReturn(world);
        EntityDamageEvent e = new EntityDamageEvent(damagee, DamageCause.BLOCK_EXPLOSION, 10D);
        listener.onExplosion(e);
        assertFalse(e.isCancelled());
    }
}
