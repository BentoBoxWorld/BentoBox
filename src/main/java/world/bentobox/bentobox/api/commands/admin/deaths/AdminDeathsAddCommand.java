package world.bentobox.bentobox.api.commands.admin.deaths;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

import java.util.List;
import java.util.UUID;

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
        setDescription("commands.admin.deaths.add.description");
        setParametersHelp("commands.admin.deaths.add.parameters");
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
        } else if (!NumberUtils.isNumber(args.get(1)) || Integer.valueOf(args.get(1)) < 0) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
        } else {
            getPlayers().setDeaths(getWorld(), target, getPlayers().getDeaths(getWorld(), target) + Integer.valueOf(args.get(1)));
            user.sendMessage("commands.admin.deaths.add.success",
                    TextVariables.NAME, args.get(0), TextVariables.NUMBER, args.get(1),
                    "[total]", String.valueOf(getPlayers().getDeaths(getWorld(), target)));
            return true;
        }

        return false;
    }
}
