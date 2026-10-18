package world.bentobox.bentobox.api.commands.admin.purge;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class AdminPurgeProtectCommand extends CompositeCommand {

    public AdminPurgeProtectCommand(CompositeCommand parent) {
        super(parent, "protect");
    }

    @Override
    public void setup() {
        setPermission("admin.purge.protect");
        setOnlyPlayer(true);
        setDescription("commands.admin.purge.protect.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        return findIsland(user, args) != null;
    }

    private Island findIsland(User user, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return null;
        }
        // Get island where the player is
        Island island = getIslands().getIslandAt(user.getLocation()).orElse(null);
        if (island == null) {
            user.sendMessage("commands.admin.purge.protect.move-to-island");
            return null;
        }
        return island;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Island island = findIsland(user, args);
        if (island == null) return false;
        boolean protectedFromPurge;
        synchronized (island) {
            protectedFromPurge = !island.isPurgeProtected();
            island.setPurgeProtected(protectedFromPurge);
        }
        if (protectedFromPurge) {
            user.sendMessage("commands.admin.purge.protect.protecting");
        } else {
            user.sendMessage("commands.admin.purge.protect.unprotecting");
        }
        return true;

    }
}
