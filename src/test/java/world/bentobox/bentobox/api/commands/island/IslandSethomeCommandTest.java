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
public class IslandSethomeCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    private UUID uuid;
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
        when(im.getMaxHomes(eq(island))).thenReturn(1);
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
    public void testIslandSethomeCommand() {
        IslandSethomeCommand cmd = new IslandSethomeCommand(ic);
        assertEquals("sethome", cmd.getName());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#setup()}.
     */
    @Test
    public void testSetup() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertEquals("bskyblock.island.sethome", isc.getPermission());
        assertTrue(isc.isOnlyPlayer());
        assertEquals("commands.island.sethome.description", isc.getDescription());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
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
    public void testCanExecuteNotOnIsland() {
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
    public void testCanExecuteTooManyHomes() {
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
    public void testCanExecute() {
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
    public void testExecuteUserStringListOfString() {
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringHomeSuccess() {
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
    public void testExecuteUserStringListOfStringMultiHomeTooMany() {
        when(im.getMaxHomes(island)).thenReturn(3);
        when(im.getNumberOfHomesIfAdded(eq(island), anyString())).thenReturn(5);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.canExecute(user, "island", Collections.singletonList("13")));
        verify(user).sendMessage(eq("commands.island.sethome.too-many-homes"), eq("[number]"), eq("3"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNether() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherNotAllowed() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.nether.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringNetherConfirmation() {
        when(iwm.isNether(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInNether()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage(eq("commands.island.sethome.nether.confirmation"));
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("0"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEnd() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInNether()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.canExecute(user, "island", Collections.emptyList()));
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.home-set");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndNotAllowed() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(false);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(false);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertFalse(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendMessage("commands.island.sethome.the-end.not-allowed");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.IslandSethomeCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringEndConfirmation() {
        when(iwm.isEnd(any())).thenReturn(true);
        WorldSettings ws = mock(WorldSettings.class);
        when(ws.isAllowSetHomeInTheEnd()).thenReturn(true);
        when(ws.isRequireConfirmationToSetHomeInTheEnd()).thenReturn(true);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        IslandSethomeCommand isc = new IslandSethomeCommand(ic);
        assertTrue(isc.execute(user, "island", Collections.emptyList()));
        verify(user).sendRawMessage(eq("commands.island.sethome.the-end.confirmation"));
        verify(user).sendMessage(eq("commands.confirmation.confirm"), eq("[seconds]"), eq("0"));
    }
}
