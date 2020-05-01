package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class AdminSwitchtoCommand extends ConfirmableCommand {

    private UUID targetUUID;
    private @NonNull List<Island> islands;

    /**
     * Switch player's island to the numbered one in trash
     * @param parent - admin command
     * @since 1.3.0
     */
    public AdminSwitchtoCommand(CompositeCommand parent) {
        super(parent, "switchto");
        islands = new ArrayList<>();
    }

    @Override
    public void setup() {
        setPermission("admin.switchto");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.switchto.parameters");
        setDescription("commands.admin.switchto.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 2) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        targetUUID = Util.getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Check island number
        islands = getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("commands.admin.trash.no-islands-in-trash");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (NumberUtils.isDigits(args.get(1))) {
            try {
                Integer n = Integer.valueOf(args.get(1));
                if (n < 1 || n > islands.size()) {
                    user.sendMessage("commands.admin.switchto.out-of-range", TextVariables.NUMBER, String.valueOf(islands.size()), TextVariables.LABEL, getTopLabel());
                    return false;
                }
                this.askConfirmation(user, () -> {
                    if (getIslands().switchIsland(getWorld(), targetUUID, islands.get(n -1))) {
                        user.sendMessage("commands.admin.switchto.success");
                    } else {
                        user.sendMessage("commands.admin.switchto.cannot-switch");
                    }
                });
                return true;
            } catch (Exception e) {
                showHelp(this, user);
                return false;
            }
        }
        return true;
    }

}
