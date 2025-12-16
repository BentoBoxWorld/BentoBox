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
public class AdminTeamKickCommandTest extends CommonTestSetup {

    @Mock
    private CompositeCommand ac;
    private UUID uuid;
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

        // Player
        when(user.isOp()).thenReturn(false);
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

        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island2.getOwner()).thenReturn(notUUID);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(im.getIslands(world, uuid)).thenReturn(List.of(island, island2));

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

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
    public void testCanExecuteNoTarget() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertFalse(itl.canExecute(user, itl.getLabel(), new ArrayList<>()));
        // Show help
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "tastybento");
    }

    /**
     * Test method for {@link AdminTeamKickCommand#canExecute(User, String, List)}.
     */
    @Test
    public void testCanExecutePlayerNotInTeam() {
        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        when(pm.getUUID(any())).thenReturn(notUUID);
        // when(im.getMembers(any(), any())).thenReturn(new HashSet<>());
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.admin.team.kick.not-in-team"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.admin.team.AdminTeamKickCommand#execute(User, String, List)}.
     */
    @Test
    public void testExecute() {
        when(im.inTeam(any(), any())).thenReturn(true);
        String name = "tastybento";
        when(pm.getUUID(any())).thenReturn(uuid);
        when(pm.getName(any())).thenReturn(name);

        AdminTeamKickCommand itl = new AdminTeamKickCommand(ac);
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList(name)));
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList(name)));
        verify(im, never()).removePlayer(island, uuid);
        verify(im).removePlayer(island2, uuid);
        verify(user).sendMessage(eq("commands.admin.team.kick.success"), eq(TextVariables.NAME), any(), eq("[owner]"), eq(name));
        // Offline so event will be called 3 times
        verify(pim, times(3)).callEvent(any());
    }

}
