package world.bentobox.bentobox.api.commands.admin.schem;

import java.io.File;
import java.util.List;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;

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
        Clipboard clipboard = parent.getClipboards().getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), parent.getSchemsFolder()));

        if (clipboard.isFull()) {
            // Check if file exists
            File newFile = new File(parent.getSchemsFolder(), args.get(0) + ".schem");
            if (newFile.exists()) {
                this.askConfirmation(user, user.getTranslation("commands.admin.schem.file-exists"), () -> {
                    parent.hideClipboard(user);
                    clipboard.save(user, args.get(0));
                });
                return false;
            } else {
                parent.hideClipboard(user);
                return clipboard.save(user, args.get(0));
            }
        } else {
            user.sendMessage("commands.admin.schem.copy-first");
            return false;
        }
    }
}
