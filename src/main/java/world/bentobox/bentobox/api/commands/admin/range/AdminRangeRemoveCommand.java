package world.bentobox.bentobox.api.commands.admin.range;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @since 1.10.0
 * @author Poslovitch
 */
public class AdminRangeRemoveCommand extends AbstractAdminRangeCommand {

    public AdminRangeRemoveCommand(AdminRangeCommand parent) {
        super(parent, "remove");
    }

    @Override
    public void setup() {
        setPermission("admin.range.remove");
        setDescription("commands.admin.range.remove.description");
        setParametersHelp("commands.admin.range.remove.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }

        int newRange = targetIsland.getProtectionRange() - Integer.parseInt(args.get(1));

        if (newRange <= 1) {
            user.sendMessage("commands.admin.range.invalid-value.too-low", TextVariables.NUMBER,
                    String.valueOf(targetIsland.getRange()));
            return false;
        } else if (newRange == targetIsland.getProtectionRange()) {
            user.sendMessage("commands.admin.range.invalid-value.same-as-before", TextVariables.NUMBER, args.get(1));
            return false;
        }

        // Get old range for event
        int oldRange = targetIsland.getProtectionRange();

        // Well, now it can be applied without taking any risks!
        targetIsland.setProtectionRange(newRange);

        // Call Protection Range Change event. Does not support cancelling.
        IslandEvent.builder()
                .island(targetIsland).location(targetIsland.getCenter())
        .reason(IslandEvent.Reason.RANGE_CHANGE)
        .involvedPlayer(targetUUID)
        .admin(true)
        .protectionRange(newRange, oldRange)
        .build();

        user.sendMessage("commands.admin.range.remove.success",
                TextVariables.NAME, args.getFirst(), TextVariables.NUMBER, args.get(1),
                "[total]", String.valueOf(newRange));

        return true;
    }
}
