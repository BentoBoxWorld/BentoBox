package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminPurgeProtectCommand extends CompositeCommand {

    private Island island;

    public AdminPurgeProtectCommand(CompositeCommand parent) {
        super(parent, "protect");
    }

    @Override
    public void setup() {
        inheritPermission();
        setOnlyPlayer(true);
        setDescription("commands.admin.purge.protect.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get island where the player is
        if (!getIslands().getIslandAt(user.getLocation()).map(i -> {
            island = i;
            return true;
        }).orElse(false)) {
            user.sendMessage("commands.admin.purge.protect.move-to-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        island.setPurgeProtected(!island.getPurgeProtected());
        if (island.getPurgeProtected()) {
            user.sendMessage("commands.admin.purge.protect.protecting");
        } else {
            user.sendMessage("commands.admin.purge.protect.unprotecting");
        }
        return true;

    }
}
