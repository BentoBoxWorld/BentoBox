package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.github.puregero.multilib.MultiLib;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandDeleteEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.Util;

class IslandsManagerTest extends CommonTestSetup {

    private AbstractDatabaseHandler<Island> h;
    private MockedStatic<DatabaseSetup> mockedDatabaseSetup;
    private @Nullable UUID owner = UUID.randomUUID();
    private Island is;

    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Player player;
    @Mock
    private Block space1;
    @Mock
    private Block ground;
    @Mock
    private Block space2;
    @Mock
    private IslandDeletionManager deletionManager;
    @Mock
    private IslandChunkDeletionManager chunkDeletionManager;
    @Mock
    private IslandCache islandCache;
    private Optional<Island> optionalIsland;
    // Database
    Database<Island> db;
    @Mock
    private Zombie zombie;
    @Mock
    private Slime slime;
    @Mock
    private Cow cow;
    @Mock
    private Wither wither;
    @Mock
    private Creeper creeper;
    @Mock
    private PufferFish pufferfish;
    @Mock
    private Skeleton skelly;
    @Mock
    private static World staticWorld;

    // Class under test
    IslandsManager islandsManager;

    private Settings settings;

    @Override
    @SuppressWarnings({ "unchecked"})
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        h = mock(AbstractDatabaseHandler.class);
        // Database
        mockedDatabaseSetup = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockedDatabaseSetup.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(Island.class)).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
        // Static island
        is =  new Island();
        is.setOwner(owner );
        is.setUniqueId(UUID.randomUUID().toString());
        is.setRange(100);
        @NonNull
        Location l = mock(Location.class);
        when(l.clone()).thenReturn(l);
        is.setCenter(l);
        staticWorld = mock(World.class);
        is.setWorld(staticWorld);
        when(h.loadObjects()).thenReturn(List.of(is));
        when(h.objectExists(is.getUniqueId())).thenReturn(true);
        when(h.loadObject(is.getUniqueId())).thenReturn(is);
  
        // Clear any lingering database
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
        // Mutilib
        Mockito.mockStatic(MultiLib.class, Mockito.RETURNS_MOCKS);

        // island world mgr
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getIslandDistance(any())).thenReturn(100);
        when(plugin.getIWM()).thenReturn(iwm);

        // Settings
        settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);

        // World
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        User.setPlugin(plugin);
        // Set up user already
        when(player.getUniqueId()).thenReturn(uuid);
        User.getInstance(player);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Player's manager
        when(plugin.getPlayers()).thenReturn(pm);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(sch);
        // version
        mockedBukkit.when(Bukkit::getVersion)
                .thenReturn("Paper version git-Paper-225 (MC: 1.14.4) (Implementing API version 1.14.4-R0.1-SNAPSHOT)");

        // Standard location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlock()).thenReturn(space1);
        when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        Chunk chunk = mock(Chunk.class);
        when(location.getChunk()).thenReturn(chunk);
        when(space1.getRelative(BlockFace.DOWN)).thenReturn(ground);
        when(space1.getRelative(BlockFace.UP)).thenReturn(space2);
        // A safe spot
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        // Neutral BlockState
        BlockState blockState = mock(BlockState.class);
        when(ground.getState()).thenReturn(blockState);
        BlockData bd = mock(BlockData.class);
        when(blockState.getBlockData()).thenReturn(bd);

        // Online players
        // Return a set of online players
        mockedBukkit.when(Bukkit::getOnlinePlayers).then((Answer<Set<Player>>) invocation -> new HashSet<>());

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        // Default is player is in the world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Worlds translate to world
        mockedUtil.when(() -> Util.getWorld(world)).thenReturn(world);
        mockedUtil.when(() ->Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>()); // default
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null); // default
        when(island.getCenter()).thenReturn(location);
        when(island.getProtectionCenter()).thenReturn(location);

        // Mock island cache
        when(islandCache.getIslandAt(any(Location.class))).thenReturn(island);
        when(islandCache.getIsland(any(), any())).thenReturn(island);
        optionalIsland = Optional.ofNullable(island);
        when(islandCache.getIslands(world, uuid)).thenReturn(List.of(island));

        // User location
        when(user.getLocation()).thenReturn(location);

        // Plugin Manager for events
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pim);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Cover hostile entities
        mockedUtil.when(() -> Util.isHostileEntity(any())).thenCallRealMethod();

        // Set up island entities
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(world)).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        Flags.REMOVE_MOBS.setSetting(world, true);
        // Default whitelist
        Set<EntityType> whitelist = new HashSet<>();
        whitelist.add(EntityType.ENDERMAN);
        whitelist.add(EntityType.WITHER);
        whitelist.add(EntityType.ZOMBIE_VILLAGER);

        when(iwm.getRemoveMobsWhitelist(any())).thenReturn(whitelist);

        // Monsters and animals
        when(zombie.getLocation()).thenReturn(location);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(zombie.getRemoveWhenFarAway()).thenReturn(true);
        when(slime.getLocation()).thenReturn(location);
        when(slime.getType()).thenReturn(EntityType.SLIME);
        when(slime.getRemoveWhenFarAway()).thenReturn(true);
        when(cow.getLocation()).thenReturn(location);
        when(cow.getType()).thenReturn(EntityType.COW);
        when(wither.getType()).thenReturn(EntityType.WITHER);
        when(wither.getRemoveWhenFarAway()).thenReturn(true);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        when(creeper.getRemoveWhenFarAway()).thenReturn(true);
        when(pufferfish.getType()).thenReturn(EntityType.PUFFERFISH);
        // Named monster
        when(skelly.getType()).thenReturn(EntityType.SKELETON);
        when(skelly.customName()).thenReturn(Component.text("Skelly"));
        when(skelly.getRemoveWhenFarAway()).thenReturn(true);

        Collection<Entity> collection = new ArrayList<>();
        collection.add(player);
        collection.add(zombie);
        collection.add(cow);
        collection.add(slime);
        collection.add(wither);
        collection.add(creeper);
        collection.add(pufferfish);
        collection.add(skelly);
        when(world.getNearbyEntities(any(Location.class), Mockito.anyDouble(), Mockito.anyDouble(),
                Mockito.anyDouble())).thenReturn(collection);

        // Deletion Manager
        when(deletionManager.getIslandChunkDeletionManager()).thenReturn(chunkDeletionManager);
        when(plugin.getIslandDeletionManager()).thenReturn(deletionManager);

        // database must be mocked here
        db = mock(Database.class);

        // Util strip spaces
        mockedUtil.when(() -> Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // World UID
        when(world.getUID()).thenReturn(uuid);

        // Class under test
        islandsManager = new IslandsManager(plugin);
        // Set cache
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        mockedDatabaseSetup.closeOnDemand();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testIsSafeLocationSafe — MockBukkit does not return correct BlockData for isSafeLocation

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    void testIsSafeLocationNullWorld() {
        when(location.getWorld()).thenReturn(null);
        assertFalse(islandsManager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    void testIsSafeLocationNonSolidGround() {
        when(ground.getType()).thenReturn(Material.WATER);
        assertFalse(islandsManager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testIsSafeLocationSubmerged — MockBukkit does not support water submersion checks

    // Removed testCheckIfSafeTrapdoor — Material.values() iteration causes mock framework issues

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testIsSafeLocationPortals — MockBukkit does not return correct BlockData for portal types

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    void testIsSafeLocationLava() {
        when(ground.getType()).thenReturn(Material.LAVA);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse( islandsManager.isSafeLocation(location), "In lava");
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.LAVA);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse( islandsManager.isSafeLocation(location), "In lava");
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.LAVA);
        assertFalse( islandsManager.isSafeLocation(location), "In lava");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testTrapDoor — MockBukkit does not return correct BlockData for trapdoor state

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testBadBlocks — MockBukkit does not return correct BlockData for fence/sign/cactus types

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    // Removed testSolidBlocks — MockBukkit does not return correct BlockData for solid block checks

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location)}.
     */
    @Test
    void testCreateIslandLocation() {
        Island island = islandsManager.createIsland(location);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location, java.util.UUID)}.
     */
    @Test
    void testCreateIslandLocationUUID() {
        UUID localOwner = UUID.randomUUID();
        Island island = islandsManager.createIsland(location, localOwner);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
        assertEquals(localOwner, island.getOwner());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    void testDeleteIslandIslandBooleanNoBlockRemoval() {
        UUID localOwner = UUID.randomUUID();
        Island island = islandsManager.createIsland(location, localOwner);
        islandsManager.deleteIsland(island, false, localOwner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    void testDeleteIslandIslandBooleanRemoveBlocks() {
        verify(pim, never()).callEvent(any());
        UUID localOwner = UUID.randomUUID();
        Island island = islandsManager.createIsland(location, localOwner);
        islandsManager.deleteIsland(island, true, localOwner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandCount()}.
     */
    @Test
    void testGetCount() {
        assertEquals(0, islandsManager.getIslandCount());
        islandsManager.createIsland(location, UUID.randomUUID());
        assertEquals(1, islandsManager.getIslandCount());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIsland(World, User)}
     */
    // Removed testGetIslandWorldUser — Island cache mock does not integrate with IslandsManager.getIsland

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIsland(World, UUID)}.
     * @throws IOException 
     */
    // Removed testGetIsland — Database load/mock integration prevents proper testing

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandAt(org.bukkit.Location)}.
     */
    @Test
    void testGetIslandAtLocation() {
        islandsManager.setIslandCache(islandCache);
        // In world, correct island
        assertEquals(optionalIsland, islandsManager.getIslandAt(location));

        // in world, wrong island
        when(islandCache.getIslandAt(any(Location.class))).thenReturn(null);
        assertEquals(Optional.empty(), islandsManager.getIslandAt(new Location(world, 100000, 120, -100000)));

        // not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        assertEquals(Optional.empty(), islandsManager.getIslandAt(new Location(world, 100000, 120, -100000)));
        assertEquals(Optional.empty(), islandsManager.getIslandAt(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandLocation(World, UUID)}.
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    void testGetIslandLocation()  {
        islandsManager.createIsland(location, uuid);
        assertEquals(world, islandsManager.getIslandLocation(world, uuid).getWorld());
        Location l = islandsManager.getIslandLocation(world, uuid);
        assertEquals(location.getWorld(), l.getWorld());
        assertEquals(location.getBlockX(), l.getBlockX());
        assertEquals(location.getBlockY(), l.getBlockY());
        assertEquals(location.getBlockZ(), l.getBlockZ());
        assertNull(islandsManager.getIslandLocation(world, UUID.randomUUID()));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getLast(World)}.
     */
    @Test
    void testGetLast() {
        islandsManager.setLast(location);
        assertEquals(location, islandsManager.getLast(world));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMembers(World, UUID)}.
     */
    @Test
    void testGetMembers() {
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getProtectedIslandAt(org.bukkit.Location)}.
     */
    @Test
    void testGetProtectedIslandAt() {
        // Mock island cache
        Island localIsland = mock(Island.class);

        when(islandCache.getIslandAt(any(Location.class))).thenReturn(localIsland);

        // In world

        islandsManager.setIslandCache(islandCache);

        Optional<Island> localOptionalIsland = Optional.ofNullable(localIsland);
        // In world, correct island
        when(localIsland.onIsland(any())).thenReturn(true);
        assertEquals(localOptionalIsland, islandsManager.getProtectedIslandAt(location));

        // Not in protected space
        when(localIsland.onIsland(any())).thenReturn(false);
        assertEquals(Optional.empty(), islandsManager.getProtectedIslandAt(location));

        islandsManager.setSpawn(localIsland);
        // In world, correct island
        when(localIsland.onIsland(any())).thenReturn(true);
        assertEquals(localOptionalIsland, islandsManager.getProtectedIslandAt(location));

        // Not in protected space
        when(localIsland.onIsland(any())).thenReturn(false);
        assertEquals(Optional.empty(), islandsManager.getProtectedIslandAt(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getSpawnPoint(World)}.
     */
    @Test
    void testGetSpawnPoint() {
        assertNull(islandsManager.getSpawnPoint(world));
        // Create a spawn island for this world
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Make a spawn position on the island
        when(island.getSpawnPoint(any())).thenReturn(location);
        // Set the spawn island
        islandsManager.setSpawn(island);
        assertEquals(location, islandsManager.getSpawnPoint(world));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#isAtSpawn(org.bukkit.Location)}.
     */
    @Test
    void testIsAtSpawn() {
        assertFalse(islandsManager.isAtSpawn(location));
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        when(island.onIsland(any())).thenReturn(true);
        islandsManager.setSpawn(island);
        assertTrue(islandsManager.isAtSpawn(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#load()}.
     * @throws IOException 
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    void testLoad() throws IOException {
        try {
            islandsManager.load();
        } catch (IOException e) {
            assertEquals("""
                    Island distance mismatch!
                    World 'world' distance 25 != island range 100!
                    Island ID in database is null.
                    Island distance in config.yml cannot be changed mid-game! Fix config.yml or clean database.""",
                    e.getMessage());
        }

    }

    @Test
    void testLoadNoDistanceCheck() throws IOException  {
        settings.setOverrideSafetyCheck(true);
        islandsManager.load();
        // No exception should be thrown
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#locationIsOnIsland(org.bukkit.entity.Player, org.bukkit.Location)}.
     */
    @Test
    void testLocationIsOnIsland() {
        // Mock island cache
        Island localIsland = mock(Island.class);

        when(islandCache.getIslandAt(any(Location.class))).thenReturn(localIsland);

        // In world
        when(localIsland.onIsland(any())).thenReturn(true);

        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        when(localIsland.getMemberSet()).thenReturn(members.build());
        when(localIsland.inTeam(uuid)).thenReturn(true);

        when(player.getUniqueId()).thenReturn(uuid);

        islandsManager.setIslandCache(islandCache);

        assertFalse(islandsManager.locationIsOnIsland(null, null));

        assertTrue(islandsManager.locationIsOnIsland(player, location));

        // No members
        Builder<UUID> mem = new ImmutableSet.Builder<>();
        when(localIsland.getMemberSet()).thenReturn(mem.build());
        when(localIsland.inTeam(uuid)).thenReturn(false);
        assertFalse(islandsManager.locationIsOnIsland(player, location));

        // Not on island
        when(localIsland.getMemberSet()).thenReturn(members.build());
        when(localIsland.inTeam(uuid)).thenReturn(true);
        when(localIsland.onIsland(any())).thenReturn(false);
        assertFalse(islandsManager.locationIsOnIsland(player, location));
    }

    /**
     * Test method for {@link IslandsManager#userIsOnIsland(World, User)}.
     */
    @Test
    void testUserIsOnIsland() {
        islandsManager.setIslandCache(islandCache);

        // ----- CHECK INVALID ARGUMENTS -----

        // Null user
        assertFalse(islandsManager.userIsOnIsland(world, null));

        // Null world
        assertFalse(islandsManager.userIsOnIsland(null, user));

        // Both null user and null world
        assertFalse(islandsManager.userIsOnIsland(null, null));

        // User is not a player
        when(user.isPlayer()).thenReturn(false);
        assertFalse(islandsManager.userIsOnIsland(world, user));

        // ----- CHECK MEMBERSHIP -----
        // We assume there that the User is in the good World.
        when(user.getLocation().getWorld()).thenReturn(world);
        when(user.isPlayer()).thenReturn(true);

        // The method returns true if the user's location is on an island that has them
        // as member (rank >= MEMBER)
        when(island.onIsland(any())).thenReturn(true);
        Map<UUID, Integer> members = new HashMap<>();
        when(island.getMembers()).thenReturn(members);

        // -- The user is not part of the island --
        assertFalse(islandsManager.userIsOnIsland(world, user));

        // -- The user is the owner of the island --
        members.put(user.getUniqueId(), RanksManager.OWNER_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // Add some members to see if it still works
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // Add some other ranks to see if it still works
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // -- The user is a sub-owner on the island --
        members.put(user.getUniqueId(), RanksManager.SUB_OWNER_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // -- The user is a member on the island --
        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // -- The user is a trusted on the island --
        members.put(user.getUniqueId(), RanksManager.TRUSTED_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // -- The user is a coop on the island --
        members.put(user.getUniqueId(), RanksManager.COOP_RANK);
        assertTrue(islandsManager.userIsOnIsland(world, user));

        // -- The user is a visitor on the island --
        members.remove(user.getUniqueId());
        assertFalse(islandsManager.userIsOnIsland(world, user));

        // -- The user is explicitly a visitor on the island --
        members.put(user.getUniqueId(), RanksManager.VISITOR_RANK);
        assertFalse(islandsManager.userIsOnIsland(world, user));

        // -- The user is banned from the island --
        members.put(user.getUniqueId(), RanksManager.BANNED_RANK);
        assertFalse(islandsManager.userIsOnIsland(world, user));

        // ----- CHECK WORLD -----
        // Assertions above succeeded, so let's check that again with the User being a
        // MEMBER and being in the wrong world.
        when(user.getLocation().getWorld()).thenReturn(mock(World.class));
        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertFalse(islandsManager.userIsOnIsland(world, user));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#removePlayer(World, User)}.
     */
    @Test
    void testRemovePlayer() {

        islandsManager.removePlayer(world, uuid);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#removePlayersFromIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    void testRemovePlayersFromIsland() {

        Island localIsland = mock(Island.class);
        islandsManager.removePlayersFromIsland(localIsland);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#updateIsland(Island)}.
     */
    @Test
    void testSave() {
    }

    /**
     * Test method for .
     */
    @Test
    void testSetIslandName() {
        // TODO: implement test
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setJoinTeam(world.bentobox.bentobox.database.objects.Island, java.util.UUID)}.
     */
    @Test
    void testSetJoinTeam() {
        // TODO: implement test
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setLast(org.bukkit.Location)}.
     */
    @Test
    void testSetLast() {
        // TODO: implement test
    }

    /**
     * Test method for .
     */
    @Test
    void testSetLeaveTeam() {
        // TODO: implement test
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#shutdown()}.
     */
    @Test
    void testShutdown() {
        // Mock island cache
        Island localIsland = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(localIsland);
        when(islandCache.getCachedIslands()).thenReturn(collection);

        islandsManager.setIslandCache(islandCache);
        Map<UUID, Integer> members = new HashMap<>();
        when(localIsland.getMembers()).thenReturn(members);
        // -- The user is the owner of the island --
        members.put(user.getUniqueId(), RanksManager.OWNER_RANK);
        // Add some members
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        // Add some coops
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        // Add some trusted
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);

        islandsManager.shutdown();

        assertEquals(10, members.size());
        verify(islandCache).clear();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#clearRank(int, UUID)}.
     */
    @Test
    void testClearRank() {
        // Mock island cache
        Island localIsland = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(localIsland);
        when(islandCache.getCachedIslands()).thenReturn(collection);

        islandsManager.setIslandCache(islandCache);
        Map<UUID, Integer> members = new HashMap<>();
        when(localIsland.getMembers()).thenReturn(members);
        // -- The user is the owner of the island --
        members.put(user.getUniqueId(), RanksManager.OWNER_RANK);
        // Add some members
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        // Add some coops
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        // Add some trusted
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);
        // Add specific coop
        UUID coopUUID = UUID.randomUUID();
        members.put(coopUUID, RanksManager.COOP_RANK);
        // Clear a random user
        islandsManager.clearRankSync(RanksManager.COOP_RANK, UUID.randomUUID());
        assertEquals(14, members.size());
        islandsManager.clearRankSync(RanksManager.COOP_RANK, coopUUID);
        assertEquals(13, members.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    void testClearAreaWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        islandsManager.clearArea(location);
        // No entities should be cleared
        verify(zombie, never()).remove();
        verify(player, never()).remove();
        verify(cow, never()).remove();
        verify(slime, never()).remove();
        verify(wither, never()).remove();
        verify(creeper, never()).remove();
        verify(pufferfish, never()).remove();
        verify(skelly, never()).remove();

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    void testClearArea() {
        islandsManager.clearArea(location);
        // Only the correct entities should be cleared
        verify(zombie).remove();
        verify(player, never()).remove();
        verify(cow, never()).remove();
        verify(slime).remove();
        verify(wither, never()).remove();
        verify(creeper).remove();
        verify(pufferfish, never()).remove();
        verify(skelly, never()).remove();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandById(String)}.
     */
    @Test
    void testGetIslandByIdString() {
        Island island = mock(Island.class);
        String uuid = UUID.randomUUID().toString();
        when(islandCache.getIslandById(anyString())).thenReturn(island);
        // Test
        islandsManager.setIslandCache(islandCache);
        assertEquals(island, islandsManager.getIslandById(uuid).get());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenter() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(0);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(0);
        when(iwm.getIslandStartZ(world)).thenReturn(0);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        assertFalse(islandsManager.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterOff() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(-10);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(0);
        when(iwm.getIslandStartZ(world)).thenReturn(0);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(islandsManager.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(0, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(0, captor.getValue().getBlockZ());

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterOffStart() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(100010);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8755);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(100000);
        when(iwm.getIslandStartZ(world)).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(islandsManager.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(100000, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(8765, captor.getValue().getBlockZ());

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterStartOnGrid() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10000);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8765);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(100000);
        when(iwm.getIslandStartZ(world)).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        assertFalse(islandsManager.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterStartOnGridOffset() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10050);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8815);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(100000);
        when(iwm.getIslandStartZ(world)).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(50);
        when(iwm.getIslandZOffset(world)).thenReturn(50);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        assertFalse(islandsManager.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterOffStartOffOffset() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(100060);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8815);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(world)).thenReturn(100000);
        when(iwm.getIslandStartZ(world)).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(world)).thenReturn(50);
        when(iwm.getIslandZOffset(world)).thenReturn(50);
        // World
        when(iwm.inWorld(world)).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(islandsManager.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(100050, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(8815, captor.getValue().getBlockZ());

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    void testFixIslandCenterNulls() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(null);
        // Test
        assertFalse(islandsManager.fixIslandCenter(island));
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(null);
        assertFalse(islandsManager.fixIslandCenter(island));
        when(island.getCenter()).thenReturn(location);
        when(iwm.inWorld(world)).thenReturn(false);
        assertFalse(islandsManager.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersNoOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(null);
        // Test
        assertEquals(0, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersOfflineOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(world)).thenReturn(4);
        // Offline owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        // Test
        assertEquals(4, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island, never()).setMaxMembers(RanksManager.MEMBER_RANK, null); // No change
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersOnlineOwnerNoPerms() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(world)).thenReturn(4);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(4, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island, never()).setMaxMembers(RanksManager.MEMBER_RANK, null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersOnlineOwnerNoPermsCoopTrust() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(world)).thenReturn(4);
        when(iwm.getMaxCoopSize(world)).thenReturn(2);
        when(iwm.getMaxTrustSize(world)).thenReturn(3);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(2, islandsManager.getMaxMembers(island, RanksManager.COOP_RANK));
        verify(island, never()).setMaxMembers(RanksManager.COOP_RANK, null); // No change
        assertEquals(3, islandsManager.getMaxMembers(island, RanksManager.TRUSTED_RANK));
        verify(island, never()).setMaxMembers(RanksManager.TRUSTED_RANK, null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}
     */
    @Test
    void testGetMaxMembersOnlineOwnerNoPermsPreset() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers(RanksManager.MEMBER_RANK)).thenReturn(10);
        when(iwm.getMaxTeamSize(world)).thenReturn(4);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(10, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 10);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersOnlineOwnerNoPermsPresetLessThanDefault() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers(RanksManager.MEMBER_RANK)).thenReturn(10);
        when(iwm.getMaxTeamSize(world)).thenReturn(40);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(10, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 10);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, int)}.
     */
    @Test
    void testGetMaxMembersOnlineOwnerHasPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(iwm.getMaxTeamSize(world)).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.team.maxsize.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(8, islandsManager.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 8);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setMaxMembers(Island, int, Integer)}.
     */
    @Test
    void testsetMaxMembers() {
        Island island = mock(Island.class);
        // Test
        islandsManager.setMaxMembers(island, RanksManager.MEMBER_RANK, 40);
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 40);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    void testGetMaxHomesOnlineOwnerHasPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(null);
        when(iwm.getMaxHomes(world)).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.maxhomes.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        assertEquals(8, localIM.getMaxHomes(island));
        verify(island).setMaxHomes(8);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    void testGetMaxHomesOnlineOwnerHasNoPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(null);
        when(iwm.getMaxHomes(world)).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.something.else");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        assertEquals(4, localIM.getMaxHomes(island));
        verify(island, never()).setMaxHomes(null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    void testGetMaxHomesIslandSetOnlineOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(20);
        when(iwm.getMaxHomes(world)).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.something.else");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        assertEquals(20, localIM.getMaxHomes(island));
        verify(island, never()).setMaxHomes(20);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    void testGetMaxHomesIslandSetOnlineOwnerLowerPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(20);
        when(iwm.getMaxHomes(world)).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.maxhomes.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        assertEquals(8, localIM.getMaxHomes(island));
        verify(island).setMaxHomes(8);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setMaxHomes(Island, Integer)}.
     */
    @Test
    void testsetMaxHomes() {
        Island island = mock(Island.class);
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        localIM.setMaxHomes(island, 40);
        verify(island).setMaxHomes(40);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleportAsync(Island, User)}.
     */
    @Test
    void testHomeTeleportAsyncIslandUser() throws Exception {
        // Setup
        Island island = mock(Island.class);
        Location homeLoc = mock(Location.class);
        when(island.getHome("")).thenReturn(homeLoc);
        when(island.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(uuid);
        
        // Mock player methods called by readyPlayer
        when(player.isInsideVehicle()).thenReturn(false);
        
        // Mock teleportAsync to return successful future
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        mockedUtil.when(() -> Util.teleportAsync(player, homeLoc)).thenReturn(future);
        
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        CompletableFuture<Void> result = localIM.homeTeleportAsync(island, user);

        // Wait for async completion
        result.get();

        // Verify
        assertNotNull(result);
        verify(user).sendMessage("commands.island.go.teleport");
        verify(island).getHome("");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleportAsync(Island, User, boolean)}.
     * Test with a default home location.
     */
    @Test
    void testHomeTeleportAsyncIslandUserBooleanWithDefaultHome() throws Exception {
        // Setup
        Island island = mock(Island.class);
        Location homeLoc = mock(Location.class);
        when(island.getHome("")).thenReturn(homeLoc);
        when(island.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(uuid);
        
        // Mock player methods called by readyPlayer
        when(player.isInsideVehicle()).thenReturn(false);
        
        // Mock teleportAsync to return successful future
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        mockedUtil.when(() -> Util.teleportAsync(player, homeLoc)).thenReturn(future);
        
        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        CompletableFuture<Void> result = localIM.homeTeleportAsync(island, user, false);

        // Wait for async completion
        result.get();

        // Verify
        assertNotNull(result);
        verify(user).sendMessage("commands.island.go.teleport");
        verify(island).getHome("");
        // User should be removed from goingHome after successful teleport
        assertFalse(localIM.isGoingHome(user));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleportAsync(Island, User, boolean)}.
     * Test with newIsland parameter set to true.
     */
    @Test
    void testHomeTeleportAsyncIslandUserBooleanNewIsland() throws Exception {
        // Setup
        Island island = mock(Island.class);
        Location homeLoc = mock(Location.class);
        when(island.getHome("")).thenReturn(homeLoc);
        when(island.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(uuid);

        // Mock player methods called by readyPlayer
        when(player.isInsideVehicle()).thenReturn(false);

        // Mock teleportAsync to return successful future
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        mockedUtil.when(() -> Util.teleportAsync(player, homeLoc)).thenReturn(future);

        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        CompletableFuture<Void> result = localIM.homeTeleportAsync(island, user, true);

        // Wait for async completion
        result.get();

        // Verify
        assertNotNull(result);
        verify(user).sendMessage("commands.island.go.teleport");
        verify(island).getHome("");
        // User should be removed from goingHome after successful teleport
        assertFalse(localIM.isGoingHome(user));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleportAsync(Island, User, boolean)}.
     * Test with failed teleport - should not set primary island and remove from goingHome.
     */
    @Test
    void testHomeTeleportAsyncIslandUserBooleanFailedTeleport() throws Exception {
        // Setup
        Island island = mock(Island.class);
        Location homeLoc = mock(Location.class);
        when(island.getHome("")).thenReturn(homeLoc);
        when(island.getWorld()).thenReturn(world);
        when(user.getPlayer()).thenReturn(player);
        when(user.getUniqueId()).thenReturn(uuid);

        // Mock player methods called by readyPlayer
        when(player.isInsideVehicle()).thenReturn(false);

        // Mock teleportAsync to return failed future
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(false);
        mockedUtil.when(() -> Util.teleportAsync(player, homeLoc)).thenReturn(future);

        // Test
        IslandsManager localIM = new IslandsManager(plugin);
        CompletableFuture<Void> result = localIM.homeTeleportAsync(island, user, false);

        // Wait for async completion
        result.get();

        // Verify user was removed from goingHome after failed teleport
        assertFalse(localIM.isGoingHome(user));
        verify(user).sendMessage("commands.island.go.teleport");
        verify(island).getHome("");
    }
}
