package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.util.List;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.managers.BlueprintClipboardManager;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;


/**
 * This method allows to save blueprint from the clipboard.
 */
public class AdminBlueprintSaveCommand extends ConfirmableCommand
{
    public AdminBlueprintSaveCommand(AdminBlueprintCommand parent)
    {
        super(parent, "save");
    }


    @Override
    public void setup()
    {
        this.inheritPermission();
        this.setParametersHelp("commands.admin.blueprint.save.parameters");
        this.setDescription("commands.admin.blueprint.save.description");
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        if (args.size() != 1)
        {
            // Blueprint must have a name.
            this.showHelp(this, user);
            return false;
        }

        BlueprintClipboard clipboard = ((AdminBlueprintCommand) this.getParent()).getClipboards().
            computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        if (!clipboard.isFull())
        {
            // Clipboard is not set up.
            user.sendMessage("commands.admin.blueprint.copy-first");
            return false;
        }

        if (clipboard.getBlueprint().getBedrock() == null)
        {
            // Bedrock is required for all blueprints.
            user.sendMessage("commands.admin.blueprint.bedrock-required");
            return false;
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) this.getParent();
        BlueprintClipboard clipboard = parent.getClipboards().
            computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        String fileName = Util.sanitizeInput(args.get(0));

        // Check if file exists
        File newFile = new File(parent.getBlueprintsFolder(), fileName + BlueprintsManager.BLUEPRINT_SUFFIX);

        if (newFile.exists())
        {
            this.askConfirmation(user,
                user.getTranslation("commands.admin.blueprint.file-exists"),
                () -> this.hideAndSave(user, parent, clipboard, fileName, args.get(0)));
            return false;
        }

        return this.hideAndSave(user, parent, clipboard, fileName, args.get(0));
    }


    /**
     * This method saves given blueprint.
     * @param user User that triggers blueprint save.
     * @param parent Parent command that contains clipboard.
     * @param clipboard Active clipboard.
     * @param name Filename for the blueprint
     * @param displayName Display name for the blueprint.
     * @return {@code true} if blueprint is saved, {@code false} otherwise.
     */
    private boolean hideAndSave(User user,
        AdminBlueprintCommand parent,
        BlueprintClipboard clipboard,
        String name,
        String displayName)
    {
        parent.hideClipboard(user);
        boolean result = new BlueprintClipboardManager(this.getPlugin(),
            parent.getBlueprintsFolder(), clipboard).
            save(user, name, displayName);

        if (result && clipboard.isFull())
        {
            this.getPlugin().getBlueprintsManager().addBlueprint(this.getAddon(), clipboard.getBlueprint());
        }

        return result;
    }
}
