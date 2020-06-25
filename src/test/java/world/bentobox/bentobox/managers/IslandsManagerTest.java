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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
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
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import io.papermc.lib.PaperLib;
import io.papermc.lib.environments.CraftBukkitEnvironment;
import io.papermc.lib.environments.Environment;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandDeleteEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class, Location.class })
public class IslandsManagerTest {

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
    private IslandsManager manager;
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
    private IslandCache islandCache;
    private Optional<Island> optionalIsland;
    @Mock
    private Island is;
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


    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Clear any lingering database
        tearDown();
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // island world mgr
        when(world.getName()).thenReturn("world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);
        when(s.getDatabaseType()).thenReturn(DatabaseType.JSON);

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
        User.getInstance(player);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Has team
        when(plugin.getPlayers()).thenReturn(pm);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        // version
        when(Bukkit.getVersion()).thenReturn("Paper version git-Paper-225 (MC: 1.14.4) (Implementing API version 1.14.4-R0.1-SNAPSHOT)");

        // Standard location
        manager = new IslandsManager(plugin);
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

        // Mock island cache
        when(islandCache.getIslandAt(any(Location.class))).thenReturn(is);
        optionalIsland = Optional.ofNullable(is);

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
        when(slime.getLocation()).thenReturn(location);
        when(slime.getType()).thenReturn(EntityType.SLIME);
        when(cow.getLocation()).thenReturn(location);
        when(cow.getType()).thenReturn(EntityType.COW);
        when(wither.getType()).thenReturn(EntityType.WITHER);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        when(pufferfish.getType()).thenReturn(EntityType.PUFFERFISH);
        // Named monster
        when(skelly.getType()).thenReturn(EntityType.SKELETON);
        when(skelly.getCustomName()).thenReturn("Skelly");

        Collection<Entity> collection = new ArrayList<>();
        collection.add(player);
        collection.add(zombie);
        collection.add(cow);
        collection.add(slime);
        collection.add(wither);
        collection.add(creeper);
        collection.add(pufferfish);
        collection.add(skelly);
        when(world
                .getNearbyEntities(any(Location.class), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble()))
        .thenReturn(collection);



        // database must be mocked here
        db = mock(Database.class);

        // Signs
        sign = Material.getMaterial("SIGN");
        if (sign == null) {
            sign = Material.getMaterial("OAK_SIGN");
        }
        wallSign = Material.getMaterial("WALL_SIGN");
        if (wallSign == null) {
            wallSign = Material.getMaterial("OAK_WALL_SIGN");
        }

        // PaperLib
        env = new CraftBukkitEnvironment();
        PaperLib.setCustomEnvironment(env);

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();
    }

    @After
    public void tearDown() throws IOException{
        //remove any database data
        File file = new File("database");
        Path pathToBeDeleted = file.toPath();
        if (file.exists()) {
            Files.walk(pathToBeDeleted)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationSafe() {
        IslandsManager manager = new IslandsManager(plugin);
        assertTrue(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationNullWorld() {
        when(location.getWorld()).thenReturn(null);
        IslandsManager manager = new IslandsManager(plugin);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationNonSolidGround() {
        when(ground.getType()).thenReturn(Material.WATER);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationSubmerged() {
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.WATER);
        when(space2.getType()).thenReturn(Material.WATER);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationPortals() {
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.NETHER_PORTAL);
        assertTrue(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.END_PORTAL);
        assertTrue(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.NETHER_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.END_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.NETHER_PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.END_PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationLava() {
        when(ground.getType()).thenReturn(Material.LAVA);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.LAVA);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.LAVA);
        assertFalse("In lava", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testTrapDoor() {
        when(ground.getType()).thenReturn(Material.OAK_TRAPDOOR);
        assertFalse("Open trapdoor", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.IRON_TRAPDOOR);
        assertFalse("Open iron trapdoor", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testBadBlocks() {
        // Fences
        Arrays.stream(Material.values()).filter(m -> m.toString().contains("FENCE")).forEach(m -> {
            when(ground.getType()).thenReturn(m);
            assertFalse("Fence :" + m.toString(), manager.isSafeLocation(location));
        });
        // Signs
        when(ground.getType()).thenReturn(sign);
        assertFalse("Sign", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(wallSign);
        assertFalse("Sign", manager.isSafeLocation(location));
        // Bad Blocks
        Material[] badMats = {Material.CACTUS, Material.OAK_BOAT};
        Arrays.asList(badMats).forEach(m -> {
            when(ground.getType()).thenReturn(m);
            assertFalse("Bad mat :" + m.toString(), manager.isSafeLocation(location));
        });

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testSolidBlocks() {
        when(space1.getType()).thenReturn(Material.STONE);
        assertFalse("Solid", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.STONE);
        assertFalse("Solid", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(wallSign);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(wallSign);
        assertTrue("Wall sign 2", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(sign);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(sign);
        assertTrue("Wall sign 2", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#bigScan(org.bukkit.Location, int)}.
     */
    @Test
    public void testBigScan() {
        IslandsManager manager = new IslandsManager(plugin);

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        Block space1 = mock(Block.class);
        Block ground = mock(Block.class);
        Block space2 = mock(Block.class);

        when(location.getBlock()).thenReturn(space1);

        when(ground.getType()).thenReturn(Material.GRASS);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        when(space1.getRelative(BlockFace.DOWN)).thenReturn(ground);
        when(space1.getRelative(BlockFace.UP)).thenReturn(space2);

        BlockState blockState = mock(BlockState.class);
        when(ground.getState()).thenReturn(blockState);

        // Negative value = full island scan
        // No island here yet
        assertNull(manager.bigScan(location, -1));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location)}.
     */
    @Test
    public void testCreateIslandLocation() {
        IslandsManager im = new IslandsManager(plugin);
        Island island = im.createIsland(location);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#createIsland(org.bukkit.Location, java.util.UUID)}.
     */
    @Test
    public void testCreateIslandLocationUUID() {
        UUID owner = UUID.randomUUID();
        IslandsManager im = new IslandsManager(plugin);
        Island island = im.createIsland(location, owner);
        assertNotNull(island);
        assertEquals(island.getCenter().getWorld(), location.getWorld());
        assertEquals(owner, island.getOwner());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testDeleteIslandIslandBooleanNoBlockRemoval() {
        IslandsManager im = new IslandsManager(plugin);
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        im.deleteIsland(island, false, owner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#deleteIsland(world.bentobox.bentobox.database.objects.Island, boolean)}.
     */
    @Test
    public void testDeleteIslandIslandBooleanRemoveBlocks() {
        verify(pim, never()).callEvent(any());
        IslandsManager im = new IslandsManager(plugin);
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        im.deleteIsland(island, true, owner);
        assertNull(island.getOwner());
        verify(pim).callEvent(any(IslandDeleteEvent.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getIslandCount()}.
     */
    @Test
    public void testGetCount() {
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.getIslandCount() == 0);
        im.createIsland(location, UUID.randomUUID());
        assertTrue(im.getIslandCount() == 1);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getIsland(World, UUID)}.
     */
    @Test
    public void testGetIsland() {
        UUID owner = UUID.randomUUID();
        IslandsManager im = new IslandsManager(plugin);
        Island island = im.createIsland(location, owner);
        assertEquals(island, im.getIsland(world, owner));
        assertNull(im.getIsland(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getIslandAt(org.bukkit.Location)}.
     * @throws Exception
     */
    @Test
    public void testGetIslandAtLocation() throws Exception {
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getIslandLocation(World, UUID)}.
     */
    @Test
    public void testGetIslandLocation() {
        IslandsManager im = new IslandsManager(plugin);
        im.createIsland(location, uuid);
        assertEquals(world, im.getIslandLocation(world, uuid).getWorld());
        assertNull(im.getIslandLocation(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getLast(World)}.
     */
    @Test
    public void testGetLast() {
        IslandsManager im = new IslandsManager(plugin);
        im.setLast(location);
        assertEquals(location, im.getLast(world));
        assertNull(im.getLast(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getMembers(World, UUID)}.
     */
    @Test
    public void testGetMembers() {
        // Mock island cache
        Set<UUID> members = new HashSet<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(islandCache.getMembers(any(), any(), Mockito.anyInt())).thenReturn(members);
        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);
        assertEquals(members, im.getMembers(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getProtectedIslandAt(org.bukkit.Location)}.
     */
    @Test
    public void testGetProtectedIslandAt() {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(any(Location.class))).thenReturn(is);

        // In world
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getSafeHomeLocation(World, User, int)}.
     */
    @Test
    public void testGetSafeHomeLocation() {
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(any(), any(User.class), eq(0))).thenReturn(null);
        when(pm.getHomeLocation(any(), any(User.class), eq(1))).thenReturn(location);
        assertEquals(location, im.getSafeHomeLocation(world, user, 0));
        // Change location so that it is not safe
        // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getSafeHomeLocation(World, User, int)}.
     * Ensures that the method returns {@code null} if the world is not an island world.
     */
    @Test
    public void testGetSafeHomeLocationWorldNotIslandWorld() {
        IslandsManager im = new IslandsManager(plugin);
        when(iwm.inWorld(world)).thenReturn(false);
        assertNull(im.getSafeHomeLocation(world, user, 0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getSpawnPoint(World)}.
     */
    @Test
    public void testGetSpawnPoint() {
        IslandsManager im = new IslandsManager(plugin);
        assertNull(im.getSpawnPoint(world));
        // Create a spawn island for this world
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Make a spawn position on the island
        when(island.getSpawnPoint(any())).thenReturn(location);
        // Set the spawn island
        im.setSpawn(island);
        assertEquals(location,im.getSpawnPoint(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleport(World, Player, int)}.
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testHomeTeleportPlayerInt() {
        when(iwm.getDefaultGameMode(world)).thenReturn(GameMode.SURVIVAL);
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(any(), any(User.class), eq(0))).thenReturn(null);
        when(pm.getHomeLocation(any(), any(User.class), eq(1))).thenReturn(location);
        im.homeTeleport(world, player, 0);
        verify(player).teleport(eq(location), any());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isAtSpawn(org.bukkit.Location)}.
     */
    @Test
    public void testIsAtSpawn() {
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.isAtSpawn(location));
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        when(island.onIsland(any())).thenReturn(true);
        im.setSpawn(island);
        assertTrue(im.isAtSpawn(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isOwner(World, UUID)}.
     */
    @Test
    public void testIsOwner() {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(any())).thenReturn(is);

        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);

        assertFalse(im.isOwner(world, null));

        when(islandCache.hasIsland(any(), any())).thenReturn(false);
        assertFalse(im.isOwner(world, UUID.randomUUID()));

        when(islandCache.hasIsland(any(), any())).thenReturn(true);
        when(islandCache.get(any(), any(UUID.class))).thenReturn(is);
        UUID owner = UUID.randomUUID();
        when(is.getOwner()).thenReturn(owner);
        UUID notOwner = UUID.randomUUID();
        while (owner.equals(notOwner)) {
            notOwner = UUID.randomUUID();
        }
        assertFalse(im.isOwner(world, notOwner));
        assertTrue(im.isOwner(world, owner));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#load()}.
     */
    @Test
    public void testLoad() {
        //IslandsManager im = new IslandsManager(plugin);
        //im.load();

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#locationIsOnIsland(org.bukkit.entity.Player, org.bukkit.Location)}.
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

        when(player.getUniqueId()).thenReturn(uuid);

        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);

        assertFalse(im.locationIsOnIsland(null, null));

        assertTrue(im.locationIsOnIsland(player, location));

        // No members
        Builder<UUID> mem = new ImmutableSet.Builder<>();
        when(is.getMemberSet()).thenReturn(mem.build());
        assertFalse(im.locationIsOnIsland(player, location));

        // Not on island
        when(is.getMemberSet()).thenReturn(members.build());
        when(is.onIsland(any())).thenReturn(false);
        assertFalse(im.locationIsOnIsland(player, location));
    }

    /**
     * Test method for {@link IslandsManager#userIsOnIsland(World, User)}.
     */
    @Test
    public void testUserIsOnIsland() {
        IslandsManager im = new IslandsManager(plugin);
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

        // The method returns true if the user's location is on an island that has them as member (rank >= MEMBER)
        when(is.onIsland(any())).thenReturn(true);
        Map<UUID, Integer> members = new HashMap<>();
        when(is.getMembers()).thenReturn(members);

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
        // Assertions above succeeded, so let's check that again with the User being a MEMBER and being in the wrong world.
        when(user.getLocation().getWorld()).thenReturn(mock(World.class));
        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertFalse(im.userIsOnIsland(world, user));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#removePlayer(World, User)}.
     */
    @Test
    public void testRemovePlayer() {
        IslandsManager im = new IslandsManager(plugin);
        im.removePlayer(world, uuid);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#removePlayersFromIsland(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testRemovePlayersFromIsland() {
        IslandsManager im = new IslandsManager(plugin);
        Island is = mock(Island.class);
        im.removePlayersFromIsland(is);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#save(Island)}.
     */
    @Test
    public void testSave() {
        //fail("Not yet implemented"); // TODO - warning saving stuff will go on the file system
    }

    /**
     * Test method for .
     */
    @Test
    public void testSetIslandName() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#setJoinTeam(world.bentobox.bentobox.database.objects.Island, java.util.UUID)}.
     */
    @Test
    public void testSetJoinTeam() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#setLast(org.bukkit.Location)}.
     */
    @Test
    public void testSetLast() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for .
     */
    @Test
    public void testSetLeaveTeam() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        // Mock island cache
        Island is = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(is);
        when(islandCache.getIslands()).thenReturn(collection);
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearRank(int, UUID)}.
     */
    @Test
    public void testClearRank() {
        // Mock island cache
        Island is = mock(Island.class);

        Collection<Island> collection = new ArrayList<>();
        collection.add(is);
        when(islandCache.getIslands()).thenReturn(collection);
        IslandsManager im = new IslandsManager(plugin);
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
        im.clearRank(RanksManager.COOP_RANK, UUID.randomUUID());
        assertEquals(14, members.size());
        im.clearRank(RanksManager.COOP_RANK, coopUUID);
        assertEquals(13, members.size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    public void testClearAreaWrongWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    public void testClearArea() {
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getIslandById(String)}.
     */
    @Test
    public void testGetIslandByIdString() {
        Island island = mock(Island.class);
        String uuid = UUID.randomUUID().toString();
        when(islandCache.getIslandById(anyString())).thenReturn(island);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);
        assertEquals(island, im.getIslandById(uuid).get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(0, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(0, captor.getValue().getBlockZ());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(100000, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(8765, captor.getValue().getBlockZ());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.fixIslandCenter(island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
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
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.fixIslandCenter(island));
        // Verify location
        verify(island).setCenter(captor.capture());
        assertEquals(world, captor.getValue().getWorld());
        assertEquals(100050, captor.getValue().getBlockX());
        assertEquals(120, captor.getValue().getBlockY());
        assertEquals(8815, captor.getValue().getBlockZ());

    }
    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#fixIslandCenter(Island)}.
     */
    @Test
    public void testFixIslandCenterNulls() {
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(null);
        // Test
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.fixIslandCenter(island));
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(null);
        assertFalse(im.fixIslandCenter(island));
        when(island.getCenter()).thenReturn(location);
        when(iwm.inWorld(eq(world))).thenReturn(false);
        assertFalse(im.fixIslandCenter(island));
    }

}
