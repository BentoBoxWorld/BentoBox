package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class })
public class MobTeleportListenerTest {

    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
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
    @Mock
    private World world;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        PowerMockito.mockStatic(BentoBox.class, Mockito.RETURNS_MOCKS);
        when(BentoBox.getInstance()).thenReturn(plugin);
        // Island World Manager
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
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
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
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
