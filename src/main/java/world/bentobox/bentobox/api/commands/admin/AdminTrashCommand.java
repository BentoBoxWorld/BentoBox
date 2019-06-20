package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminTrashCommand extends CompositeCommand {

    /**
     * A command for viewing islands in the database trash
     * @param parent - admin command
     * @since 1.3.0
     */
    public AdminTrashCommand(CompositeCommand parent) {
        super(parent, "trash");
    }

    @Override
    public void setup() {
        setPermission("admin.trash");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.trash.parameters");
        setDescription("commands.admin.trash.description");
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
        // Show trash can info for this player
        List<Island> islands = getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            if (args.isEmpty()) {
                user.sendMessage("commands.admin.trash.no-unowned-in-trash");
            } else {
                user.sendMessage("commands.admin.trash.no-islands-in-trash");
            }
            return false;
        } else {
            if (targetUUID == null) {
                showTrash(user, islands);
            } else {
                getIslands().getQuarantineCache().values().forEach(v -> showTrash(user, v));
            }
            return true;
        }
    }

    private void showTrash(User user, List<Island> islands) {
        user.sendMessage("commands.admin.trash.title");
        for (int i = 0; i < islands.size(); i++) {
            user.sendMessage("commands.admin.trash.count", TextVariables.NUMBER, String.valueOf(i+1));
            islands.get(i).showInfo(user);
        }
        user.sendMessage("commands.admin.trash.use-switch", TextVariables.LABEL, getTopLabel());
        user.sendMessage("commands.admin.trash.use-emptytrash", TextVariables.LABEL, getTopLabel());

    }
}
