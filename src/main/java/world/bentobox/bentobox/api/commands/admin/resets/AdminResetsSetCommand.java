package world.bentobox.bentobox.api.commands.admin.resets;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class AdminResetsSetCommand extends CompositeCommand {

    public AdminResetsSetCommand(CompositeCommand parent) {
        super(parent, "set");
    }

    @Override
    public void setup() {
        setDescription("commands.admin.resets.set.description");
        setParametersHelp("commands.admin.resets.set.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty() || args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
        } else if (!NumberUtils.isNumber(args.get(1)) || Integer.valueOf(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            getPlayers().setResets(getWorld(), target, Integer.valueOf(args.get(1)));
            user.sendMessage("general.success");
            return true;
        }

        return false;
    }
}
