package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminEmptyTrashCommand extends ConfirmableCommand {

    public AdminEmptyTrashCommand(CompositeCommand parent) {
        super(parent, "emptytrash");
    }

    @Override
    public void setup() {
        setPermission("admin.info.trash");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.info.emptytrash.parameters");
        setDescription("commands.admin.info.emptytrash.description");
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
                user.sendMessage("commands.admin.info.trash.no-unowned-in-trash");
            } else {
                user.sendMessage("general.errors.player-has-no-island");
            }
            return false;
        } else {
            this.askConfirmation(user, () -> {
                getIslands().deleteQuarantinedIslandByUser(getWorld(), targetUUID);
                user.sendMessage("general.success");
            });
            return true;
        }
    }
}
