package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

public class AdminVersionCommand extends CompositeCommand {

    public AdminVersionCommand(CompositeCommand adminCommand) {
        super(adminCommand, "version", "v", "ver");
    }

    @Override
    public void setup() {
        // Permission
        setPermission("admin.version");
        setDescription("commands.admin.version.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        user.sendMessage("commands.bentobox.version.addon-syntax", TextVariables.NAME, getAddon().getDescription().getName(),
                TextVariables.VERSION, getAddon().getDescription().getVersion());
        return true;
    }

}
