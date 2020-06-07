package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class })
public class IslandTeamInviteCommandTest {

    @Mock
    private IslandTeamCommand ic;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private PlayersManager pm;
    @Mock
    private Settings s;
    @Mock
    private User target;
    @Mock
    private User user;

    private UUID uuid;
    private UUID islandUUID;
    private IslandTeamInviteCommand itl;
    private UUID notUUID;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

        // Player & users
        PowerMockito.mockStatic(User.class);

        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isOnline()).thenReturn(true);
        // Permission to invite 3 more players
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        when(User.getInstance(eq(uuid))).thenReturn(user);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));

        User.setPlugin(plugin);
        // Target
        notUUID = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(notUUID);
        when(target.getPlayer()).thenReturn(p);
        when(target.isOnline()).thenReturn(true);
        when(target.getName()).thenReturn("target");
        when(User.getInstance(eq(notUUID))).thenReturn(target);


        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island
        islandUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn(islandUUID.toString());

        // Player has island to begin with
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        when(im.getOwner(any(), eq(uuid))).thenReturn(uuid);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), eq(user))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUUID(eq("tastybento"))).thenReturn(uuid);
        when(pm.getUUID(eq("target"))).thenReturn(notUUID);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn(null);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Parent command
        when(ic.getTopLabel()).thenReturn("island");

        // Ranks Manager
        RanksManager rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);

        // Command under test
        itl = new IslandTeamInviteCommand(ic);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCoolDownActive() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        itl.setCooldown(islandUUID, notUUID, 100);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("general.errors.you-must-wait"), eq(TextVariables.NUMBER), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteDifferentPlayerInTeam() {
        when(im.inTeam(any(), any())).thenReturn(true);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.already-on-team"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("general.errors.insufficient-rank"), eq(TextVariables.RANK), eq("ranks.member"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show help
        verify(user).sendMessage(eq("commands.help.header"),eq(TextVariables.LABEL),eq("commands.help.console"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOfflinePlayer() {
        when(target.isOnline()).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("general.errors.offline-player"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSamePlayer() {
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.cannot-invite-self"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSuccess() {
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(eq("target"))).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("target"));
    }


    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteFullIsland() {
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(0);
        testCanExecuteSuccess();
        assertFalse(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.island-is-full"));
        verify(user).getPermissionValue(eq("nullteam.maxsize"), eq(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessTargetHasIsland() {
        when(im.hasIsland(any(), eq(notUUID))).thenReturn(true);
        testCanExecuteSuccess();
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(user, never()).sendMessage(eq("commands.island.team.invite.removing-invite"));
        verify(ic).addInvite(eq(Invite.Type.TEAM), eq(uuid), eq(notUUID));
        verify(user).sendMessage(eq("commands.island.team.invite.invitation-sent"), eq(TextVariables.NAME), eq("target"));
        verify(target).sendMessage(eq("commands.island.team.invite.name-has-invited-you"), eq(TextVariables.NAME), eq("tastybento"));
        verify(target).sendMessage(eq("commands.island.team.invite.to-accept-or-reject"), eq(TextVariables.LABEL), eq("island"));
        verify(target).sendMessage(eq("commands.island.team.invite.you-will-lose-your-island"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessTargetHasNoIsland() {
        testCanExecuteSuccess();
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(user, never()).sendMessage(eq("commands.island.team.invite.removing-invite"));
        verify(ic).addInvite(eq(Invite.Type.TEAM), eq(uuid), eq(notUUID));
        verify(user).sendMessage(eq("commands.island.team.invite.invitation-sent"), eq(TextVariables.NAME), eq("target"));
        verify(target).sendMessage(eq("commands.island.team.invite.name-has-invited-you"), eq(TextVariables.NAME), eq("tastybento"));
        verify(target).sendMessage(eq("commands.island.team.invite.to-accept-or-reject"), eq(TextVariables.LABEL), eq("island"));
        verify(target, never()).sendMessage(eq("commands.island.team.invite.you-will-lose-your-island"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteTargetAlreadyInvited() {
        testCanExecuteSuccess();

        when(ic.isInvited(eq(notUUID))).thenReturn(true);
        // Set up invite
        when(ic.getInviter(eq(notUUID))).thenReturn(uuid);
        Invite invite = mock(Invite.class);
        when(invite.getType()).thenReturn(Type.TEAM);
        when(ic.getInvite(eq(notUUID))).thenReturn(invite);
        assertTrue(itl.execute(user, itl.getLabel(), Collections.singletonList("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(ic).removeInvite(eq(notUUID));
        verify(user).sendMessage(eq("commands.island.team.invite.removing-invite"));
    }

}
