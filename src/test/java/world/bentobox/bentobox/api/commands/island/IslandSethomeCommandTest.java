package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
class IslandSethomeCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private WorldSettings ws;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString())).thenAnswer(i -> i.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        when(ic.getPermissionPrefix()).thenReturn("bskyblock.");
        when(ic.getWorld()).thenReturn(world);

        // Island for player to begin with
        when(im.hasIsland(world, user)).thenReturn(true);
        when(im.getIslands(world, user)).thenReturn(List.of(island));

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getOwner()).thenReturn(uuid);
        when(island.onIsland(any())).thenReturn(true);
        when(im.getMaxHomes(island)).thenReturn(1);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Not in nether
        when(iwm.isNether(any())).thenReturn(false);
        // Not in end
        when(iwm.isEnd(any())).thenReturn(false);
        // Number of homes default
        when(iwm.getMaxHomes(any())).thenReturn(3);
        // World settings
        when(iwm.getWorldSettings(any(World.class))).thenReturn(ws);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#IslandSethomeCommand(world.bentobox.bentobox.api.commands.CompositeCommand)}.
     */
    @Test
    void testIslandSethomeCommand() {
        IslandSethomeCommand cmd = new IslandSethomeCommand(ic);
        assertEquals("sethome", cmd.getName());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#setup()}.
     */
    @Test
    void testSetup() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertEquals("bskyblock.island.sethome", isc.getPermission());
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.sethome.description", isc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteNoIsland() {
        // Player doesn't have an island
        when(im.getIsland(world, user)).thenReturn(null);

        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteNotOnIsland() {
        when(island.onIsland(any())).thenReturn(false);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
        verify(user).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteTooManyHomes() {
        when(im.getMaxHomes(island)).thenReturn(9);
        when(im.getNumberOfHomesIfAdded(eq(island), anyString())).thenReturn(11);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.too-many-homes", TextVariables.NUMBER, "9");
        verify(user).sendMessage("commands.island.sethome.homes-are");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecute() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        verify(user, never()).sendMessage("general.errors.no-island");
        verify(user, never()).sendMessage("commands.island.sethome.must-be-on-your-island");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfString() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringHomeSuccess() {
        when(island.getMaxHomes()).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.singletonList("home")));
        assertTrue(isc.execute(user, "island", Collections.singletonList("home")));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringMultiHomeTooMany() {
        when(im.getMaxHomes(island)).thenReturn(3);
        when(im.getNumberOfHomesIfAdded(eq(island), anyString())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.singletonList("13")));
        verify(user).sendMessage("commands.island.sethome.too-many-homes", "[number]", "3");
    }

    /**
     * Test that a user with maxhomes permission of 1 can set a named (non-numeric) home.
     * This verifies the fix for issue #1564, where users with maxhomes.1 previously received
     * an incorrect "missing permission" error instead of being allowed to set a named home.
     */
    @Test
    void testCanExecuteMaxHomes1WithNamedHome() {
        // maxHomes = 1 is already set in setUp via when(im.getMaxHomes(island)).thenReturn(1)
        // Simulate island with default home already set: adding "MyHome" would make 2 homes total
        when(im.getNumberOfHomesIfAdded(island, "MyHome")).thenReturn(2);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        // Should succeed: 2 <= maxHomes + 1 = 2
        assertTrue(isc.canExecute(user, "island", Collections.singletonList("MyHome")));
        // Must NOT show any permission error
        verify(user, never()).sendMessage(eq("general.errors.no-permission"), anyString(), anyString());
        // Must NOT show too-many-homes error
        verify(user, never()).sendMessage(eq("commands.island.sethome.too-many-homes"), anyString(), anyString());
    }

    /**
     * Test that a user with maxhomes permission of 1 can set a home named "1" (numeric).
     * This verifies that numeric home names are treated the same as non-numeric names.
     */
    @Test
    void testCanExecuteMaxHomes1WithNumericHomeOne() {
        // maxHomes = 1 is already set in setUp
        // Simulate island with default home: adding "1" would make 2 homes total
        when(im.getNumberOfHomesIfAdded(island, "1")).thenReturn(2);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        // Should succeed: 2 <= maxHomes + 1 = 2
        assertTrue(isc.canExecute(user, "island", Collections.singletonList("1")));
        // Must NOT show any permission error
        verify(user, never()).sendMessage(eq("general.errors.no-permission"), anyString(), anyString());
    }

    /**
     * Test that a user with maxhomes permission of 1 cannot set an additional home when already
     * at the limit (default home + 1 named home = 2 total). The error should be "too-many-homes",
     * NOT a "missing permission" error. This verifies the fix for issue #1564.
     */
    @Test
    void testCanExecuteMaxHomes1AtLimitShowsTooManyHomesNotPermissionError() {
        // maxHomes = 1 is already set in setUp
        // Simulate island with default + "MyHome" already set: adding "home2" would make 3 homes
        when(im.getNumberOfHomesIfAdded(island, "home2")).thenReturn(3);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        // Should fail: 3 > maxHomes + 1 = 2
        assertFalse(isc.canExecute(user, "island", Collections.singletonList("home2")));
        // Must show too-many-homes error (not a permission error)
        verify(user).sendMessage("commands.island.sethome.too-many-homes", TextVariables.NUMBER, "1");
        // Must NOT show a missing permission error
        verify(user, never()).sendMessage(eq("general.errors.no-permission"), anyString(), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringNether() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInNether()).thenReturn(true);
        when(localWs.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringNetherNotAllowed() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInNether()).thenReturn(false);
        when(localWs.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.nether.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringNetherConfirmation() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInNether()).thenReturn(true);
        when(localWs.isRequireConfirmationToSetHomeInNether()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage("commands.island.sethome.nether.confirmation");
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringEnd() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(localWs.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringEndNotAllowed() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInTheEnd()).thenReturn(false);
        when(localWs.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.the-end.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringEndConfirmation() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings localWs = mock(WorldSettings.class);
        when(localWs.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(localWs.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(localWs);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage("commands.island.sethome.the-end.confirmation");
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }
}
