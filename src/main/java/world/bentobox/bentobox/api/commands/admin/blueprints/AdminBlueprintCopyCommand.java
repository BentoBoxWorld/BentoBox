package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;

public class AdminBlueprintCopyCommand extends CompositeCommand {

    public AdminBlueprintCopyCommand(AdminBlueprintCommand parent) {
        super(parent, "copy");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.copy.parameters");
        setDescription("commands.admin.blueprint.copy.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            showHelp(this, user);
            return false;
        }

        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());
        boolean copyAir = (args.size() == 1 && args.get(0).equalsIgnoreCase("air"));
        return clipboard.copy(user, copyAir);
    }
}
