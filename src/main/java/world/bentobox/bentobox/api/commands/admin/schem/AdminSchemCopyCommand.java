package world.bentobox.bentobox.api.commands.admin.schem;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.schems.Clipboard;

public class AdminSchemCopyCommand extends CompositeCommand {

    public AdminSchemCopyCommand(AdminSchemCommand parent) {
        super(parent, "copy");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.copy.parameters");
        setDescription("commands.admin.schem.copy.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            showHelp(this, user);
            return false;
        }

        AdminSchemCommand parent = (AdminSchemCommand) getParent();

        Clipboard clipboard = parent.getClipboards().getOrDefault(user.getUniqueId(), new Clipboard(getPlugin(), parent.getSchemsFolder()));
        boolean copyAir = (args.size() == 1 && args.get(0).equalsIgnoreCase("air"));
        return clipboard.copy(user, copyAir);
    }
}
