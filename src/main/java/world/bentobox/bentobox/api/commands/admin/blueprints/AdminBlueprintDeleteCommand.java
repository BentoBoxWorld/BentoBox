package world.bentobox.bentobox.api.commands.admin.blueprints;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * Command that deletes a Blueprint.
 * @author Poslovitch
 * @since 1.9.0
 */
public class AdminBlueprintDeleteCommand extends ConfirmableCommand {

    public AdminBlueprintDeleteCommand(AdminBlueprintCommand parent) {
        super(parent, "delete", "remove");
    }

    @Override
    public void setup() {
        inheritPermission();
        setParametersHelp("commands.admin.blueprint.delete.parameters");
        setDescription("commands.admin.blueprint.delete.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }

        String blueprintName = args.get(0).toLowerCase(Locale.ENGLISH);

        // Check if blueprint exist
        if (getPlugin().getBlueprintsManager().getBlueprints(getAddon()).containsKey(blueprintName)) {
            askConfirmation(user, user.getTranslation("commands.admin.blueprint.delete.confirmation"), () -> {
                getPlugin().getBlueprintsManager().deleteBlueprint(getAddon(), blueprintName);
                user.sendMessage("commands.admin.blueprint.delete.success", TextVariables.NAME, blueprintName);
            });
            return true;
        } else {
            user.sendMessage("commands.admin.blueprint.delete.no-blueprint", TextVariables.NAME, blueprintName);
            return false;
        }
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        return Optional.of(new LinkedList<>(getPlugin().getBlueprintsManager().getBlueprints(getAddon()).keySet()));
    }
}
