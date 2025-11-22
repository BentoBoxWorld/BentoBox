package world.bentobox.bentobox.api.commands.island.team;

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
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamEventBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class IslandTeamInviteAcceptCommandTest extends RanksManagerTestSetup {

    @Mock
    private IslandTeamCommand itc;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Settings s;
    @Mock
    private IslandTeamInviteAcceptCommand c;
    @Mock
    private TeamInvite invite;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

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
        when(itc.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(itc.getPermissionPrefix()).thenReturn("bskyblock.");
        when(itc.getInvite(any())).thenReturn(invite);
        when(itc.getInviter(any())).thenReturn(notUUID);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        // Island
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRank(any(UUID.class))).thenReturn(RanksManager.OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        when(plugin.getPlayers()).thenReturn(pm);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(user.getTranslation(anyString())).thenReturn("mock translation2");

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        TestWorldSettings worldSettings = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSettings);

        // Invite
        when(invite.getType()).thenReturn(Type.TEAM);

        // Team invite accept command
        c = new IslandTeamInviteAcceptCommand(itc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#IslandTeamInviteAcceptCommand(world.bentobox.bentobox.api.commands.island.team.IslandTeamCommand)}.
     */
    @Test
    public void testIslandTeamInviteAcceptCommand() {
        assertEquals("accept", c.getLabel());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#setup()}.
     */
    @Test
    public void testSetup() {
        // TODO: test permission inheritance?
        assertTrue(c.isOnlyPlayer());
        assertEquals("commands.island.team.invite.accept.description", c.getDescription());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoInvite() {
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.island.team.invite.errors.none-invited-you");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteInTeam() {
        when(itc.isInvited(any())).thenReturn(true);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteInvalidInvite() {
        when(itc.isInvited(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(island.getRank(any(UUID.class))).thenReturn(RanksManager.VISITOR_RANK);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user).sendMessage("commands.island.team.invite.errors.invalid-invite");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSubOwnerRankInvite() {
        when(itc.isInvited(any())).thenReturn(true);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(island.getRank(any(UUID.class))).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.SUB_OWNER_RANK);
        assertTrue(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
        verify(pim).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteInvalidInviteNull() {
        when(itc.getInviter(any())).thenReturn(null);
        when(itc.isInvited(any())).thenReturn(true);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user).sendMessage("commands.island.team.invite.errors.invalid-invite");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOkay() {
        when(itc.isInvited(any())).thenReturn(true);
        when(itc.getInviter(any())).thenReturn(notUUID);
        when(itc.getInvite(any())).thenReturn(invite);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertTrue(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
        verify(pim).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOkayTrust() {
        when(itc.isInvited(any())).thenReturn(true);
        when(itc.getInviter(any())).thenReturn(notUUID);
        when(itc.getInvite(any())).thenReturn(invite);
        when(invite.getType()).thenReturn(Type.TRUST);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertTrue(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
        // No event
        verify(pim, never()).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOkayCoop() {
        when(itc.isInvited(any())).thenReturn(true);
        when(itc.getInviter(any())).thenReturn(notUUID);
        when(itc.getInvite(any())).thenReturn(invite);
        when(invite.getType()).thenReturn(Type.COOP);
        when(im.inTeam(any(), any())).thenReturn(false);
        assertTrue(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
        // No event
        verify(pim, never()).callEvent(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testCanExecuteEventBlocked() {
        when(itc.isInvited(any())).thenReturn(true);
        when(itc.getInviter(any())).thenReturn(notUUID);
        when(itc.getInvite(any())).thenReturn(invite);
        when(im.inTeam(any(), any())).thenReturn(false);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        // Block event
        MockedStatic<TeamEvent> mockedTeamEvent = Mockito.mockStatic(TeamEvent.class);
        TeamEventBuilder teb = mock(TeamEventBuilder.class);
        when(teb.island(any())).thenReturn(teb);
        when(teb.involvedPlayer(any())).thenReturn(teb);
        when(teb.reason(any())).thenReturn(teb);
        IslandBaseEvent ibe = mock(IslandBaseEvent.class);
        when(ibe.isCancelled()).thenReturn(true);
        when(teb.build()).thenReturn(ibe);
        mockedTeamEvent.when(() -> TeamEvent.builder()).thenReturn(teb);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfString() {
        // Team
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).getTranslation("commands.island.team.invite.accept.confirmation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringCoop() {
        // Coop
        when(invite.getType()).thenReturn(Type.COOP);
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    public void testExecuteUserStringListOfStringTrust() {
        // Trust
        when(invite.getType()).thenReturn(Type.TRUST);
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

}
