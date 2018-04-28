package us.tastybento.bskyblock.commands.island.teams;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.team.TeamEvent;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandTeamSetownerCommand extends AbstractIslandTeamCommand {

    public IslandTeamSetownerCommand(IslandTeamCommand islandTeamCommand) {
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
        boolean inTeam = getPlugin().getPlayers().inTeam(playerUUID);
        UUID teamLeaderUUID = getTeamLeader(user);
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
        if (!getPlayers().inTeam(playerUUID)) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (targetUUID.equals(playerUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
            return false;
        }
        if (!getPlugin().getIslands().getMembers(playerUUID).contains(targetUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.target-is-not-member");
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent event = TeamEvent.builder()
                .island(getIslands()
                        .getIsland(playerUUID))
                .reason(TeamEvent.Reason.MAKELEADER)
                .involvedPlayer(targetUUID)
                .build();
        getPlugin().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        makeLeader(user, targetUUID);

        getIslands().save(true);
        return true;
    }

    private void makeLeader(User user, UUID targetUUID) {
        // target is the new leader
        Island island = getIslands().getIsland(user.getUniqueId());
        island.setOwner(targetUUID);
        user.sendMessage("commands.island.team.setowner.name-is-the-owner", "[name]", getPlayers().getName(targetUUID));

        // Check if online
        User target = User.getInstance(targetUUID);
        target.sendMessage("commands.island.team.setowner.you-are-the-owner");
        if (target.isOnline()) {
            // Check if new leader has a lower range permission than the island size
            int range = getMaxRangeSize(user);
            // Range can go up or down
            if (range != island.getProtectionRange()) {
                user.sendMessage("commands.admin.setrange.range-updated", "[number]", String.valueOf(range));
                target.sendMessage("commands.admin.setrange.range-updated", "[number]", String.valueOf(range));
                getPlugin().log("Makeleader: Island protection range changed from " + island.getProtectionRange() + " to "
                                + range + " for " + user.getName() + " due to permission.");
            }
            island.setProtectionRange(range);

        }
    }

    @Override
    public Optional<List<String>> tabComplete(final User user, final String alias, final LinkedList<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = (!args.isEmpty() ? args.getLast() : "");
        for (UUID member : getPlugin().getIslands().getMembers(user.getUniqueId())) {
            options.add(getPlugin().getServer().getOfflinePlayer(member).getName());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}