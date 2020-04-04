package world.bentobox.bentobox.api.commands.admin.deaths;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @author Poslovitch
 */
public class AdminDeathsResetCommand extends CompositeCommand {

    public AdminDeathsResetCommand(AdminDeathsCommand parent) {
        super(parent, "reset");
        inheritPermission();
        setDescription("commands.admin.deaths.reset.description");
        setParametersHelp("commands.admin.deaths.reset.parameters");
    }

    @Override
    public boolean execute(User user, String label, @NonNull List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        } else {
            getPlayers().setDeaths(getWorld(), target, 0);
            user.sendMessage("commands.admin.deaths.reset.success", TextVariables.NAME, args.get(0));
            return true;
        }
    }
}
