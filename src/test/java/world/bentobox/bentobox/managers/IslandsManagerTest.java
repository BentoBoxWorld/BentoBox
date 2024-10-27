package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.github.puregero.multilib.MultiLib;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.CraftBukkitEnvironment;
import io.papermc.lib.environments.Environment;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, Util.class, Location.class, MultiLib.class, DatabaseSetup.class })
public class IslandsManagerTest extends AbstractCommonSetup {

    private static AbstractDatabaseHandler<Object> h;
    @Mock
    private BentoBox plugin;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    @Mock
    private Player player;
    @Mock
    private World world;
    @Mock
    private Block space1;
    @Mock
    private Block ground;
    @Mock
    private Block space2;
    @Mock
    private Location location;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandDeletionManager deletionManager;
    @Mock
    private IslandChunkDeletionManager chunkDeletionManager;
    @Mock
    private IslandCache islandCache;
    private Optional<Island> optionalIsland;
    @Mock
    private Island island;
    @Mock
    private PluginManager pim;
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

    private Material sign;
    private Material wallSign;

    private Environment env;

    // Class under test
    IslandsManager im;

    private Settings settings;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void beforeClass() throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // This has to be done beforeClass otherwise the tests will interfere with each
        // other
        h = mock(AbstractDatabaseHandler.class);
        // Database
        PowerMockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        when(DatabaseSetup.getDatabase()).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Clear any lingering database
        tearDown();
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Mutilib
        PowerMockito.mockStatic(MultiLib.class, Mockito.RETURNS_MOCKS);

        // island world mgr
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getIslandDistance(any())).thenReturn(25);
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
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        // version
        when(Bukkit.getVersion())
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
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> new HashSet<>());

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        // Default is player is in the world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Worlds translate to world
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

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
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Cover hostile entities
        when(Util.isHostileEntity(any())).thenCallRealMethod();

        // Set up island entities
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(eq(world))).thenReturn(ws);
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
        when(skelly.getCustomName()).thenReturn("Skelly");
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

        // Signs
        sign = Material.BIRCH_SIGN;
        wallSign = Material.ACACIA_WALL_SIGN;

        // PaperLib
        env = new CraftBukkitEnvironment();
        PaperLib.setCustomEnvironment(env);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // World UID
        when(world.getUID()).thenReturn(uuid);

        // Class under test
        im = new IslandsManager(plugin);
        // Set cache
        // im.setIslandCache(islandCache);
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
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testIsSafeLocationSafe() {
        assertTrue(im.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationNullWorld() {
        when(location.getWorld()).thenReturn(null);
        assertFalse(im.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testIsSafeLocationNonSolidGround() {
        when(ground.getType()).thenReturn(Material.WATER);
        assertFalse(im.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testIsSafeLocationSubmerged() {
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.WATER);
        when(space2.getType()).thenReturn(Material.WATER);
        assertFalse(im.isSafeLocation(location));
    }

    @SuppressWarnings("deprecation")
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testCheckIfSafeTrapdoor() {
        for (Material d : Material.values()) {
            if (d.name().contains("DOOR")) {
                for (Material s : Material.values()) {
                    if (s.name().contains("_SIGN") && !s.isLegacy()) {
                        assertFalse("Fail " + d.name() + " " + s.name(), im.checkIfSafe(world, d, s, Material.AIR));
                    }
                }
            }
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testIsSafeLocationPortals() {
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.NETHER_PORTAL);
        assertTrue(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.END_PORTAL);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.NETHER_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.END_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.NETHER_PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.END_PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.END_GATEWAY);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.END_GATEWAY);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.END_GATEWAY);
        assertFalse(im.isSafeLocation(location));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testIsSafeLocationLava() {
        when(ground.getType()).thenReturn(Material.LAVA);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.LAVA);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.LAVA);
        assertFalse("In lava", im.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testTrapDoor() {
        when(ground.getType()).thenReturn(Material.OAK_TRAPDOOR);
        assertFalse("Open trapdoor", im.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.IRON_TRAPDOOR);
        assertFalse("Open iron trapdoor", im.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testBadBlocks() {
        // Fences
        when(ground.getType()).thenReturn(Material.SPRUCE_FENCE);
        assertFalse("Fence :" + Material.SPRUCE_FENCE.toString(), im.isSafeLocation(location));
        // Signs
        sign = Material.BIRCH_SIGN;
        when(ground.getType()).thenReturn(sign);
        assertFalse("Sign", im.isSafeLocation(location));
        wallSign = Material.ACACIA_WALL_SIGN;
        when(ground.getType()).thenReturn(wallSign);
        assertFalse("Sign", im.isSafeLocation(location));
        // Bad Blocks
        Material[] badMats = {Material.CACTUS, Material.OAK_BOAT};
        Arrays.asList(badMats).forEach(m -> {
            when(ground.getType()).thenReturn(m);
            assertFalse("Bad mat :" + m.toString(), im.isSafeLocation(location));
        });

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    @Ignore("Material#isSolid() cannot be tested")
    public void testSolidBlocks() {
        when(space1.getType()).thenReturn(Material.STONE);
        assertFalse("Solid", im.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.STONE);
        assertFalse("Solid", im.isSafeLocation(location));

        when(space1.getType()).thenReturn(wallSign);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", im.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(wallSign);
        assertTrue("Wall sign 2", im.isSafeLocation(location));

        when(space1.getType()).thenReturn(sign);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", im.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(sign);
        assertTrue("Wall sign 2", im.isSafeLocation(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location)}.
     */
    @Test
    public void testCreateIslandLocation() {
        Island island = im.createIsland(location);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location, java.util.UUID)}.
     */
    @Test
    public void testCreateIslandLocationUUID() {
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
        assertEquals(owner, island.getOwner());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testDeleteIslandIslandBooleanNoBlockRemoval() {
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        im.deleteIsland(island, false, owner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testDeleteIslandIslandBooleanRemoveBlocks() {
        verify(pim, never()).callEvent(any());
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        im.deleteIsland(island, true, owner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandCount()}.
     */
    @Test
    public void testGetCount() {
        assertEquals(0, im.getIslandCount());
        im.createIsland(location, UUID.randomUUID());
        assertEquals(1, im.getIslandCount());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIsland(World, User)}
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testGetIslandWorldUser() throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        Island is = im.createIsland(location, user.getUniqueId());
        when(h.loadObject(anyString())).thenReturn(is);
        assertEquals(is, im.getIsland(world, user));
        assertNull(im.getIsland(world, (User) null));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIsland(World, UUID)}.
     * @throws IntrospectionException 
     * @throws NoSuchMethodException 
     * @throws ClassNotFoundException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testGetIsland() throws InstantiationException, IllegalAccessException, InvocationTargetException,
            ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        UUID owner = UUID.randomUUID();
        Island is = im.createIsland(location, owner);
        when(h.loadObject(anyString())).thenReturn(is);
        assertEquals(is, im.getIsland(world, owner));
        assertNull(im.getIsland(world, UUID.randomUUID()));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getIslandAt(org.bukkit.Location)}.
     */
    @Test
    public void testGetIslandAtLocation() throws Exception {
        im.setIslandCache(islandCache);
        // In world, correct island
        assertEquals(optionalIsland, im.getIslandAt(location));

        // in world, wrong island
        when(islandCache.getIslandAt(any(Location.class))).thenReturn(null);
        assertEquals(Optional.empty(), im.getIslandAt(new Location(world, 100000, 120, -100000)));

        // not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        assertEquals(Optional.empty(), im.getIslandAt(new Location(world, 100000, 120, -100000)));
        assertEquals(Optional.empty(), im.getIslandAt(location));
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
    public void testGetIslandLocation() throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        // Store island in database
        when(h.loadObject(anyString())).thenReturn(island);
        im.createIsland(location, uuid);
        assertEquals(world, im.getIslandLocation(world, uuid).getWorld());
        Location l = im.getIslandLocation(world, uuid);
        assertEquals(location.getWorld(), l.getWorld());
        assertEquals(location.getBlockX(), l.getBlockX());
        assertEquals(location.getBlockY(), l.getBlockY());
        assertEquals(location.getBlockZ(), l.getBlockZ());
        assertNull(im.getIslandLocation(world, UUID.randomUUID()));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getLast(World)}.
     */
    @Test
    public void testGetLast() {
        im.setLast(location);
        assertEquals(location, im.getLast(world));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMembers(World, UUID)}.
     */
    @Test
    public void testGetMembers() {
        // Mock island cache
        Set<UUID> members = new HashSet<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        /*
         * when(islandCache.getMembers(any(), any(),
         * Mockito.anyInt())).thenReturn(members); im.setIslandCache(islandCache);
         * assertEquals(members, im.getMembers(world, UUID.randomUUID()));
         */
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getProtectedIslandAt(org.bukkit.Location)}.
     */
    @Test
    public void testGetProtectedIslandAt() {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(any(Location.class))).thenReturn(is);

        // In world

        im.setIslandCache(islandCache);

        Optional<Island> optionalIsland = Optional.ofNullable(is);
        // In world, correct island
        when(is.onIsland(any())).thenReturn(true);
        assertEquals(optionalIsland, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(any())).thenReturn(false);
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));

        im.setSpawn(is);
        // In world, correct island
        when(is.onIsland(any())).thenReturn(true);
        assertEquals(optionalIsland, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(any())).thenReturn(false);
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getSpawnPoint(World)}.
     */
    @Test
    public void testGetSpawnPoint() {
        assertNull(im.getSpawnPoint(world));
        // Create a spawn island for this world
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Make a spawn position on the island
        when(island.getSpawnPoint(any())).thenReturn(location);
        // Set the spawn island
        im.setSpawn(island);
        assertEquals(location, im.getSpawnPoint(world));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#isAtSpawn(org.bukkit.Location)}.
     */
    @Test
    public void testIsAtSpawn() {
        assertFalse(im.isAtSpawn(location));
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        when(island.onIsland(any())).thenReturn(true);
        im.setSpawn(island);
        assertTrue(im.isAtSpawn(location));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#isOwner(World, UUID)}.
     */
    /*
     * @Test public void testIsOwner() { // Mock island cache Island is =
     * mock(Island.class);
     * 
     * when(islandCache.getIslandAt(any())).thenReturn(is);
     * 
     * 
     * im.setIslandCache(islandCache);
     * 
     * assertFalse(im.isOwner(world, null));
     * 
     * when(islandCache.hasIsland(any(), any())).thenReturn(false);
     * assertFalse(im.isOwner(world, UUID.randomUUID()));
     * 
     * when(islandCache.hasIsland(any(), any())).thenReturn(true);
     * when(islandCache.get(any(), any(UUID.class))).thenReturn(is); UUID owner =
     * UUID.randomUUID(); when(is.getOwner()).thenReturn(owner); UUID notOwner =
     * UUID.randomUUID(); while (owner.equals(notOwner)) { notOwner =
     * UUID.randomUUID(); } assertFalse(im.isOwner(world, notOwner));
     * assertTrue(im.isOwner(world, owner)); }
     */
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
    public void testLoad() throws IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        when(island.getRange()).thenReturn(100);
        when(h.loadObjects()).thenReturn(List.of(island));
        try {
            im.load();
        } catch (IOException e) {
            assertEquals("Island distance mismatch!\n" + "World 'world' distance 25 != island range 100!\n"
                    + "Island ID in database is null.\n"
                    + "Island distance in config.yml cannot be changed mid-game! Fix config.yml or clean database.",
                    e.getMessage());
        }

    }

    @Test
    public void testLoadNoDistanceCheck() throws IOException, InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException, NoSuchMethodException, IntrospectionException {
        settings.setOverrideSafetyCheck(true);
        when(island.getUniqueId()).thenReturn(UUID.randomUUID().toString());
        when(island.getRange()).thenReturn(100);
        when(h.loadObjects()).thenReturn(List.of(island));
        im.load();
        // No exception should be thrown
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#locationIsOnIsland(org.bukkit.entity.Player, org.bukkit.Location)}.
     */
    @Test
    public void testLocationIsOnIsland() {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(any(Location.class))).thenReturn(is);

        // In world
        when(is.onIsland(any())).thenReturn(true);

        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        when(is.getMemberSet()).thenReturn(members.build());
        when(is.inTeam(uuid)).thenReturn(true);

        when(player.getUniqueId()).thenReturn(uuid);

        im.setIslandCache(islandCache);

        assertFalse(im.locationIsOnIsland(null, null));

        assertTrue(im.locationIsOnIsland(player, location));

        // No members
        Builder<UUID> mem = new ImmutableSet.Builder<>();
        when(is.getMemberSet()).thenReturn(mem.build());
        when(is.inTeam(uuid)).thenReturn(false);
        assertFalse(im.locationIsOnIsland(player, location));

        // Not on island
        when(is.getMemberSet()).thenReturn(members.build());
        when(is.inTeam(uuid)).thenReturn(true);
        when(is.onIsland(any())).thenReturn(false);
        assertFalse(im.locationIsOnIsland(player, location));
    }

    /**
     * Test method for {@link IslandsManager#userIsOnIsland(World, User)}.
     */
    @Test
    public void testUserIsOnIsland() {
        im.setIslandCache(islandCache);

        // ----- CHECK INVALID ARGUMENTS -----

        // Null user
        assertFalse(im.userIsOnIsland(world, null));

        // Null world
        assertFalse(im.userIsOnIsland(null, user));

        // Both null user and null world
        assertFalse(im.userIsOnIsland(null, null));

        // User is not a player
        when(user.isPlayer()).thenReturn(false);
        assertFalse(im.userIsOnIsland(world, user));

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
        assertFalse(im.userIsOnIsland(world, user));

        // -- The user is the owner of the island --
        members.put(user.getUniqueId(), RanksManager.OWNER_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // Add some members to see if it still works
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        members.put(UUID.randomUUID(), RanksManager.MEMBER_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // Add some other ranks to see if it still works
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        members.put(UUID.randomUUID(), RanksManager.COOP_RANK);
        members.put(UUID.randomUUID(), RanksManager.TRUSTED_RANK);
        members.put(UUID.randomUUID(), RanksManager.BANNED_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // -- The user is a sub-owner on the island --
        members.put(user.getUniqueId(), RanksManager.SUB_OWNER_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // -- The user is a member on the island --
        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // -- The user is a trusted on the island --
        members.put(user.getUniqueId(), RanksManager.TRUSTED_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // -- The user is a coop on the island --
        members.put(user.getUniqueId(), RanksManager.COOP_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        // -- The user is a visitor on the island --
        members.remove(user.getUniqueId());
        assertFalse(im.userIsOnIsland(world, user));

        // -- The user is explicitly a visitor on the island --
        members.put(user.getUniqueId(), RanksManager.VISITOR_RANK);
        assertFalse(im.userIsOnIsland(world, user));

        // -- The user is banned from the island --
        members.put(user.getUniqueId(), RanksManager.BANNED_RANK);
        assertFalse(im.userIsOnIsland(world, user));

        // ----- CHECK WORLD -----
        // Assertions above succeeded, so let's check that again with the User being a
        // MEMBER and being in the wrong world.
        when(user.getLocation().getWorld()).thenReturn(mock(World.class));
        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertFalse(im.userIsOnIsland(world, user));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#removePlayer(World, User)}.
     */
    @Test
    public void testRemovePlayer() {

        im.removePlayer(world, uuid);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#removePlayersFromIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testRemovePlayersFromIsland() {

        Island is = mock(Island.class);
        im.removePlayersFromIsland(is);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#updateIsland(Island)}.
     */
    @Test
    public void testSave() {
        // fail("Not yet implemented"); // TODO - warning saving stuff will go on the
        // file system
    }

    /**
     * Test method for .
     */
    @Test
    public void testSetIslandName() {
        // fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setJoinTeam(world.bentobox.bentobox.database.objects.Island, java.util.UUID)}.
     */
    @Test
    public void testSetJoinTeam() {
        // fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setLast(org.bukkit.Location)}.
     */
    @Test
    public void testSetLast() {
        // fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for .
     */
    @Test
    public void testSetLeaveTeam() {
        // fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        // Mock island cache
        Island is = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(is);
        when(islandCache.getCachedIslands()).thenReturn(collection);

        im.setIslandCache(islandCache);
        Map<UUID, Integer> members = new HashMap<>();
        when(is.getMembers()).thenReturn(members);
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

        im.shutdown();

        assertEquals(10, members.size());
        verify(islandCache).clear();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#clearRank(int, UUID)}.
     */
    @Test
    public void testClearRank() {
        // Mock island cache
        Island is = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(is);
        when(islandCache.getCachedIslands()).thenReturn(collection);

        im.setIslandCache(islandCache);
        Map<UUID, Integer> members = new HashMap<>();
        when(is.getMembers()).thenReturn(members);
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
        im.clearRankSync(RanksManager.COOP_RANK, UUID.randomUUID());
        assertEquals(14, members.size());
        im.clearRankSync(RanksManager.COOP_RANK, coopUUID);
        assertEquals(13, members.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    public void testClearAreaWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        im.clearArea(location);
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
    public void testClearArea() {
        im.clearArea(location);
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
    public void testGetIslandByIdString() {
        Island island = mock(Island.class);
        String uuid = UUID.randomUUID().toString();
        when(islandCache.getIslandById(anyString())).thenReturn(island);
        // Test
        im.setIslandCache(islandCache);
        assertEquals(island, im.getIslandById(uuid).get());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    public void testFixIslandCenter() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(0);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(0);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(0);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(0);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    public void testFixIslandCenterOff() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(-10);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(0);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(0);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(0);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(im.fixIslandCenter(island));
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
    public void testFixIslandCenterOffStart() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(100010);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8755);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(100000);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(0);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(im.fixIslandCenter(island));
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
    public void testFixIslandCenterStartOnGrid() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10000);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8765);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(100000);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(0);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(0);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    public void testFixIslandCenterStartOnGridOffset() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(10050);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8815);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(100000);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(50);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(50);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    public void testFixIslandCenterOffStartOffOffset() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Island center
        when(location.getBlockX()).thenReturn(100060);
        when(location.getBlockY()).thenReturn(120);
        when(location.getBlockZ()).thenReturn(8815);
        when(island.getCenter()).thenReturn(location);
        // Start x,z
        when(iwm.getIslandStartX(eq(world))).thenReturn(100000);
        when(iwm.getIslandStartZ(eq(world))).thenReturn(8765);
        // Offset x,z
        when(iwm.getIslandXOffset(eq(world))).thenReturn(50);
        when(iwm.getIslandZOffset(eq(world))).thenReturn(50);
        // World
        when(iwm.inWorld(eq(world))).thenReturn(true);
        // Island distance
        when(iwm.getIslandDistance(eq(world))).thenReturn(100);
        // Test
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        assertTrue(im.fixIslandCenter(island));
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
    public void testFixIslandCenterNulls() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(null);
        // Test
        assertFalse(im.fixIslandCenter(island));
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(null);
        assertFalse(im.fixIslandCenter(island));
        when(island.getCenter()).thenReturn(location);
        when(iwm.inWorld(eq(world))).thenReturn(false);
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersNoOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(null);
        // Test
        assertEquals(0, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(eq(null));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOfflineOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(4);
        // Offline owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        // Test
        assertEquals(4, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island, never()).setMaxMembers(eq(RanksManager.MEMBER_RANK), eq(null)); // No change
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOnlineOwnerNoPerms() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(4);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(4, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island, never()).setMaxMembers(RanksManager.MEMBER_RANK, null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOnlineOwnerNoPermsCoopTrust() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(island.getMaxMembers(Mockito.anyInt())).thenReturn(null);
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(4);
        when(iwm.getMaxCoopSize(eq(world))).thenReturn(2);
        when(iwm.getMaxTrustSize(eq(world))).thenReturn(3);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(2, im.getMaxMembers(island, RanksManager.COOP_RANK));
        verify(island, never()).setMaxMembers(RanksManager.COOP_RANK, null); // No change
        assertEquals(3, im.getMaxMembers(island, RanksManager.TRUSTED_RANK));
        verify(island, never()).setMaxMembers(RanksManager.TRUSTED_RANK, null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOnlineOwnerNoPermsPreset() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers(eq(RanksManager.MEMBER_RANK))).thenReturn(10);
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(4);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(10, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 10);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOnlineOwnerNoPermsPresetLessThanDefault() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers(eq(RanksManager.MEMBER_RANK))).thenReturn(10);
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(40);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(10, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 10);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxMembers(Island, Integer)}.
     */
    @Test
    public void testGetMaxMembersOnlineOwnerHasPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxMembers()).thenReturn(new HashMap<>());
        when(iwm.getMaxTeamSize(eq(world))).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.team.maxsize.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        assertEquals(8, im.getMaxMembers(island, RanksManager.MEMBER_RANK));
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 8);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setMaxMembers(Island, Integer, Integer)}.
     */
    @Test
    public void testsetMaxMembers() {
        Island island = mock(Island.class);
        // Test
        im.setMaxMembers(island, RanksManager.MEMBER_RANK, 40);
        verify(island).setMaxMembers(RanksManager.MEMBER_RANK, 40);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    public void testGetMaxHomesOnlineOwnerHasPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(null);
        when(iwm.getMaxHomes(eq(world))).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.maxhomes.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        assertEquals(8, im.getMaxHomes(island));
        verify(island).setMaxHomes(eq(8));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    public void testGetMaxHomesOnlineOwnerHasNoPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(null);
        when(iwm.getMaxHomes(eq(world))).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.something.else");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        assertEquals(4, im.getMaxHomes(island));
        verify(island, never()).setMaxHomes(null);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    public void testGetMaxHomesIslandSetOnlineOwner() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(20);
        when(iwm.getMaxHomes(eq(world))).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.something.else");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        assertEquals(20, im.getMaxHomes(island));
        verify(island, never()).setMaxHomes(20);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#getMaxHomes(Island)}.
     */
    @Test
    public void testGetMaxHomesIslandSetOnlineOwnerLowerPerm() {
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getWorld()).thenReturn(world);
        when(island.getMaxHomes()).thenReturn(20);
        when(iwm.getMaxHomes(eq(world))).thenReturn(4);
        // Permission
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getValue()).thenReturn(true);
        when(pai.getPermission()).thenReturn("bskyblock.island.maxhomes.8");
        Set<PermissionAttachmentInfo> set = Collections.singleton(pai);
        when(player.getEffectivePermissions()).thenReturn(set);
        // Online owner
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        assertEquals(8, im.getMaxHomes(island));
        verify(island).setMaxHomes(8);
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.IslandsManager#setMaxHomes(Island, Integer)}.
     */
    @Test
    public void testsetMaxHomes() {
        Island island = mock(Island.class);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        im.setMaxHomes(island, 40);
        verify(island).setMaxHomes(eq(40));
    }
}
