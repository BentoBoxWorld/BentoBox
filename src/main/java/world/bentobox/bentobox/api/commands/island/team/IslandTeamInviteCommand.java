package world.bentobox.bentobox.api.commands.island.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamInviteCommand extends CompositeCommand {

    private final IslandTeamCommand itc;
    private @Nullable User invitedPlayer;

    public IslandTeamInviteCommand(IslandTeamCommand parent) {
        super(parent, "invite");
        itc = parent;
    }

    @Override
    public void setup() {
        setPermission("island.team.invite");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.description");
        setConfigurableRankCommand();
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        IslandsManager islandsManager = getIslands();

        // Player issuing the command must have an island or be in a team
        if (!islandsManager.inTeam(getWorld(), playerUUID) && !islandsManager.hasIsland(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        if (args.size() != 1) {
            return handleCommandWithNoArgs(user);
        }

        Island island = islandsManager.getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);

        return checkRankAndInvitePlayer(user, island, rank, args.get(0));
    }

    private boolean handleCommandWithNoArgs(User user) {
        UUID playerUUID = user.getUniqueId();
        Type inviteType = getInviteType(playerUUID);

        if (inviteType != null) {
            String name = getPlayers().getName(playerUUID);
            switch (inviteType) {
            case COOP ->  user.sendMessage("commands.island.team.invite.name-has-invited-you.coop", TextVariables.NAME, name);
            case TRUST -> user.sendMessage("commands.island.team.invite.name-has-invited-you.trust", TextVariables.NAME, name);
            default -> user.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, name);
            }
            return true;
        }

        showHelp(this, user);
        return false;
    }

    private boolean checkRankAndInvitePlayer(User user, Island island, int rank, String playerName) {
        RanksManager ranksManager = getPlugin().getRanksManager();
        PlayersManager playersManager = getPlayers();
        UUID playerUUID = user.getUniqueId();

        // Check rank to use command
        int requiredRank = island.getRankCommand(getUsage());
        if (rank < requiredRank) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(ranksManager.getRank(rank)));
            return false;
        }

        // Check for space on team
        int maxMembers = getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK);
        if (island.getMemberSet().size() >= maxMembers) {
            user.sendMessage("commands.island.team.invite.errors.island-is-full");
            return false;
        }

        UUID invitedPlayerUUID = playersManager.getUUID(playerName);
        if (invitedPlayerUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, playerName);
            return false;
        }
        // Write to field as this is used by execute method
        invitedPlayer = User.getInstance(invitedPlayerUUID);
        if (!canInvitePlayer(user, invitedPlayer)) {
            return false;
        }

        // Check cooldown
        if (this.getSettings().getInviteCooldown() > 0 && checkCooldown(user, island.getUniqueId(), invitedPlayerUUID.toString())) {
            return false;
        }

        // Player cannot invite someone already on a team
        if (getIslands().inTeam(getWorld(), invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }

        if (isInvitedByUser(invitedPlayerUUID, playerUUID) && isInviteTypeTeam(invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.you-have-already-invited");
            return false;
        }

        return true;
    }

    private Type getInviteType(UUID playerUUID) {
        if (itc.isInvited(playerUUID)) {
            Invite invite = itc.getInvite(playerUUID);
            return invite.getType();
        }
        return null;
    }

    private boolean canInvitePlayer(User user, User invitedPlayer) {
        UUID playerUUID = user.getUniqueId();
        if (!invitedPlayer.isOnline() || !user.getPlayer().canSee(invitedPlayer.getPlayer())) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        if (playerUUID.equals(invitedPlayer.getUniqueId())) {
            user.sendMessage("commands.island.team.invite.errors.cannot-invite-self");
            return false;
        }
        return true;
    }

    private boolean isInvitedByUser(UUID invitedPlayerUUID, UUID inviterUUID) {
        return itc.isInvited(invitedPlayerUUID) && itc.getInviter(invitedPlayerUUID).equals(inviterUUID);
    }

    private boolean isInviteTypeTeam(UUID invitedPlayerUUID) {
        return Objects.requireNonNull(itc.getInvite(invitedPlayerUUID)).getType().equals(Type.TEAM);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Rare case when invited player is null. Could be a race condition.
        if (invitedPlayer == null) return false;
        // If that player already has an invite out then retract it.
        // Players can only have one invite one at a time - interesting
        if (itc.isInvited(invitedPlayer.getUniqueId())) {
            itc.removeInvite(invitedPlayer.getUniqueId());
            user.sendMessage("commands.island.team.invite.removing-invite");
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent e = TeamEvent.builder()
                .island(getIslands().getIsland(getWorld(), user.getUniqueId()))
                .reason(TeamEvent.Reason.INVITE)
                .involvedPlayer(invitedPlayer.getUniqueId())
                .build();
        if (e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled())) {
            return false;
        }
        // Put the invited player (key) onto the list with inviter (value)
        // If someone else has invited a player, then this invite will overwrite the previous invite!
        itc.addInvite(Invite.Type.TEAM, user.getUniqueId(), invitedPlayer.getUniqueId());
        user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, invitedPlayer.getName(), TextVariables.DISPLAY_NAME, invitedPlayer.getDisplayName());
        // Send message to online player
        invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
        invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
        if (getIslands().hasIsland(getWorld(), invitedPlayer.getUniqueId())) {
            invitedPlayer.sendMessage("commands.island.team.invite.you-will-lose-your-island");
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
