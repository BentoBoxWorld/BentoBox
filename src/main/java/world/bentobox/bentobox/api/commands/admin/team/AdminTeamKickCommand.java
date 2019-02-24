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

public class AdminTeamKickCommand extends CompositeCommand {

    public AdminTeamKickCommand(CompositeCommand parent) {
        super(parent, "kick");

    }

    @Override
    public void setup() {
        setPermission("admin.team");
        setParametersHelp("commands.admin.team.kick.parameters");
        setDescription("commands.admin.team.kick.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
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
            user.sendMessage("commands.admin.team.kick.not-in-team");
            return false;
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID.equals(getIslands().getOwner(getWorld(), targetUUID))) {
            user.sendMessage("commands.admin.team.kick.cannot-kick-owner");
            getIslands().getIsland(getWorld(), targetUUID).showMembers(user);
            return false;
        }
        User.getInstance(targetUUID).sendMessage("commands.admin.team.kick.admin-kicked");
        getIslands().removePlayer(getWorld(), targetUUID);
        user.sendMessage("general.success");
        // Fire event so add-ons know
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        IslandBaseEvent event = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.KICK)
                .involvedPlayer(targetUUID)
                .admin(true)
                .build();
        Bukkit.getServer().getPluginManager().callEvent(event);
        return true;
    }
}
