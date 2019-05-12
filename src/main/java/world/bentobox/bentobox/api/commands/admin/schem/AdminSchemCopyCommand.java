package world.bentobox.bentobox.api.commands.admin.schem;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;

import java.util.List;

public class AdminSchemCopyCommand extends CompositeCommand {

    public AdminSchemCopyCommand(AdminSchemCommand parent) {
        super(parent, "copy");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.copy.parameters");
        setDescription("commands.admin.schem.copy.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            showHelp(this, user);
            return false;
        }

        AdminSchemCommand parent = (AdminSchemCommand) getParent();

        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());
        boolean copyAir = (args.size() == 1 && args.get(0).equalsIgnoreCase("air"));
        return clipboard.copy(user, copyAir);
    }
}
