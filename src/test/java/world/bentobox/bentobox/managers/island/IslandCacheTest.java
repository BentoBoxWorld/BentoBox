package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, Util.class, Location.class, DatabaseSetup.class, })
public class IslandCacheTest extends AbstractCommonSetup {

    private static AbstractDatabaseHandler<Object> handler;
    @Mock
    private World world;
    @Mock
    private Island island;
    // UUID
    private final UUID owner = UUID.randomUUID();
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
    // Database
    Database<Island> db;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(handler);
        when(handler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));

    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        // IWM
        when(iwm.getDefaultIslandFlags(any())).thenReturn(Collections.singletonMap(flag, 400));
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // Mock up IslandsManager
        when(plugin.getIslands()).thenReturn(im);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        // Island
        when(island.getWorld()).thenReturn(world);
        when(island.getUniqueId()).thenReturn("uniqueId");
        when(island.inIslandSpace(anyInt(), anyInt())).thenReturn(true);
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

        // database must be mocked here
        db = mock(Database.class);
        when(db.loadObject(anyString())).thenReturn(island);
        when(db.saveObjectAsync(any())).thenReturn(CompletableFuture.completedFuture(true));

        // New cache
        ic = new IslandCache(db);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }

    private void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

    }

    /**
     * Test for {@link IslandCache#addIsland(Island)}
     */
    @Test
    public void testAddIsland() {
        assertTrue(ic.addIsland(island));
        assertEquals(island, ic.getIslandAt(island.getCenter()));
        // Check if they are added
        assertEquals(island, ic.getIsland(world, owner));
    }

    /**
     * Test for {@link IslandCache#addPlayer(UUID, Island)}
     */
    @Test
    public void testAddPlayer() {
        ic.addIsland(island);
        UUID playerUUID = UUID.randomUUID();
        ic.addPlayer(playerUUID, island);
        // Check if they are added
        assertEquals(island, ic.getIsland(world, playerUUID));
        assertNotSame(island, ic.getIsland(world, UUID.randomUUID()));

    }

    /**
     * Test for {@link IslandCache#clear()}
     */
    @Test
    public void testClear() {
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.getIsland(world, owner));
        ic.clear();
        assertNull(ic.getIsland(world, owner));
    }

    /**
     * Test for {@link IslandCache#getIsland(World, UUID)}
     */
    @Test
    public void testGetUUID() {
        ic.addIsland(island);
        // Check if they are added
        assertEquals(island, ic.getIsland(world, owner));
    }

    /**
     * Test for {@link IslandCache#getIslandAt(Location)}
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testGetIslandAtLocation() throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        // Set coords to be in island space
        when(island.inIslandSpace(any(Integer.class), any(Integer.class))).thenReturn(true);
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
        when(island.inIslandSpace(any(Integer.class), any(Integer.class))).thenReturn(false);
        assertNull(ic.getIslandAt(location2));
    }

    /**
     * Test for {@link IslandCache#hasIsland(World, UUID)}
     */
    @Test
    public void testHasIsland() {
        ic.addIsland(island);

        assertTrue(ic.hasIsland(world, owner));
        assertFalse(ic.hasIsland(world, UUID.randomUUID()));
    }

    /**
     * Test for {@link IslandCache#removePlayer(World, UUID)}
     */
    @Test
    public void testRemovePlayer() {
        ic.addIsland(island);
        assertTrue(ic.hasIsland(world, owner));
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
        assertEquals(island, ic.getIsland(world, newOwnerUUID));
    }

    /**
     * Test for
     * {@link IslandCache#resetFlag(World, world.bentobox.bentobox.api.flags.Flag)}
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
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        ic.resetAllFlags(world);

        verify(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#IslandCache(world.bentobox.bentobox.database.Database)}.
     */
    @Test
    public void testIslandCache() {
        assertNotNull(ic);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#updateIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testUpdateIsland() {
        // Add island to cache
        ic.setIslandById(island);
        // Copy island
        Island newIsland = mock(Island.class);
        when(newIsland.getUniqueId()).thenReturn("uniqueId");
        when(newIsland.getMembers()).thenReturn(Map.of()); // no members

        ic.updateIsland(newIsland);
        verify(plugin, never()).logError(anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#deleteIslandFromCache(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testDeleteIslandFromCacheIsland() {
        // Fill the cache
        ic.addIsland(island);
        ic.setIslandById(island);
        // Remove it
        ic.deleteIslandFromCache(island);
        // TODO need to verify
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#deleteIslandFromCache(java.lang.String)}.
     */
    @Test
    public void testDeleteIslandFromCacheString() {
        // Fill the cache
        ic.addIsland(island);
        ic.setIslandById(island);

        ic.deleteIslandFromCache("uniqueId");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIsland(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIsland() {
        assertNull(ic.getIsland(world, owner));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslands(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testGetIslandsWorldUUID() {
        assertNull(ic.getIsland(world, this.owner));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#setPrimaryIsland(java.util.UUID, world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testSetPrimaryIsland() {
        ic.setPrimaryIsland(owner, island);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslandAt(org.bukkit.Location)}.
     */
    @Test
    public void testGetIslandAt() {
        ic.addIsland(island);
        ic.setIslandById(island);
        assertEquals(island, ic.getIslandAt(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslands()}.
     */
    @Test
    public void testGetIslands() {
        assertTrue(ic.getIslands().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslands(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandsWorld() {
        assertTrue(ic.getIslands(world).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#removePlayer(org.bukkit.World, java.util.UUID)}.
     */
    @Test
    public void testRemovePlayerWorldUUID() {
        assertTrue(ic.getIslands(owner).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#removePlayer(world.bentobox.bentobox.database.objects.Island, java.util.UUID)}.
     */
    @Test
    public void testRemovePlayerIslandUUID() {
        ic.addIsland(island);
        ic.setIslandById(island);
        ic.removePlayer(island, owner);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#size(org.bukkit.World)}.
     */
    @Test
    public void testSizeWorld() {
        assertEquals(0, ic.size(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslandById(java.lang.String)}.
     */
    @Test
    public void testGetIslandById() {
        ic.addIsland(island);
        ic.setIslandById(island);

        assertEquals(island, ic.getIslandById("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getAllIslandIds()}.
     */
    @Test
    public void testGetAllIslandIds() {
        assertTrue(ic.getAllIslandIds().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslands(java.util.UUID)}.
     */
    @Test
    public void testGetIslandsUUID() {
        assertTrue(ic.getIslands(owner).isEmpty());
    }

}
