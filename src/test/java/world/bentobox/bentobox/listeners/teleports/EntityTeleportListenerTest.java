package world.bentobox.bentobox.listeners.teleports;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.entity.EntityPortalEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Util.class, Bukkit.class })
public class EntityTeleportListenerTest extends AbstractCommonSetup {

	private EntityTeleportListener etl;
	@Mock
	private IslandsManager im;

	/**
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		when(plugin.getIslands()).thenReturn(im);
		when(plugin.getIslandsManager()).thenReturn(im);

		when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));

		etl = new EntityTeleportListener(plugin);
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
		PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
		when(Util.getWorld(any())).thenReturn(null);
		EntityPortalEvent event = new EntityPortalEvent(player, location, location, 10);
		etl.onEntityPortal(event);
		assertFalse(event.isCancelled());
	}

	/**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortalWrongWorld2() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        EntityPortalEvent event = new EntityPortalEvent(player, location, location, 10);
        etl.onEntityPortal(event);
        assertFalse(event.isCancelled());
    }

	/**
	 * Test method for
	 * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
	 */
	@Test
	public void testOnEntityPortalNullTo() {
		EntityPortalEvent event = new EntityPortalEvent(player, location, null, 10);
		etl.onEntityPortal(event);
		assertFalse(event.isCancelled());
	}

	/**
	 * Test method for
	 * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
	 */
	@Test
	public void testOnEntityPortalTeleportDisabled() {
		EntityPortalEvent event = new EntityPortalEvent(player, location, location, 10);
		etl.onEntityPortal(event);
		assertTrue(event.isCancelled());
	}

	/**
	 * Test method for
	 * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
	 */
	@Test
	public void testOnEntityPortalTeleportEnabled() {
		PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
		when(Util.getWorld(any())).thenReturn(world);
		when(world.getEnvironment()).thenReturn(Environment.NORMAL);

		Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
		EntityPortalEvent event = new EntityPortalEvent(player, location, location, 10);
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
        
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getWorld(any())).thenReturn(world2);

        when(location.getWorld()).thenReturn(world);
        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world, true);
        EntityPortalEvent event = new EntityPortalEvent(player, location, location2, 10);
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
        
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(player, location, location2, 10);
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
        
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getWorld(any())).thenReturn(world2);

        Flags.ENTITY_PORTAL_TELEPORT.setSetting(world2, true);
        EntityPortalEvent event = new EntityPortalEvent(player, location, location2, 10);
        etl.onEntityPortal(event);
        assertTrue(event.isCancelled());
        
    }

	/**
	 * Test method for
	 * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityEnterPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
	 */
	@Test
	public void testOnEntityEnterPortal() {
	}

	/**
	 * Test method for
	 * {@link world.bentobox.bentobox.listeners.teleports.EntityTeleportListener#onEntityExitPortal(org.bukkit.event.entity.EntityPortalExitEvent)}.
	 */
	@Test
	public void testOnEntityExitPortal() {
	}

}
