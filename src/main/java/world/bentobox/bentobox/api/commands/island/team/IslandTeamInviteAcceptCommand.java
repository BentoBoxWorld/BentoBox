package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 */
public class IslandTeamInviteAcceptCommand extends ConfirmableCommand {

    private IslandTeamCommand itc;
    private UUID playerUUID;
    private UUID prospectiveOwnerUUID;

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
        playerUUID = user.getUniqueId();
        // Check if player has been invited
        if (!itc.isInvited(playerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.none-invited-you");
            return false;
        }
        // Get the island owner
        prospectiveOwnerUUID = itc.getInviter(playerUUID);
        if (prospectiveOwnerUUID == null) {
            user.sendMessage("commands.island.team.invite.errors.invalid-invite");
            return false;
        }
        // Check rank to of inviter
        Island island = getIslands().getIsland(getWorld(), prospectiveOwnerUUID);
        String inviteUsage = getParent().getSubCommand("invite").map(CompositeCommand::getUsage).orElse("");
        if (island == null || island.getRank(prospectiveOwnerUUID) < island.getRankCommand(inviteUsage)) {
            user.sendMessage("commands.island.team.invite.errors.invalid-invite");
            itc.removeInvite(playerUUID);
            return false;
        }
        Invite invite = itc.getInvite(playerUUID);
        if (invite.getType().equals(Type.TEAM)) {
            // Check if player is already in a team
            if (getIslands().inTeam(getWorld(), playerUUID)) {
                user.sendMessage("commands.island.team.invite.errors.you-already-are-in-team");
                return false;
            }
            // Fire event so add-ons can run commands, etc.
            return !TeamEvent.builder()
                    .island(getIslands().getIsland(getWorld(), prospectiveOwnerUUID))
                    .reason(TeamEvent.Reason.JOIN)
                    .involvedPlayer(playerUUID)
                    .build()
                    .isCancelled();
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get the invite
        Invite invite = itc.getInvite(playerUUID);
        switch (invite.getType()) {
        case COOP:
            askConfirmation(user, () -> acceptCoopInvite(user, invite));
            break;
        case TRUST:
            askConfirmation(user, () -> acceptTrustInvite(user, invite));
            break;
        default:
            askConfirmation(user, user.getTranslation("commands.island.team.invite.accept.confirmation"), () -> acceptTeamInvite(user, invite));
        }
        return true;
    }

    private void acceptTrustInvite(User user, Invite invite) {
        // Remove the invite
        itc.removeInvite(playerUUID);
        User inviter = User.getInstance(invite.getInviter());
        if (inviter != null) {
            Island island = getIslands().getIsland(getWorld(), inviter);
            if (island != null) {
                island.setRank(user, RanksManager.TRUSTED_RANK);
                IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(user), RanksManager.TRUSTED_RANK)
                .build();
                inviter.sendMessage("commands.island.team.trust.success", TextVariables.NAME, user.getName());
                user.sendMessage("commands.island.team.trust.you-are-trusted", TextVariables.NAME, inviter.getName());
            }
        }
    }

    private void acceptCoopInvite(User user, Invite invite) {
        // Remove the invite
        itc.removeInvite(playerUUID);
        User inviter = User.getInstance(invite.getInviter());
        if (inviter != null) {
            Island island = getIslands().getIsland(getWorld(), inviter);
            if (island != null) {
                island.setRank(user, RanksManager.COOP_RANK);
                IslandEvent.builder()
                .island(island)
                .involvedPlayer(user.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(user), RanksManager.COOP_RANK)
                .build();
                inviter.sendMessage("commands.island.team.coop.success", TextVariables.NAME, user.getName());
                user.sendMessage("commands.island.team.coop.you-are-a-coop-member", TextVariables.NAME, inviter.getName());
            }
        }
    }

    private void acceptTeamInvite(User user, Invite invite) {
        // Remove the invite
        itc.removeInvite(playerUUID);
        // Get the player's island - may be null if the player has no island
        Island island = getIslands().getIsland(getWorld(), playerUUID);
        // Get the team's island
        Island teamIsland = getIslands().getIsland(getWorld(), prospectiveOwnerUUID);
        // Remove player as owner of the old island
        getIslands().removePlayer(getWorld(), playerUUID);
        // Remove money inventory etc. for leaving
        cleanPlayer(user);
        // Add the player as a team member of the new island
        getIslands().setJoinTeam(teamIsland, playerUUID);
        //Move player to team's island
        getPlayers().clearHomeLocations(getWorld(), playerUUID);
        getIslands().homeTeleportAsync(getWorld(), user.getPlayer()).thenRun(() -> {
            // Delete the old island
            if (island != null) {
                getIslands().deleteIsland(island, true, user.getUniqueId());
            }
            // Put player back into normal mode
            user.setGameMode(getIWM().getDefaultGameMode(getWorld()));

        });
        // Reset deaths
        if (getIWM().isTeamJoinDeathReset(getWorld())) {
            getPlayers().setDeaths(getWorld(), playerUUID, 0);
        }
        user.sendMessage("commands.island.team.invite.accept.you-joined-island", TextVariables.LABEL, getTopLabel());
        User inviter = User.getInstance(invite.getInviter());
        if (inviter != null) {
            inviter.sendMessage("commands.island.team.invite.accept.name-joined-your-island", TextVariables.NAME, user.getName());
        }
        getIslands().save(teamIsland);
        // Fire event
        TeamEvent.builder()
        .island(getIslands().getIsland(getWorld(), prospectiveOwnerUUID))
        .reason(TeamEvent.Reason.JOINED)
        .involvedPlayer(playerUUID)
        .build();
        IslandEvent.builder()
        .island(teamIsland)
        .involvedPlayer(user.getUniqueId())
        .admin(false)
        .reason(IslandEvent.Reason.RANK_CHANGE)
        .rankChange(teamIsland.getRank(user), RanksManager.MEMBER_RANK)
        .build();
    }

    private void cleanPlayer(User user) {
        if (getIWM().isOnLeaveResetEnderChest(getWorld()) || getIWM().isOnJoinResetEnderChest(getWorld())) {
            user.getPlayer().getEnderChest().clear();
        }
        if (getIWM().isOnLeaveResetInventory(getWorld()) || getIWM().isOnJoinResetInventory(getWorld())) {
            user.getPlayer().getInventory().clear();
        }
        if (getSettings().isUseEconomy() && (getIWM().isOnLeaveResetMoney(getWorld()) || getIWM().isOnJoinResetMoney(getWorld()))) {
            getPlugin().getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }

        // Reset the health
        if (getIWM().isOnJoinResetHealth(getWorld())) {
            user.getPlayer().setHealth(20.0D);
        }

        // Reset the hunger
        if (getIWM().isOnJoinResetHunger(getWorld())) {
            user.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (getIWM().isOnJoinResetXP(getWorld())) {
            user.getPlayer().setTotalExperience(0);
        }
    }
}
