package world.bentobox.bentobox.api.commands.admin.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private @Nullable UUID targetUUID;
    private @Nullable Island island;

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

        Map<String, Island> kickable = getMemberIslandsXYZ(targetUUID);
        if (kickable.isEmpty()) {
            // Either the target is not on any team island, or every team island they
            // are on is one they own — kick is the wrong tool for that case.
            if (ownsAnyTeamIsland(targetUUID)) {
                user.sendMessage("commands.admin.team.kick.cannot-kick-owner");
            } else {
                user.sendMessage("commands.admin.team.kick.not-in-team");
            }
            return false;
        }

        if (args.size() == 1) {
            if (kickable.size() == 1) {
                island = kickable.values().iterator().next();
            } else {
                // Multiple islands – require the player to specify which one
                user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
                kickable.keySet().forEach(coords ->
                        user.sendMessage("commands.admin.unregister.errors.specify-island-location",
                                TextVariables.XYZ, coords));
                return false;
            }
        } else {
            // args.size() == 2: xyz was supplied
            String coord = args.get(1);
            if (!kickable.containsKey(coord)) {
                // Distinguish "target owns this island" from "no island at this coord
                // for this target" so the admin gets actionable feedback.
                if (ownsTeamIslandAt(targetUUID, coord)) {
                    user.sendMessage("commands.admin.team.kick.cannot-kick-owner");
                } else {
                    user.sendMessage("commands.admin.unregister.errors.unknown-island-location");
                }
                return false;
            }
            island = kickable.get(coord);
        }
        return true;
    }

    /**
     * Returns a map of x,y,z → island for all team islands in this world that the
     * target player is a member of but does not own. Owner islands must be handled
     * via setowner or disband, not kick.
     */
    private Map<String, Island> getMemberIslandsXYZ(UUID target) {
        return getIslands().getIslands(getWorld(), target).stream()
                .filter(Island::hasTeam)
                .filter(i -> !target.equals(i.getOwner()))
                .collect(Collectors.toMap(i -> Util.xyz(i.getCenter().toVector()), i -> i));
    }

    private boolean ownsAnyTeamIsland(UUID target) {
        return getIslands().getIslands(getWorld(), target).stream()
                .filter(Island::hasTeam)
                .anyMatch(i -> target.equals(i.getOwner()));
    }

    private boolean ownsTeamIslandAt(UUID target, String xyz) {
        return getIslands().getIslands(getWorld(), target).stream()
                .filter(Island::hasTeam)
                .filter(i -> target.equals(i.getOwner()))
                .anyMatch(i -> xyz.equals(Util.xyz(i.getCenter().toVector())));
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        Objects.requireNonNull(island);
        Objects.requireNonNull(targetUUID);
        User target = User.getInstance(targetUUID);
        target.sendMessage("commands.admin.team.kick.admin-kicked");
        getIslands().removePlayer(island, targetUUID);
        user.sendMessage("commands.admin.team.kick.success", TextVariables.NAME, target.getName(), "[owner]",
                getPlayers().getName(island.getOwner()));
        // Fire events so add-ons know
        TeamEvent.builder().island(island).reason(TeamEvent.Reason.KICK).involvedPlayer(targetUUID).admin(true).build();
        IslandEvent.builder().island(island).involvedPlayer(targetUUID).admin(true)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(target), RanksManager.VISITOR_RANK).build();
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        } else if (args.size() == 2) {
            // Completing the xyz arg: show the islands the target is a member of
            UUID targetId = getPlayers().getUUID(args.getFirst());
            if (targetId != null) {
                return Optional.of(Util.tabLimit(new ArrayList<>(getMemberIslandsXYZ(targetId).keySet()), lastArg));
            }
        }
        return Optional.empty();
    }
}
