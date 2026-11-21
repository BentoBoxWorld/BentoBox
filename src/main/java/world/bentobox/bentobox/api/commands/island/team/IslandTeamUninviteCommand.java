package world.bentobox.bentobox.api.commands.island.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Withdraws a pending team, trust or coop invite: {@code /island team uninvite <player>}.
 *
 * <p>The player who sent the invite can always withdraw it. So can anyone on the
 * same island whose rank is high enough to invite in the first place. The command
 * shares the invite command's permission, so no new permission node is needed.
 *
 * @since 3.23.8
 */
public class IslandTeamUninviteCommand extends CompositeCommand {

    private final IslandTeamCommand itc;

    public IslandTeamUninviteCommand(IslandTeamCommand parent) {
        super(parent, "uninvite");
        this.itc = parent;
    }

    @Override
    public void setup() {
        setPermission("island.team.invite");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.kick.parameters");
        setDescription("commands.island.team.invite.cancel.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        Island island = getIslands().getIsland(getWorld(), user);
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        @Nullable
        TeamInvite invite = itc.isInvited(targetUUID) ? itc.getInvite(targetUUID) : null;
        // Only invites to this island can be withdrawn from it
        if (invite == null || !island.getUniqueId().equals(invite.getIslandID())) {
            user.sendMessage("commands.island.team.invite.cancel.no-invite", TextVariables.NAME, args.getFirst());
            return false;
        }
        // The inviter may always withdraw; otherwise the rank needed to invite is required
        int rank = island.getRank(user);
        if (!user.getUniqueId().equals(invite.getInviter())
                && rank < island.getRankCommand(itc.getInviteCommand().getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        // Fire event so add-ons can react or veto
        IslandBaseEvent e = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.UNINVITE)
                .involvedPlayer(targetUUID)
                .build();
        if (e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled())) {
            return false;
        }
        itc.removeInvite(targetUUID);
        user.sendMessage("commands.island.team.invite.cancel.success", TextVariables.NAME, args.getFirst());
        return true;
    }

    /**
     * Offers the names of players who have an open invite to the user's island.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user);
        if (island == null) {
            return Optional.empty();
        }
        String lastArg = args.isEmpty() ? "" : args.getLast();
        List<String> options = itc.getInvitees(island).stream().map(getPlayers()::getName)
                .filter(name -> name != null && !name.isEmpty()).sorted().toList();
        return Optional.of(Util.tabLimit(new ArrayList<>(options), lastArg));
    }
}
