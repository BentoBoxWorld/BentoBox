package world.bentobox.bentobox.api.commands.admin.range;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

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
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getPlugin().getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        // Get new range
        if (!StringUtils.isNumeric(args.get(1))) {
            user.sendMessage("commands.admin.range.set.invalid-value.not-numeric", TextVariables.NUMBER, args.get(1));
            return false;
        }
        int range = Integer.parseInt(args.get(1));

        // Get island
        Island island = getIslands().getIsland(getWorld(), targetUUID);

        // Do some sanity checks to make sure the new protection range won't cause problems
        if (range <= 1) {
            user.sendMessage("commands.admin.range.set.invalid-value.too-low", TextVariables.NUMBER, args.get(1));
            return false;
        }
        if (range > island.getRange()) {
            user.sendMessage("commands.admin.range.set.invalid-value.too-high", TextVariables.NUMBER, String.valueOf(island.getRange()));
            return false;
        }
        if (range == island.getProtectionRange()) {
            user.sendMessage("commands.admin.range.set.invalid-value.same-as-before", TextVariables.NUMBER, args.get(1));
            return false;
        }

        // Well, now it can be applied without taking any risks !
        island.setProtectionRange(range);
        user.sendMessage("commands.admin.range.set.success", TextVariables.NUMBER, String.valueOf(range));

        return true;
    }
}
