package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class AdminUnregisterCommand extends ConfirmableCommand {

    private record UnregisterTarget(Island island, UUID owner) { }

    public AdminUnregisterCommand(CompositeCommand parent) {
        super(parent, "unregister");
    }

    @Override
    public void setup() {
        setPermission("admin.unregister");
        setParametersHelp("commands.admin.unregister.parameters");
        setDescription("commands.admin.unregister.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return resolveTarget(user, args) != null;
    }

    private @Nullable UnregisterTarget resolveTarget(User user, List<String> args) {
        // If args are not right, show help
        if (args.isEmpty()) {
            showHelp(this, user);
            return null;
        }
        // Get target
        UUID targetUUID = Util.getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }
        // Check if the player has more than one island
        Map<String, Island> islands = getIslandsXYZ(targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        } else if (args.size() == 1) {
            if (islands.size() == 1) {
                return new UnregisterTarget(islands.values().iterator().next(), targetUUID);
            } else {
                // They need to specify which island
                user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
                user.sendMessage("commands.admin.unregister.errors.specify-island-location");
                return null;
            }
        } else if (args.size() != 2) {
            // Check if the name given works
            user.sendMessage("commands.admin.unregister.errors.specify-island-location");
            return null;
        } else if (!islands.containsKey(args.get(1))) {
            if (args.get(1).equalsIgnoreCase("help")) {
                this.showHelp(this, user);
                return null;
            }
            user.sendMessage("commands.admin.unregister.errors.unknown-island-location");
            return null;
        }
        return new UnregisterTarget(islands.get(args.get(1)), targetUUID);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UnregisterTarget target = resolveTarget(user, args);
        if (target == null) return false;
        askConfirmation(user, () -> unregisterIsland(user, target.island(), target.owner()));
        return true;
    }

    void unregisterIsland(User user, Island targetIsland, UUID targetUUID) {
        if (!targetUUID.equals(targetIsland.getOwner())
                || !getIslands().getOwnedIslands(getWorld(), targetUUID).contains(targetIsland)) {
            user.sendMessage("general.errors.player-has-no-island");
            return;
        }
        // Unregister island
        IslandEvent.builder()
        .island(targetIsland)
        .location(targetIsland.getCenter())
        .reason(IslandEvent.Reason.UNREGISTERED)
        .involvedPlayer(targetUUID)
        .admin(true)
        .build();
        IslandEvent.builder()
        .island(targetIsland)
        .involvedPlayer(targetUUID)
        .admin(true)
        .reason(IslandEvent.Reason.RANK_CHANGE)
        .rankChange(RanksManager.OWNER_RANK, RanksManager.VISITOR_RANK)
        .build();
        targetIsland.setOwner(null);
        // Remove all island members. Iterate over every rank, not getMemberSet(): coop and
        // trusted players would keep a stale UUID index entry otherwise.
        new HashSet<>(targetIsland.getMembers().keySet()).forEach(m -> getIslands().removePlayer(targetIsland, m));
        // Remove all island players that reference this island
        targetIsland.getMembers().clear();
        if (user.isPlayer()) {
            assert targetUUID != null;
            targetIsland.log(new LogEntry.Builder(LogType.UNREGISTER).data("player", targetUUID.toString())
                    .data("admin", user.getUniqueId().toString()).build());
        } else {
            assert targetUUID != null;
            targetIsland.log(new LogEntry.Builder(LogType.UNREGISTER).data("player", targetUUID.toString())
                    .data("admin", "console").build());
        }
        user.sendMessage("commands.admin.unregister.unregistered-island", TextVariables.XYZ, Util.xyz(targetIsland.getCenter().toVector()),
                TextVariables.NAME, getPlayers().getName(targetUUID));
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        } else if (args.size() == 2) {
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            return Optional.of(Util.tabLimit(options, lastArg));
        } else if (args.size() > 2) {
            // Find out which user
            UUID uuid = getPlayers().getUUID(args.get(1));
            if (uuid != null) {
                return Optional.of(Util.tabLimit(new ArrayList<>(getIslandsXYZ(uuid).keySet()), lastArg));
            }
        }
        return Optional.empty();
    }

    private Map<String, Island> getIslandsXYZ(UUID target) {
        return getIslands().getOwnedIslands(getWorld(), target).stream()
                .collect(Collectors.toMap(island -> Util.xyz(island.getCenter().toVector()), island -> island));
    }

}
