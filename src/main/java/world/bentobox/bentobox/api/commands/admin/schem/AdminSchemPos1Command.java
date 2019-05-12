package world.bentobox.bentobox.api.commands.admin.schem;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.util.Util;

import java.util.List;

public class AdminSchemPos1Command extends CompositeCommand {

    public AdminSchemPos1Command(AdminSchemCommand parent) {
        super(parent, "pos1");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.pos1.parameters");
        setDescription("commands.admin.schem.pos1.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminSchemCommand parent = (AdminSchemCommand) getParent();
        BlueprintClipboard clipboard = parent.getClipboards().computeIfAbsent(user.getUniqueId(), v -> new BlueprintClipboard());

        if (user.getLocation().equals(clipboard.getPos2())) {
            user.sendMessage("commands.admin.schem.set-different-pos");
            return false;
        }
        clipboard.setPos1(user.getLocation());
        user.sendMessage("commands.admin.schem.set-pos1", "[vector]", Util.xyz(clipboard.getPos1().toVector()));
        parent.getClipboards().put(user.getUniqueId(), clipboard);
        parent.showClipboard(user);
        return true;
    }
}
