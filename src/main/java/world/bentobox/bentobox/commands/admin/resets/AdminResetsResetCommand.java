package world.bentobox.bentobox.commands.admin.resets;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

import java.util.List;
import java.util.UUID;

public class AdminResetsResetCommand extends CompositeCommand {

    public AdminResetsResetCommand(CompositeCommand parent) {
        super(parent, "reset");
    }

    @Override
    public void setup() {
        setDescription("commands.admin.resets.reset.description");
        setParametersHelp("commands.admin.resets.reset.parameters");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty() || args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        UUID target = getPlayers().getUUID(args.get(0));
        if (target == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        } else {
            getPlayers().setResets(getWorld(), target, 0);
            user.sendMessage("general.success");
            return true;
        }
    }
}
