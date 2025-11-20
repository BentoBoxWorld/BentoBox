package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandUnbanCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.getName()).thenReturn("tastybento");
        when(user.getDisplayName()).thenReturn("&Ctastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    // Island ban command by itself

    // *** Error conditions ***
    // Unban without an island
    // Unban as not an owner
    // Unban unknown user
    // Unban self
    // Unban someone not banned

    // *** Working conditions ***
    // Unban user

    @Test
    public void testNoArgs() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), new ArrayList<>()));
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testNoIsland() {
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testTooLowRank() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testUnknownUser() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(null);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.unknown-player", "[name]", "bill");
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testBanSelf() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        when(pm.getUUID(Mockito.anyString())).thenReturn(uuid);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.unban.cannot-unban-yourself");
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testBanNotBanned() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        UUID bannedUser = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(bannedUser);
        when(island.isBanned(eq(bannedUser))).thenReturn(false);
        assertFalse(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.unban.player-not-banned");
    }

    /**
     * Test method for {@link IslandUnbanCommand#execute(User, String, List)}
     */
    @Test
    public void testUnbanUser() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        UUID targetUUID = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUUID);
        MockedStatic<User> mockedUser = Mockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(targetUser.getName()).thenReturn("target");
        when(targetUser.getDisplayName()).thenReturn("&Ctarget");
        mockedUser.when(() -> User.getInstance(any(UUID.class))).thenReturn(targetUser);
        // Mark as banned
        when(island.isBanned(eq(targetUUID))).thenReturn(true);

        // Allow removing from ban list
        when(island.unban(any(), any())).thenReturn(true);
        assertTrue(iubc.canExecute(user, iubc.getLabel(), Collections.singletonList("bill")));
        assertTrue(iubc.execute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.unban.player-unbanned", TextVariables.NAME, targetUser.getName(),
                TextVariables.DISPLAY_NAME, targetUser.getDisplayName());
        verify(targetUser).sendMessage("commands.island.unban.you-are-unbanned", TextVariables.NAME, user.getName(),
                TextVariables.DISPLAY_NAME, user.getDisplayName());
    }

    /**
     * Test method for {@link IslandUnbanCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabComplete() {
        Set<UUID> banned = new HashSet<>();
        // Add ten people to the banned list
        for (int i = 0; i < 10; i++) {
            banned.add(UUID.randomUUID());
        }
        when(island.getBanned()).thenReturn(banned);
        when(pm.getName(any())).thenReturn("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Optional<List<String>> result = iubc.tabComplete(user, "", new LinkedList<>());
        assertTrue(result.isPresent());
        String[] names = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j" };
        assertTrue(Arrays.equals(names, result.get().toArray()));
    }

    /**
     * Test method for {@link IslandUnbanCommand#tabComplete(User, String, List)}
     */
    @Test
    public void testTabCompleteNoIsland() {
        // No island
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        // Set up the user
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        // Get the tab-complete list with one argument
        LinkedList<String> args = new LinkedList<>();
        args.add("");
        Optional<List<String>> result = iubc.tabComplete(user, "", args);
        assertFalse(result.isPresent());

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("d");
        result = iubc.tabComplete(user, "", args);
        assertFalse(result.isPresent());

        // Get the tab-complete list with one letter argument
        args = new LinkedList<>();
        args.add("fr");
        result = iubc.tabComplete(user, "", args);
        assertFalse(result.isPresent());
    }
}
