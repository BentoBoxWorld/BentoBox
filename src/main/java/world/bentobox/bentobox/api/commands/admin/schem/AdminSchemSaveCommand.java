package world.bentobox.bentobox.api.commands.admin.schem;

import java.io.File;
import java.util.List;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BPClipboard;
import world.bentobox.bentobox.managers.BlueprintsManager;

public class AdminSchemSaveCommand extends ConfirmableCommand {

    public AdminSchemSaveCommand(AdminSchemCommand parent) {
        super(parent, "save");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.save.parameters");
        setDescription("commands.admin.schem.save.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        AdminSchemCommand parent = (AdminSchemCommand) getParent();
        BPClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BPClipboard());

        if (clipboard.isFull()) {
            // Check if file exists
            File newFile = new File(parent.getSchemsFolder(), args.get(0) + ".json");
            clipboard.getBp().setName(args.get(0));
            if (newFile.exists()) {
                this.askConfirmation(user, user.getTranslation("commands.admin.schem.file-exists"), () -> {
                    parent.hideClipboard(user);
                    new BlueprintsManager(getPlugin()).saveBlueprint(parent.getSchemsFolder(), clipboard.getBp());
                });
                return false;
            } else {
                parent.hideClipboard(user);
                new BlueprintsManager(getPlugin()).saveBlueprint(parent.getSchemsFolder(), clipboard.getBp());
                return true;
            }
        } else {
            user.sendMessage("commands.admin.schem.copy-first");
            return false;
        }
    }
}
