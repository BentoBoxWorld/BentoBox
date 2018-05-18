package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamSetownerCommand extends AbstractIslandTeamCommand {

    public IslandTeamSetownerCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "setleader");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.team");
        setOnlyPlayer(true);
        setParameters("commands.island.team.setowner.parameters");
        setDescription("commands.island.team.setowner.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Can use if in a team
        boolean inTeam = getPlugin().getIslands().inTeam(user.getWorld(), playerUUID);
        UUID teamLeaderUUID = getTeamLeader(user.getWorld(), user);
        if (!(inTeam && teamLeaderUUID.equals(playerUUID))) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().inTeam(user.getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (targetUUID.equals(playerUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
            return false;
        }
        if (!getPlugin().getIslands().getMembers(user.getWorld(), playerUUID).contains(targetUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.target-is-not-member");
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                        .getIsland(user.getWorld(), playerUUID))
                .reason(TeamEvent.Reason.MAKELEADER)
                .involvedPlayer(targetUUID)
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        getIslands().makeLeader(user.getWorld(), user, targetUUID);
        getIslands().save(true);
        return true;
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        for (UUID member : getPlugin().getIslands().getMembers(user.getWorld(), user.getUniqueId())) {
            options.add(getPlugin().getServer().getOfflinePlayer(member).getName());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}