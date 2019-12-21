package world.bentobox.bentobox.api.commands.admin.resets;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

import java.util.List;
import java.util.UUID;

/**
 * @author Poslovitch
 * @since 1.8.0
 */
public class AdminResetsAddCommand extends CompositeCommand {

    public AdminResetsAddCommand(AdminResetsCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        setDescription("commands.admin.resets.add.description");
        setParametersHelp("commands.admin.resets.add.parameters");
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
        } else if (!Util.isInteger(args.get(1), true) || Integer.valueOf(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            getPlayers().setResets(getWorld(), target, getPlayers().getResets(getWorld(), target) + Integer.valueOf(args.get(1)));
            user.sendMessage("commands.admin.resets.add.success",
                    TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(getPlayers().getResets(getWorld(), target)));
            return true;
        }

        return false;
    }
}
