package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class AdminTeamKickCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Island island2;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Admin player (user)
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getLocation()).thenReturn(location);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);

        // island is owned by admin (uuid), island2 is owned by notUUID (the target)
        when(island.getOwner()).thenReturn(uuid);
        when(island2.getOwner()).thenReturn(notUUID);

        // By default, admin is standing on island2 (the target's island)
        when(im.getIslandAt(location)).thenReturn(Optional.of(island2));
        // Target (notUUID) is a member of island2
        when(island2.inTeam(notUUID)).thenReturn(true);

        // Target is in a team
        when(im.inTeam(any(), eq(notUUID))).thenReturn(true);
        // Target is on island2 only (single island by default)
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island2));

        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(pm.getName(any())).thenReturn("target");

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Util
        mockedUtil.when(() -> Util.getOnlinePlayerList(any())).thenReturn(List.of());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteNoTarget() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteTooManyArgs() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("a", "b", "c")));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecuteUnknownPlayer() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    void testCanExecutePlayerNotInTeam() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(notUUID);
        when(im.inTeam(any(), eq(notUUID))).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage("commands.admin.team.kick.not-in-team");
    }

    /**
     * Test that an invalid second argument shows help.
     */
    @Test
    void testCanExecuteBadSecondArg() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target", "badarg")));
    }

    /**
     * Test that a non-player (console) user without --all gets an error.
     */
    @Test
    void testCanExecuteConsoleWithoutAllFlag() {
        when(user.isPlayer()).thenReturn(false);
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage("commands.admin.team.kick.must-stand-on-island");
    }

    /**
     * Test that a player not standing on any island gets an error.
     */
    @Test
    void testCanExecuteNotOnIsland() {
        when(im.getIslandAt(location)).thenReturn(Optional.empty());
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage("commands.admin.team.kick.must-stand-on-island");
    }

    /**
     * Test that kicking a player not on the specific island gives an error.
     */
    @Test
    void testCanExecuteTargetNotMemberOfThisIsland() {
        when(island2.inTeam(notUUID)).thenReturn(false);
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage("commands.admin.team.kick.not-member-of-this-island");
    }

    /**
     * Test that standing on the island allows a kick.
     */
    @Test
    void testCanExecuteSuccess() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
    }

    /**
     * Test that --all flag is accepted.
     */
    @Test
    void testCanExecuteWithAllFlag() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target", "--all")));
    }

    /**
     * Test that --all flag is case-insensitive.
     */
    @Test
    void testCanExecuteWithAllFlagCaseInsensitive() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target", "--ALL")));
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)}.
     * Admin is standing on island2 (owned by target), kicks target from that island only.
     */
    @Test
    void testExecuteSingleIsland() {
        String name = "target";
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList(name)));
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList(name)));
        verify(im).removePlayer(island2, notUUID);
        verify(im, never()).removePlayer(island, notUUID);
        verify(user).sendMessage(eq("commands.admin.team.kick.success"), eq(TextVariables.NAME), any(), eq("[owner]"),
                any());
        verify(user, never()).sendMessage("commands.admin.team.kick.success-all");
        // 3 events: TeamEvent + IslandEvent (2 callEvent calls in IslandEvent.build)
        verify(pim, times(3)).callEvent(any());
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)}.
     * --all flag kicks target from all islands in the world.
     */
    @Test
    void testExecuteAllIslands() {
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island, island2));
        String name = "target";
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of(name, "--all")));
        assertTrue(itl.execute(user, itl.getLabel(), List.of(name, "--all")));
        // island is owned by uuid (admin); kickFromIsland skips it because user IS the owner
        verify(im, never()).removePlayer(island, notUUID);
        verify(im).removePlayer(island2, notUUID);
        verify(user).sendMessage("commands.admin.team.kick.success-all");
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)}.
     * When --all is used but target is on no island, execute returns false.
     */
    @Test
    void testExecuteAllNoIslands() {
        when(im.getIslands(world, notUUID)).thenReturn(List.of());
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target", "--all")));
        assertFalse(itl.execute(user, itl.getLabel(), List.of("target", "--all")));
    }

    /**
     * Test tab complete returns player list for first arg.
     */
    @Test
    void testTabCompleteFirstArg() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of(""));
        assertTrue(result.isPresent());
    }

    /**
     * Test tab complete returns --all when target is on multiple islands.
     */
    @Test
    void testTabCompleteSecondArgMultipleIslands() {
        when(pm.getUUID("target")).thenReturn(notUUID);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island, island2));
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of("target", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("--all"));
    }

    /**
     * Test tab complete does not return --all when target is on one island only.
     */
    @Test
    void testTabCompleteSecondArgSingleIsland() {
        when(pm.getUUID("target")).thenReturn(notUUID);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island2));
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of("target", ""));
        // Returns player list (no --all hint for single island)
        assertTrue(result.isPresent());
        assertFalse(result.get().contains("--all"));
    }
}
