package world.bentobox.bentobox.api.commands.admin.range;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

public class AdminRangeSetCommand extends AbstractAdminRangeCommand {

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
        // Get new range
        if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }
        int range = Integer.parseInt(args.get(1));

        // Do some sanity checks to make sure the new protection range won't cause problems
        if (range < 1) {
            user.sendMessage("commands.admin.range.invalid-value.too-low", TextVariables.NUMBER, args.get(1));
            return false;
        }
        if (range > targetIsland.getRange() * 2) {
            user.sendMessage("commands.admin.range.invalid-value.too-high", TextVariables.NUMBER,
                    String.valueOf(2 * targetIsland.getRange()));
            return false;
        }
        if (range == targetIsland.getProtectionRange()) {
            user.sendMessage("commands.admin.range.invalid-value.same-as-before", TextVariables.NUMBER, args.get(1));
            return false;
        }

        // Get old range for event
        int oldRange = targetIsland.getProtectionRange();

        // Well, now it can be applied without taking any risks!
        targetIsland.setProtectionRange(range);

        // Call Protection Range Change event. Does not support canceling.
        IslandEvent.builder()
                .island(targetIsland).location(targetIsland.getCenter())
        .reason(IslandEvent.Reason.RANGE_CHANGE)
        .involvedPlayer(targetUUID)
        .admin(true)
        .protectionRange(range, oldRange)
        .build();

        user.sendMessage("commands.admin.range.set.success", TextVariables.NUMBER, String.valueOf(range));

        return true;
    }

}
