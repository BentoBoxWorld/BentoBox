package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class})
public class IslandCacheTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private Island island;
    // UUID
    private UUID owner = UUID.randomUUID();
    @Mock
    private Location location;
    // Test class
    private IslandCache ic;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Flag flag;
    @Mock
    private IslandsManager im;

    @Before
    public void setUp() throws Exception {
        // Plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        // IWM
        when(iwm.getDefaultIslandFlags(any())).thenReturn(Collections.singletonMap(flag, 400));
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // Mock up IslandsManager
        when(plugin.getIslands()).thenReturn(im);

        // Island
        when(island.getWorld()).thenReturn(world);
        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(island.getCenter()).thenReturn(location);
        when(island.getOwner()).thenReturn(owner);
        when(island.isOwned()).thenReturn(true);
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(island.getMemberSet(Mockito.anyInt())).thenReturn(members.build());
        when(island.getMemberSet()).thenReturn(members.build());
        when(island.getMinX()).thenReturn(-200);
        when(island.getMinZ()).thenReturn(-200);

        // New cache
        ic = new IslandCache();
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test for {@link IslandCache#addIsland(Island)}
     */
    @Test
    public void testAddIsland() {
        assertTrue(ic.addIsland(island));
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
        assertEquals(island, ic.get(location));
    }

    /**
     * Test for {@link IslandCache#addPlayer(UUID, Island)}
     */
    @Test
    public void testAddPlayer() {
        UUID playerUUID = UUID.randomUUID();
        ic.addPlayer(playerUUID, island);
        // Check if they are added
        assertEquals(island, ic.get(world, playerUUID));
        assertNotSame(island, ic.get(world, UUID.randomUUID()));

    }

    /**
     * Test for {@link IslandCache#clear()}
     */
    @Test
    public void testClear() {
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
        assertEquals(island, ic.get(location));
        ic.clear();
        assertNull(ic.get(world, owner));
        assertNull(ic.get(location));
    }

    /**
     * Test for {@link IslandCache#deleteIslandFromCache(Island)}
     */
    @Test
    public void testDeleteIslandFromCache() {
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

    /**
     * Test for {@link IslandCache#get(Location)}
     */
    @Test
    public void testGetLocation() {
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(location));
    }

    /**
     * Test for {@link IslandCache#get(World, UUID)}
     */
    @Test
    public void testGetUUID() {
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.get(world, owner));
    }

    /**
     * Test for {@link IslandCache#getIslandAt(Location)}
     */
    @Test
    public void testGetIslandAtLocation() {
        // Set coords to be in island space
        when(island.inIslandSpace(Mockito.any(Integer.class), Mockito.any(Integer.class))).thenReturn(true);
        // Set plugin
        Util.setPlugin(plugin);
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

    /**
     * Test for {@link IslandCache#getMembers(World, UUID, int)}
     */
    @Test
    public void testGetMembers() {
        ic.addIsland(island);

        assertTrue(ic.getMembers(world, null, RanksManager.MEMBER_RANK).isEmpty());
        assertTrue(ic.getMembers(world, UUID.randomUUID(), RanksManager.MEMBER_RANK).isEmpty());
        assertFalse(ic.getMembers(world, island.getOwner(), RanksManager.MEMBER_RANK).isEmpty());
        assertEquals(3, ic.getMembers(world, island.getOwner(), RanksManager.MEMBER_RANK).size());

    }

    /**
     * Test for {@link IslandCache#getOwner(World, UUID)}
     */
    @Test
    public void testGetOwner() {
        ic.addIsland(island);

        assertEquals(owner, ic.getOwner(world, owner));
        assertNull(ic.getOwner(world, null));
        assertNull(ic.getOwner(world, UUID.randomUUID()));
    }

    /**
     * Test for {@link IslandCache#hasIsland(World, UUID)}
     */
    @Test
    public void testHasIsland() {
        ic.addIsland(island);

        assertTrue(ic.hasIsland(world, owner));
        assertFalse(ic.hasIsland(world, UUID.randomUUID()));
        assertFalse(ic.hasIsland(world, null));
    }

    /**
     * Test for {@link IslandCache#removePlayer(World, UUID)}
     */
    @Test
    public void testRemovePlayer() {
        ic.addIsland(island);
        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, null);
        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, UUID.randomUUID());
        assertTrue(ic.hasIsland(world, owner));
        ic.removePlayer(world, owner);
        assertFalse(ic.hasIsland(world, owner));
    }

    /**
     * Test for {@link IslandCache#size()}
     */
    @Test
    public void testSize() {
        ic.addIsland(island);
        assertEquals(1, ic.size());
    }

    /**
     * Test for {@link IslandCache#setOwner(Island, UUID)}
     */
    @Test
    public void testSetOwner() {
        ic.addIsland(island);
        UUID newOwnerUUID = UUID.randomUUID();
        ic.setOwner(island, newOwnerUUID);

        Mockito.verify(island).setOwner(newOwnerUUID);
        assertEquals(island, ic.get(world, newOwnerUUID));
        assertEquals(island, ic.get(island.getCenter()));
    }

    /**
     * Test for {@link IslandCache#resetFlag(World, world.bentobox.bentobox.api.flags.Flag)}
     */
    @Test
    public void testResetFlag() {
        ic.addIsland(island);
        ic.resetFlag(world, flag);
        verify(island).setFlag(eq(flag), eq(400));
    }

    /**
     * Test for {@link IslandCache#resetAllFlags(World)}
     */
    @Test
    public void testResetAllFlags() {
        ic.addIsland(island);
        ic.resetAllFlags(world);
        verify(island).setFlagsDefaults();
    }
}
