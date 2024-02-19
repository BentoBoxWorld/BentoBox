package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
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
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class AdminUnregisterCommand extends ConfirmableCommand {

    private Island targetIsland;
    private @Nullable UUID targetUUID;

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
        // If args are not right, show help
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // Get target
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Check if the player has more than one island
        Map<String, Island> islands = getIslandsXYZ(targetUUID);
        if (islands.size() == 0) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        } else if (args.size() == 1) {
            if (islands.size() == 1) {
                targetIsland = islands.values().iterator().next();
                return true;
            } else {
                // They need to specify which island
                user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
                user.sendMessage("commands.admin.unregister.errors.specify-island-location");
                return false;
            }
        } else if (args.size() != 2) {
            // Check if the name given works
            user.sendMessage("commands.admin.unregister.errors.specify-island-location");
            return false;
        } else if (!islands.containsKey(args.get(1))) {
            user.sendMessage("commands.admin.unregister.errors.unknown-island-location");
            return false;
        }
        targetIsland = islands.get(args.get(1));
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (targetIsland == null || targetUUID == null) {
            return true;
        }
        // Everything's fine, we can set the island as spawn :)
        askConfirmation(user, () -> unregisterIsland(user));
        return true;
    }

    void unregisterIsland(User user) {
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
        // Remove all island members
        targetIsland.getMemberSet().forEach(m -> getIslands().removePlayer(targetIsland, m));
        // Remove all island players that reference this island
        targetIsland.getMembers().clear();
        getIslands().save(targetIsland);
        user.sendMessage("commands.admin.unregister.unregistered-island", TextVariables.XYZ, Util.xyz(targetIsland.getCenter().toVector()),
                TextVariables.NAME, getPlayers().getName(targetUUID));
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        } else if (args.size() == 2) {
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
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