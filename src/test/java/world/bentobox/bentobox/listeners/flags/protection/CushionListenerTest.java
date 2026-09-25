package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Cushion;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import io.papermc.paper.event.entity.EntityBreakEvent.RemoveCause;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;

/**
 * Tests {@link CushionListener}.
 * <p>
 * The listener needs the Minecraft 26.3 API at runtime (the cushion entity and Paper's
 * {@code EntityBreakByEntityEvent}). Tests run against the API line MockBukkit supports
 * ({@code testPaperVersion} in the build), so the whole class is skipped until that is 26.3+.
 * Only local variables (never method signatures) name 26.3 types, so JUnit can still scan the
 * class on an older runtime and evaluate the condition.
 *
 * @author tastybento
 */
@EnabledIf("cushionApiPresent")
class CushionListenerTest extends CommonTestSetup {

    private CushionListener l;

    /**
     * Condition for {@link EnabledIf}: is the 26.3 cushion API on the test runtime classpath?
     */
    static boolean cushionApiPresent() {
        try {
            Class.forName("org.bukkit.entity.Cushion");
            Class.forName("io.papermc.paper.event.entity.EntityBreakByEntityEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Default is that everything is allowed
        when(island.isAllowed(any(), any())).thenReturn(true);
        l = new CushionListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private Entity cushion() {
        // Local variable only: a 26.3 type in a method signature would break class scanning on 26.2
        Cushion cushion = mock(Cushion.class);
        when(cushion.getLocation()).thenReturn(location);
        when(cushion.getWorld()).thenReturn(world);
        return cushion;
    }

    private EntityPlaceEvent place(Entity entity) {
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        return new EntityPlaceEvent(entity, mockPlayer, block, BlockFace.UP, EquipmentSlot.HAND);
    }

    // --- placing ---

    @Test
    void testOnCushionPlaceAllowed() {
        EntityPlaceEvent e = place(cushion());
        l.onCushionPlace(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void testOnCushionPlaceNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.PLACE_BLOCKS))).thenReturn(false);
        EntityPlaceEvent e = place(cushion());
        l.onCushionPlace(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnCushionPlaceIgnoresOtherEntities() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getLocation()).thenReturn(location);
        EntityPlaceEvent e = place(frame);
        l.onCushionPlace(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnCushionPlaceNoPlayer() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Block block = mock(Block.class);
        EntityPlaceEvent e = new EntityPlaceEvent(cushion(), null, block, BlockFace.UP, EquipmentSlot.HAND);
        l.onCushionPlace(e);
        assertFalse(e.isCancelled());
    }

    // --- breaking ---

    @Test
    void testOnCushionBreakByPlayerAllowed() {
        EntityBreakByEntityEvent e = new EntityBreakByEntityEvent(cushion(), mockPlayer,
                mock(DamageSource.class), RemoveCause.ENTITY);
        l.onCushionBreak(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void testOnCushionBreakByPlayerNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(false);
        EntityBreakByEntityEvent e = new EntityBreakByEntityEvent(cushion(), mockPlayer,
                mock(DamageSource.class), RemoveCause.ENTITY);
        l.onCushionBreak(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnCushionBreakByPlayerProjectileNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.BREAK_BLOCKS))).thenReturn(false);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(mockPlayer);
        EntityBreakByEntityEvent e = new EntityBreakByEntityEvent(cushion(), arrow,
                mock(DamageSource.class), RemoveCause.ENTITY);
        l.onCushionBreak(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnCushionBreakByMobIgnored() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        EntityBreakByEntityEvent e = new EntityBreakByEntityEvent(cushion(), mock(Zombie.class),
                mock(DamageSource.class), RemoveCause.ENTITY);
        l.onCushionBreak(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void testOnCushionBreakIgnoresHangings() {
        // Item frames are handled by the hanging listener; do not double-report them here
        when(island.isAllowed(any(), any())).thenReturn(false);
        ItemFrame frame = mock(ItemFrame.class);
        when(frame.getLocation()).thenReturn(location);
        EntityBreakByEntityEvent e = new EntityBreakByEntityEvent(frame, mockPlayer,
                mock(DamageSource.class), RemoveCause.ENTITY);
        l.onCushionBreak(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    // --- sitting ---

    @Test
    void testOnCushionSitAllowed() {
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, cushion());
        l.onCushionSit(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void testOnCushionSitNotAllowed() {
        when(island.isAllowed(any(), eq(Flags.RIDING))).thenReturn(false);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, cushion());
        l.onCushionSit(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    @Test
    void testOnCushionSitIgnoresOtherEntities() {
        when(island.isAllowed(any(), any())).thenReturn(false);
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, zombie);
        l.onCushionSit(e);
        assertFalse(e.isCancelled());
    }
}
