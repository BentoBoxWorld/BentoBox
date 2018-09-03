package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Zombie;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.island.IslandCache;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class, Location.class })
public class IslandsManagerTest {

    private static BentoBox plugin;
    private UUID uuid;
    private User user;
    private PlayersManager pm;
    private Player player;
    private static World world;
    private IslandsManager manager;
    private Block space1;
    private Block ground;
    private Block space2;
    private Location location;
    private IslandWorldManager iwm;
    private IslandCache islandCache;
    private Optional<Island> optionalIsland;
    private Island is;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // World
        world = mock(World.class);
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        player = mock(Player.class);
        // Sometimes use: Mockito.withSettings().verboseLogging()
        user = mock(User.class);
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


        // Has team
        pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler

        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Standard location
        manager = new IslandsManager(plugin);
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        space1 = mock(Block.class);
        ground = mock(Block.class);
        space2 = mock(Block.class);
        when(location.getBlock()).thenReturn(space1);
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
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any())).thenReturn(true);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // Scheduler
        when(Bukkit.getScheduler()).thenReturn(mock(BukkitScheduler.class));

        // Mock island cache
        islandCache = mock(IslandCache.class);
        is = mock(Island.class);
        when(islandCache.getIslandAt(Mockito.any(Location.class))).thenReturn(is);
        optionalIsland = Optional.ofNullable(is);


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
    public void testIsSafeLocationNull() {
        IslandsManager manager = new IslandsManager(plugin);
        assertFalse(manager.isSafeLocation(null));
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
        when(space1.isLiquid()).thenReturn(true);
        when(space2.isLiquid()).thenReturn(true);
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
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.END_PORTAL);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.NETHER_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.END_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
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

        // Open trapdoor
        Openable trapDoor = mock(Openable.class);
        when(trapDoor.isOpen()).thenReturn(true);
        when(ground.getBlockData()).thenReturn(trapDoor);

        assertFalse("Open trapdoor", manager.isSafeLocation(location));

        when(trapDoor.isOpen()).thenReturn(false);
        assertTrue("Closed trapdoor", manager.isSafeLocation(location));

        when(ground.getType()).thenReturn(Material.IRON_TRAPDOOR);
        assertTrue("Closed iron trapdoor", manager.isSafeLocation(location));
        when(trapDoor.isOpen()).thenReturn(true);
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
        when(ground.getType()).thenReturn(Material.SIGN);
        assertFalse("Sign", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.WALL_SIGN);
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

        when(space1.getType()).thenReturn(Material.WALL_SIGN);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.WALL_SIGN);
        assertTrue("Wall sign 2", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.SIGN);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.SIGN);
        assertTrue("Wall sign 2", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#IslandsManager(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testIslandsManager() {
        assertNotNull(new IslandsManager(plugin));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#bigScan(org.bukkit.Location, int)}.
     */
    @Test
    public void testBigScan() throws Exception {
        Settings settings = mock(Settings.class);

        when(plugin.getSettings()).thenReturn(settings);

        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);

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
        // Null location should get a null response
        assertNull(manager.bigScan(null, -1));
        // No island here yet
        assertNull(manager.bigScan(location, -1));
        // Try null location, > 0 scan value
        assertNull(manager.bigScan(null, 10));

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
    public void testDeleteIslandIslandBoolean() {
        IslandsManager im = new IslandsManager(plugin);

        im.deleteIsland((Island)null, true);
        UUID owner = UUID.randomUUID();
        Island island = im.createIsland(location, owner);
        im.deleteIsland(island, false);
        island = im.createIsland(location, owner);
        im.deleteIsland(island, true);
        assertNull(island);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getCount()}.
     */
    @Test
    public void testGetCount() {
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.getCount() == 0);
        im.createIsland(location, UUID.randomUUID());
        assertTrue(im.getCount() == 1);
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
        when(islandCache.getIslandAt(Mockito.any(Location.class))).thenReturn(null);
        assertEquals(Optional.empty(), im.getIslandAt(new Location(world, 100000, 120, -100000)));

        // not in world
        when(iwm.inWorld(any())).thenReturn(true);
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
     * @throws Exception
     */
    @Test
    public void testGetMembers() throws Exception {
        // Mock island cache
        Set<UUID> members = new HashSet<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(islandCache.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);
        assertEquals(members, im.getMembers(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getProtectedIslandAt(org.bukkit.Location)}.
     * @throws Exception
     */
    @Test
    public void testGetProtectedIslandAt() throws Exception {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(Mockito.any(Location.class))).thenReturn(is);

        // In world
        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);

        Optional<Island> optionalIsland = Optional.ofNullable(is);
        // In world, correct island
        when(is.onIsland(Mockito.any())).thenReturn(true);
        assertEquals(optionalIsland, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(Mockito.any())).thenReturn(false);
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));

        im.setSpawn(is);
        // In world, correct island
        when(is.onIsland(Mockito.any())).thenReturn(true);
        assertEquals(optionalIsland, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(Mockito.any())).thenReturn(false);
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#getSafeHomeLocation(World, User, int)}.
     */
    @Test
    public void testGetSafeHomeLocation() {
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(0))).thenReturn(null);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(1))).thenReturn(location);
        assertEquals(location, im.getSafeHomeLocation(world, user, 0));
        // Change location so that it is not safe
        // TODO
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
        when(island.getSpawnPoint(Mockito.any())).thenReturn(location);
        // Set the spawn island
        im.setSpawn(island);
        assertEquals(location,im.getSpawnPoint(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleport(World, Player, int)}.
     */
    @Test
    public void testHomeTeleportPlayerInt() {
        when(iwm.getDefaultGameMode(world)).thenReturn(GameMode.SURVIVAL);
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(0))).thenReturn(null);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(1))).thenReturn(location);
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        im.homeTeleport(world, player, 0);
        Mockito.verify(player).teleport(location);
        Mockito.verify(player).setGameMode(GameMode.SURVIVAL);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#homeTeleport(World, Player, int)}.
     */
    @Test
    public void testHomeTeleportPlayerIntDifferentGameMode() {
        when(iwm.getDefaultGameMode(world)).thenReturn(GameMode.CREATIVE);
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(0))).thenReturn(null);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(1))).thenReturn(location);
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        im.homeTeleport(world, player, 0);
        Mockito.verify(player).teleport(location);
        Mockito.verify(player).setGameMode(GameMode.CREATIVE);

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
        when(island.onIsland(Mockito.any())).thenReturn(true);
        im.setSpawn(island);
        assertTrue(im.isAtSpawn(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#isOwner(World, UUID)}.
     * @throws Exception
     */
    @Test
    public void testIsOwner() throws Exception {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(Mockito.any())).thenReturn(is);

        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);

        assertFalse(im.isOwner(world, null));

        when(islandCache.hasIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(im.isOwner(world, UUID.randomUUID()));

        when(islandCache.hasIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        when(islandCache.get(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
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
     * @throws Exception
     */
    @Test
    public void testLocationIsOnIsland() throws Exception {
        // Mock island cache
        Island is = mock(Island.class);

        when(islandCache.getIslandAt(Mockito.any(Location.class))).thenReturn(is);

        // In world
        when(is.onIsland(Mockito.any())).thenReturn(true);

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
        when(is.onIsland(Mockito.any())).thenReturn(false);
        assertFalse(im.locationIsOnIsland(player, location));
    }

    /**
     * Test method for user is on island
     * @throws Exception
     */
    @Test
    public void testUserIsOnIsland() throws Exception {
        IslandsManager im = new IslandsManager(plugin);
        im.setIslandCache(islandCache);

        // Null user
        assertFalse(im.userIsOnIsland(world, null));


        // User is on island is determined by whether the user's location is on
        // an island that has them as a memebr (rank > 0)
        when(is.onIsland(Mockito.any())).thenReturn(true);
        Map<UUID, Integer> members = new HashMap<>();
        when(is.getMembers()).thenReturn(members);
        assertFalse(im.userIsOnIsland(world, user));

        members.put(user.getUniqueId(), RanksManager.MEMBER_RANK);
        assertTrue(im.userIsOnIsland(world, user));

        members.put(user.getUniqueId(), RanksManager.BANNED_RANK);
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
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#save(boolean)}.
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
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandsManager#clearArea(Location)}.
     */
    @Test
    public void testClearArea() {
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        Flags.REMOVE_MOBS.setSetting(world, true);
        // Default whitelist
        Set<EntityType> whitelist = new HashSet<>();
        whitelist.add(EntityType.ENDERMAN);
        whitelist.add(EntityType.WITHER);
        whitelist.add(EntityType.ZOMBIE_VILLAGER);
        whitelist.add(EntityType.PIG_ZOMBIE);
        when(iwm.getRemoveMobsWhitelist(Mockito.any())).thenReturn(whitelist);


        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        when(slime.getType()).thenReturn(EntityType.SLIME);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);
        when(cow.getType()).thenReturn(EntityType.COW);
        Wither wither = mock(Wither.class);
        when(wither.getType()).thenReturn(EntityType.WITHER);
        Creeper creeper = mock(Creeper.class);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);


        Collection<Entity> collection = new ArrayList<>();
        collection.add(player);
        collection.add(zombie);
        collection.add(cow);
        collection.add(slime);
        collection.add(wither);
        collection.add(creeper);
        when(world
                .getNearbyEntities(Mockito.any(Location.class), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble()))
        .thenReturn(collection);

        IslandsManager im = new IslandsManager(plugin);
        im.clearArea(location);

        Mockito.verify(zombie).remove();
        Mockito.verify(player, Mockito.never()).remove();
        Mockito.verify(cow, Mockito.never()).remove();
        Mockito.verify(slime, Mockito.never()).remove();
        Mockito.verify(wither, Mockito.never()).remove();
        Mockito.verify(creeper).remove();
    }

}
