package world.bentobox.bentobox.api.commands.island.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamSetownerCommand extends CompositeCommand {

    private @Nullable UUID targetUUID;

    public IslandTeamSetownerCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "setowner");
    }

    @Override
    public void setup() {
        setPermission("island.team.setowner");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.setowner.parameters");
        setDescription("commands.island.team.setowner.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Can use if in a team
        Island is = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (is == null || !is.inTeam(user.getUniqueId())) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        UUID ownerUUID = is.getOwner();
        if (ownerUUID == null || !ownerUUID.equals(user.getUniqueId())) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }
        targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        if (targetUUID.equals(user.getUniqueId())) {
            user.sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
            return false;
        }
        if (!is.inTeam(targetUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.target-is-not-member");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        return setOwner(user, targetUUID);

    }

    protected boolean setOwner(User user, @NonNull UUID targetUUID2) {
        // Fire event so add-ons can run commands, etc.
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent e = TeamEvent.builder().island(island).reason(TeamEvent.Reason.SETOWNER)
                .involvedPlayer(targetUUID2).build();
        if (e.isCancelled()) {
            return false;
        }
        getIslands().setOwner(getWorld(), user, targetUUID2);
        // Call the event for the new owner
        IslandEvent.builder().island(island).involvedPlayer(targetUUID2).admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(User.getInstance(targetUUID2)), RanksManager.OWNER_RANK).build();
        // Call the event for the previous owner
        IslandEvent.builder().island(island).involvedPlayer(user.getUniqueId()).admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE).rankChange(RanksManager.OWNER_RANK, RanksManager.SUB_OWNER_RANK)
                .build();
        // Add historu record
        island.log(new LogEntry.Builder(LogType.NEWOWNER).data(targetUUID2.toString(), "new owner")
                .data(user.getUniqueId().toString(), "old owner").build());
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()) == null) {
            return Optional.empty();
        }
        return Optional.of(Util.tabLimit(
                getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()).getMemberSet().stream()
                        .filter(uuid -> !user.getUniqueId().equals(uuid)).map(getPlayers()::getName).toList(),
                lastArg));
    }

}
