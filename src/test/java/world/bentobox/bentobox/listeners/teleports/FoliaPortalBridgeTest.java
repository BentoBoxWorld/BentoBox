package world.bentobox.bentobox.listeners.teleports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.util.Util;

/**
 * Tests the decisions {@link FoliaPortalBridge} makes about Luminol's pre-portal event.
 */
class FoliaPortalBridgeTest extends CommonTestSetup {

    /**
     * Stand-in for Luminol's PreEntityPortalEvent. The bridge reads the event reflectively,
     * so any class with the same three getters and Cancellable can be used here.
     */
    public static class FakePortalEvent extends Event implements Cancellable {
        private final Entity entity;
        private final Location portalPos;
        private final World destination;
        private boolean cancelled;

        FakePortalEvent(Entity entity, Location portalPos, World destination) {
            this.entity = entity;
            this.portalPos = portalPos;
            this.destination = destination;
        }

        public Entity getEntity() {
            return entity;
        }

        public Location getPortalPos() {
            return portalPos;
        }

        public World getDestination() {
            return destination;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            this.cancelled = cancel;
        }

        @Override
        public HandlerList getHandlers() {
            return getHandlerList();
        }

        public static HandlerList getHandlerList() {
            return new HandlerList();
        }
    }

    private PlayerTeleportListener ptl;
    private Method getEntity;
    private Method getPortalPos;
    private Method getDestination;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        ptl = new PlayerTeleportListener(plugin);
        getEntity = FakePortalEvent.class.getMethod("getEntity");
        getPortalPos = FakePortalEvent.class.getMethod("getPortalPos");
        getDestination = FakePortalEvent.class.getMethod("getDestination");
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        when(iwm.isNetherIslands(world)).thenReturn(true);
    }

    private FakePortalEvent reroute(World destination) {
        FakePortalEvent event = new FakePortalEvent(mockPlayer, location, destination);
        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination);
        return event;
    }

    @Test
    void testFoliaApiIsReportedMissingWhenNotOnClasspath() {
        assertFalse(FoliaPortalBridge.isSupported());
    }

    @Test
    void testAddonNetherTransferIsCancelledAndRerouted() {
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);

        FakePortalEvent event = reroute(nether);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, portalEvent.getCause());
        assertEquals(location, portalEvent.getFrom());
    }

    @Test
    void testAddonEndTransferIsCancelledAndRerouted() {
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);

        FakePortalEvent event = reroute(end);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.END_PORTAL, portalEvent.getCause());
    }

    @Test
    void testLeavingTheAddonEndIsReroutedAsAnEndPortal() {
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);
        mockedUtil.when(() -> Util.getWorld(end)).thenReturn(world);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        when(iwm.isEndIslands(world)).thenReturn(true);
        Location endPortal = mock(Location.class);
        when(endPortal.getWorld()).thenReturn(end);
        // The server's destination for an end portal in the end is the normal overworld.
        World serverOverworld = mock(World.class);
        when(serverOverworld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        FakePortalEvent event = new FakePortalEvent(mockPlayer, endPortal, serverOverworld);

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination);

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.END_PORTAL, portalEvent.getCause());
    }

    @Test
    void testEnvironmentIsTakenFromPortalAndDestination() {
        World overworld = mock(World.class);
        when(overworld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        World nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        World end = mock(World.class);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);

        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(overworld, nether));
        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(nether, overworld));
        assertEquals(World.Environment.NETHER, FoliaPortalBridge.environmentOf(end, nether));
        assertEquals(World.Environment.THE_END, FoliaPortalBridge.environmentOf(overworld, end));
        assertEquals(World.Environment.THE_END, FoliaPortalBridge.environmentOf(end, overworld));
    }

    @Test
    void testStandardNetherIsReroutedThroughBentoBox() {
        // islands: false - the nether is a single shared world, but BentoBox still decides
        // which world that is, so the transfer must not be left to the server.
        when(iwm.isNetherIslands(world)).thenReturn(false);

        FakePortalEvent event = reroute(mock(World.class));

        assertTrue(event.isCancelled());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(pim).callEvent(captor.capture());
        PlayerPortalEvent portalEvent = assertInstanceOf(PlayerPortalEvent.class, captor.getValue());
        assertEquals(TeleportCause.NETHER_PORTAL, portalEvent.getCause());
    }

    @Test
    void testDisabledDimensionIsCancelledWithoutTeleport() {
        when(iwm.isNetherGenerate(world)).thenReturn(false);

        FakePortalEvent event = reroute(mock(World.class));

        assertTrue(event.isCancelled());
        verifyNoInteractions(pim);
    }

    @Test
    void testNonBentoBoxWorldIsLeftToTheServer() {
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(null);

        FakePortalEvent event = reroute(mock(World.class));

        assertFalse(event.isCancelled());
        verifyNoInteractions(pim);
    }

    @Test
    void testNonPlayerEntityIsIgnored() {
        Entity sheep = mock(Entity.class);
        FakePortalEvent event = new FakePortalEvent(sheep, location, mock(World.class));

        FoliaPortalBridge.reroute(plugin, ptl, event, getEntity, getPortalPos, getDestination);

        assertFalse(event.isCancelled());
        verifyNoInteractions(pim);
    }
}
