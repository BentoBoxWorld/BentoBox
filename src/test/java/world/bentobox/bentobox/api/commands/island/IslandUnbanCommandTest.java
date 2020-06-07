package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandUnbanCommandTest {

    @Mock
    private CompositeCommand ic;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        User.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Server and Plugin Manager for events
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);


    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
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
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    /**
     * Test method for {@link IslandUnbanCommand#canExecute(User, String, List)}
     */
    @Test
    public void testUnknownUser() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        UUID targetUUID = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUUID);
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(User.getInstance(any(UUID.class))).thenReturn(targetUser);
        // Mark as banned
        when(island.isBanned(eq(targetUUID))).thenReturn(true);

        // Allow removing from ban list
        when(island.unban(any(), any())).thenReturn(true);

        assertTrue(iubc.execute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.unban.player-unbanned", TextVariables.NAME, targetUser.getName());
        verify(targetUser).sendMessage("commands.island.unban.you-are-unbanned", TextVariables.NAME, user.getName());
    }

    /**
     * Test method for {@link IslandUnbanCommand#execute(User, String, List)}
     */
    @Test
    public void testCancelledUnban() {
        IslandUnbanCommand iubc = new IslandUnbanCommand(ic);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        UUID targetUUID = UUID.randomUUID();
        when(pm.getUUID(Mockito.anyString())).thenReturn(targetUUID);
        PowerMockito.mockStatic(User.class);
        User targetUser = mock(User.class);
        when(targetUser.isOp()).thenReturn(false);
        when(targetUser.isPlayer()).thenReturn(true);
        when(targetUser.isOnline()).thenReturn(false);
        when(User.getInstance(any(UUID.class))).thenReturn(targetUser);
        // Mark as banned
        when(island.isBanned(eq(targetUUID))).thenReturn(true);

        // Allow removing from ban list
        when(island.unban(any(), any())).thenReturn(false);

        assertFalse(iubc.execute(user, iubc.getLabel(), Collections.singletonList("bill")));
        verify(user, never()).sendMessage("commands.island.unban.player-unbanned", TextVariables.NAME, targetUser.getName());
        verify(targetUser, never()).sendMessage("commands.island.unban.you-are-unbanned", "[owner]", user.getName());
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
        String[] names = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};
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
