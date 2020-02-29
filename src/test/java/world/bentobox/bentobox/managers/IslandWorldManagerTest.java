package world.bentobox.bentobox.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.Nullable;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class, Location.class })
public class IslandWorldManagerTest {

    @Mock
    private BentoBox plugin;

    private IslandWorldManager iwm;

    @Mock
    private Location location;

    @Mock
    private World world;

    @Mock
    private WorldSettings ws;

    @Mock
    private @Nullable World netherWorld;

    @Mock
    private @Nullable World endWorld;

    @Mock
    private GameModeAddon gm;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        iwm = new IslandWorldManager(plugin);
        // World
        when(world.getName()).thenReturn("test-world");
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(world.getMaxHeight()).thenReturn(256);
        when(location.getWorld()).thenReturn(world);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        // No flags right now
        when(fm.getFlags()).thenReturn(new ArrayList<>());
        when(plugin.getFlagsManager()).thenReturn(fm);
        // Gamemode
        when(ws.getFriendlyName()).thenReturn("friendly");
        when(gm.getWorldSettings()).thenReturn(ws);
        when(gm.getOverWorld()).thenReturn(world);
        when(gm.getNetherWorld()).thenReturn(netherWorld);
        when(gm.getEndWorld()).thenReturn(endWorld);
        iwm.addGameMode(gm);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#registerWorldsToMultiverse()}.
     */
    @Test
    public void testRegisterWorldsToMultiverse() {
        iwm.registerWorldsToMultiverse();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.Location)}.
     */
    @Test
    public void testInWorldLocation() {
        assertTrue(iwm.inWorld(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.Location)}.
     */
    @Test
    public void testInWorldLocationNull() {
        assertFalse(iwm.inWorld((Location)null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.World)}.
     */
    @Test
    public void testInWorldWorld() {
        assertTrue(iwm.inWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.World)}.
     */
    @Test
    public void testInWorldWorldNull() {
        assertFalse(iwm.inWorld((World)null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorlds()}.
     */
    @Test
    public void testGetWorlds() {
        assertTrue(iwm.getWorlds().contains(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorlds()}.
     */
    @Test
    public void testGetOverWorlds() {
        assertTrue(iwm.getOverWorlds().contains(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorldNames()}.
     */
    @Test
    public void testGetOverWorldNames() {
        Map<String, String> map = iwm.getOverWorldNames();
        map.forEach((k,v) -> {
            assertEquals("test-world", k);
            assertEquals("friendly", v);
        });
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isKnownFriendlyWorldName(java.lang.String)}.
     */
    @Test
    public void testIsKnownFriendlyWorldName() {
        assertTrue(iwm.isKnownFriendlyWorldName("friendly"));
        assertFalse(iwm.isKnownFriendlyWorldName("not-friendly"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#addGameMode(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testAddGameMode() {
        // Add a second one
        // Gamemode
        GameModeAddon gm = mock(GameModeAddon.class);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.getFriendlyName()).thenReturn("friendly2");
        when(gm.getWorldSettings()).thenReturn(ws);
        when(gm.getOverWorld()).thenReturn(world);

        iwm.addGameMode(gm);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorldSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetWorldSettings() {
        assertEquals(ws, iwm.getWorldSettings(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorldSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetWorldSettingsNull() {
        assertNull(iwm.getWorldSettings(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorld(java.lang.String)}.
     */
    @Test
    public void testGetOverWorld() {
        assertEquals(world, iwm.getOverWorld("friendly"));
        assertNull(iwm.getOverWorld("not-friendly"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandDistance(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandDistance() {
        assertEquals(0, iwm.getIslandDistance(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeight() {
        assertEquals(0, iwm.getIslandHeight(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeightOverMax() {
        when(ws.getIslandHeight()).thenReturn(500);
        assertEquals(255, iwm.getIslandHeight(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeightSubZero() {
        when(ws.getIslandHeight()).thenReturn(-50);
        assertEquals(0, iwm.getIslandHeight(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandProtectionRange(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandProtectionRange() {
        assertEquals(0, iwm.getIslandProtectionRange(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandStartX(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandStartX() {
        assertEquals(0, iwm.getIslandStartX(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandStartZ(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandStartZ() {
        assertEquals(0, iwm.getIslandStartZ(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandXOffset(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandXOffset() {
        assertEquals(0, iwm.getIslandXOffset(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandZOffset(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandZOffset() {
        assertEquals(0, iwm.getIslandZOffset(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxIslands(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxIslands() {
        assertEquals(0, iwm.getMaxIslands(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherSpawnRadius(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherSpawnRadius() {
        assertEquals(0, iwm.getNetherSpawnRadius(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getSeaHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetSeaHeight() {
        assertEquals(0, iwm.getSeaHeight(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorldName(org.bukkit.World)}.
     */
    @Test
    public void testGetWorldName() {
        when(ws.getWorldName()).thenReturn("test-world");
        assertEquals("test-world", iwm.getWorldName(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEndGenerate(org.bukkit.World)}.
     */
    @Test
    public void testIsEndGenerate() {
        assertFalse(iwm.isEndGenerate(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEndIslands(org.bukkit.World)}.
     */
    @Test
    public void testIsEndIslands() {
        assertFalse(iwm.isEndIslands(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNetherGenerate(org.bukkit.World)}.
     */
    @Test
    public void testIsNetherGenerate() {
        assertFalse(iwm.isNetherGenerate(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNetherIslands(org.bukkit.World)}.
     */
    @Test
    public void testIsNetherIslands() {
        assertFalse(iwm.isNetherIslands(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNether(org.bukkit.World)}.
     */
    @Test
    public void testIsNether() {
        assertFalse(iwm.isNether(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isIslandNether(org.bukkit.World)}.
     */
    @Test
    public void testIsIslandNether() {
        assertFalse(iwm.isIslandNether(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEnd(org.bukkit.World)}.
     */
    @Test
    public void testIsEnd() {
        assertFalse(iwm.isEnd(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isIslandEnd(org.bukkit.World)}.
     */
    @Test
    public void testIsIslandEnd() {
        assertFalse(iwm.isIslandEnd(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherWorld() {
        assertEquals(netherWorld, iwm.getNetherWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherWorldNull() {
        assertNull(iwm.getNetherWorld(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getEndWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetEndWorld() {
        assertEquals(endWorld, iwm.getEndWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getEndWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetEndWorldNull() {
        assertNull(iwm.getEndWorld(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isDragonSpawn(org.bukkit.World)}.
     */
    @Test
    public void testIsDragonSpawn() {
        assertTrue(iwm.isDragonSpawn(endWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isDragonSpawn(org.bukkit.World)}.
     */
    @Test
    public void testIsDragonSpawnNull() {
        assertTrue(iwm.isDragonSpawn(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getFriendlyNames()}.
     */
    @Test
    public void testGetFriendlyNames() {
        // Add a second one
        // Gamemode
        GameModeAddon gm2 = mock(GameModeAddon.class);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.getFriendlyName()).thenReturn("fri2");
        when(gm2.getWorldSettings()).thenReturn(ws);
        when(gm2.getOverWorld()).thenReturn(mock(World.class));
        iwm.addGameMode(gm2);
        // String can be in any order
        String result = iwm.getFriendlyNames();
        assertTrue(result.contains("fri2"));
        assertTrue(result.contains("friendly"));
        assertTrue(result.contains(", "));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandWorld(java.lang.String)}.
     */
    @Test
    public void testGetIslandWorld() {
        assertEquals(world, iwm.getIslandWorld("friendly"));
        assertNull(iwm.getIslandWorld("not-friendly"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxTeamSize(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxTeamSize() {
        assertEquals(0, iwm.getMaxTeamSize(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxHomes(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxHomes() {
        assertEquals(0, iwm.getMaxHomes(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getFriendlyName(org.bukkit.World)}.
     */
    @Test
    public void testGetFriendlyName() {
        assertEquals("friendly", iwm.getFriendlyName(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getPermissionPrefix(org.bukkit.World)}.
     */
    @Test
    public void testGetPermissionPrefix() {
        when(ws.getPermissionPrefix()).thenReturn("bsky");
        assertEquals("bsky.", iwm.getPermissionPrefix(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIvSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetIvSettings() {
        List<String> list = Collections.singletonList("blah");
        when(ws.getIvSettings()).thenReturn(list);
        assertEquals(list, iwm.getIvSettings(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isWorldFlag(org.bukkit.World, world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testIsWorldFlag() {
        // TODO
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDefaultGameMode(org.bukkit.World)}.
     */
    @Test
    public void testGetDefaultGameMode() {
        when(ws.getDefaultGameMode()).thenReturn(GameMode.ADVENTURE);
        assertEquals(GameMode.ADVENTURE, iwm.getDefaultGameMode(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getRemoveMobsWhitelist(org.bukkit.World)}.
     */
    @Test
    public void testGetRemoveMobsWhitelist() {
        Set<EntityType> set = new HashSet<>();
        when(ws.getRemoveMobsWhitelist()).thenReturn(set);
        assertEquals(set, iwm.getRemoveMobsWhitelist(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetMoney(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetMoney() {
        assertFalse(iwm.isOnJoinResetMoney(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetInventory(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetInventory() {
        assertFalse(iwm.isOnJoinResetInventory(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetEnderChest(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetEnderChest() {
        assertFalse(iwm.isOnJoinResetEnderChest(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetMoney(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetMoney() {
        assertFalse(iwm.isOnLeaveResetMoney(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetInventory(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetInventory() {
        assertFalse(iwm.isOnLeaveResetInventory(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetEnderChest(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetEnderChest() {
        assertFalse(iwm.isOnLeaveResetEnderChest(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDataFolder(org.bukkit.World)}.
     */
    @Test
    public void testGetDataFolder() {
        File dataFolder = mock(File.class);
        when(gm.getDataFolder()).thenReturn(dataFolder);
        assertEquals(dataFolder, iwm.getDataFolder(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getAddon(org.bukkit.World)}.
     */
    @Test
    public void testGetAddon() {
        assertEquals(gm, iwm.getAddon(world).get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getAddon(org.bukkit.World)}.
     */
    @Test
    public void testGetAddonNull() {
        assertEquals(Optional.empty(), iwm.getAddon(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDefaultIslandFlags(org.bukkit.World)}.
     */
    @Test
    public void testGetDefaultIslandFlags() {
        Map<Flag, Integer> flags = new HashMap<>();
        when(ws.getDefaultIslandFlags()).thenReturn(flags);
        assertEquals(flags, iwm.getDefaultIslandFlags(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getHiddenFlags(org.bukkit.World)}.
     */
    @Test
    public void testGetVisibleSettings() {
        List<String> list = new ArrayList<>();
        when(ws.getHiddenFlags()).thenReturn(list);
        assertEquals(list, iwm.getHiddenFlags(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDefaultIslandSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetDefaultIslandSettings() {
        Map<Flag, Integer> flags = new HashMap<>();
        when(ws.getDefaultIslandFlags()).thenReturn(flags);
        assertEquals(flags,iwm.getDefaultIslandSettings(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isUseOwnGenerator(org.bukkit.World)}.
     */
    @Test
    public void testIsUseOwnGenerator() {
        assertFalse(iwm.isUseOwnGenerator(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getVisitorBannedCommands(org.bukkit.World)}.
     */
    @Test
    public void testGetVisitorBannedCommands() {
        List<String> list = new ArrayList<>();
        when(ws.getVisitorBannedCommands()).thenReturn(list);
        assertEquals(list, iwm.getVisitorBannedCommands(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isWaterNotSafe(org.bukkit.World)}.
     */
    @Test
    public void testIsWaterNotSafe() {
        assertFalse(iwm.isWaterNotSafe(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getGeoLimitSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetGeoLimitSettings() {
        List<String> list = new ArrayList<>();
        when(ws.getGeoLimitSettings()).thenReturn(list);
        assertEquals(list, iwm.getGeoLimitSettings(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getResetLimit(org.bukkit.World)}.
     */
    @Test
    public void testGetResetLimit() {
        assertEquals(0,iwm.getResetLimit(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getResetEpoch(org.bukkit.World)}.
     */
    @Test
    public void testGetResetEpoch() {
        assertEquals(0,iwm.getResetEpoch(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#setResetEpoch(org.bukkit.World)}.
     */
    @Test
    public void testSetResetEpoch() {
        iwm.setResetEpoch(world);
        Mockito.verify(ws).setResetEpoch(Mockito.anyLong());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isTeamJoinDeathReset(org.bukkit.World)}.
     */
    @Test
    public void testIsTeamJoinDeathReset() {
        assertFalse(iwm.isTeamJoinDeathReset(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDeathsMax(org.bukkit.World)}.
     */
    @Test
    public void testGetDeathsMax() {
        assertEquals(0, iwm.getDeathsMax(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getBanLimit(org.bukkit.World)}.
     */
    @Test
    public void testGetBanLimit() {
        assertEquals(0, iwm.getBanLimit(world));
    }

}
