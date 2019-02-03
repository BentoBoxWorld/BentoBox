package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminTeamSetownerCommand extends CompositeCommand {

    public AdminTeamSetownerCommand(CompositeCommand parent) {
        super(parent, "setowner");
    }

    @Override
    public void setup() {
        setPermission("admin.team");
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
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.not-in-team");
            return false;
        }
        if (getIslands().getOwner(getWorld(), targetUUID).equals(targetUUID)) {
            user.sendMessage("commands.admin.team.setowner.already-owner");
            return false;
        }
        // Make new owner
        getIslands().setOwner(getWorld(), user, targetUUID);
        user.sendMessage("general.success");
        // Fire event so add-ons know
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        IslandBaseEvent event = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.SETOWNER)
                .involvedPlayer(targetUUID)
                .admin(true)
                .build();
        Bukkit.getServer().getPluginManager().callEvent(event);
        return true;
    }
}
