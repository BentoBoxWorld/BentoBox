package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
public class IslandCacheTest {

    BentoBox plugin;
    private static World world;

    Island island;
    UUID owner = UUID.randomUUID();
    Location location;


    @Before
    public void setUp() throws Exception {
        plugin = mock(BentoBox.class);

        world = mock(World.class);

        // Worlds
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any())).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // Mock up IslandsManager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);



        island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(island.getCenter()).thenReturn(location);
        when(island.getOwner()).thenReturn(owner);
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(island.getMemberSet()).thenReturn(members.build());
        when(island.getMinX()).thenReturn(-200);
        when(island.getMinZ()).thenReturn(-200);

    }

    @Test
    public void testIslandCache() {
        assertNotNull(new IslandCache());
    }

    @Test
    public void testAddIsland() {
        IslandCache ic = new IslandCache();
        assertTrue(ic.addIsland(island));
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
        assertEquals(island, ic.get(location));
    }

    @Test
    public void testAddPlayer() {
        IslandCache ic = new IslandCache();
        UUID playerUUID = UUID.randomUUID();
        ic.addPlayer(playerUUID, island);
        // Check if they are added
        assertEquals(island, ic.get(world, playerUUID));
        assertNotSame(island, ic.get(world, UUID.randomUUID()));

    }

    @Test
    public void testClear() {
        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
        assertEquals(island, ic.get(location));
        ic.clear();
        assertNull(ic.get(world, owner));
        assertNull(ic.get(location));
    }

    @Test
    public void testDeleteIslandFromCache() {

        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
        assertEquals(island, ic.get(location));
        boolean result = ic.deleteIslandFromCache(island);
        assertTrue(result);
        assertNull(ic.get(world, owner));
        assertNull(ic.get(location));

        // Test removing an island that is not in the cache
        World world = mock(World.class);
        Island island2 = mock(Island.class);
        Location location2 = mock(Location.class);
        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(0);
        when(location2.getBlockY()).thenReturn(0);
        when(location2.getBlockZ()).thenReturn(0);
        when(island2.getCenter()).thenReturn(location2);
        when(island2.getOwner()).thenReturn(UUID.randomUUID());
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(island2.getMemberSet()).thenReturn(members.build());
        when(island2.getMinX()).thenReturn(-400);
        when(island2.getMinZ()).thenReturn(-400);

        assertFalse(ic.deleteIslandFromCache(island2));

    }

    @Test
    public void testGetLocation() {
        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(location));

    }

    @Test
    public void testGetUUID() {
        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
    }

    @Test
    public void testGetIslandAtLocation() {
        // Set coords to be in island space
        when(island.inIslandSpace(Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(true);
        // Set plugin
        Util.setPlugin(plugin);
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);

        // Check exact match for location
        assertEquals(island, ic.getIslandAt(island.getCenter()));


        Location location2 = mock(Location.class);
        when(location2.getWorld()).thenReturn(world);
        when(location2.getBlockX()).thenReturn(10);
        when(location2.getBlockY()).thenReturn(10);
        when(location2.getBlockZ()).thenReturn(10);


        assertEquals(island, ic.getIslandAt(location2));
        when(island.inIslandSpace(Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(false);
        assertNull(ic.getIslandAt(location2));
    }

    /*
    @Test
    public void testGetIslands() {
        fail("Not yet implemented"); // TODO
    }
     */
    @Test
    public void testgetMembers() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);

        assertTrue(ic.getMembers(world, null).isEmpty());
        assertTrue(ic.getMembers(world, UUID.randomUUID()).isEmpty());
        assertFalse(ic.getMembers(world, island.getOwner()).isEmpty());
        assertEquals(3, ic.getMembers(world, island.getOwner()).size());

    }
    @Test
    public void testGetTeamLeader() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);

        assertEquals(owner, ic.getTeamLeader(world, owner));
        assertNull(ic.getTeamLeader(world, null));
        assertNull(ic.getTeamLeader(world, UUID.randomUUID()));


    }

    @Test
    public void testHasIsland() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);

        assertTrue(ic.hasIsland(world, owner));
        assertFalse(ic.hasIsland(world, UUID.randomUUID()));
        assertFalse(ic.hasIsland(world, null));
    }

    @Test
    public void testRemovePlayer() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);

        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, null);
        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, UUID.randomUUID());
        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, owner);
        assertFalse(ic.hasIsland(world, owner));
    }

    @Test
    public void testSize() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        assertEquals(1, ic.size());
    }

    @Test
    public void testSetOwner() {
        // New cache
        IslandCache ic = new IslandCache();
        ic.addIsland(island);
        UUID newOwnerUUID = UUID.randomUUID();
        ic.setOwner(island, newOwnerUUID);

        Mockito.verify(island).setOwner(newOwnerUUID);
        assertEquals(island, ic.get(world, newOwnerUUID));
        assertEquals(island, ic.get(island.getCenter()));
    }
}
