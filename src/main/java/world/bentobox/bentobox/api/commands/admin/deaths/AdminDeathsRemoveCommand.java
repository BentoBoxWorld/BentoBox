package world.bentobox.bentobox.api.commands.admin.deaths;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @since 1.8.0
 * @author Poslovitch
 */
public class AdminDeathsRemoveCommand extends CompositeCommand {

    public AdminDeathsRemoveCommand(AdminDeathsCommand parent) {
        super(parent, "remove");
    }

    @Override
    public void setup() {
        setPermission("admin.deaths.remove");
        setDescription("commands.admin.deaths.remove.description");
        setParametersHelp("commands.admin.deaths.remove.parameters");
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
        } else if (!Util.isInteger(args.get(1), true) || Integer.parseInt(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            // Make sure it cannot go under 0.
            int newDeaths = Math.max(getPlayers().getDeaths(getWorld(), targetUUID) - Integer.parseInt(args.get(1)), 0);
            getPlayers().setDeaths(getWorld(), targetUUID, newDeaths);
            user.sendMessage("commands.admin.deaths.remove.success",
                    TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(newDeaths));
            return true;
        }

        return false;
    }
}
