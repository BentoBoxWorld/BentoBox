package world.bentobox.bentobox.api.commands.admin.resets;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @author Poslovitch
 * @since 1.8.0
 */
public class AdminResetsRemoveCommand extends CompositeCommand {

    public AdminResetsRemoveCommand(AdminResetsCommand parent) {
        super(parent, "remove");
    }

    @Override
    public void setup() {
        inheritPermission();
        setDescription("commands.admin.resets.remove.description");
        setParametersHelp("commands.admin.resets.remove.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        UUID targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
        } else if (!Util.isInteger(args.get(1), true) || Integer.valueOf(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            // Make sure it cannot go under 0.
            int newResets = Math.max(getPlayers().getResets(getWorld(), targetUUID) - Integer.valueOf(args.get(1)), 0);
            getPlayers().setResets(getWorld(), targetUUID, newResets);
            user.sendMessage("commands.admin.resets.remove.success",
                    TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(newResets));
            return true;
        }

        return false;
    }
}
