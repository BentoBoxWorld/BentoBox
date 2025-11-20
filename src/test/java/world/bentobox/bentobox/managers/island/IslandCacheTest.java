package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class IslandCacheTest extends AbstractCommonSetup {

    private AbstractDatabaseHandler<Island> handler;
    // UUID
    private final UUID owner = UUID.randomUUID();
    // Test class
    private IslandCache ic;
    @Mock
    private Flag flag;
    // Database
    Database<Island> db;
    private MockedStatic<DatabaseSetup> mockedDatabaseSetup;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        handler = mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(() -> DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(eq(Island.class))).thenReturn(handler);
        when(handler.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
          // IWM
        when(iwm.getDefaultIslandFlags(any())).thenReturn(Collections.singletonMap(flag, 400));
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

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
        when(island.getRange()).thenReturn(400);

        // database must be mocked here
        db = mock(Database.class);
        when(db.loadObject("uniqueId")).thenReturn(island);
        when(db.objectExists(anyString())).thenReturn(true);
        when(db.saveObjectAsync(any())).thenReturn(CompletableFuture.completedFuture(true));

        // New cache
        ic = new IslandCache(db);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
        this.mockedDatabaseSetup.closeOnDemand();
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
        when(location2.getBlockX()).thenReturn(10000);
        when(location2.getBlockY()).thenReturn(100);
        when(location2.getBlockZ()).thenReturn(10000);

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
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(scheduler);
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
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#updateMultiLibIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testUpdateMultiLibIsland() {
        // Add island to cache
        ic.setIslandById(island);
        // Copy island
        Island newIsland = mock(Island.class);
        when(newIsland.getUniqueId()).thenReturn("uniqueId");
        when(newIsland.getMembers()).thenReturn(Map.of()); // no members

        ic.updateMultiLibIsland(newIsland);
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
        assertTrue(ic.isIslandCached("uniqueId"));

        // Remove it
        ic.deleteIslandFromCache(island);

        // verify
        assertFalse(ic.isIslandCached("uniqueId"));
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

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslands(java.util.UUID)}.
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testGetIslandsUUIDNoIslands() throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        // Test is WIP.
        when(handler.loadObject(anyString())).thenReturn(null);
        assertTrue(ic.getIslands(owner).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testAddIslandIslandNullWorld() {
        // Null world
        when(island.getWorld()).thenReturn(null);
        assertFalse(ic.addIsland(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testAddIslandIslandNullCenter() {
        // Try to add an island with a null center
        when(island.getCenter()).thenReturn(null);
        assertFalse(ic.addIsland(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testAddIslandIslandDuplicate() {
        assertTrue(ic.addIsland(island));
        assertTrue(ic.addIsland(island)); // Okay to add
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testAddIslandIslandBooleanNullWorld() {
        // Null world
        when(island.getWorld()).thenReturn(null);
        assertFalse(ic.addIsland(island, true));
        assertFalse(ic.isIslandCached("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testAddIslandIslandBooleanNullCenter() {
        // Try to add an island with a null center
        when(island.getCenter()).thenReturn(null);
        assertFalse(ic.addIsland(island, true));
        assertFalse(ic.isIslandCached("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#addIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testAddIslandIslandBooleanDuplicate() {
        // Duplicate
        assertTrue(ic.addIsland(island, true));
        assertTrue(ic.addIsland(island, true));
        // Overlapping
        Island island2 = mock(Island.class);
        when(island2.getUniqueId()).thenReturn("different");
        assertFalse(ic.addIsland(island2, true));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#loadIsland(java.lang.String)}.
     */
    @Test
    public void testLoadIsland() {
        assertNull(ic.loadIsland(""));
        assertNotNull(ic.loadIsland("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getCachedIslands()}.
     */
    @Test
    public void testGetCachedIslands() {
        assertTrue(ic.getCachedIslands().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslandById(java.lang.String)}.
     */
    @Test
    public void testGetIslandByIdString() {
        assertNotNull(ic.getIslandById("uniqueId"));
        assertTrue(ic.isIslandCached("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#getIslandById(java.lang.String, boolean)}.
     */
    @Test
    public void testGetIslandByIdStringBoolean() {
        assertNotNull(ic.getIslandById("uniqueId", false));
        assertFalse(ic.isIslandCached("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#expireIslandById(java.lang.String)}.
     */
    @Test
    public void testExpireIslandById() {
        // Fill the cache
        ic.addIsland(island);
        ic.setIslandById(island);
        assertTrue(ic.isIslandCached("uniqueId"));
        // Remove it
        ic.expireIslandById("uniqueId");
        // verify
        assertFalse(ic.isIslandCached("uniqueId"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#setIslandById(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testSetIslandById() {
        assertFalse(ic.isIslandId("uniqueId"));
        ic.setIslandById(island);
        assertTrue(ic.isIslandId("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#isIslandId(java.lang.String)}.
     */
    @Test
    public void testIsIslandId() {
        assertFalse(ic.isIslandId("uniqueId"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.IslandCache#isIslandCached(java.lang.String)}.
     */
    @Test
    public void testIsIslandCached() {
        assertFalse(ic.isIslandCached("uniqueId"));
    }

}
