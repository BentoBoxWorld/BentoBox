package us.tastybento.bskyblock.commands.admin.range;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;

import java.util.List;
import java.util.UUID;

public class AdminRangeResetCommand extends CompositeCommand {

    public AdminRangeResetCommand(CompositeCommand parent) {
        super(parent, "reset");
    }

    @Override
    public void setup() {
        setPermission("admin.range.reset");
        setParameters("commands.admin.range.reset.parameters");
        setDescription("commands.admin.range.reset.description");
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
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getPlugin().getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        // Get island
        Island island = getIslands().getIsland(getWorld(), targetUUID);

        // Reset the protection range
        island.setProtectionRange(getSettings().getIslandProtectionRange());
        // TODO send message?

        return true;
    }
}
