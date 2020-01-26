package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.util.Util;

public class AdminBlueprintPos1Command extends CompositeCommand {

    public AdminBlueprintPos1Command(AdminBlueprintCommand parent) {
        super(parent, "pos1");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.pos1.parameters");
        setDescription("commands.admin.blueprint.pos1.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();
        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        if (user.getLocation().equals(clipboard.getPos2())) {
            user.sendMessage("commands.admin.blueprint.set-different-pos");
            return false;
        }
        clipboard.setPos1(user.getLocation());
        user.sendMessage("commands.admin.blueprint.set-pos1", "[vector]", Util.xyz(clipboard.getPos1().toVector()));
        parent.getClipboards().put(user.getUniqueId(), clipboard);
        parent.showClipboard(user);
        return true;
    }
}
