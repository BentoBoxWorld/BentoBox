package world.bentobox.bentobox.api.commands.admin.range;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

import java.util.List;
import java.util.UUID;

/**
 * @since 1.10.0
 * @author Poslovitch
 */
public class AdminRangeAddCommand extends CompositeCommand {

    public AdminRangeAddCommand(AdminRangeCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setDescription("commands.admin.range.add.description");
        setParametersHelp("commands.admin.range.add.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }

        if (!(getIslands().hasIsland(getWorld(), target) || getIslands().inTeam(getWorld(), target))) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        if (!NumberUtils.isNumber(args.get(1)) || Integer.valueOf(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }

        Island island = getIslands().getIsland(getWorld(), target);
        int newRange = island.getProtectionRange() + Integer.valueOf(args.get(1));

        if (newRange > island.getRange()) {
            user.sendMessage("commands.admin.range.invalid-value.too-high", TextVariables.NUMBER, String.valueOf(island.getRange()));
            return false;
        } else if (newRange == island.getProtectionRange()) {
            user.sendMessage("commands.admin.range.invalid-value.same-as-before", TextVariables.NUMBER, args.get(1));
            return false;
        }

        // Well, now it can be applied without taking any risks !
        island.setProtectionRange(newRange);
        user.sendMessage("commands.admin.range.add.success",
                TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                "[total]", String.valueOf(newRange));

        return true;
    }
}
