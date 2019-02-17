package world.bentobox.bentobox.api.commands.admin.schem;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;

public class AdminSchemPasteCommand extends CompositeCommand {

    public AdminSchemPasteCommand(AdminSchemCommand parent) {
        super(parent, "paste");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.paste.parameters");
        setDescription("commands.admin.schem.paste.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminSchemCommand parent = (AdminSchemCommand) getParent();

        Clipboard clipboard = parent.getClipboards().getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), parent.getSchemsFolder()));
        if (clipboard.isFull()) {
            clipboard.pasteClipboard(user.getLocation());
            user.sendMessage("general.success");
            return true;
        }

        user.sendMessage("commands.admin.schem.copy-first");
        return false;
    }
}
