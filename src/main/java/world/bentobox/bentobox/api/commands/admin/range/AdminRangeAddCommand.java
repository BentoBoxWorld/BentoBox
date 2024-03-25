package world.bentobox.bentobox.api.commands.admin.range;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @since 1.10.0
 * @author Poslovitch
 */
public class AdminRangeAddCommand extends AbstractAdminRangeCommand {

    public AdminRangeAddCommand(AdminRangeCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setPermission("admin.range.add");
        setDescription("commands.admin.range.add.description");
        setParametersHelp("commands.admin.range.add.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        int newRange = targetIsland.getProtectionRange() + Integer.parseInt(args.get(1));

        if (newRange > targetIsland.getRange()) {
            user.sendMessage("commands.admin.range.invalid-value.too-high", TextVariables.NUMBER,
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
                .reason(IslandEvent.Reason.RANGE_CHANGE).involvedPlayer(targetUUID).admin(true)
                .protectionRange(newRange, oldRange).build();

        user.sendMessage("commands.admin.range.add.success",
                TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                "[total]", String.valueOf(newRange));

        return true;
    }


}
