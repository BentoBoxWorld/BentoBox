package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class AdminTeamSetownerCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID = UUID.randomUUID();
    private AdminTeamSetownerCommand itl;

   @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        Settings settings = new Settings();
        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getName()).thenReturn("tastybento");
        User.getInstance(mockPlayer);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        User.setPlugin(plugin);

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getLocalesManager()).thenReturn(lm);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        @NonNull
        WorldSettings worldSettings = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // Location
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));
        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getCenter()).thenReturn(location);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // DUT
        itl = new AdminTeamSetownerCommand(ac);

    }

   @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    void testExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
        verify(user).sendMessage("commands.help.header", TextVariables.LABEL, "commands.help.console");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#setup()}
     */
    @Test
    void testSetup() {
        assertEquals("commands.admin.team.setowner.description", itl.getDescription());
        assertEquals("commands.admin.team.setowner.parameters", itl.getParameters());
        // No longer only-player: the console can name the island explicitly
        assertFalse(itl.isOnlyPlayer());
        assertEquals("mod.team.setowner", itl.getPermission());
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    void testExecuteUnknownPlayer() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#canExecute(User, String, List)}.
     */
    @Test
    void testExecuteMakeOwnerAlreadyOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(uuid);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.team.setowner.already-owner", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteSuccess() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        assertTrue(itl.execute(user, itl.getLabel(), List.of("tastybento")));
        // Add other verifications
        verify(user).getTranslation("commands.admin.team.setowner.confirmation", TextVariables.NAME, "tastybento",
                TextVariables.XYZ, "1,2,3");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    void testChangeOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }

    /**
     * Test method for {@link AdminTeamSetownerCommand#changeOwner(User)}
     */
    @Test
    void testChangeOwnerNoOwner() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(null);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        itl.changeOwner(user);
        // Add other verifications
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }

    /**
     * The console names the island's current owner explicitly - no location and no confirmation.
     */
    @Test
    void testExecuteConsoleWithIslandOwnerArg() {
        when(user.isPlayer()).thenReturn(false);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(Util.getUUID("victim")).thenReturn(notUUID);
        when(im.getIsland(any(), eq(notUUID))).thenReturn(island);
        when(island.getOwner()).thenReturn(notUUID);

        List<String> args = List.of("tastybento", "victim");
        assertTrue(itl.canExecute(user, itl.getLabel(), args));
        assertTrue(itl.execute(user, itl.getLabel(), args));
        // No confirmation prompt for the console - the transfer happens immediately
        verify(user).sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, "tastybento");
    }

    /**
     * The console must name the island - it cannot rely on a standing location.
     */
    @Test
    void testCanExecuteConsoleNoIslandArg() {
        when(user.isPlayer()).thenReturn(false);
        when(Util.getUUID("tastybento")).thenReturn(uuid);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.team.setowner.specify-island");
    }

    /**
     * Named island owner is unknown.
     */
    @Test
    void testCanExecuteUnknownIslandOwner() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(Util.getUUID("ghost")).thenReturn(null);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "ghost")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "ghost");
    }

    /**
     * Named island owner has no island.
     */
    @Test
    void testCanExecuteIslandOwnerHasNoIsland() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(Util.getUUID("victim")).thenReturn(notUUID);
        when(im.getIsland(any(), eq(notUUID))).thenReturn(null);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "victim")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * The named player's active island is a team island they do not own - must not be transferred.
     */
    @Test
    void testCanExecuteIslandOwnerNotActualOwner() {
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(Util.getUUID("member")).thenReturn(notUUID);
        // getIsland() resolves an island owned by someone else (a team island the player belongs to)
        when(im.getIsland(any(), eq(notUUID))).thenReturn(island);
        when(island.getOwner()).thenReturn(UUID.randomUUID());

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento", "member")));
        verify(user).sendMessage("general.errors.player-has-no-island");
    }

    /**
     * Recipient already owns the maximum allowed concurrent islands - transfer must be refused.
     */
    @Test
    void testCanExecuteTargetAtConcurrentIslandsCap() {
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(island.getOwner()).thenReturn(notUUID);
        when(Util.getUUID("tastybento")).thenReturn(uuid);
        when(im.getNumberOfConcurrentIslands(eq(uuid), any())).thenReturn(1);

        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage("commands.admin.team.setowner.errors.at-max", TextVariables.NAME, "tastybento",
                TextVariables.NUMBER, "1", "[max]", "1");
    }
}
