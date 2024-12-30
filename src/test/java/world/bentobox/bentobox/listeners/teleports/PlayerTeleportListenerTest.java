package world.bentobox.bentobox.listeners.teleports;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
@Ignore("Needs update to work with PaperAPI")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Util.class, Bukkit.class, ServerBuildInfo.class })
public class PlayerTeleportListenerTest extends AbstractCommonSetup {

    private PlayerTeleportListener ptl;
    @Mock
    private Block block;
    @Mock
    private BukkitScheduler scheduler;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Bukkit
        when(Bukkit.getAllowNether()).thenReturn(true);
        when(Bukkit.getAllowEnd()).thenReturn(true);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.getSpawnLocation()).thenReturn(location);
        // Location
        Vector vector = mock(Vector.class);
        when(vector.toLocation(world)).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        // IWM
        when(iwm.getNetherWorld(world)).thenReturn(world);
        when(iwm.getEndWorld(world)).thenReturn(world);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);

        // Util
        when(Util.getWorld(world)).thenReturn(world);

        // IM
        when(plugin.getIslandsManager()).thenReturn(im);

        // Block
        when(location.getBlock()).thenReturn(block);

        ptl = new PlayerTeleportListener(plugin);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#PlayerTeleportListener(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testPlayerTeleportListener() {
        assertNotNull(ptl);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnPlayerPortalEventNether() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnPlayerPortalEventEnd() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL, 0, false,
                0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortalEvent(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnPlayerPortalEventUnknown() {
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.UNKNOWN, 0, false, 0);
        ptl.onPlayerPortalEvent(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessNotBentoboxWorld() {
        when(Util.getWorld(location.getWorld())).thenReturn(null);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that no further processing occurs
        assertFalse(e.isCancelled());
        verifyNoMoreInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessWorldDisabledInConfig() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the event was cancelled
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessWorldDisabledOnServer() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the event was cancelled
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessAlreadyInTeleport() {
        ptl.getInTeleport().add(uuid);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify no further processing occurs
        assertFalse(e.isCancelled());
    }

    @Test
    public void testPortalProcessStandardNetherOrEnd() {
        // Mocking required dependencies
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, World.Environment.NETHER)).thenReturn(false);

        // Creating the event
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);

        // Running the method
        ptl.onPlayerPortalEvent(e);

        // Validating that the event destination is unchanged (indicating standard processing occurred)
        assertFalse(e.isCancelled());
        assertNotNull(e.getTo());
        assertEquals(location.getWorld(), e.getTo().getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessIslandTeleport() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(ptl.isAllowedInConfig(world, World.Environment.NETHER)).thenReturn(true);
        when(ptl.isAllowedOnServer(World.Environment.NETHER)).thenReturn(true);
        when(ptl.isIslandWorld(world, World.Environment.NETHER)).thenReturn(true);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.NETHER_PORTAL, 0,
                false, 0);
        ptl.onPlayerPortalEvent(e);
        // Verify that the portal creation settings were adjusted
        assertEquals(2, e.getCreationRadius());
        assertNotNull(e.getTo());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#portalProcess(org.bukkit.event.player.PlayerPortalEvent, org.bukkit.World.Environment)}.
     */
    @Test
    public void testPortalProcessEndVelocityReset() {
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        PlayerPortalEvent e = new PlayerPortalEvent(mockPlayer, location, location, TeleportCause.END_PORTAL, 0, false,
                0);
        ptl.onPlayerPortalEvent(e);
        // Verify player velocity and fall distance were reset
        verify(mockPlayer, times(1)).setVelocity(new Vector(0, 0, 0));
        verify(mockPlayer, times(1)).setFallDistance(0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    public void testOnPlayerPortalNonPlayerEntity() {
        // Mock a non-player entity
        Entity mockEntity = mock(Entity.class);
        when(mockEntity.getType()).thenReturn(EntityType.ZOMBIE);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockEntity, location);
        ptl.onPlayerPortal(e);

        // Verify no further processing for non-player entities
        verifyNoInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    public void testOnPlayerPortalAlreadyInPortal() {
        // Simulate player already in portal
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);
        ptl.onPlayerPortal(e);

        // Verify no further processing occurs
        verifyNoInteractions(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerPortal(org.bukkit.event.entity.EntityPortalEnterEvent)}.
     */
    @Test
    public void testOnPlayerPortalNetherPortalDisabled() {
        // Mock configuration for Nether disabled
        when(Bukkit.getAllowNether()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.NETHER_PORTAL);

        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);
        ptl.onPlayerPortal(e);

        // Verify PlayerPortalEvent is scheduled
        verify(Bukkit.getScheduler(), times(1)).runTaskLater(eq(plugin), any(Runnable.class), eq(40L));
    }

    @Test
    public void testOnPlayerPortalEndPortalDisabled() {
        // Mock configuration for End disabled
        when(Bukkit.getAllowEnd()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.END_PORTAL);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Check if the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify the event behavior indirectly by confirming the origin world was stored
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));
    }

    @Test
    public void testOnPlayerPortalEndGatewayDisabled() {
        // Mock configuration for End disabled
        when(Bukkit.getAllowEnd()).thenReturn(false);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.END_GATEWAY);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Check if the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify the event behavior indirectly by confirming the origin world was stored
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));
    }

    @Test
    public void testOnPlayerPortalValidBentoBoxWorld() {
        // Mock configuration for a valid BentoBox world
        when(Bukkit.getAllowNether()).thenReturn(true);
        when(Bukkit.getAllowEnd()).thenReturn(true);
        when(Util.getWorld(location.getWorld())).thenReturn(world);
        when(plugin.getIWM().inWorld(world)).thenReturn(true);
        when(block.getType()).thenReturn(Material.NETHER_PORTAL);

        // Create the event
        EntityPortalEnterEvent e = new EntityPortalEnterEvent(mockPlayer, location);

        // Execute the method
        ptl.onPlayerPortal(e);

        // Verify the player was added to inPortal
        assertTrue(ptl.getInPortal().contains(mockPlayer.getUniqueId()));

        // Verify teleportOrigin was updated with the correct world
        assertEquals(location.getWorld(), ptl.getTeleportOrigin().get(mockPlayer.getUniqueId()));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onExitPortal(org.bukkit.event.player.PlayerMoveEvent)}.
     */
    @Test
    public void testOnExitPortalPlayerNotInPortal() {
        // Mock a player who is not in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, location);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that no changes occurred to inPortal or other collections
        assertFalse(ptl.getInPortal().contains(playerId));
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    @Test
    public void testOnExitPortalPlayerStillInPortal() {
        // Mock a player in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);

        // Mock the destination block type as a Nether portal
        when(location.getBlock().getType()).thenReturn(Material.NETHER_PORTAL);

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, location);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that the player is still in the inPortal list
        assertTrue(ptl.getInPortal().contains(playerId));

        // Verify that no changes occurred to inTeleport or teleportOrigin
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    @Test
    public void testOnExitPortalPlayerExitsPortal() {
        // Mock a player in the inPortal list
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getInPortal().add(playerId);
        ptl.getInTeleport().add(playerId);
        ptl.getTeleportOrigin().put(playerId, location.getWorld());

        // Mock the destination block type as something other than a Nether portal
        Location toLocation = mock(Location.class);
        when(toLocation.getBlock()).thenReturn(block);
        when(toLocation.getBlock().getType()).thenReturn(Material.AIR);

        // Create the event
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, location, toLocation);

        // Execute the method
        ptl.onExitPortal(e);

        // Verify that the player was removed from inPortal, inTeleport, and teleportOrigin
        assertFalse(ptl.getInPortal().contains(playerId));
        assertFalse(ptl.getInTeleport().contains(playerId));
        assertNull(ptl.getTeleportOrigin().get(playerId));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.teleports.PlayerTeleportListener#onPlayerExitPortal(org.bukkit.event.player.PlayerRespawnEvent)}.
     */
    @Test
    public void testOnPlayerExitPortalPlayerAlreadyProcessed() {
        // Mock a player who is not in teleportOrigin
        UUID playerId = mockPlayer.getUniqueId();

        // Create the event
        @SuppressWarnings("deprecation")
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that no changes occurred to the event
        assertEquals(location, event.getRespawnLocation());
    }

    @Test
    public void testOnPlayerExitPortalNotBentoBoxWorld() {
        // Mock teleportOrigin with a world not in BentoBox
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);

        // Mock the world not being a BentoBox world
        when(Util.getWorld(world)).thenReturn(null);

        // Create the event
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that no changes occurred to the event
        assertEquals(location, event.getRespawnLocation());
    }

    @Test
    public void testOnPlayerExitPortalIslandExistsRespawnInsideProtection() {
        // Set up teleportOrigin with a valid world
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);

        // Create the event
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that the respawn location remains unchanged
        assertEquals(location, event.getRespawnLocation());
    }

    @Test
    public void testOnPlayerExitPortalIslandExistsRespawnOutsideProtection() {
        // Set up teleportOrigin with a valid world
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);

        // Create the event
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that the respawn location was updated to the island spawn point
        assertEquals(location, event.getRespawnLocation());
    }

    @Test
    public void testOnPlayerExitPortalIslandExistsNoSpawnPoint() {
        // Set up teleportOrigin with a valid world
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);


        // Create the event
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that the respawn location was updated to the island's protection center
        assertEquals(location, event.getRespawnLocation());
    }

    @Test
    public void testOnPlayerExitPortalNoIsland() {
        // Set up teleportOrigin with a valid world
        UUID playerId = mockPlayer.getUniqueId();
        ptl.getTeleportOrigin().put(playerId, world);

        // Create the event
        PlayerRespawnEvent event = new PlayerRespawnEvent(mockPlayer, location, false);

        // Execute the method
        ptl.onPlayerExitPortal(event);

        // Verify that the respawn location was updated to the world spawn location
        assertEquals(location, event.getRespawnLocation());
    }


}
