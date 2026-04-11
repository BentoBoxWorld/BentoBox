package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests for {@link Util}.
 */
class UtilTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ---- blockFaceToFloat ----

    @Test
    void testBlockFaceToFloatNorth() {
        assertEquals(0F, Util.blockFaceToFloat(BlockFace.NORTH));
    }

    @Test
    void testBlockFaceToFloatEast() {
        assertEquals(90F, Util.blockFaceToFloat(BlockFace.EAST));
    }

    @Test
    void testBlockFaceToFloatSouth() {
        assertEquals(180F, Util.blockFaceToFloat(BlockFace.SOUTH));
    }

    @Test
    void testBlockFaceToFloatWest() {
        assertEquals(270F, Util.blockFaceToFloat(BlockFace.WEST));
    }

    @Test
    void testBlockFaceToFloatNorthEast() {
        assertEquals(45F, Util.blockFaceToFloat(BlockFace.NORTH_EAST));
    }

    @Test
    void testBlockFaceToFloatSouthEast() {
        assertEquals(135F, Util.blockFaceToFloat(BlockFace.SOUTH_EAST));
    }

    @Test
    void testBlockFaceToFloatSouthWest() {
        assertEquals(225F, Util.blockFaceToFloat(BlockFace.SOUTH_WEST));
    }

    @Test
    void testBlockFaceToFloatNorthWest() {
        assertEquals(315F, Util.blockFaceToFloat(BlockFace.NORTH_WEST));
    }

    @Test
    void testBlockFaceToFloatEastNorthEast() {
        assertEquals(67.5F, Util.blockFaceToFloat(BlockFace.EAST_NORTH_EAST));
    }

    @Test
    void testBlockFaceToFloatNorthNorthEast() {
        assertEquals(22.5F, Util.blockFaceToFloat(BlockFace.NORTH_NORTH_EAST));
    }

    @Test
    void testBlockFaceToFloatNorthNorthWest() {
        assertEquals(337.5F, Util.blockFaceToFloat(BlockFace.NORTH_NORTH_WEST));
    }

    @Test
    void testBlockFaceToFloatSouthSouthEast() {
        assertEquals(157.5F, Util.blockFaceToFloat(BlockFace.SOUTH_SOUTH_EAST));
    }

    @Test
    void testBlockFaceToFloatSouthSouthWest() {
        assertEquals(202.5F, Util.blockFaceToFloat(BlockFace.SOUTH_SOUTH_WEST));
    }

    @Test
    void testBlockFaceToFloatWestNorthWest() {
        assertEquals(292.5F, Util.blockFaceToFloat(BlockFace.WEST_NORTH_WEST));
    }

    @Test
    void testBlockFaceToFloatWestSouthWest() {
        assertEquals(247.5F, Util.blockFaceToFloat(BlockFace.WEST_SOUTH_WEST));
    }

    @Test
    void testBlockFaceToFloatDefault() {
        // UP, DOWN, SELF should return 0
        assertEquals(0F, Util.blockFaceToFloat(BlockFace.UP));
    }

    // ---- isVersionCompatible ----

    @Test
    void testIsVersionCompatibleExactMatch() {
        assertTrue(Util.isVersionCompatible("1.20.4", "1.20.4"));
    }

    @Test
    void testIsVersionCompatibleNewer() {
        assertTrue(Util.isVersionCompatible("1.21.0", "1.20.4"));
    }

    @Test
    void testIsVersionCompatibleOlder() {
        assertFalse(Util.isVersionCompatible("1.19.4", "1.20.0"));
    }

    @Test
    void testIsVersionCompatibleSnapshotVsRelease() {
        // Snapshot is lower precedence than release
        assertFalse(Util.isVersionCompatible("1.20.4-SNAPSHOT", "1.20.4"));
    }

    @Test
    void testIsVersionCompatibleReleaseVsSnapshot() {
        assertTrue(Util.isVersionCompatible("1.20.4", "1.20.4-SNAPSHOT"));
    }

    @Test
    void testIsVersionCompatibleBothSnapshots() {
        assertTrue(Util.isVersionCompatible("1.20.4-SNAPSHOT", "1.20.4-SNAPSHOT"));
    }

    @Test
    void testIsVersionCompatibleDifferentLengths() {
        assertTrue(Util.isVersionCompatible("1.21", "1.20.4"));
    }

    @Test
    void testIsVersionCompatibleShorterRequired() {
        assertTrue(Util.isVersionCompatible("1.20.4", "1.20"));
    }

    // ---- parseGitHubDate ----

    @Test
    void testParseGitHubDateValid() {
        Date result = Util.parseGitHubDate("2024-01-15T10:30:00Z");
        assertNotNull(result);
    }

    @Test
    void testParseGitHubDateInvalid() {
        Date result = Util.parseGitHubDate("not-a-date");
        assertNull(result);
    }

    // ---- getLocationString ----

    @Test
    void testGetLocationStringNull() {
        assertNull(Util.getLocationString(null));
    }

    @Test
    void testGetLocationStringEmpty() {
        assertNull(Util.getLocationString(""));
    }

    @Test
    void testGetLocationStringBlank() {
        assertNull(Util.getLocationString("   "));
    }

    @Test
    void testGetLocationStringValid() {
        // Format: world:x:y:z:yaw:pitch
        mockedBukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
        int yawBits = Float.floatToIntBits(90F);
        int pitchBits = Float.floatToIntBits(0F);
        String loc = "world:10:64:20:" + yawBits + ":" + pitchBits;
        Location result = Util.getLocationString(loc);
        assertNotNull(result);
        assertEquals(world, result.getWorld());
        assertEquals(10.5D, result.getX());
        assertEquals(64D, result.getY());
        assertEquals(20.5D, result.getZ());
    }

    @Test
    void testGetLocationStringWrongParts() {
        assertNull(Util.getLocationString("world:10:64"));
    }

    @Test
    void testGetLocationStringUnknownWorld() {
        mockedBukkit.when(() -> Bukkit.getWorld("unknown")).thenReturn(null);
        int bits = Float.floatToIntBits(0F);
        assertNull(Util.getLocationString("unknown:10:64:20:" + bits + ":" + bits));
    }

    // ---- broadcast ----

    @Test
    void testBroadcastNoPlayers() {
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
        assertEquals(0, Util.broadcast("test.message"));
    }

    @Test
    void testBroadcastWithPermittedPlayer() {
        // Use the mockPlayer from CommonTestSetup which is already wired up
        when(mockPlayer.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(true);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(mockPlayer));

        int count = Util.broadcast("test.message");
        assertEquals(1, count);
    }

    @Test
    void testBroadcastSkipsUnpermitted() {
        when(mockPlayer.hasPermission(Server.BROADCAST_CHANNEL_USERS)).thenReturn(false);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(mockPlayer));

        assertEquals(0, Util.broadcast("test.message"));
    }

    // ---- runCommands ----

    @Test
    void testRunCommandsSudo() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("TestPlayer");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand("warp home")).thenReturn(true);

        Util.runCommands(user, List.of("[SUDO]warp home"), "test");
        verify(user).performCommand("warp home");
    }

    @Test
    void testRunCommandsSudoOfflineLogsError() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("OfflinePlayer");
        when(user.isOnline()).thenReturn(false);

        Util.runCommands(user, List.of("[SUDO]warp home"), "test");
        verify(plugin).logError(contains("Could not execute test command for OfflinePlayer"));
    }

    @Test
    void testRunCommandsSudoFails() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("TestPlayer");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand(anyString())).thenReturn(false);

        Util.runCommands(user, List.of("[SUDO]bad command"), "test");
        verify(plugin).logError(contains("Could not execute test command for TestPlayer"));
    }

    @Test
    void testRunCommandsEmptyList() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("TestPlayer");

        // Should not throw
        Util.runCommands(user, List.of(), "test");
    }

    @Test
    void testRunCommandsPlayerPlaceholderReplaced() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("TestPlayer");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand("give TestPlayer diamond")).thenReturn(true);

        Util.runCommands(user, List.of("[SUDO]give [player] diamond"), "test");
        verify(user).performCommand("give TestPlayer diamond");
    }

    @Test
    void testRunCommandsOwnerPlaceholderReplaced() {
        User user = mock(User.class);
        when(user.getName()).thenReturn("TestPlayer");
        when(user.isOnline()).thenReturn(true);
        when(user.performCommand("msg OwnerName hello")).thenReturn(true);

        Util.runCommands(user, "OwnerName", List.of("[SUDO]msg [owner] hello"), "test");
        verify(user).performCommand("msg OwnerName hello");
    }

    // ---- bukkitToAdventure ----

    @Test
    void testBukkitToAdventureNull() {
        Component result = Util.bukkitToAdventure(null);
        assertEquals(Component.empty(), result);
    }

    @Test
    void testBukkitToAdventureSimpleString() {
        Component result = Util.bukkitToAdventure("Hello World");
        assertNotNull(result);
    }

    @Test
    void testBukkitToAdventureLegacyCodes() {
        Component result = Util.bukkitToAdventure("\u00a7aGreen text");
        assertNotNull(result);
    }
}
