package world.bentobox.bentobox.listeners.teleports;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.entity.EntityPortalEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class EntityTeleportListenerTest extends CommonTestSetup {

    private EntityTeleportListener etl;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));

        etl = new EntityTeleportListener(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#EntityTeleportListener(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testEntityTeleportListener() {
        assertNotNull(etl);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalWrongWorld() {
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(null);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalWrongWorld2() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalNullTo() {
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, null, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalTeleportDisabled() {
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalTeleportEnabled() {
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalTeleportEnabledMissingWorld() {
        when(iwm.isNetherGenerate(any())).thenReturn(false);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);
        
        when(location.getWorld()).thenReturn(world);
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalTeleportEnabledIsNotAllowedInConfig() {
        when(iwm.isNetherGenerate(any())).thenReturn(false);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalTeleportEnabledIsAllowedInConfig() {
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(true);

        Location location2 = mock(Location.class);
        World world2 = mock(World.class);
        when(location2.getWorld()).thenReturn(world2);
        when(world2.getEnvironment()).thenReturn(Environment.NETHER);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(mockPlayer, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityEnterPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    public void testOnEntityEnterPortal() {
        // TODO
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityExitPortal(org.bukkit.event.entity.EntityPortalExitEvent)}.
     */
    @Test
    public void testOnEntityExitPortal() {
        // TODO
    }

}
