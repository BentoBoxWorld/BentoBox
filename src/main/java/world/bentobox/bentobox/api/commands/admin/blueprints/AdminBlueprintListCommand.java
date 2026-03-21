package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.BlueprintsManager;

public class AdminBlueprintListCommand extends CompositeCommand
{

    public AdminBlueprintListCommand(AdminBlueprintCommand parent)
    {
        super(parent, "list");
    }


    @Override
    public void setup()
    {
        setPermission("admin.blueprint.list");
        this.setDescription("commands.admin.blueprint.list.description");
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        if (!args.isEmpty())
        {
            this.showHelp(this, user);
            return false;
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        File blueprints = new File(this.getAddon().getDataFolder(), BlueprintsManager.FOLDER_NAME);

        if (!blueprints.exists())
        {
            user.sendMessage("commands.admin.blueprint.list.no-blueprints");
            return false;
        }

        FilenameFilter blueprintFilter = (File dir, String name) -> name.endsWith(BlueprintsManager.BLUEPRINT_SUFFIX);

        List<String> blueprintList = Arrays.stream(Objects.requireNonNull(blueprints.list(blueprintFilter))).
                map(name -> name.substring(0, name.length() - BlueprintsManager.BLUEPRINT_SUFFIX.length())).
                toList();

        if (blueprintList.isEmpty())
        {
            user.sendMessage("commands.admin.blueprint.list.no-blueprints");
            return false;
        }

        user.sendMessage("commands.admin.blueprint.list.available-blueprints");
        blueprintList.forEach(user::sendRawMessage);
        return true;
    }
}
