package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.util.List;
import java.util.Locale;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * Renames an existing blueprint.
 * @author Poslovitch
 * @since 1.10.0
 */
public class AdminBlueprintRenameCommand extends ConfirmableCommand {

    public AdminBlueprintRenameCommand(AdminBlueprintCommand parent) {
        super(parent, "rename");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.rename.parameters");
        setDescription("commands.admin.blueprint.rename.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            showHelp(this, user);
            return false;
        }

        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        // Check if the names are the same
        String from = args.get(0).toLowerCase(Locale.ENGLISH);
        String to = args.get(1).toLowerCase(Locale.ENGLISH);

        if (from.equals(to)) {
            user.sendMessage("commands.admin.blueprint.rename.pick-different-name");
            return false;
        }

        // Check if the 'from' file exists
        File fromFile = new File(parent.getBlueprintsFolder(), from + BlueprintsManager.BLUEPRINT_SUFFIX);
        if (!fromFile.exists()) {
            user.sendMessage("commands.admin.blueprint.no-such-file");
            return false;
        }

        // Check if the 'to' file exists

        File toFile = new File(parent.getBlueprintsFolder(), to + BlueprintsManager.BLUEPRINT_SUFFIX);
        if (toFile.exists()) {
            // Ask for confirmation to overwrite
            askConfirmation(user, user.getTranslation("commands.admin.blueprint.file-exists"), () -> rename(user, from, to));
        } else {
            askConfirmation(user, () -> rename(user, from, to));
        }
        return true;
    }

    private void rename(User user, String blueprintName, String newName) {
        Blueprint blueprint = getPlugin().getBlueprintsManager().getBlueprints(getAddon()).get(blueprintName);
        getPlugin().getBlueprintsManager().renameBlueprint(getAddon(), blueprint, newName);
        user.sendMessage("commands.admin.blueprint.rename.success", "[old]", blueprintName, TextVariables.NAME, blueprint.getName());
    }
}
