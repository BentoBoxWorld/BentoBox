package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class MobTeleportListenerTest extends CommonTestSetup {

    private MobTeleportListener mtl;

    @Mock
    private Entity enderman;
    @Mock
    private Entity shulker;
    @Mock
    private Entity other;
    @Mock
    private Location from;
    @Mock
    private Location to;
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        // Island Manager
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.isAllowed(Flags.ENDERMAN_TELEPORT)).thenReturn(true);
        when(island.isAllowed(Flags.SHULKER_TELEPORT)).thenReturn(true);

        when(to.getWorld()).thenReturn(world);
        when(from.getWorld()).thenReturn(world);
        when(enderman.getType()).thenReturn(EntityType.ENDERMAN);
        when(shulker.getType()).thenReturn(EntityType.SHULKER);
        when(other.getType()).thenReturn(EntityType.AXOLOTL);
        mtl = new MobTeleportListener();

    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventEndermanNotAllowed() {
        Flags.ENDERMAN_TELEPORT.setSetting(world, false);
        when(island.isAllowed(Flags.ENDERMAN_TELEPORT)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(enderman, from, to);
        mtl.onEntityTeleportEvent(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventEndermanNotAllowedWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        Flags.ENDERMAN_TELEPORT.setSetting(world, false);
        when(island.isAllowed(Flags.ENDERMAN_TELEPORT)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(enderman, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventEndermanNotAllowedNotOnIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        Flags.ENDERMAN_TELEPORT.setSetting(world, false);
        EntityTeleportEvent e = new EntityTeleportEvent(enderman, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventEndermanAllowedDefault() {
        EntityTeleportEvent e = new EntityTeleportEvent(enderman, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventOther() {
        EntityTeleportEvent e = new EntityTeleportEvent(other, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventEndermanNotAllowedButOther() {
        Flags.ENDERMAN_TELEPORT.setSetting(world, false);
        Flags.SHULKER_TELEPORT.setSetting(world, false);
        when(island.isAllowed(Flags.ENDERMAN_TELEPORT)).thenReturn(false);
        when(island.isAllowed(Flags.SHULKER_TELEPORT)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(other, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventShulkerNotAllowed() {
        Flags.SHULKER_TELEPORT.setSetting(world, false);
        when(island.isAllowed(Flags.SHULKER_TELEPORT)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(shulker, from, to);
        mtl.onEntityTeleportEvent(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventShulkerNotAllowedWrongWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        Flags.SHULKER_TELEPORT.setSetting(world, false);
        when(island.isAllowed(Flags.SHULKER_TELEPORT)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(shulker, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventShulkerNotAllowedNotOnIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        Flags.SHULKER_TELEPORT.setSetting(world, false);
        EntityTeleportEvent e = new EntityTeleportEvent(shulker, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener#onEntityTeleportEvent(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnEntityTeleportEventShulkerAllowedDefault() {
        EntityTeleportEvent e = new EntityTeleportEvent(shulker, from, to);
        mtl.onEntityTeleportEvent(e);
        assertFalse(e.isCancelled());
    }

}
