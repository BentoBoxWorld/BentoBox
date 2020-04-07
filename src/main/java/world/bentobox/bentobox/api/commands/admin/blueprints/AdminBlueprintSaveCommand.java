package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.util.List;
import java.util.Locale;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintClipboardManager;
import world.bentobox.bentobox.managers.BlueprintsManager;

public class AdminBlueprintSaveCommand extends ConfirmableCommand {

    public AdminBlueprintSaveCommand(AdminBlueprintCommand parent) {
        super(parent, "save");
    }

    @Override
    public void setup() {
        inheritPermission();
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
        String fileName = args.get(0).toLowerCase(Locale.ENGLISH);
        if (clipboard.isFull()) {
            // Check if blueprint had bedrock
            if (clipboard.getBlueprint().getBedrock() == null) {
                user.sendMessage("commands.admin.blueprint.bedrock-required");
                return false;
            }
            // Check if file exists
            File newFile = new File(parent.getBlueprintsFolder(), fileName + BlueprintsManager.BLUEPRINT_SUFFIX);
            if (newFile.exists()) {
                this.askConfirmation(user, user.getTranslation("commands.admin.blueprint.file-exists"), () -> hideAndSave(user, parent, clipboard, fileName));
                return false;
            }
            return hideAndSave(user, parent, clipboard, fileName);
        } else {
            user.sendMessage("commands.admin.blueprint.copy-first");
            return false;
        }
    }

    private boolean hideAndSave(User user, AdminBlueprintCommand parent, BlueprintClipboard clipboard, String name) {
        parent.hideClipboard(user);
        boolean result = new BlueprintClipboardManager(getPlugin(), parent.getBlueprintsFolder(), clipboard).save(user, name);
        if (result) {
            getPlugin().getBlueprintsManager().addBlueprint(getAddon(), clipboard.getBlueprint());
        }
        return result;
    }
}
