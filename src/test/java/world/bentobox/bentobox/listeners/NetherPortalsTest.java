/**
 *
 */
package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class })
public class NetherPortalsTest {

    private BentoBox plugin;
    private IslandsManager im;
    private PlayersManager pm;
    private IslandWorldManager iwm;
    private World world;
    private World nether;
    private World end;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // island world mgr
        iwm = mock(IslandWorldManager.class);
        world = mock(World.class);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        nether = mock(World.class);
        when(nether.getEnvironment()).thenReturn(Environment.NETHER);
        when(nether.getSpawnLocation()).thenReturn(mock(Location.class));
        end = mock(World.class);
        when(end.getEnvironment()).thenReturn(Environment.THE_END);
        when(iwm.getEndWorld(Mockito.any())).thenReturn(end);
        when(iwm.getIslandWorld(Mockito.any())).thenReturn(world);
        when(iwm.getNetherWorld(Mockito.any())).thenReturn(nether);
        when(iwm.inWorld(any())).thenReturn(true);
        when(iwm.getNetherSpawnRadius(Mockito.any())).thenReturn(100);
        when(plugin.getIWM()).thenReturn(iwm);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Set up spawn
        Location netherSpawn = mock(Location.class);
        when(netherSpawn.toVector()).thenReturn(new Vector(0,0,0));
        when(nether.getSpawnLocation()).thenReturn(netherSpawn);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        User user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        UUID notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(), Mockito.any())).thenReturn(true);
        when(im.getTeamLeader(Mockito.any(), Mockito.any())).thenReturn(uuid);
        Optional<Island> optionalIsland = Optional.empty();
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);
        when(plugin.getIslands()).thenReturn(im);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Normally in world
        Util.setPlugin(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#NetherPortals(world.bentobox.bentobox.BentoBox)}.
     */
    @Test
    public void testNetherPortals() {
        assertNotNull(new NetherPortals(plugin));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakNoAction() {
        NetherPortals np = new NetherPortals(plugin);
        Block block = mock(Block.class);
        Player player = mock(Player.class);
        // Ops can do anything
        when(player.isOp()).thenReturn(true);
        BlockBreakEvent e = new BlockBreakEvent(block, player);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
        // not op, but not in right world
        when(player.isOp()).thenReturn(false);
        World w = mock(World.class);
        when(w.getEnvironment()).thenReturn(Environment.NORMAL);
        when(player.getWorld()).thenReturn(w);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
        // not op, island world
        when(player.getWorld()).thenReturn(world);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
        // Nether, but not standard nether
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(Mockito.any())).thenReturn(true);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
        // End, but not standard end
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isEndIslands(Mockito.any())).thenReturn(true);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakActionAwayFromSpawn() {
        NetherPortals np = new NetherPortals(plugin);
        Block block = mock(Block.class);
        Location far = mock(Location.class);
        when(far.toVector()).thenReturn(new Vector(10000, 56, 2000));
        when(far.getWorld()).thenReturn(nether);
        when(block.getLocation()).thenReturn(far);
        Player player = mock(Player.class);
        // Standard nether
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        np.onBlockBreak(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakActionAtSpawn() {
        NetherPortals np = new NetherPortals(plugin);
        Block block = mock(Block.class);
        Location near = mock(Location.class);
        when(near.toVector()).thenReturn(new Vector(0, 56, 0));
        when(near.getWorld()).thenReturn(nether);
        when(block.getLocation()).thenReturn(near);
        Player player = mock(Player.class);
        // Standard nether
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);

        BlockBreakEvent e = new BlockBreakEvent(block, player);
        np.onBlockBreak(e);
        Mockito.verify(block).getLocation();
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmpty() {
        NetherPortals np = new NetherPortals(plugin);
        Block block = mock(Block.class);
        Location near = mock(Location.class);
        when(near.toVector()).thenReturn(new Vector(0, 56, 0));
        when(near.getWorld()).thenReturn(nether);
        when(block.getLocation()).thenReturn(near);
        Player player = mock(Player.class);
        // Standard nether
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);

        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(player, block, null, null, null);
        np.onBucketEmpty(e);
        Mockito.verify(block).getLocation();
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalNotEnd() {
        NetherPortals np = new NetherPortals(plugin);
        // Wrong cause
        PlayerPortalEvent e = new PlayerPortalEvent(null, null, null, null, TeleportCause.CHORUS_FRUIT);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalWrongWorld() {
        NetherPortals np = new NetherPortals(plugin);
        Location loc = mock(Location.class);

        // Right cause, end exists, wrong world
        when(loc.getWorld()).thenReturn(mock(World.class));
        when(iwm.inWorld(any())).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(null, loc, null, null, TeleportCause.END_PORTAL);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onEndIslandPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnEndIslandPortalHome() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from end
        when(from.getWorld()).thenReturn(end);

        // Player has no island
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        // Right cause, end exists, right world
        PlayerPortalEvent e = new PlayerPortalEvent(player, from, null, null, TeleportCause.END_PORTAL);
        when(iwm.isEndGenerate(world)).thenReturn(true);
        np.onEndIslandPortal(e);
        assertFalse(e.isCancelled());
        // Give player an island
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        np.onEndIslandPortal(e);
        assertTrue(e.isCancelled());
        Mockito.verify(im).homeTeleport(Mockito.any(), Mockito.eq(player));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onEntityPortal(org.bukkit.event.entity.EntityPortalEvent)}.
     */
    @Test
    public void testOnEntityPortal() {
        NetherPortals np = new NetherPortals(plugin);
        Entity ent = mock(Entity.class);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        // Not in world
        when(iwm.inWorld(any())).thenReturn(false);
        EntityPortalEvent e = new EntityPortalEvent(ent, from, null, null);
        np.onEntityPortal(e);
        assertFalse(e.isCancelled());
        // In world
        when(iwm.inWorld(any())).thenReturn(true);
        e = new EntityPortalEvent(ent, from, null, null);
        np.onEntityPortal(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotInWorld() {
        NetherPortals np = new NetherPortals(plugin);
        // Null entity
        Entity en = null;
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        // Not in world
        when(iwm.inWorld(any())).thenReturn(false);

        EntityExplodeEvent e = new EntityExplodeEvent(en, from, affectedBlocks, 0);

        assertFalse(np.onExplosion(e));
    }


    @Test
    public void testOnExplosionInWorldNotNetherOrEnd() {
        NetherPortals np = new NetherPortals(plugin);
        // Null entity
        Entity en = null;
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        EntityExplodeEvent e = new EntityExplodeEvent(en, from, affectedBlocks, 0);
        assertFalse(np.onExplosion(e));
    }

    @Test
    public void testOnExplosionIslands() {
        NetherPortals np = new NetherPortals(plugin);
        // Null entity
        Entity en = null;
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);

        // In world, in nether, nether islands
        when(from.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(true);
        EntityExplodeEvent e = new EntityExplodeEvent(en, from, affectedBlocks, 0);
        assertFalse(np.onExplosion(e));

        // In world, in end, end islands
        when(from.getWorld()).thenReturn(end);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isEndIslands(world)).thenReturn(true);
        assertFalse(np.onExplosion(e));
    }

    @Test
    public void testOnExplosionNullEntity() {
        NetherPortals np = new NetherPortals(plugin);
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);
        // In world, in nether, nether islands
        when(from.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        EntityExplodeEvent e = new EntityExplodeEvent(null, from, affectedBlocks, 0);
        assertFalse(np.onExplosion(e));
    }

    @Test
    public void testOnExplosionAwayFromSpawn() {
        NetherPortals np = new NetherPortals(plugin);
        // Null entity
        Entity en = mock(Entity.class);
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);

        // In world, in nether, standard nether, null entity
        when(from.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);

        EntityExplodeEvent e = new EntityExplodeEvent(en, from, affectedBlocks, 0);
        // Real entity, away from spawn
        assertTrue(np.onExplosion(e));
        // Block should still exist because it is away from spawn
        assertFalse(e.blockList().isEmpty());
    }

    @Test
    public void testOnExplosion() {
        NetherPortals np = new NetherPortals(plugin);
        // Null entity
        Entity en = null;
        // Entity, Location, list of Blocks, yield
        Block block = mock(Block.class);
        Location blockLoc = mock(Location.class);
        when(blockLoc.toVector()).thenReturn(new Vector(10000,0,10000));
        when(block.getLocation()).thenReturn(blockLoc);
        when(blockLoc.getWorld()).thenReturn(nether);
        // One block will be blown up by the wither
        List<Block> affectedBlocks = new ArrayList<>();
        affectedBlocks.add(block);

        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        // In world, in nether, standard nether, null entity
        when(from.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);


        // Real entity, next to  spawn
        en = mock(Entity.class);
        when(blockLoc.toVector()).thenReturn(new Vector(0,0,0));
        EntityExplodeEvent e = new EntityExplodeEvent(en, from, affectedBlocks, 0);
        // Block exists before
        assertFalse(e.blockList().isEmpty());
        assertTrue(np.onExplosion(e));
        // Block removed
        assertTrue(e.blockList().isEmpty());

    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalNotPortal() {
        NetherPortals np = new NetherPortals(plugin);
        PlayerPortalEvent e = new PlayerPortalEvent(null, null, null, null, TeleportCause.COMMAND);
        assertFalse(np.onNetherPortal(e));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalWrongWorld() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        when(from.getWorld()).thenReturn(mock(World.class));
        when(iwm.inWorld(any())).thenReturn(false);
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        assertFalse(np.onNetherPortal(e));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIsland() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandWithSpawnDefined() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);

        Island island = mock(Island.class);
        Location spawnLoc = mock(Location.class);
        when(island.getSpawnPoint(Mockito.any())).thenReturn(spawnLoc);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherIslandWithNoSpawnDefined() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);

        Island island = mock(Island.class);
        when(island.getSpawnPoint(Mockito.any())).thenReturn(null);
        Optional<Island> optionalIsland = Optional.of(island);
        // Island exists at location
        when(im.getIslandAt(Mockito.any())).thenReturn(optionalIsland);


        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If nether islands, then to = from but in nether
        Mockito.verify(from).toVector();
        // Do not go to spawn
        Mockito.verify(nether, Mockito.never()).getSpawnLocation();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromWorldToNetherStandard() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from world to nether
        when(from.getWorld()).thenReturn(world);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If regular nether, then to = spawn point of nether
        Mockito.verify(from, Mockito.never()).toVector();
        Mockito.verify(nether).getSpawnLocation();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     * @throws Exception
     */
    @Test
    public void testOnNetherPortalFromNetherStandard() throws Exception {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(nether);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());

        PlayerPortalEvent e = new PlayerPortalEvent(p, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands inactive
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isNetherGenerate(world)).thenReturn(true);

        // Player should be teleported to their island
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If regular nether, then to = island location
        Mockito.verify(from, Mockito.never()).toVector();
        Mockito.verify(im).getIslandLocation(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onNetherPortal(org.bukkit.event.player.PlayerPortalEvent)}.
     */
    @Test
    public void testOnNetherPortalFromNetherIsland() {
        NetherPortals np = new NetherPortals(plugin);
        Location from = mock(Location.class);
        // Teleport from nether to world
        when(from.getWorld()).thenReturn(nether);
        when(from.toVector()).thenReturn(new Vector(1,2,3));
        PlayerPortalEvent e = new PlayerPortalEvent(null, from, null, null, TeleportCause.NETHER_PORTAL);
        // Nether islands active
        when(iwm.isNetherIslands(world)).thenReturn(true);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        assertTrue(np.onNetherPortal(e));
        // Verify
        assertTrue(e.isCancelled());
        // If regular nether, then to = island location
        Mockito.verify(from).toVector();
        Mockito.verify(im, Mockito.never()).getIslandLocation(Mockito.any(), Mockito.any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onPlayerBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnPlayerBlockPlace() {
        NetherPortals np = new NetherPortals(plugin);
        Block block = mock(Block.class);
        Location near = mock(Location.class);
        when(near.toVector()).thenReturn(new Vector(0, 56, 0));
        when(near.getWorld()).thenReturn(nether);
        when(block.getLocation()).thenReturn(near);
        Player player = mock(Player.class);
        // Standard nether
        when(player.getWorld()).thenReturn(nether);
        when(iwm.isNetherIslands(world)).thenReturn(false);
        when(iwm.isNetherGenerate(world)).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, null, block, null, player, false, null);
        np.onPlayerBlockPlace(e);
        Mockito.verify(block).getLocation();
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.NetherPortals#onTreeGrow(org.bukkit.event.world.StructureGrowEvent)}.
     */
    @Test
    public void testOnTreeGrow() {
        NetherPortals np = new NetherPortals(plugin);
        Location loc = mock(Location.class);
        // Wrong world to start
        when(loc.getWorld()).thenReturn(world);
        BlockState log = mock(BlockState.class);
        when(log.getType()).thenReturn(Material.OAK_LOG);
        BlockState log2 = mock(BlockState.class);
        when(log2.getType()).thenReturn(Material.ACACIA_LOG);
        BlockState leaves = mock(BlockState.class);
        when(leaves.getType()).thenReturn(Material.OAK_LEAVES);
        BlockState leaves2 = mock(BlockState.class);
        when(leaves2.getType()).thenReturn(Material.OAK_LEAVES);
        List<BlockState> blocks = new ArrayList<>();
        blocks.add(log);
        blocks.add(log2);
        blocks.add(leaves);
        blocks.add(leaves2);
        StructureGrowEvent e = new StructureGrowEvent(loc, TreeType.ACACIA, false, null, blocks);
        // No nether trees
        when(iwm.isNetherTrees(world)).thenReturn(false);
        assertFalse(np.onTreeGrow(e));
        // nether trees, wrong world
        e = new StructureGrowEvent(loc, TreeType.ACACIA, false, null, blocks);
        when(iwm.isNetherTrees(world)).thenReturn(true);
        assertFalse(np.onTreeGrow(e));
        // Make the world nether
        when(iwm.isNetherTrees(nether)).thenReturn(true);
        when(loc.getWorld()).thenReturn(nether);
        e = new StructureGrowEvent(loc, TreeType.ACACIA, false, null, blocks);
        /*
         * Temporary
         * TODO: Fix for 1.13
        assertTrue(np.onTreeGrow(e));
        Mockito.verify(log).setType(Material.GRAVEL);
        Mockito.verify(log2).setType(Material.GRAVEL);
        Mockito.verify(leaves).setType(Material.GLOWSTONE);
        Mockito.verify(leaves2).setType(Material.GLOWSTONE);
         */
    }

}
