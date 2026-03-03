package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

public class BannedCommandsTest extends CommonTestSetup {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFallingBannedCommands(any())).thenReturn(new ArrayList<>());
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Player
        when(mockPlayer.isOp()).thenReturn(false);
        when(mockPlayer.hasPermission(Mockito.anyString())).thenReturn(false);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getLocation()).thenReturn(mock(Location.class));
        User.getInstance(mockPlayer);

        server.setPlayers(10);
        when(mockPlayer.getServer()).thenReturn(server);

        // Island manager
        // Default not on island, so is a visitor
        when(im.locationIsOnIsland(any(), any())).thenReturn(false);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Set flag
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, true);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that the command is not cancelled when the player is not in a game world.
     */
    @Test
    public void testInstantReturnNotInWorld() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that the command is not cancelled when the player is an operator.
     */
    @Test
    public void testInstantReturnOp() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        when(mockPlayer.isOp()).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that the command is not cancelled when the player has bypass permission.
     */
    @Test
    public void testInstantReturnBypassPerm() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        when(mockPlayer.hasPermission(Mockito.anyString())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that the command is not cancelled when the player is on their own island.
     */
    @Test
    public void testInstantReturnOnOwnIsland() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        when(im.locationIsOnIsland(any(), any())).thenReturn(true);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testEmptyBannedCommands() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/blah");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that commands matching banned entries are cancelled for visitors.
     */
    @ParameterizedTest
    @MethodSource("provideCancelledVisitorCommands")
    public void testVisitorBannedCommandCancelled(String command, List<String> bannedList) {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, command);
        BannedCommands bvc = new BannedCommands(plugin);
        when(iwm.getVisitorBannedCommands(any())).thenReturn(bannedList);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertTrue(e.isCancelled());
    }

    static Stream<Arguments> provideCancelledVisitorCommands() {
        return Stream.of(
                // Exact match
                Arguments.of("/banned_command", List.of("banned_command", "another_banned_command")),
                // Banned command with extra args
                Arguments.of("/banned_command with extra stuff", List.of("banned_command", "another_banned_command")),
                // Full match including args
                Arguments.of("/banned_command with extra stuff", List.of("banned_command with extra stuff", "another_banned_command")),
                // Another banned command with extra args
                Arguments.of("/another_banned_command with extra stuff", List.of("banned_command", "another_banned_command")),
                // Multi-word banned command exact match
                Arguments.of("/cmi sethome", List.of("cmi sethome"))
        );
    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}.
     * Verifies that commands not matching banned entries are not cancelled for visitors.
     */
    @ParameterizedTest
    @MethodSource("provideNonCancelledVisitorCommands")
    public void testVisitorBannedCommandNotCancelled(String command, List<String> bannedList) {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, command);
        BannedCommands bvc = new BannedCommands(plugin);
        when(iwm.getVisitorBannedCommands(any())).thenReturn(bannedList);
        bvc.onVisitorCommand(e);
        verify(iwm).getVisitorBannedCommands(any());
        assertFalse(e.isCancelled());
    }

    static Stream<Arguments> provideNonCancelledVisitorCommands() {
        return Stream.of(
                // Non-matching command
                Arguments.of("/blah", List.of("banned_command", "another_banned_command")),
                // Non-matching command with extra args
                Arguments.of("/blah with extra stuff", List.of("banned_command", "another_banned_command")),
                // Different command than banned multi-word
                Arguments.of("/spawn", List.of("cmi sethome")),
                // Partial match of multi-word banned command (only first word)
                Arguments.of("/cmi", List.of("cmi sethome")),
                // Different subcommand than banned
                Arguments.of("/cmi homey", List.of("cmi sethome")),
                // Non-matching with multiple banned entries
                Arguments.of("/spawn", List.of("cmi sethome", "spawn sethome now")),
                // Non-matching with three banned entries
                Arguments.of("/spawn", List.of("cmi sethome", "spawn sethome now", "cmi multi now"))
        );
    }


    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithNothing() {
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "");
        BannedCommands bvc = new BannedCommands(plugin);
        bvc.onVisitorCommand(e);
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommand() {
        when(mockPlayer.getFallDistance()).thenReturn(10F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertTrue(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNotFalling() {
        when(mockPlayer.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

    /**
     * Test for {@link BannedCommands#onCommand(PlayerCommandPreprocessEvent)}
     */
    @Test
    public void testBannedCommandsWithBannedFallingCommandNoFlag() {
        Flags.PREVENT_TELEPORT_WHEN_FALLING.setSetting(world, false);
        when(mockPlayer.getFallDistance()).thenReturn(0F);
        PlayerCommandPreprocessEvent e = new PlayerCommandPreprocessEvent(mockPlayer, "/banned_command");
        BannedCommands bvc = new BannedCommands(plugin);
        List<String> banned = new ArrayList<>();
        banned.add("banned_command");
        banned.add("another_banned_command");
        when(iwm.getFallingBannedCommands(any())).thenReturn(banned);
        bvc.onFallingCommand(e);
        assertFalse(e.isCancelled());

    }

}
