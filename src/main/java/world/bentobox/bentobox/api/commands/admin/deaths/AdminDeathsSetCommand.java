package world.bentobox.bentobox.api.commands.admin.deaths;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @author Poslovitch
 */
public class AdminDeathsSetCommand extends CompositeCommand {

    public AdminDeathsSetCommand(AdminDeathsCommand parent) {
        super(parent, "set");
    }

    @Override
    public void setup() {
        inheritPermission();
        setDescription("commands.admin.deaths.set.description");
        setParametersHelp("commands.admin.deaths.set.parameters");
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
            getPlayers().setDeaths(getWorld(), targetUUID, Integer.valueOf(args.get(1)));
            user.sendMessage("commands.admin.deaths.set.success", TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1));
            return true;
        }

        return false;
    }
}
