package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminEmptyTrashCommand extends ConfirmableCommand {

    /**
     * Clear trash for player, or all unowned islands in trash
     * @param parent - admin command
     * @since 1.3.0
     */
    public AdminEmptyTrashCommand(CompositeCommand parent) {
        super(parent, "emptytrash");
    }

    @Override
    public void setup() {
        setPermission("admin.trash");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.emptytrash.parameters");
        setDescription("commands.admin.emptytrash.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        UUID targetUUID = args.isEmpty() ? null : getPlayers().getUUID(args.get(0));
        if (!args.isEmpty() && targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Remove trash for this player
        final List<Island> islands = getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            if (args.isEmpty()) {
                user.sendMessage("commands.admin.trash.no-unowned-in-trash");
            } else {
                user.sendMessage("commands.admin.trash.no-islands-in-trash");
            }
            return false;
        } else {
            this.askConfirmation(user, () -> {
                getIslands().deleteQuarantinedIslandByUser(getWorld(), targetUUID);
                user.sendMessage("commands.admin.emptytrash.success");
            });
            return true;
        }
    }
}
