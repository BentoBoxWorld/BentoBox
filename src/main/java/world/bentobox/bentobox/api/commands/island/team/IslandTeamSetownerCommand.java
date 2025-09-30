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

/**
 * Handles the transfer of island ownership between team members.
 * <p>
 * Features:
 * <ul>
 *   <li>Transfer island ownership to another team member</li>
 *   <li>Automatic rank adjustments for old and new owner</li>
 *   <li>Event firing for addons</li>
 *   <li>History logging of ownership changes</li>
 * </ul>
 * <p>
 * Restrictions:
 * <ul>
 *   <li>Only the current owner can transfer ownership</li>
 *   <li>Target must be a team member</li>
 *   <li>Cannot transfer to self</li>
 *   <li>Requires specific permission</li>
 * </ul>
 */
public class IslandTeamSetownerCommand extends CompositeCommand {

    /** UUID of target player to receive ownership - stored between canExecute and execute */
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

    /**
     * Validates ownership transfer requirements:
     * - Command sender must be in a team
     * - Command sender must be the owner
     * - Target player must exist and be on the team
     * - Cannot transfer to self
     */
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
        targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
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

    /**
     * Processes the ownership transfer:
     * - Fires pre-transfer event
     * - Updates island ownership
     * - Fires rank change events for both players
     * - Logs the transfer in island history
     * 
     * @param user current owner executing the command
     * @param targetUUID2 new owner's UUID
     * @return true if transfer successful, false if cancelled
     */
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

    /**
     * Provides tab completion for team member names.
     * Only shows team members excluding the current owner.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        if (getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()) == null) {
            return Optional.empty();
        }
        return Optional.of(Util.tabLimit(
                getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()).getMemberSet().stream()
                        .filter(uuid -> !user.getUniqueId().equals(uuid)).map(getPlayers()::getName).toList(),
                lastArg));
    }

}
