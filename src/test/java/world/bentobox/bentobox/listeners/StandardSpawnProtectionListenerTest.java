package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@Ignore("Needs update for PaperAPI")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class , ServerBuildInfo.class})
public class StandardSpawnProtectionListenerTest extends AbstractCommonSetup {

    @Mock
    private PlayersManager pm;
    @Mock
    private World nether;
    @Mock
    private World end;
    @Mock
    private Block block;

    private StandardSpawnProtectionListener ssp;
    @Mock
    private BlockState blockState;
    @Mock
    private Location spawnLocation;
    @Mock
    private WorldSettings ws;

    /**
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();
        // Worlds
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(nether.getEnvironment()).thenReturn(World.Environment.NETHER);
        when(end.getEnvironment()).thenReturn(World.Environment.THE_END);

        when(world.getSpawnLocation()).thenReturn(spawnLocation);
        when(nether.getSpawnLocation()).thenReturn(spawnLocation);
        when(end.getSpawnLocation()).thenReturn(spawnLocation);
        // IWM
        // Standard nether and end
        when(iwm.isNetherIslands(any())).thenReturn(false);
        when(iwm.isEndIslands(any())).thenReturn(false);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.getNetherSpawnRadius(any())).thenReturn(25);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Util
        when(Util.getWorld(any())).thenReturn(world);
        // Location
        when(location.toVector()).thenReturn(new Vector(5,5,5));
        when(location.getWorld()).thenReturn(nether);
        when(spawnLocation.toVector()).thenReturn(new Vector(0,0,0));
        when(spawnLocation.getWorld()).thenReturn(nether);
        // Player
        when(mockPlayer.getWorld()).thenReturn(nether);
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        User.getInstance(mockPlayer);
        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(Mockito.any(), Mockito.any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        // Block
        when(block.getLocation()).thenReturn(location);

        // Util translate color codes (used in user translate methods)
        when(Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Set up class
        ssp = new StandardSpawnProtectionListener(plugin);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceDisallowed() {
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertTrue(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceDisallowedNoProtection() {
        when(iwm.isNetherIslands(any())).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceAllowed() {
        when(mockPlayer.isOp()).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceAllowedOutsideSpawn() {
        when(iwm.getNetherSpawnRadius(any())).thenReturn(1);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceAllowedWrongWorld() {
        when(location.getWorld()).thenReturn(world);
        when(mockPlayer.getWorld()).thenReturn(world);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceAllowedNetherIslandWorlds() {
        when(iwm.isNetherIslands(any())).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockPlace(org.bukkit.event.block.BlockPlaceEvent)}.
     */
    @Test
    public void testOnBlockPlaceAllowedEndIslandWorlds() {
        when(location.getWorld()).thenReturn(end);
        when(mockPlayer.getWorld()).thenReturn(end);
        when(spawnLocation.getWorld()).thenReturn(end);
        when(iwm.isEndIslands(any())).thenReturn(true);
        BlockPlaceEvent e = new BlockPlaceEvent(block, blockState, null, null, mockPlayer, true, EquipmentSlot.HAND);
        ssp.onBlockPlace(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakDisallowed() {
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        ssp.onBlockBreak(e);
        assertTrue(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakDisallowedNoProtection() {
        when(ws.isMakeNetherPortals()).thenReturn(true);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        ssp.onBlockBreak(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBlockBreak(org.bukkit.event.block.BlockBreakEvent)}.
     */
    @Test
    public void testOnBlockBreakAllowed() {
        when(mockPlayer.isOp()).thenReturn(true);
        BlockBreakEvent e = new BlockBreakEvent(block, mockPlayer);
        ssp.onBlockBreak(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosion() {
        List<Block> blockList = new ArrayList<>();
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        // Make some inside and outside spawn
        when(location.toVector()).thenReturn(new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(10000,0,0));
        EntityExplodeEvent e = getExplodeEvent(mockPlayer, location, blockList);
        ssp.onExplosion(e);
        // 4 blocks inside the spawn should be removed, leaving one
        assertEquals(1, blockList.size());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNoProtection() {
        when(ws.isMakeNetherPortals()).thenReturn(true);
        List<Block> blockList = new ArrayList<>();
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        blockList.add(block);
        // Make some inside and outside spawn
        when(location.toVector()).thenReturn(new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(0,0,0),
                new Vector(10000,0,0));
        EntityExplodeEvent e = getExplodeEvent(mockPlayer, location, blockList);
        ssp.onExplosion(e);
        // No blocks should be removed
        assertEquals(5, blockList.size());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyDisallowed() {
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(mockPlayer, block, block, BlockFace.DOWN, null, null,
                null);
        ssp.onBucketEmpty(e);
        assertTrue(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyDisallowedNoProtection() {
        when(ws.isMakeNetherPortals()).thenReturn(true);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(mockPlayer, block, block, BlockFace.DOWN, null, null,
                null);
        ssp.onBucketEmpty(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.StandardSpawnProtectionListener#onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent)}.
     */
    @Test
    public void testOnBucketEmptyAllowed() {
        when(mockPlayer.isOp()).thenReturn(true);
        PlayerBucketEmptyEvent e = new PlayerBucketEmptyEvent(mockPlayer, block, block, BlockFace.DOWN, null, null,
                null);
        ssp.onBucketEmpty(e);
        assertFalse(e.isCancelled());
        checkSpigotMessage("protection.spawn-protected", 0);
    }

}
