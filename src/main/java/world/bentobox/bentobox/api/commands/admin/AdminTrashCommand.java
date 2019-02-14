package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminTrashCommand extends CompositeCommand {

    public AdminTrashCommand(CompositeCommand parent) {
        super(parent, "trash");
    }

    @Override
    public void setup() {
        setPermission("admin.info");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.info.trash.parameters");
        setDescription("commands.admin.info.trash.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Show trash can info for this player
        List<Island> islands = getIslands().getQuarantinedIslandByUser(getWorld(), targetUUID);
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        } else {
            user.sendMessage("commands.admin.info.trash.title");
            islands.forEach(i -> i.showInfo(user));
            return true;
        }
    }
}
