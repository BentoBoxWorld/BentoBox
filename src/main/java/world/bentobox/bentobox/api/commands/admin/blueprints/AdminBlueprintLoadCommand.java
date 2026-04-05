package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.BlueprintClipboardManager;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.util.Util;

public class AdminBlueprintLoadCommand extends CompositeCommand {

    private static final FilenameFilter BLUEPRINT_FILTER = (File dir, String name) -> name
            .endsWith(BlueprintsManager.BLUEPRINT_SUFFIX) || name.endsWith(BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX);

    public AdminBlueprintLoadCommand(AdminBlueprintCommand parent) {
        super(parent, "load");
    }

    @Override
    public void setup() {
        setPermission("admin.blueprint.load");
        setParametersHelp("commands.admin.blueprint.load.parameters");
        setDescription("commands.admin.blueprint.load.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();

        BlueprintClipboardManager bp = new BlueprintClipboardManager(getPlugin(), parent.getBlueprintsFolder());
        if (bp.load(user, Util.sanitizeInput(args.getFirst()))) {
            parent.getClipboards().put(user.getUniqueId(), bp.getClipboard());
            return true;
        }

        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        AdminBlueprintCommand parent = (AdminBlueprintCommand) getParent();
        File folder = parent.getBlueprintsFolder();
        if (folder.exists()) {
            options = Arrays.stream(Objects.requireNonNull(folder.list(BLUEPRINT_FILTER)))
                    .map(AdminBlueprintLoadCommand::removeBlueprintSuffix)
                    .toList();
        }
        String lastArg = !args.isEmpty() ? args.getLast() : "";

        return Optional.of(Util.tabLimit(options, lastArg));
    }

    private static String removeBlueprintSuffix(String name) {
        if (name.endsWith(BlueprintsManager.BLUEPRINT_SUFFIX)) {
            return name.substring(0, name.length() - BlueprintsManager.BLUEPRINT_SUFFIX.length());
        }
        if (name.endsWith(BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX)) {
            return name.substring(0, name.length() - BlueprintsManager.LEGACY_BLUEPRINT_SUFFIX.length());
        }
        return name;
    }
}
