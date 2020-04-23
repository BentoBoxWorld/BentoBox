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

public class AdminTeamDisbandCommand extends CompositeCommand {

    public AdminTeamDisbandCommand(CompositeCommand parent) {
        super(parent, "disband");
    }

    @Override
    public void setup() {
        setPermission("mod.team");
        setParametersHelp("commands.admin.team.disband.parameters");
        setDescription("commands.admin.team.disband.description");
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
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (!getIslands().getOwner(getWorld(), targetUUID).equals(targetUUID)) {
            user.sendMessage("commands.admin.team.disband.use-disband-owner", "[owner]", getPlayers().getName(getIslands().getOwner(getWorld(), targetUUID)));
            return false;
        }
        // Disband team
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        getIslands().getMembers(getWorld(), targetUUID).forEach(m -> {
            User mUser = User.getInstance(m);
            mUser.sendMessage("commands.admin.team.disband.disbanded");
            // The owner gets to keep the island
            if (!m.equals(targetUUID)) {
                getIslands().setLeaveTeam(getWorld(), m);
                TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.KICK)
                .involvedPlayer(m)
                .admin(true)
                .build();
                IslandEvent.builder()
                .island(island)
                .involvedPlayer(targetUUID)
                .admin(true)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(mUser), RanksManager.VISITOR_RANK)
                .build();
            }
        });
        user.sendMessage("commands.admin.team.disband.success", TextVariables.NAME, args.get(0));
        return true;
    }
}
