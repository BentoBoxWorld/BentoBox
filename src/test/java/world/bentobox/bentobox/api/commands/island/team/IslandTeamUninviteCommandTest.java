package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Tests for {@link IslandTeamUninviteCommand} (#11).
 */
class IslandTeamUninviteCommandTest extends RanksManagerTestSetup {

    private static final String ISLAND_ID = "island-1";

    @Mock
    private IslandTeamCommand itc;
    @Mock
    private IslandTeamInviteCommand inviteCommand;
    @Mock
    private User user;
    @Mock
    private PlayersManager pm;

    private UUID invitee;
    private IslandTeamUninviteCommand cmd;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getCommandsManager()).thenReturn(mock(CommandsManager.class));
        when(plugin.getPlayers()).thenReturn(pm);
        User.setPlugin(plugin);

        uuid = UUID.randomUUID();
        invitee = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getTranslation(any())).thenReturn("");

        when(itc.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(itc.getWorld()).thenReturn(world);
        when(itc.getInviteCommand()).thenReturn(inviteCommand);
        when(inviteCommand.getUsage()).thenReturn("/island team invite");

        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(island.getUniqueId()).thenReturn(ISLAND_ID);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        when(pm.getUUID("bill")).thenReturn(invitee);

        cmd = new IslandTeamUninviteCommand(itc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void pendingInvite(UUID inviter, String islandId) {
        when(itc.isInvited(invitee)).thenReturn(true);
        when(itc.getInvite(invitee)).thenReturn(new TeamInvite(TeamInvite.Type.TEAM, inviter, invitee, islandId));
    }

    @Test
    void testNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        assertFalse(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    void testUnknownPlayer() {
        assertFalse(cmd.execute(user, "uninvite", Collections.singletonList("nobody")));
        verify(user).sendMessage("general.errors.unknown-player", TextVariables.NAME, "nobody");
    }

    @Test
    void testNoPendingInvite() {
        assertFalse(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.team.invite.cancel.no-invite", TextVariables.NAME, "bill");
        verify(itc, never()).removeInvite(any());
    }

    /** An invite to somebody else's island cannot be withdrawn from this one. */
    @Test
    void testInviteIsForAnotherIsland() {
        pendingInvite(uuid, "some-other-island");
        assertFalse(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(user).sendMessage("commands.island.team.invite.cancel.no-invite", TextVariables.NAME, "bill");
        verify(itc, never()).removeInvite(any());
    }

    /** The player who sent the invite can withdraw it whatever their rank. */
    @Test
    void testInviterWithdrawsOwnInvite() {
        pendingInvite(uuid, ISLAND_ID);
        assertTrue(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(itc).removeInvite(invitee);
        verify(user).sendMessage("commands.island.team.invite.cancel.success", TextVariables.NAME, "bill");
    }

    /** Someone else's invite needs the rank that inviting needs. */
    @Test
    void testOtherMemberNeedsInviteRank() {
        pendingInvite(UUID.randomUUID(), ISLAND_ID);
        assertFalse(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
        verify(itc, never()).removeInvite(any());
    }

    @Test
    void testOwnerWithdrawsAnotherMembersInvite() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        pendingInvite(UUID.randomUUID(), ISLAND_ID);
        assertTrue(cmd.execute(user, "uninvite", Collections.singletonList("bill")));
        verify(itc).removeInvite(invitee);
    }

    @Test
    void testTabCompleteOffersPendingInvitees() {
        UUID other = UUID.randomUUID();
        when(itc.getInvitees(island)).thenReturn(List.of(invitee, other));
        when(pm.getName(invitee)).thenReturn("bill");
        when(pm.getName(other)).thenReturn("alice");

        assertEquals(List.of("alice", "bill"), cmd.tabComplete(user, "uninvite", List.of("uninvite", "")).orElseThrow());
        assertEquals(List.of("bill"), cmd.tabComplete(user, "uninvite", List.of("uninvite", "b")).orElseThrow());
    }

    @Test
    void testTabCompleteNoIsland() {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        assertTrue(cmd.tabComplete(user, "uninvite", List.of("uninvite", "")).isEmpty());
    }
}
