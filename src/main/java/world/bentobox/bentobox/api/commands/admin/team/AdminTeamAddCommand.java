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

public class AdminTeamAddCommand extends CompositeCommand {

    public AdminTeamAddCommand(CompositeCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setPermission("admin.team");
        setParametersHelp("commands.admin.team.add.parameters");
        setDescription("commands.admin.team.add.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }
        // Get owner and target
        UUID ownerUUID = getPlayers().getUUID(args.get(0));
        if (ownerUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.get(1));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(1));
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), ownerUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(getWorld(), ownerUUID) && !getIslands().getOwner(getWorld(), ownerUUID).equals(ownerUUID)) {
            user.sendMessage("commands.admin.team.add.name-not-owner", TextVariables.NAME, args.get(0));
            getIslands().getIsland(getWorld(), ownerUUID).showMembers(user);
            return false;
        }
        if (getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }
        if (getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("commands.admin.team.add.name-has-island", TextVariables.NAME, args.get(1));
            return false;
        }
        // Success
        User target = User.getInstance(targetUUID);
        User owner = User.getInstance(ownerUUID);
        owner.sendMessage("commands.island.team.invite.accept.name-joined-your-island", TextVariables.NAME, getPlugin().getPlayers().getName(targetUUID));
        target.sendMessage("commands.island.team.invite.accept.you-joined-island", TextVariables.LABEL, getTopLabel());
        Island teamIsland = getIslands().getIsland(getWorld(), ownerUUID);
        if (teamIsland != null) {
            getIslands().setJoinTeam(teamIsland, targetUUID);
            user.sendMessage("general.success");
            IslandBaseEvent event = TeamEvent.builder()
                    .island(teamIsland)
                    .reason(TeamEvent.Reason.JOINED)
                    .involvedPlayer(targetUUID)
                    .admin(true)
                    .build();
            Bukkit.getServer().getPluginManager().callEvent(event);
            return true;
        } else {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

    }


}
