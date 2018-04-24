package us.tastybento.bskyblock.managers.island;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.util.Util;

@RunWith(PowerMockRunner.class)
public class IslandCacheTest {

    BSkyBlock plugin;
    private static World world;

    Island island;
    UUID owner = UUID.randomUUID();
    Location location;


    @Before
    public void setUp() throws Exception {
        plugin = mock(BSkyBlock.class);

        world = mock(World.class);

        // Worlds
        IslandWorld iwm = mock(IslandWorld.class);
        when(plugin.getIslandWorldManager()).thenReturn(iwm);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getNetherWorld()).thenReturn(world);
        when(iwm.getEndWorld()).thenReturn(world);

        // Mock up IslandsManager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);



        island = mock(Island.class);
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
        assertNotNull(new IslandCache(plugin));
    }

    @Test
    public void testAddIsland() {
        IslandCache ic = new IslandCache(plugin);
        assertTrue(ic.addIsland(island));
        // Check if they are added
        assertEquals(island, ic.get(owner)); 
        assertEquals(island, ic.get(location));
    }

    @Test
    public void testAddPlayer() {
        IslandCache ic = new IslandCache(plugin);
        UUID playerUUID = UUID.randomUUID();
        ic.addPlayer(playerUUID, island);
        // Check if they are added
        assertEquals(island, ic.get(playerUUID));
        assertNotSame(island, ic.get(UUID.randomUUID()));

    }

    @Test
    public void testClear() {
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(owner)); 
        assertEquals(island, ic.get(location));
        ic.clear();
        assertNull(ic.get(owner)); 
        assertNull(ic.get(location));
    }

    @Test
    public void testDeleteIslandFromCache() {

        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(owner)); 
        assertEquals(island, ic.get(location));
        boolean result = ic.deleteIslandFromCache(island);
        assertTrue(result);
        assertNull(ic.get(owner)); 
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
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(location));

    }

    @Test
    public void testGetUUID() {
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(owner)); 
    }

    @Test
    public void testGetIslandAtIntInt() {
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        when(island.inIslandSpace(Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(true);
        // Check if they are added
        assertEquals(island, ic.getIslandAt(0,0));
        assertNull(ic.getIslandAt(-2000,-2000));
    }

    @Test
    public void testGetIslandAtLocation() {        
        // Set coords to be in island space
        when(island.inIslandSpace(Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(true);
        // Set plugin
        Util.setPlugin(plugin);
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Check islands is in world
        assertTrue(Util.inWorld(island.getCenter()));
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

    @Test
    public void testGetIslandName() {
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        // Test with empty name
        when(island.getName()).thenReturn("");
        // Random player
        when(pm.getName(Mockito.any())).thenReturn("tastybento");
        assertEquals("tastybento" + ChatColor.RESET, ic.getIslandName(UUID.randomUUID()));
        // Island owner
        when(pm.getName(owner)).thenReturn("ownersname");
        assertEquals("ownersname" + ChatColor.RESET, ic.getIslandName(island.getOwner()));

        // Test with null name
        when(island.getName()).thenReturn(null);
        // Random player
        when(pm.getName(Mockito.any())).thenReturn("tastybento");
        assertEquals("tastybento" + ChatColor.RESET, ic.getIslandName(UUID.randomUUID()));
        // Island owner
        when(pm.getName(owner)).thenReturn("ownersname");
        assertEquals("ownersname" + ChatColor.RESET, ic.getIslandName(island.getOwner()));

        // Test with island name set
        when(island.getName()).thenReturn("islandnameset");
        // Random player
        when(pm.getName(Mockito.any())).thenReturn("tastybento");
        assertEquals("tastybento" + ChatColor.RESET, ic.getIslandName(UUID.randomUUID()));
        // Island owner
        when(pm.getName(owner)).thenReturn("ownersname");
        assertEquals("islandnameset" + ChatColor.RESET, ic.getIslandName(island.getOwner()));
    }
    /*
    @Test
    public void testGetIslands() {
        fail("Not yet implemented"); // TODO
    }
     */
    @Test
    public void testGetMembers() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);

        assertTrue(ic.getMembers(null).isEmpty());
        assertTrue(ic.getMembers(UUID.randomUUID()).isEmpty());
        assertFalse(ic.getMembers(island.getOwner()).isEmpty());
        assertEquals(3, ic.getMembers(island.getOwner()).size());

    }
    @Test
    public void testGetTeamLeader() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);

        assertEquals(owner, ic.getTeamLeader(owner));
        assertNull(ic.getTeamLeader(null));
        assertNull(ic.getTeamLeader(UUID.randomUUID()));


    }

    @Test
    public void testHasIsland() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);

        assertTrue(ic.hasIsland(owner));
        assertFalse(ic.hasIsland(UUID.randomUUID()));
        assertFalse(ic.hasIsland(null));
    }

    @Test
    public void testRemovePlayer() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);

        assertTrue(ic.hasIsland(owner));
        ic.removePlayer(null);
        assertTrue(ic.hasIsland(owner));
        ic.removePlayer(UUID.randomUUID());
        assertTrue(ic.hasIsland(owner));
        ic.removePlayer(owner);
        assertFalse(ic.hasIsland(owner));
    }

    @Test
    public void testSetIslandName() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        assertTrue(ic.setIslandName(owner, "test tes test"));
        assertFalse(ic.setIslandName(UUID.randomUUID(), "sadfas"));
        assertFalse(ic.setIslandName(null, "sadfas"));
    }

    @Test
    public void testSize() {
        // New cache
        IslandCache ic = new IslandCache(plugin);        
        ic.addIsland(island);
        assertEquals(1, ic.size());
    }

}
