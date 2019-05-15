package world.bentobox.bentobox.api.commands.admin.blueprints;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintClipboardManager;
import world.bentobox.bentobox.managers.BlueprintsManager;

import java.io.File;
import java.util.List;

public class AdminBlueprintSaveCommand extends ConfirmableCommand {

    public AdminBlueprintSaveCommand(AdminBlueprintCommand parent) {
        super(parent, "save");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.blueprint.save.parameters");
        setDescription("commands.admin.blueprint.save.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();
        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        if (clipboard.isFull()) {
            // Check if file exists
            File newFile = new File(parent.getBlueprintsFolder(), args.get(0) + BlueprintsManager.BLUEPRINT_SUFFIX);
            if (newFile.exists()) {
                this.askConfirmation(user, user.getTranslation("commands.admin.blueprint.file-exists"), () -> {
                    parent.hideClipboard(user);
                    new BlueprintClipboardManager(getPlugin(), parent.getBlueprintsFolder(), clipboard).save(user, args.get(0));
                });
                return false;
            } else {
                parent.hideClipboard(user);
                return new BlueprintClipboardManager(getPlugin(), parent.getBlueprintsFolder(), clipboard).save(user, args.get(0));
            }
        } else {
            user.sendMessage("commands.admin.blueprint.copy-first");
            return false;
        }
    }
}
