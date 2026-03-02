package world.bentobox.bentobox.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag;

/**
 * @author tastybento
 *
 */
public class IslandWorldManagerTest extends CommonTestSetup {

    private IslandWorldManager testIwm;

    @Mock
    private World testWorld;

    @Mock
    private WorldSettings ws;

    @Mock
    private @Nullable World netherWorld;

    @Mock
    private @Nullable World endWorld;

    @Mock
    private GameModeAddon gm;

    /**
     */
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        testIwm = new IslandWorldManager(plugin);
        // World
        when(testWorld.getName()).thenReturn("test-world");
        when(testWorld.getEnvironment()).thenReturn(World.Environment.NORMAL);
        when(testWorld.getMaxHeight()).thenReturn(256);
        when(location.getWorld()).thenReturn(testWorld);

        // Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);

        // Flags Manager
        FlagsManager fm = mock(FlagsManager.class);
        // No flags right now
        when(fm.getFlags()).thenReturn(new ArrayList<>());
        when(plugin.getFlagsManager()).thenReturn(fm);
        // Gamemode
        when(ws.getFriendlyName()).thenReturn("friendly");
        when(gm.getWorldSettings()).thenReturn(ws);
        when(gm.getOverWorld()).thenReturn(testWorld);
        when(gm.getNetherWorld()).thenReturn(netherWorld);
        when(gm.getEndWorld()).thenReturn(endWorld);
        testIwm.addGameMode(gm);
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#registerWorldsToMultiverse()}.
     */
    @Test
    public void testRegisterWorldsToMultiverse() {
        testIwm.registerWorldsToMultiverse(true);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.Location)}.
     */
    @Test
    public void testInWorldLocation() {
        assertTrue(testIwm.inWorld(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.Location)}.
     */
    @Test
    public void testInWorldLocationNull() {
        assertFalse(testIwm.inWorld((Location)null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.World)}.
     */
    @Test
    public void testInWorldWorld() {
        assertTrue(testIwm.inWorld(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#inWorld(org.bukkit.World)}.
     */
    @Test
    public void testInWorldWorldNull() {
        assertFalse(testIwm.inWorld((World)null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorlds()}.
     */
    @Test
    public void testGetWorlds() {
        assertTrue(testIwm.getWorlds().contains(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorlds()}.
     */
    @Test
    public void testGetOverWorlds() {
        assertTrue(testIwm.getOverWorlds().contains(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorldNames()}.
     */
    @Test
    public void testGetOverWorldNames() {
        Map<String, String> map = testIwm.getOverWorldNames();
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
        assertTrue(testIwm.isKnownFriendlyWorldName("friendly"));
        assertFalse(testIwm.isKnownFriendlyWorldName("not-friendly"));
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
        when(gm.getOverWorld()).thenReturn(testWorld);

        testIwm.addGameMode(gm);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorldSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetWorldSettings() {
        assertEquals(ws, testIwm.getWorldSettings(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getOverWorld(java.lang.String)}.
     */
    @Test
    public void testGetOverWorld() {
        assertEquals(testWorld, testIwm.getOverWorld("friendly"));
        assertNull(testIwm.getOverWorld("not-friendly"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandDistance(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandDistance() {
        assertEquals(0, testIwm.getIslandDistance(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeight() {
        assertEquals(0, testIwm.getIslandHeight(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeightOverMax() {
        when(ws.getIslandHeight()).thenReturn(500);
        assertEquals(255, testIwm.getIslandHeight(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandHeightSubZero() {
        when(ws.getIslandHeight()).thenReturn(-50);
        assertEquals(0, testIwm.getIslandHeight(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandProtectionRange(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandProtectionRange() {
        assertEquals(0, testIwm.getIslandProtectionRange(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandStartX(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandStartX() {
        assertEquals(0, testIwm.getIslandStartX(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandStartZ(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandStartZ() {
        assertEquals(0, testIwm.getIslandStartZ(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandXOffset(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandXOffset() {
        assertEquals(0, testIwm.getIslandXOffset(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandZOffset(org.bukkit.World)}.
     */
    @Test
    public void testGetIslandZOffset() {
        assertEquals(0, testIwm.getIslandZOffset(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxIslands(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxIslands() {
        assertEquals(0, testIwm.getMaxIslands(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherSpawnRadius(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherSpawnRadius() {
        assertEquals(0, testIwm.getNetherSpawnRadius(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getSeaHeight(org.bukkit.World)}.
     */
    @Test
    public void testGetSeaHeight() {
        assertEquals(0, testIwm.getSeaHeight(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getWorldName(org.bukkit.World)}.
     */
    @Test
    public void testGetWorldName() {
        when(ws.getWorldName()).thenReturn("test-world");
        assertEquals("test-world", testIwm.getWorldName(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEndGenerate(org.bukkit.World)}.
     */
    @Test
    public void testIsEndGenerate() {
        assertFalse(testIwm.isEndGenerate(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEndIslands(org.bukkit.World)}.
     */
    @Test
    public void testIsEndIslands() {
        assertFalse(testIwm.isEndIslands(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNetherGenerate(org.bukkit.World)}.
     */
    @Test
    public void testIsNetherGenerate() {
        assertFalse(testIwm.isNetherGenerate(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNetherIslands(org.bukkit.World)}.
     */
    @Test
    public void testIsNetherIslands() {
        assertFalse(testIwm.isNetherIslands(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isNether(org.bukkit.World)}.
     */
    @Test
    public void testIsNether() {
        assertFalse(testIwm.isNether(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isIslandNether(org.bukkit.World)}.
     */
    @Test
    public void testIsIslandNether() {
        assertFalse(testIwm.isIslandNether(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isEnd(org.bukkit.World)}.
     */
    @Test
    public void testIsEnd() {
        assertFalse(testIwm.isEnd(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isIslandEnd(org.bukkit.World)}.
     */
    @Test
    public void testIsIslandEnd() {
        assertFalse(testIwm.isIslandEnd(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherWorld() {
        assertEquals(netherWorld, testIwm.getNetherWorld(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getNetherWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetNetherWorldNull() {
        assertNull(testIwm.getNetherWorld(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getEndWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetEndWorld() {
        assertEquals(endWorld, testIwm.getEndWorld(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getEndWorld(org.bukkit.World)}.
     */
    @Test
    public void testGetEndWorldNull() {
        assertNull(testIwm.getEndWorld(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isDragonSpawn(org.bukkit.World)}.
     */
    @Test
    public void testIsDragonSpawn() {
        assertTrue(testIwm.isDragonSpawn(endWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isDragonSpawn(org.bukkit.World)}.
     */
    @Test
    public void testIsDragonSpawnNull() {
        assertTrue(testIwm.isDragonSpawn(null));
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
        testIwm.addGameMode(gm2);
        // String can be in any order
        String result = testIwm.getFriendlyNames();
        assertTrue(result.contains("fri2"));
        assertTrue(result.contains("friendly"));
        assertTrue(result.contains(", "));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIslandWorld(java.lang.String)}.
     */
    @Test
    public void testGetIslandWorld() {
        assertEquals(testWorld, testIwm.getIslandWorld("friendly"));
        assertNull(testIwm.getIslandWorld("not-friendly"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxTeamSize(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxTeamSize() {
        assertEquals(0, testIwm.getMaxTeamSize(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getMaxHomes(org.bukkit.World)}.
     */
    @Test
    public void testGetMaxHomes() {
        assertEquals(0, testIwm.getMaxHomes(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getFriendlyName(org.bukkit.World)}.
     */
    @Test
    public void testGetFriendlyName() {
        assertEquals("friendly", testIwm.getFriendlyName(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getPermissionPrefix(org.bukkit.World)}.
     */
    @Test
    public void testGetPermissionPrefix() {
        when(ws.getPermissionPrefix()).thenReturn("bsky");
        assertEquals("bsky.", testIwm.getPermissionPrefix(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getIvSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetIvSettings() {
        List<String> list = Collections.singletonList("blah");
        when(ws.getIvSettings()).thenReturn(list);
        assertEquals(list, testIwm.getIvSettings(testWorld));
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
        assertEquals(GameMode.ADVENTURE, testIwm.getDefaultGameMode(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getRemoveMobsWhitelist(org.bukkit.World)}.
     */
    @Test
    public void testGetRemoveMobsWhitelist() {
        Set<EntityType> set = new HashSet<>();
        when(ws.getRemoveMobsWhitelist()).thenReturn(set);
        assertEquals(set, testIwm.getRemoveMobsWhitelist(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetMoney(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetMoney() {
        assertFalse(testIwm.isOnJoinResetMoney(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetInventory(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetInventory() {
        assertFalse(testIwm.isOnJoinResetInventory(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnJoinResetEnderChest(org.bukkit.World)}.
     */
    @Test
    public void testIsOnJoinResetEnderChest() {
        assertFalse(testIwm.isOnJoinResetEnderChest(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetMoney(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetMoney() {
        assertFalse(testIwm.isOnLeaveResetMoney(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetInventory(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetInventory() {
        assertFalse(testIwm.isOnLeaveResetInventory(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isOnLeaveResetEnderChest(org.bukkit.World)}.
     */
    @Test
    public void testIsOnLeaveResetEnderChest() {
        assertFalse(testIwm.isOnLeaveResetEnderChest(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDataFolder(org.bukkit.World)}.
     */
    @Test
    public void testGetDataFolder() {
        File dataFolder = mock(File.class);
        when(gm.getDataFolder()).thenReturn(dataFolder);
        assertEquals(dataFolder, testIwm.getDataFolder(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getAddon(org.bukkit.World)}.
     */
    @Test
    public void testGetAddon() {
        assertEquals(gm, testIwm.getAddon(testWorld).get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getAddon(org.bukkit.World)}.
     */
    @Test
    public void testGetAddonNull() {
        assertEquals(Optional.empty(), testIwm.getAddon(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDefaultIslandFlags(org.bukkit.World)}.
     */
    @SuppressWarnings("removal")
    @Test
    public void testGetDefaultIslandFlags() {
        Map<Flag, Integer> flags = new HashMap<>();
        when(ws.getDefaultIslandFlags()).thenReturn(flags);
        assertEquals(flags, testIwm.getDefaultIslandFlags(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getHiddenFlags(org.bukkit.World)}.
     */
    @Test
    public void testGetVisibleSettings() {
        List<String> list = new ArrayList<>();
        when(ws.getHiddenFlags()).thenReturn(list);
        assertEquals(list, testIwm.getHiddenFlags(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDefaultIslandSettings(org.bukkit.World)}.
     */
    @SuppressWarnings("removal")
    @Test
    public void testGetDefaultIslandSettings() {
        Map<Flag, Integer> flags = new HashMap<>();
        when(ws.getDefaultIslandFlags()).thenReturn(flags);
        assertEquals(flags,testIwm.getDefaultIslandSettings(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isUseOwnGenerator(org.bukkit.World)}.
     */
    @Test
    public void testIsUseOwnGenerator() {
        assertFalse(testIwm.isUseOwnGenerator(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getVisitorBannedCommands(org.bukkit.World)}.
     */
    @Test
    public void testGetVisitorBannedCommands() {
        List<String> list = new ArrayList<>();
        when(ws.getVisitorBannedCommands()).thenReturn(list);
        assertEquals(list, testIwm.getVisitorBannedCommands(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isWaterNotSafe(org.bukkit.World)}.
     */
    @Test
    public void testIsWaterNotSafe() {
        assertFalse(testIwm.isWaterNotSafe(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getGeoLimitSettings(org.bukkit.World)}.
     */
    @Test
    public void testGetGeoLimitSettings() {
        List<String> list = new ArrayList<>();
        when(ws.getGeoLimitSettings()).thenReturn(list);
        assertEquals(list, testIwm.getGeoLimitSettings(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getResetLimit(org.bukkit.World)}.
     */
    @Test
    public void testGetResetLimit() {
        assertEquals(0,testIwm.getResetLimit(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getResetEpoch(org.bukkit.World)}.
     */
    @Test
    public void testGetResetEpoch() {
        assertEquals(0,testIwm.getResetEpoch(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#setResetEpoch(org.bukkit.World)}.
     */
    @Test
    public void testSetResetEpoch() {
        testIwm.setResetEpoch(testWorld);
        Mockito.verify(ws).setResetEpoch(Mockito.anyLong());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#isTeamJoinDeathReset(org.bukkit.World)}.
     */
    @Test
    public void testIsTeamJoinDeathReset() {
        assertFalse(testIwm.isTeamJoinDeathReset(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getDeathsMax(org.bukkit.World)}.
     */
    @Test
    public void testGetDeathsMax() {
        assertEquals(0, testIwm.getDeathsMax(testWorld));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.IslandWorldManager#getBanLimit(org.bukkit.World)}.
     */
    @Test
    public void testGetBanLimit() {
        assertEquals(0, testIwm.getBanLimit(testWorld));
    }

}
