package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class AdminTeamAddCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private PlaceholdersManager phm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        notUUID = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bsb");

        // Player has island to begin with
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(island.getOwner()).thenReturn(uuid);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenReturn("mock translation");

        // Island World Manager
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        @NonNull
        WorldSettings ws = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(ws);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteWrongArgs() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        List<String> args = new ArrayList<>();
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
        args.add("arg1");
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
        args.add("args2");
        args.add("args3");
        assertFalse(itl.execute(user, itl.getLabel(), args));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteUnknownPlayer() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        // Unknown owner
        when(pm.getUUID("tastybento")).thenReturn(null);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);
        assertFalse(itl.execute(user, ac.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");

        // Unknown target
        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(null);
        assertFalse(itl.execute(user, ac.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "poslovich");
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteTargetTargetInTeam() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        when(im.inTeam(any(), eq(notUUID))).thenReturn(true);
        when(island.inTeam(notUUID)).thenReturn(true);
        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("commands.island.team.invite.errors.already-on-team");
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteAddNoIsland() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        // No island,
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("general.errors.player-has-no-island");

    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteAddNotOwner() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        // Has island, has team, but not an owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Island
        when(island.getOwner()).thenReturn(notUUID);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("commands.admin.team.add.name-not-owner", "[name]", "tastybento");
        verify(user).sendMessage("commands.admin.info.team-members-title");
    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteAddTargetHasIsland() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        // Has island, has team, is owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Target has island
        when(im.hasIsland(any(), eq(notUUID))).thenReturn(true);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("commands.admin.team.add.name-has-island", "[name]", "poslovich");

    }

    /**
     * Test method for {@link AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteAddTargetHasIslandNoTeam() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        // Has island, no team
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        // Target has island
        when(im.hasIsland(any(), eq(notUUID))).thenReturn(true);

        assertFalse(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(user).sendMessage("commands.admin.team.add.name-has-island", "[name]", "poslovich");

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamAddCommand#execute(User, String, List)}.
     */
    @Test
    void testExecuteSuccess() {
        AdminTeamAddCommand itl = new AdminTeamAddCommand(ac);
        String[] name = { "tastybento", "poslovich" };

        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("poslovich")).thenReturn(notUUID);

        // Has island, no team
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        // Target has no island
        when(im.hasIsland(any(), eq(notUUID))).thenReturn(false);

        // Island
        Island island = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(island);

        // Player name
        when(pm.getName(uuid)).thenReturn("tastybento");
        when(pm.getName(notUUID)).thenReturn("poslovich");
        when(plugin.getPlayers()).thenReturn(pm);

        // Success
        assertTrue(itl.execute(user, itl.getLabel(), Arrays.asList(name)));
        verify(im).setJoinTeam(island, notUUID);
        // Null name for target because it is created out of mocking via User.getPlayer(notUUID)
        verify(user).sendMessage("commands.admin.team.add.success", TextVariables.NAME, null, "[owner]", "tastybento");
    }

}
