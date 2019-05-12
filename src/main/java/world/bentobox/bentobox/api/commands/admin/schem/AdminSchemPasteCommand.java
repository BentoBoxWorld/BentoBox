package world.bentobox.bentobox.api.commands.admin.schem;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.blueprints.BlueprintPaster;

import java.util.List;

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
        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());
        if (clipboard.isFull()) {
            new BlueprintPaster(getPlugin(), clipboard, user.getLocation(), () -> user.sendMessage("general.success"));
            user.sendMessage("commands.admin.schem.paste.pasting");
            return true;
        }

        user.sendMessage("commands.admin.schem.copy-first");
        return false;
    }
}
