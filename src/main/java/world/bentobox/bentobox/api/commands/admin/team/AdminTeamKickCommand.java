package world.bentobox.bentobox.api.commands.admin.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Kicks the specified player from the island team.
 * @author tastybento
 */
public class AdminTeamKickCommand extends CompositeCommand {

    private static final String ALL_FLAG = "--all";

    private @Nullable UUID targetUUID;
    private @Nullable Island island;
    private boolean kickAll;

    public AdminTeamKickCommand(CompositeCommand parent) {
        super(parent, "kick");
    }

    @Override
    public void setup() {
        setPermission("mod.team.kick");
        setParametersHelp("commands.admin.team.kick.parameters");
        setDescription("commands.admin.team.kick.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // If args are not right, show help
        if (args.isEmpty() || args.size() > 2) {
            showHelp(this, user);
            return false;
        }

        // Get target
        targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        if (!getIslands().inTeam(getWorld(), targetUUID)) {
            user.sendMessage("commands.admin.team.kick.not-in-team");
            return false;
        }

        // Check for --all flag (kick from every island in this world)
        kickAll = args.size() == 2 && ALL_FLAG.equalsIgnoreCase(args.get(1));
        if (args.size() == 2 && !kickAll) {
            showHelp(this, user);
            return false;
        }

        if (!kickAll) {
            // Default: kick from the island the admin is currently standing on
            if (!user.isPlayer()) {
                user.sendMessage("commands.admin.team.kick.must-stand-on-island");
                return false;
            }
            Optional<Island> islandOpt = getIslands().getIslandAt(user.getLocation());
            if (islandOpt.isEmpty()) {
                user.sendMessage("commands.admin.team.kick.must-stand-on-island");
                return false;
            }
            island = islandOpt.get();
            // Verify the target is actually a member of this specific island
            if (!island.inTeam(targetUUID)) {
                user.sendMessage("commands.admin.team.kick.not-member-of-this-island");
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (kickAll) {
            List<Island> islands = getIslands().getIslands(getWorld(), targetUUID);
            if (islands.isEmpty()) {
                return false;
            }
            islands.forEach(i -> kickFromIsland(user, i));
            user.sendMessage("commands.admin.team.kick.success-all");
        } else {
            kickFromIsland(user, Objects.requireNonNull(island));
        }
        return true;
    }

    /**
     * Removes the target player from a single island and fires the relevant events.
     */
    private void kickFromIsland(User user, Island i) {
        if (!user.getUniqueId().equals(i.getOwner())) {
            User target = User.getInstance(Objects.requireNonNull(targetUUID));
            target.sendMessage("commands.admin.team.kick.admin-kicked");
            getIslands().removePlayer(i, targetUUID);
            user.sendMessage("commands.admin.team.kick.success", TextVariables.NAME, target.getName(), "[owner]",
                    getPlayers().getName(i.getOwner()));
            // Fire events so add-ons know
            TeamEvent.builder().island(i).reason(TeamEvent.Reason.KICK).involvedPlayer(targetUUID).admin(true).build();
            IslandEvent.builder().island(i).involvedPlayer(targetUUID).admin(true)
                    .reason(IslandEvent.Reason.RANK_CHANGE)
                    .rankChange(i.getRank(target), RanksManager.VISITOR_RANK).build();
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        // The second-to-last arg is the player name when we are completing the second positional arg
        if (args.size() >= 2) {
            String possiblePlayer = args.get(args.size() - 2);
            UUID possibleUUID = getPlayers().getUUID(possiblePlayer);
            if (possibleUUID != null && getIslands().getIslands(getWorld(), possibleUUID).size() > 1) {
                return Optional.of(Util.tabLimit(List.of(ALL_FLAG), lastArg));
            }
        }
        // Default: complete player names
        return Optional.of(Util.tabLimit(new ArrayList<>(Util.getOnlinePlayerList(user)), lastArg));
    }
}
