package world.bentobox.bentobox.api.commands.admin.team;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.util.Vector;
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

    private static final String XYZ = "0,0,0";

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
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getWorld()).thenReturn(world);

        // island2 is owned by notUUID (the target) and has a team
        when(island2.getOwner()).thenReturn(notUUID);
        when(island2.hasTeam()).thenReturn(true);
        when(island2.getCenter()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));

        // Target (notUUID) is in a team and is a member of island2
        when(im.inTeam(any(), eq(notUUID))).thenReturn(true);
        // By default, target is on island2 only
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
     * Test that a player in a team on a single island can be kicked with 1 arg.
     */
    @Test
    void testCanExecuteSuccess() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
    }

    /**
     * Test that a player on multiple islands requires an xyz arg.
     */
    @Test
    void testCanExecuteMultipleIslandsRequiresXyz() {
        // Set up island (admin-owned) with a different center location so it gets a unique xyz key
        when(island.getOwner()).thenReturn(uuid);
        when(island.hasTeam()).thenReturn(true);
        org.bukkit.Location loc2 = mock(org.bukkit.Location.class);
        when(loc2.toVector()).thenReturn(new Vector(100, 64, 100));
        when(island.getCenter()).thenReturn(loc2);
        when(im.getIslands(world, notUUID)).thenReturn(List.of(island, island2));
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
    }

    /**
     * Test that an unknown xyz arg gives an error.
     */
    @Test
    void testCanExecuteUnknownIslandLocation() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target", "9,9,9")));
        verify(user).sendMessage("commands.admin.unregister.errors.unknown-island-location");
    }

    /**
     * Test that a valid xyz arg selects the correct island.
     */
    @Test
    void testCanExecuteWithValidXyz() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target", XYZ)));
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)}.
     * Target on one island; kicked with 1 arg.
     */
    @Test
    void testExecuteSingleIsland() {
        String name = "target";
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList(name)));
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList(name)));
        verify(im).removePlayer(island2, notUUID);
        verify(user).sendMessage(eq("commands.admin.team.kick.success"), eq(TextVariables.NAME), any(), eq("[owner]"),
                any());
        // 3 events: TeamEvent + IslandEvent (IslandEvent.build fires 2 callEvent calls)
        verify(pim, times(3)).callEvent(any());
    }

    /**
     * Test method for {@link AdminTeamKickCommand#execute(User, String, List)}.
     * Target on multiple islands; kicked with explicit xyz.
     */
    @Test
    void testExecuteWithXyz() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target", XYZ)));
        assertTrue(itl.execute(user, itl.getLabel(), List.of("target", XYZ)));
        verify(im).removePlayer(island2, notUUID);
        verify(user).sendMessage(eq("commands.admin.team.kick.success"), eq(TextVariables.NAME), any(), eq("[owner]"),
                any());
        verify(pim, times(3)).callEvent(any());
    }

    /**
     * Test tab complete with no args returns empty.
     */
    @Test
    void testTabCompleteNoArgs() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of(""));
        assertTrue(result.isEmpty());
    }

    /**
     * Test tab complete for second arg returns xyz of the target's team islands.
     */
    @Test
    void testTabCompleteSecondArg() {
        when(pm.getUUID("target")).thenReturn(notUUID);
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of("target", ""));
        assertTrue(result.isPresent());
        assertTrue(result.get().contains(XYZ));
    }

    /**
     * Test tab complete for second arg returns empty when player is unknown.
     */
    @Test
    void testTabCompleteSecondArgUnknownPlayer() {
        when(pm.getUUID("unknown")).thenReturn(null);
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        Optional<List<String>> result = itl.tabComplete(user, "", List.of("unknown", ""));
        assertTrue(result.isEmpty());
    }
}
