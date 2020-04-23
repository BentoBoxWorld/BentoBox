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
public class AdminDeathsAddCommand extends CompositeCommand {

    public AdminDeathsAddCommand(AdminDeathsCommand parent) {
        super(parent, "add");
    }

    @Override
    public void setup() {
        inheritPermission();
        setDescription("commands.admin.deaths.add.description");
        setParametersHelp("commands.admin.deaths.add.parameters");
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
            getPlayers().setDeaths(getWorld(), targetUUID, getPlayers().getDeaths(getWorld(), targetUUID) + Integer.valueOf(args.get(1)));
            user.sendMessage("commands.admin.deaths.add.success",
                    TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(getPlayers().getDeaths(getWorld(), targetUUID)));
            return true;
        }

        return false;
    }
}
