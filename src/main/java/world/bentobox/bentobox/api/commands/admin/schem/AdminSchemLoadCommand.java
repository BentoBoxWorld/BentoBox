package world.bentobox.bentobox.api.commands.admin.schem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.BPClipboardManager;
import world.bentobox.bentobox.util.Util;

public class AdminSchemLoadCommand extends CompositeCommand {

    public AdminSchemLoadCommand(AdminSchemCommand parent) {
        super(parent, "load");
    }

    @Override
    public void setup() {
        setParametersHelp("commands.admin.schem.load.parameters");
        setDescription("commands.admin.schem.load.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.isEmpty() || args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        AdminSchemCommand parent = (AdminSchemCommand) getParent();

        BPClipboardManager bp = new BPClipboardManager(getPlugin(), parent.getBlueprintsFolder());
        if (bp.load(user, args.get(0))) {
            parent.getClipboards().put(user.getUniqueId(), bp.getClipboard());
            return true;
        }

        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        options.add("island");
        options.add("nether-island");
        options.add("end-island");
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";

        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
