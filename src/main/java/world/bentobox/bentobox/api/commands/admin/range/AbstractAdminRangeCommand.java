package world.bentobox.bentobox.api.commands.admin.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * @author Poslovitch
 */
public abstract class AbstractAdminRangeCommand extends CompositeCommand {

    protected @Nullable UUID targetUUID;
    protected Island targetIsland;

    public AbstractAdminRangeCommand(CompositeCommand parent, String string) {
        super(parent, string);
    }

    @Override
    public boolean canExecute(User user, String label, @NonNull List<String> args) {
        if (args.size() <= 1) {
            showHelp(this, user);
            return false;
        }

        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }

        if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }
        // Check if the player has more than one island
        Map<String, Island> islands = getIslandsXYZ(targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        } else if (args.size() == 2) {
            // If they only have one island, 2 args are fine
            if (islands.size() == 1) {
                targetIsland = islands.values().iterator().next();
                return true;
            } else {
                // They need to specify which island
                user.sendMessage("commands.admin.unregister.errors.player-has-more-than-one-island");
                user.sendMessage("commands.admin.unregister.errors.specify-island-location");
                return false;
            }
        } else if (args.size() != 3) {
            // No location
            user.sendMessage("commands.admin.unregister.errors.specify-island-location");
            return false;
        } else if (!islands.containsKey(args.get(2))) {
            if (args.get(2).equalsIgnoreCase("help")) {
                this.showHelp(this, user);
                return false;
            }
            user.sendMessage("commands.admin.unregister.errors.unknown-island-location");
            return false;
        }
        targetIsland = islands.get(args.get(2));
        return true;
    }

    protected Map<String, Island> getIslandsXYZ(UUID target) {
        return getIslands().getOwnedIslands(getWorld(), target).stream()
                .collect(Collectors.toMap(island -> Util.xyz(island.getCenter().toVector()), island -> island));
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        } else if (args.size() == 3) {
            List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
            return Optional.of(Util.tabLimit(options, lastArg));
        } else if (args.size() > 4) {
            // Find out which user
            UUID uuid = getPlayers().getUUID(args.get(2));
            if (uuid != null) {
                return Optional.of(Util.tabLimit(new ArrayList<>(getIslandsXYZ(uuid).keySet()), lastArg));
            }
        }
        return Optional.empty();
    }
}
