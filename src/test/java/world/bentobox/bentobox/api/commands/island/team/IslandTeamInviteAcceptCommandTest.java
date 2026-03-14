package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableSet;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent.TeamEventBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
class IslandTeamInviteAcceptCommandTest extends RanksManagerTestSetup {

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
    void testIslandTeamInviteAcceptCommand() {
        assertEquals("accept", c.getLabel());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#setup()}.
     */
    @Test
    void testSetup() {
        // TODO: test permission inheritance?
        assertTrue(c.isOnlyPlayer());
        assertEquals("commands.island.team.invite.accept.description", c.getDescription());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteNoInvite() {
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.island.team.invite.errors.none-invited-you");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteInTeam() {
        when(itc.isInvited(any())).thenReturn(true);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#canExecute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testCanExecuteInvalidInvite() {
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
    void testCanExecuteSubOwnerRankInvite() {
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
    void testCanExecuteInvalidInviteNull() {
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
    void testCanExecuteOkay() {
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
    void testCanExecuteOkayTrust() {
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
    void testCanExecuteOkayCoop() {
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
    void testCanExecuteEventBlocked() {
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
        mockedTeamEvent.when(TeamEvent::builder).thenReturn(teb);
        assertFalse(c.canExecute(user, "accept", Collections.emptyList()));
        verify(user, never()).sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
        verify(user, never()).sendMessage("commands.island.team.invite.errors.invalid-invite");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfString() {
        // Team
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).getTranslation("commands.island.team.invite.accept.confirmation");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringCoop() {
        // Coop
        when(invite.getType()).thenReturn(Type.COOP);
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteAcceptCommand#execute(world.bentobox.bentobox.api.user.User, java.lang.String, java.util.List)}.
     */
    @Test
    void testExecuteUserStringListOfStringTrust() {
        // Trust
        when(invite.getType()).thenReturn(Type.TRUST);
        assertTrue(c.execute(user, "accept", Collections.emptyList()));
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", "0");
    }

    /**
     * Test that XP is reset when accepting a team invite and isDisallowTeamMemberIslands is true (default).
     */
    @Test
    void testAcceptTeamInvite_xpResetWhenDisallowTeamMemberIslandsTrue() {
        // Set up world settings with isDisallowTeamMemberIslands = true (default) and isOnJoinResetXP = true
        when(itc.getWorld()).thenReturn(world);
        when(iwm.isOnJoinResetXP(any())).thenReturn(true);
        when(iwm.getOnJoinCommands(any())).thenReturn(Collections.emptyList());

        // Set up team island
        String islandId = UUID.randomUUID().toString();
        when(invite.getIslandID()).thenReturn(islandId);
        when(invite.getInviter()).thenReturn(notUUID);
        Island teamIsland = mock(Island.class);
        when(teamIsland.getOwner()).thenReturn(notUUID);
        when(teamIsland.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of());
        when(im.getIslandById(islandId)).thenReturn(Optional.of(teamIsland));
        when(im.getMaxMembers(any(), anyInt())).thenReturn(4);
        when(im.getIslands(any(), any(UUID.class))).thenReturn(Collections.emptyList());
        when(im.homeTeleportAsync(any(World.class), any(Player.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Execute
        c.acceptTeamInvite(user, invite);

        // Verify XP was reset
        verify(mockPlayer).setLevel(0);
        verify(mockPlayer).setExp(0F);
        verify(mockPlayer).setTotalExperience(0);
    }

    /**
     * Test that XP is reset when accepting a team invite and isDisallowTeamMemberIslands is false.
     * This ensures XP resets also work when players are allowed to keep their own islands in a team.
     */
    @Test
    void testAcceptTeamInvite_xpResetWhenDisallowTeamMemberIslandsFalse() {
        // Override world settings to return isDisallowTeamMemberIslands = false
        WorldSettings ws = new TestWorldSettings() {
            @Override
            public boolean isDisallowTeamMemberIslands() {
                return false;
            }
        };
        when(itc.getWorld()).thenReturn(world);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(iwm.isOnJoinResetXP(any())).thenReturn(true);
        when(iwm.getOnJoinCommands(any())).thenReturn(Collections.emptyList());

        // Set up team island
        String islandId = UUID.randomUUID().toString();
        when(invite.getIslandID()).thenReturn(islandId);
        when(invite.getInviter()).thenReturn(notUUID);
        Island teamIsland = mock(Island.class);
        when(teamIsland.getOwner()).thenReturn(notUUID);
        when(teamIsland.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of());
        when(im.getIslandById(islandId)).thenReturn(Optional.of(teamIsland));
        when(im.getMaxMembers(any(), anyInt())).thenReturn(4);
        when(im.homeTeleportAsync(any(World.class), any(Player.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Execute
        c.acceptTeamInvite(user, invite);

        // Verify XP was reset even though isDisallowTeamMemberIslands is false
        verify(mockPlayer).setLevel(0);
        verify(mockPlayer).setExp(0F);
        verify(mockPlayer).setTotalExperience(0);
    }

    /**
     * Test that XP is NOT reset when isOnJoinResetXP is false and isDisallowTeamMemberIslands is false.
     */
    @Test
    void testAcceptTeamInvite_xpNotResetWhenSettingDisabled() {
        // Override world settings to return isDisallowTeamMemberIslands = false
        WorldSettings ws = new TestWorldSettings() {
            @Override
            public boolean isDisallowTeamMemberIslands() {
                return false;
            }
        };
        when(itc.getWorld()).thenReturn(world);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        when(iwm.isOnJoinResetXP(any())).thenReturn(false);
        when(iwm.getOnJoinCommands(any())).thenReturn(Collections.emptyList());

        // Set up team island
        String islandId = UUID.randomUUID().toString();
        when(invite.getIslandID()).thenReturn(islandId);
        when(invite.getInviter()).thenReturn(notUUID);
        Island teamIsland = mock(Island.class);
        when(teamIsland.getOwner()).thenReturn(notUUID);
        when(teamIsland.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of());
        when(im.getIslandById(islandId)).thenReturn(Optional.of(teamIsland));
        when(im.getMaxMembers(any(), anyInt())).thenReturn(4);
        when(im.homeTeleportAsync(any(World.class), any(Player.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Execute
        c.acceptTeamInvite(user, invite);

        // Verify XP was NOT reset
        verify(mockPlayer, never()).setLevel(0);
        verify(mockPlayer, never()).setExp(0F);
        verify(mockPlayer, never()).setTotalExperience(0);
    }

}
