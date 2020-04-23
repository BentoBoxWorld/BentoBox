package world.bentobox.bentobox.api.commands.admin.range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class AdminRangeSetCommand extends CompositeCommand {

    public AdminRangeSetCommand(CompositeCommand parent) {
        super(parent, "set");
    }

    @Override
    public void setup() {
        setPermission("admin.range.set");
        setParametersHelp("commands.admin.range.set.parameters");
        setDescription("commands.admin.range.set.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            // Show help
            showHelp(this, user);
            return false;
        }

        // Get target player
        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!(getIslands().hasIsland(getWorld(), targetUUID) || getIslands().inTeam(getWorld(), targetUUID))) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        // Get new range
        if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }
        int range = Integer.parseInt(args.get(1));

        // Get island
        Island island = getIslands().getIsland(getWorld(), targetUUID);

        // Do some sanity checks to make sure the new protection range won't cause problems
        if (range <= 1) {
            user.sendMessage("commands.admin.range.invalid-value.too-low", TextVariables.NUMBER, args.get(1));
            return false;
        }
        if (range > island.getRange()) {
            user.sendMessage("commands.admin.range.invalid-value.too-high", TextVariables.NUMBER, String.valueOf(island.getRange()));
            return false;
        }
        if (range == island.getProtectionRange()) {
            user.sendMessage("commands.admin.range.invalid-value.same-as-before", TextVariables.NUMBER, args.get(1));
            return false;
        }

        // Get old range for event
        int oldRange = island.getProtectionRange();

        // Well, now it can be applied without taking any risks!
        island.setProtectionRange(range);

        // Call Protection Range Change event. Does not support canceling.
        IslandEvent.builder()
        .island(island)
        .location(island.getCenter())
        .reason(IslandEvent.Reason.RANGE_CHANGE)
        .involvedPlayer(targetUUID)
        .admin(true)
        .protectionRange(range, oldRange)
        .build();

        user.sendMessage("commands.admin.range.set.success", TextVariables.NUMBER, String.valueOf(range));

        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
