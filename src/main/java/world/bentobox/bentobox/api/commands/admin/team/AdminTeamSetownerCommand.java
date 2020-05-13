package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Sets the owner of an island.
 * @author tastybento
 */
public class AdminTeamSetownerCommand extends CompositeCommand {

    public AdminTeamSetownerCommand(CompositeCommand parent) {
        super(parent, "setowner");
    }

    @Override
    public void setup() {
        setPermission("mod.team");
        setParametersHelp("commands.admin.team.setowner.parameters");
        setDescription("commands.admin.team.setowner.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }

        UUID previousOwnerUUID = getIslands().getOwner(getWorld(), targetUUID);
        if (targetUUID.equals(previousOwnerUUID)) {
            user.sendMessage("commands.admin.team.setowner.already-owner", TextVariables.NAME, args.get(0));
            return false;
        }

        // Get the User corresponding to the current owner
        User previousOwner = User.getInstance(previousOwnerUUID);
        User target = User.getInstance(targetUUID);

        // Fire event so add-ons know
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        // Call the setowner event
        TeamEvent.builder()
        .island(island)
        .reason(TeamEvent.Reason.SETOWNER)
        .involvedPlayer(targetUUID)
        .admin(true)
        .build();

        // Call the rank change event for the new island owner
        // We need to call it BEFORE the actual change, in order to retain the player's previous rank.
        IslandEvent.builder()
        .island(island)
        .involvedPlayer(targetUUID)
        .admin(true)
        .reason(IslandEvent.Reason.RANK_CHANGE)
        .rankChange(island.getRank(target), RanksManager.OWNER_RANK)
        .build();

        // Make new owner
        getIslands().setOwner(getWorld(), user, targetUUID);
        user.sendMessage("commands.admin.team.setowner.success", TextVariables.NAME, args.get(0));

        // Call the rank change event for the old island owner
        // We need to call it AFTER the actual change.
        IslandEvent.builder()
        .island(island)
        .involvedPlayer(previousOwnerUUID)
        .admin(true)
        .reason(IslandEvent.Reason.RANK_CHANGE)
        .rankChange(RanksManager.OWNER_RANK, island.getRank(previousOwner))
        .build();

        return true;
    }
}
