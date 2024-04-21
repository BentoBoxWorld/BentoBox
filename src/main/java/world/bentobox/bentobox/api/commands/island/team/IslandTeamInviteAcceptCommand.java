package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class IslandTeamInviteAcceptCommand extends ConfirmableCommand {

    private static final String INVALID_INVITE = "commands.island.team.invite.errors.invalid-invite";
    private final IslandTeamCommand itc;

    public IslandTeamInviteAcceptCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "accept");
        this.itc = islandTeamCommand;
    }

    @Override
    public void setup() {
        setPermission("island.team.accept");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.accept.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Check if player has been invited
        if (!itc.isInvited(playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return false;
        }
        // Get the island owner
        UUID prospectiveOwnerUUID = itc.getInviter(playerUUID);
        if (prospectiveOwnerUUID == null) {
            user.sendMessage(INVALID_INVITE);
            return false;
        }
        TeamInvite invite = itc.getInvite(playerUUID);
        if (invite.getType().equals(Type.TEAM)) {
            // Check rank to of inviter
            Island island = getIslands().getIsland(getWorld(), prospectiveOwnerUUID);
            String inviteUsage = getParent().getSubCommand("invite").map(CompositeCommand::getUsage).orElse("");
            if (island == null || island.getRank(prospectiveOwnerUUID) < island.getRankCommand(inviteUsage)) {
                user.sendMessage(INVALID_INVITE);
                itc.removeInvite(playerUUID);
                return false;
            }

            // Check if player is already in a team
            if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()
                    && getIslands().inTeam(getWorld(), playerUUID)) {
                user.sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
                return false;
            }
            // Fire event so add-ons can run commands, etc.
            IslandBaseEvent e = TeamEvent.builder().island(getIslands().getIsland(getWorld(), prospectiveOwnerUUID))
                    .reason(TeamEvent.Reason.JOIN).involvedPlayer(playerUUID).build();
            return !e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled());

        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get the invite
        TeamInvite invite = itc.getInvite(user.getUniqueId());
        switch (invite.getType()) {
        case COOP -> askConfirmation(user, () -> acceptCoopInvite(user, invite));
        case TRUST -> askConfirmation(user, () -> acceptTrustInvite(user, invite));
        default -> {
            if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()) {
                askConfirmation(user, user.getTranslation("commands.island.team.invite.accept.confirmation"),
                        () -> acceptTeamInvite(user, invite));
            } else {
                acceptTeamInvite(user, invite);
            }
        }
        }
        return true;
    }

    void acceptTrustInvite(User user, TeamInvite invite) {
        // Remove the invite
        itc.removeInvite(user.getUniqueId());
        User inviter = User.getInstance(invite.getInviter());
        Island island = getIslands().getIslandById(invite.getIslandID()).orElse(null);
        if (island != null) {
            if (island.getMemberSet(RanksManager.TRUSTED_RANK, false).size() > getIslands().getMaxMembers(island,
                    RanksManager.TRUSTED_RANK)) {
                user.sendMessage("commands.island.team.trust.is-full");
                return;
            }
            island.setRank(user, RanksManager.TRUSTED_RANK);
            IslandEvent.builder().island(island).involvedPlayer(user.getUniqueId()).admin(false)
                    .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(island.getRank(user), RanksManager.TRUSTED_RANK)
                    .build();
            if (inviter.isOnline()) {
                inviter.sendMessage("commands.island.team.trust.success", TextVariables.NAME, user.getName(),
                        TextVariables.DISPLAY_NAME, user.getDisplayName());
            }
            if (inviter.isPlayer()) {
                user.sendMessage("commands.island.team.trust.you-are-trusted", TextVariables.NAME, inviter.getName(),
                        TextVariables.DISPLAY_NAME, inviter.getDisplayName());
            }
        }
    }

    void acceptCoopInvite(User user, TeamInvite invite) {
        // Remove the invite
        itc.removeInvite(user.getUniqueId());
        User inviter = User.getInstance(invite.getInviter());
        Island island = getIslands().getIslandById(invite.getIslandID()).orElse(null);
        if (island != null) {
            if (island.getMemberSet(RanksManager.COOP_RANK, false).size() > getIslands().getMaxMembers(island,
                    RanksManager.COOP_RANK)) {
                user.sendMessage("commands.island.team.coop.is-full");
                return;
            }
            island.setRank(user, RanksManager.COOP_RANK);
            IslandEvent.builder().island(island).involvedPlayer(user.getUniqueId()).admin(false)
                    .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(island.getRank(user), RanksManager.COOP_RANK)
                    .build();
            if (inviter.isOnline()) {
                inviter.sendMessage("commands.island.team.coop.success", TextVariables.NAME, user.getName(),
                        TextVariables.DISPLAY_NAME, user.getDisplayName());
            }
            if (inviter.isPlayer()) {
                user.sendMessage("commands.island.team.coop.you-are-a-coop-member", TextVariables.NAME,
                        inviter.getName(), TextVariables.DISPLAY_NAME, inviter.getDisplayName());
            }
        }
    }

    void acceptTeamInvite(User user, TeamInvite invite) {
        // Remove the invite
        itc.removeInvite(user.getUniqueId());
        // Get the player's island - may be null if the player has no island
        List<Island> islands = getIslands().getIslands(getWorld(), user.getUniqueId());
        // Get the team's island
        Island teamIsland = getIslands().getIslandById(invite.getIslandID()).orElse(null);
        if (teamIsland == null) {
            user.sendMessage(INVALID_INVITE);
            return;
        }
        if (teamIsland.getMemberSet(RanksManager.MEMBER_RANK, true).size() >= getIslands().getMaxMembers(teamIsland,
                RanksManager.MEMBER_RANK)) {
            user.sendMessage("commands.island.team.invite.errors.island-is-full");
            return;
        }
        if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()) {
            // Remove the player's other islands
            getIslands().removePlayer(getWorld(), user.getUniqueId());
            // Remove money inventory etc. for leaving
            cleanPlayer(user);
        }
        // Add the player as a team member of the new island
        getIslands().setJoinTeam(teamIsland, user.getUniqueId());
        // Move player to team's island
        getIslands().setPrimaryIsland(user.getUniqueId(), teamIsland);
        getIslands().homeTeleportAsync(getWorld(), user.getPlayer()).thenRun(() -> {
            if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()) {
                // Delete the old islands
                islands.forEach(island -> getIslands().deleteIsland(island, true, user.getUniqueId()));
            }
            // Put player back into normal mode
            user.setGameMode(getIWM().getDefaultGameMode(getWorld()));

            // Execute commands
            String ownerName = this.getPlayers().getName(teamIsland.getOwner());
            Util.runCommands(user, ownerName, getIWM().getOnJoinCommands(getWorld()), "join");

        });
        if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()
                && getIWM().isTeamJoinDeathReset(getWorld())) {
            // Reset deaths
            getPlayers().setDeaths(getWorld(), user.getUniqueId(), 0);
        }
        user.sendMessage("commands.island.team.invite.accept.you-joined-island", TextVariables.LABEL, getTopLabel());
        User inviter = User.getInstance(invite.getInviter());
        if (inviter.isOnline()) {
            inviter.sendMessage("commands.island.team.invite.accept.name-joined-your-island", TextVariables.NAME,
                    user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
        }
        // Fire event
        TeamEvent.builder().island(teamIsland).reason(TeamEvent.Reason.JOINED).involvedPlayer(user.getUniqueId())
                .build();
        IslandEvent.builder().island(teamIsland).involvedPlayer(user.getUniqueId()).admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(teamIsland.getRank(user), RanksManager.MEMBER_RANK)
                .build();
    }

    private void cleanPlayer(User user) {
        if (getIWM().isOnLeaveResetEnderChest(getWorld()) || getIWM().isOnJoinResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld()) || getIWM().isOnJoinResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy()
                && (getIWM().isOnLeaveResetMoney(getWorld()) || getIWM().isOnJoinResetMoney(getWorld()))) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }

        // Reset the health
        if (getIWM().isOnJoinResetHealth(getWorld())) {
            Util.resetHealth(user.getPlayer());
        }

        // Reset the hunger
        if (getIWM().isOnJoinResetHunger(getWorld())) {
            user.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (getIWM().isOnJoinResetXP(getWorld())) {
            // Player collected XP (displayed)
            user.getPlayer().setLevel(0);
            user.getPlayer().setExp(0);
            // Player total XP (not displayed)
            user.getPlayer().setTotalExperience(0);
        }

    }
}
