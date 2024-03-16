package world.bentobox.bentobox.api.commands.admin.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class AdminTeamDisbandCommand extends CompositeCommand {

    private Island island;
    private @Nullable UUID targetUUID;

    /**
     * Disbands a team
     * @param parent parent command
     */
    public AdminTeamDisbandCommand(CompositeCommand parent) {
        super(parent, "disband");
    }

    @Override
    public void setup() {
        setPermission("mod.team.disband");
        setParametersHelp("commands.admin.team.disband.parameters");
        setDescription("commands.admin.team.disband.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.isEmpty() || args.size() > 2) {
            showHelp(this, user);
            return false;
        }
        // Get target
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-is-not-owner", TextVariables.NAME, args.get(0));
            return false;
        }
        // Find the island the player is an owner of
        Map<String, Island> islands = getIslandsXYZ(targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        if (islands.size() > 1) {
            if (args.size() != 2 || !islands.containsKey(args.get(1))) {
                user.sendMessage("commands.admin.team.disband.more-than-one-island", TextVariables.NAME,
                        getPlayers().getName(island.getOwner()));
                islands.keySet().forEach(coords -> user.sendMessage("commands.admin.team.disband.more-than-one-island",
                        TextVariables.XYZ, coords));
                return false;
            }
            // Get the named island
            island = islands.get(args.get(1));
        } else {
            // Get the only island
            island = islands.values().iterator().next();
        }
        return true;
    }

    private Map<String, Island> getIslandsXYZ(UUID target) {
        return getIslands().getOwnedIslands(getWorld(), target).stream().filter(is -> is.getMemberSet().size() > 1) // Filter for teams
                .collect(Collectors.toMap(is -> Util.xyz(is.getCenter().toVector()), is -> is));
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Objects.requireNonNull(island);
        Objects.requireNonNull(targetUUID);
        // Disband team
        island.getMemberSet().forEach(m -> {
            User mUser = User.getInstance(m);
            mUser.sendMessage("commands.admin.team.disband.disbanded");
            // The owner gets to keep the island
            if (!m.equals(targetUUID)) {
                getIslands().removePlayer(island, m);
                TeamEvent.builder().island(island).reason(TeamEvent.Reason.KICK).involvedPlayer(m).admin(true).build();
                IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(true)
                        .reason(IslandEvent.Reason.RANK_CHANGE)
                        .rankChange(island.getRank(mUser), RanksManager.VISITOR_RANK).build();
            }
        });
        user.sendMessage("commands.admin.team.disband.success", TextVariables.NAME, args.get(0));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        } else if (args.size() == 3) {
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            return Optional.of(Util.tabLimit(options, lastArg));
        } else if (args.size() > 3) {
            // Find out which user
            UUID uuid = getPlayers().getUUID(args.get(1));
            if (uuid != null) {
                return Optional.of(Util.tabLimit(new ArrayList<>(getIslandsXYZ(uuid).keySet()), lastArg));
            }
        }
        return Optional.empty();
    }

}
