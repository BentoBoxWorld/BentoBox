package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.blueprints.BlueprintPaster;

public class AdminBlueprintPasteCommand extends CompositeCommand {

    public AdminBlueprintPasteCommand(AdminBlueprintCommand parent) {
        super(parent, "paste");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.paste.parameters");
        setDescription("commands.admin.blueprint.paste.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();
        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());
        if (clipboard.isFull()) {
            new BlueprintPaster(getPlugin(), clipboard, user.getLocation()).paste().thenAccept(b -> {
                user.sendMessage("general.success");
                parent.showClipboard(user);
            });
            user.sendMessage("commands.admin.blueprint.paste.pasting");
            return true;
        }

        user.sendMessage("commands.admin.blueprint.copy-first");
        return false;
    }
}
