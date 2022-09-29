package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.util.List;
import java.util.Locale;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;


/**
 * Renames an existing blueprint.
 * @author Poslovitch
 * @since 1.10.0
 */
public class AdminBlueprintRenameCommand extends ConfirmableCommand
{
    public AdminBlueprintRenameCommand(AdminBlueprintCommand parent)
    {
        super(parent, "rename");
    }


    @Override
    public void setup()
    {
        this.inheritPermission();
        this.setParametersHelp("commands.admin.blueprint.rename.parameters");
        this.setDescription("commands.admin.blueprint.rename.description");
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        if (args.size() != 2)
        {
            // Blueprint must have a name.
            this.showHelp(this, user);
            return false;
        }

        String from = Util.sanitizeInput(args.get(0));
        String to = Util.sanitizeInput(args.get(1));

        // Check if name is changed.
        if (from.equals(to))
        {
            user.sendMessage("commands.admin.blueprint.rename.pick-different-name");
            return false;
        }

        // Check if the 'from' file exists
        AdminBlueprintCommand parent = (AdminBlueprintCommand) this.getParent();
        File fromFile = new File(parent.getBlueprintsFolder(), from + BlueprintsManager.BLUEPRINT_SUFFIX);

        if (!fromFile.exists())
        {
            user.sendMessage("commands.admin.blueprint.no-such-file");
            return false;
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        // Check if the names are the same
        String from = Util.sanitizeInput(args.get(0));
        String to = Util.sanitizeInput(args.get(1));

        // Check if the 'to' file exists
        File toFile = new File(parent.getBlueprintsFolder(), to + BlueprintsManager.BLUEPRINT_SUFFIX);

        if (toFile.exists())
        {
            // Ask for confirmation to overwrite
            this.askConfirmation(user,
                user.getTranslation("commands.admin.blueprint.file-exists"),
                () -> this.rename(user, from, to, args.get(1)));
        }
        else
        {
            this.askConfirmation(user, () -> this.rename(user, from, to, args.get(1)));
        }

        return true;
    }


    private void rename(User user, String blueprintName, String fileName, String displayName)
    {
        Blueprint blueprint = this.getPlugin().getBlueprintsManager().getBlueprints(this.getAddon()).get(blueprintName);

        this.getPlugin().getBlueprintsManager().renameBlueprint(this.getAddon(), blueprint, fileName, displayName);

        user.sendMessage("commands.admin.blueprint.rename.success",
            "[old]",
            blueprintName,
            TextVariables.NAME,
            blueprint.getName(),
            "[display]",
            blueprint.getDisplayName());
    }
}
