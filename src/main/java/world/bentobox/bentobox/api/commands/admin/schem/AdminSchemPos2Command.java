package world.bentobox.bentobox.api.commands.admin.schem;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;
import world.bentobox.bentobox.util.Util;

public class AdminSchemPos2Command extends CompositeCommand {

    public AdminSchemPos2Command(AdminSchemCommand parent) {
        super(parent, "pos2");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.pos2.parameters");
        setDescription("commands.admin.schem.pos2.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        AdminSchemCommand parent = (AdminSchemCommand) getParent();
        Clipboard clipboard = parent.getClipboards().getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), parent.getSchemsFolder()));

        if (user.getLocation().equals(clipboard.getPos1())) {
            user.sendMessage("commands.admin.schem.set-different-pos");
            return false;
        }
        clipboard.setPos2(user.getLocation());
        user.sendMessage("commands.admin.schem.set-pos2", "[vector]", Util.xyz(user.getLocation().toVector()));
        parent.getClipboards().put(user.getUniqueId(), clipboard);
        parent.showClipboard(user);
        return true;
    }
}
