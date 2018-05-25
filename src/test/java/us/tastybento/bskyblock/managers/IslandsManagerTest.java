package us.tastybento.bskyblock.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
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
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.material.TrapDoor;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.island.IslandCache;
import us.tastybento.bskyblock.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BSkyBlock.class, IslandsManager.class, Util.class, Location.class })
public class IslandsManagerTest {

    private static BSkyBlock plugin;
    private UUID uuid;
    private User user;
    private Settings s;
    private PlayersManager pm;
    private Player player;
    private static World world;
    private IslandsManager manager;
    private Block space1;
    private Block ground;
    private Block space2;
    private Location location;
    private BlockState blockState;
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // World
        world = mock(World.class);
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(s.getResetLimit()).thenReturn(3);
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
        blockState = mock(BlockState.class);
        when(ground.getState()).thenReturn(blockState);
        MaterialData md = mock(MaterialData.class);
        when(blockState.getData()).thenReturn(md);

        // Online players
        // Return a set of online players
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getOnlinePlayers()).then(new Answer<Set<Player>>() {

            @Override
            public Set<Player> answer(InvocationOnMock invocation) throws Throwable {

                return new HashSet<>();
            }

        });
        
        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandWorld()).thenReturn(world);
        when(iwm.getNetherWorld()).thenReturn(world);
        when(iwm.getEndWorld()).thenReturn(world);
        when(iwm.inWorld(any())).thenReturn(true);
        
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);
    }


    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationSafe() {
        IslandsManager manager = new IslandsManager(plugin);
        assertTrue(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationNull() {
        IslandsManager manager = new IslandsManager(plugin);
        assertFalse(manager.isSafeLocation(null));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationNonSolidGround() {
        when(ground.getType()).thenReturn(Material.WATER);        
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationSubmerged() {
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.isLiquid()).thenReturn(true);
        when(space2.isLiquid()).thenReturn(true);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationPortals() {        
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.PORTAL);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.ENDER_PORTAL);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.STONE);
        when(space1.getType()).thenReturn(Material.ENDER_PORTAL);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.ENDER_PORTAL);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse(manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testIsSafeLocationAcid() {
        when(ground.getType()).thenReturn(Material.GRASS);
        when(space1.getType()).thenReturn(Material.STATIONARY_WATER);
        when(space2.getType()).thenReturn(Material.AIR);

        when(s.getAcidDamage()).thenReturn(10);

        when(ground.isLiquid()).thenReturn(true);
        when(space1.isLiquid()).thenReturn(false);
        when(space2.isLiquid()).thenReturn(false);
        assertFalse("In acid", manager.isSafeLocation(location));
        when(ground.isLiquid()).thenReturn(false);
        when(space1.isLiquid()).thenReturn(true);
        when(space2.isLiquid()).thenReturn(false);
        assertFalse("In acid", manager.isSafeLocation(location));
        when(ground.isLiquid()).thenReturn(false);
        when(space1.isLiquid()).thenReturn(false);
        when(space2.isLiquid()).thenReturn(true);
        assertFalse("In acid", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
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
        when(ground.getType()).thenReturn(Material.STATIONARY_LAVA);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.STATIONARY_LAVA);
        when(space2.getType()).thenReturn(Material.AIR);
        assertFalse("In lava", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.AIR);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.STATIONARY_LAVA);
        assertFalse("In lava", manager.isSafeLocation(location));
    }  

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testTrapDoor() {
        when(ground.getType()).thenReturn(Material.TRAP_DOOR);

        // Open trapdoor
        TrapDoor trapDoor = mock(TrapDoor.class);
        when(trapDoor.isOpen()).thenReturn(true);
        when(blockState.getData()).thenReturn(trapDoor);

        assertFalse("Open trapdoor", manager.isSafeLocation(location));

        when(trapDoor.isOpen()).thenReturn(false);
        assertTrue("Closed trapdoor", manager.isSafeLocation(location));

        when(ground.getType()).thenReturn(Material.IRON_TRAPDOOR);
        assertTrue("Closed iron trapdoor", manager.isSafeLocation(location));
        when(trapDoor.isOpen()).thenReturn(true);
        assertFalse("Open iron trapdoor", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
     */
    @Test
    public void testBadBlocks() {
        // Fences
        Arrays.asList(Material.values()).stream().filter(m -> m.toString().contains("FENCE")).forEach(m -> {
            when(ground.getType()).thenReturn(m);
            assertFalse("Fence :" + m.toString(), manager.isSafeLocation(location));
        });
        // Signs
        when(ground.getType()).thenReturn(Material.SIGN_POST);
        assertFalse("Sign", manager.isSafeLocation(location));
        when(ground.getType()).thenReturn(Material.WALL_SIGN);
        assertFalse("Sign", manager.isSafeLocation(location));
        // Bad Blocks
        Material[] badMats = {Material.CACTUS, Material.BOAT};
        Arrays.asList(badMats).forEach(m -> {
            when(ground.getType()).thenReturn(m);
            assertFalse("Bad mat :" + m.toString(), manager.isSafeLocation(location));
        });

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isSafeLocation(org.bukkit.Location)}.
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

        when(space1.getType()).thenReturn(Material.SIGN_POST);
        when(space2.getType()).thenReturn(Material.AIR);
        assertTrue("Wall sign 1", manager.isSafeLocation(location));

        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.SIGN_POST);
        assertTrue("Wall sign 2", manager.isSafeLocation(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#IslandsManager(us.tastybento.bskyblock.BSkyBlock)}.
     */
    @Test
    public void testIslandsManager() {
        assertNotNull(new IslandsManager(plugin));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#bigScan(org.bukkit.Location, int)}.
     */
    @Test
    public void testBigScan() throws Exception {
        Settings settings = mock(Settings.class);

        when(plugin.getSettings()).thenReturn(settings);

        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandWorld()).thenReturn(world);


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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#createIsland(org.bukkit.Location)}.
     */
    @Test
    public void testCreateIslandLocation() {
        IslandsManager im = new IslandsManager(plugin);
        Island island = im.createIsland(location);
        assertNotNull(island);
        assertEquals(island.getCenter(), location);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#createIsland(org.bukkit.Location, java.util.UUID)}.
     */
    @Test
    public void testCreateIslandLocationUUID() {
        UUID owner = UUID.randomUUID();
        IslandsManager im = new IslandsManager(plugin);
        Island island = im.createIsland(location, owner);
        assertNotNull(island);
        assertEquals(location, island.getCenter());
        assertEquals(owner, island.getOwner());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#deleteIsland(us.tastybento.bskyblock.database.objects.Island, boolean)}.
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getCount()}.
     */
    @Test
    public void testGetCount() {
        IslandsManager im = new IslandsManager(plugin);
        assertTrue(im.getCount() == 0);
        im.createIsland(location, UUID.randomUUID());
        assertTrue(im.getCount() == 1);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getIsland(java.util.UUID)}.
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getIslandAt(org.bukkit.Location)}.
     * @throws Exception 
     */
    @Test
    public void testGetIslandAtLocation() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);
        when(ic.getIslandAt(Mockito.any(Location.class))).thenReturn(is);
        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        IslandsManager im = new IslandsManager(plugin);
        // In world, correct island
        Optional<Island> oi = Optional.ofNullable(is);
        assertEquals(oi, im.getIslandAt(location));

        // in world, wrong island
        when(ic.getIslandAt(Mockito.any(Location.class))).thenReturn(null);
        assertEquals(Optional.empty(), im.getIslandAt(new Location(world, 100000, 120, -100000)));

        // not in world
        when(iwm.inWorld(any())).thenReturn(true);
        assertEquals(Optional.empty(), im.getIslandAt(new Location(world, 100000, 120, -100000)));
        assertEquals(Optional.empty(), im.getIslandAt(location));


    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getIslandLocation(java.util.UUID)}.
     */
    @Test
    public void testGetIslandLocation() {
        IslandsManager im = new IslandsManager(plugin);
        im.createIsland(location, uuid);
        assertEquals(location, im.getIslandLocation(world, uuid));
        assertNull(im.getIslandLocation(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getLast()}.
     */
    @Test
    public void testGetLast() {
        IslandsManager im = new IslandsManager(plugin);
        im.setLast(location);
        assertEquals(location, im.getLast(world));
        assertNull(im.getLast(null));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getMembers(java.util.UUID)}.
     * @throws Exception 
     */
    @Test
    public void testGetMembers() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Set<UUID> members = new HashSet<>();
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        members.add(UUID.randomUUID());
        when(ic.getMembers(Mockito.any(), Mockito.any())).thenReturn(members);
        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);      
        IslandsManager im = new IslandsManager(plugin);
        assertEquals(members, im.getMembers(world, UUID.randomUUID()));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getProtectedIslandAt(org.bukkit.Location)}.
     * @throws Exception 
     */
    @Test
    public void testGetProtectedIslandAt() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);

        when(ic.getIslandAt(Mockito.any(Location.class))).thenReturn(is);

        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        // In world
        IslandsManager im = new IslandsManager(plugin);
        Optional<Island> oi = Optional.ofNullable(is);
        // In world, correct island
        when(is.onIsland(Mockito.any())).thenReturn(true);
        assertEquals(oi, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(Mockito.any())).thenReturn(false);        
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));

        im.setSpawn(is);
        // In world, correct island
        when(is.onIsland(Mockito.any())).thenReturn(true);
        assertEquals(oi, im.getProtectedIslandAt(location));

        // Not in protected space
        when(is.onIsland(Mockito.any())).thenReturn(false);        
        assertEquals(Optional.empty(), im.getProtectedIslandAt(location));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getSafeHomeLocation(java.util.UUID, int)}.
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getSpawnPoint()}.
     */
    @Test
    public void testGetSpawnPoint() {
        IslandsManager im = new IslandsManager(plugin);
        assertNull(im.getSpawnPoint(world));
        // Create a spawn island for this world
        Island island = mock(Island.class);
        when(island.getWorld()).thenReturn(world);
        // Make a spawn position on the island
        when(island.getSpawnPoint()).thenReturn(location);
        // Set the spawn island
        im.setSpawn(island);
        assertEquals(location,im.getSpawnPoint(world));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#homeTeleport(org.bukkit.entity.Player, int)}.
     */
    @Test
    public void testHomeTeleportPlayerInt() {
        IslandsManager im = new IslandsManager(plugin);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(0))).thenReturn(null);
        when(pm.getHomeLocation(Mockito.any(), Mockito.any(User.class), Mockito.eq(1))).thenReturn(location);
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        im.homeTeleport(world, player, 0);
        Mockito.verify(player).teleport(location);
        Mockito.verify(player).setGameMode(GameMode.SURVIVAL);

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isAtSpawn(org.bukkit.Location)}.
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isIsland(org.bukkit.Location)}.
     * @throws Exception 
     */
    @Test
    public void testIsIsland() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);

        when(ic.getIslandAt(Mockito.any())).thenReturn(is);

        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        // In world
        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.isIsland(null));
        im.createIsland(location);
        assertTrue(im.isIsland(location));

        // No island in cache
        when(ic.getIslandAt(Mockito.any())).thenReturn(null);

        // Use own generator
        when(s.isUseOwnGenerator()).thenReturn(true);
        assertFalse(im.isIsland(location));

        when(s.isUseOwnGenerator()).thenReturn(false);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(10);
        when(location.getBlockZ()).thenReturn(10);       
        when(location.getBlock()).thenReturn(space1);

        PowerMockito.whenNew(Location.class).withAnyArguments().thenReturn(location);

        when(location.getBlock()).thenReturn(space1);

        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(ground);

        // Liquid
        when(space1.isEmpty()).thenReturn(false);
        when(space1.isLiquid()).thenReturn(true);
        assertTrue(im.isIsland(location));

        // Empty space        
        when(space1.isEmpty()).thenReturn(true);
        when(space1.isLiquid()).thenReturn(false);
        assertTrue(im.isIsland(location));

    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#getClosestIsland(org.bukkit.Location)}.
     * @throws Exception 
     */
    @Test
    public void testGetClosestIsland() throws Exception {
        when(iwm.getIslandDistance(world)).thenReturn(100);
        when(iwm.getIslandXOffset(world)).thenReturn(0);
        when(iwm.getIslandZOffset(world)).thenReturn(0);
        when(iwm.getIslandHeight(world)).thenReturn(120);
        IslandsManager im = new IslandsManager(plugin);
        when(location.getBlockX()).thenReturn(456);
        when(location.getBlockZ()).thenReturn(456);
        Location l = im.getClosestIsland(location);
        assertEquals(500, l.getBlockX());
        assertEquals(500, l.getBlockZ());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#isOwner(java.util.UUID)}.
     * @throws Exception 
     */
    @Test
    public void testIsOwner() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);

        when(ic.getIslandAt(Mockito.any())).thenReturn(is);

        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.isOwner(world, null));

        when(ic.hasIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        assertFalse(im.isOwner(world, UUID.randomUUID()));

        when(ic.hasIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        when(ic.get(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#load()}.
     */
    @Test
    public void testLoad() {
        IslandsManager im = new IslandsManager(plugin);
        im.load();
        
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#locationIsOnIsland(org.bukkit.entity.Player, org.bukkit.Location)}.
     * @throws Exception 
     */
    @Test
    public void testLocationIsOnIsland() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);

        when(ic.getIslandAt(Mockito.any(Location.class))).thenReturn(is);

        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        // In world
        when(is.onIsland(Mockito.any())).thenReturn(true);
        
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        when(is.getMemberSet()).thenReturn(members.build());

        when(player.getUniqueId()).thenReturn(uuid);
        
        IslandsManager im = new IslandsManager(plugin);
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
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#userIsOnIsland(us.tastybento.bskyblock.api.user.User)}.
     * @throws Exception 
     */
    @Test
    public void testUserIsOnIsland() throws Exception {
        // Mock island cache
        IslandCache ic = mock(IslandCache.class);
        Island is = mock(Island.class);

        when(ic.get(Mockito.any(), Mockito.any(UUID.class))).thenReturn(is);

        PowerMockito.whenNew(IslandCache.class).withAnyArguments().thenReturn(ic);

        IslandsManager im = new IslandsManager(plugin);
        assertFalse(im.userIsOnIsland(world, null));
        
        when(is.onIsland(Mockito.any())).thenReturn(false);
        assertFalse(im.userIsOnIsland(world, user));
        
        when(is.onIsland(Mockito.any())).thenReturn(true);
        assertTrue(im.userIsOnIsland(world, user));       
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#removeMobs(org.bukkit.Location)}.
     */
    @Test
    public void testRemoveMobs() {
        IslandsManager im = new IslandsManager(plugin);
        im.removeMobs(location);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#removePlayer(java.util.UUID)}.
     */
    @Test
    public void testRemovePlayer() {
        IslandsManager im = new IslandsManager(plugin);
        im.removePlayer(world, uuid);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#removePlayersFromIsland(us.tastybento.bskyblock.database.objects.Island)}.
     */
    @Test
    public void testRemovePlayersFromIsland() {
        IslandsManager im = new IslandsManager(plugin);
        Island is = mock(Island.class);
        im.removePlayersFromIsland(is);
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#save(boolean)}.
     */
    @Test
    public void testSave() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#setIslandName(java.util.UUID, java.lang.String)}.
     */
    @Test
    public void testSetIslandName() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#setJoinTeam(us.tastybento.bskyblock.database.objects.Island, java.util.UUID)}.
     */
    @Test
    public void testSetJoinTeam() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#setLast(org.bukkit.Location)}.
     */
    @Test
    public void testSetLast() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#setLeaveTeam(java.util.UUID)}.
     */
    @Test
    public void testSetLeaveTeam() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.managers.IslandsManager#shutdown()}.
     */
    @Test
    public void testShutdown() {
        //fail("Not yet implemented"); // TODO
    }

}
