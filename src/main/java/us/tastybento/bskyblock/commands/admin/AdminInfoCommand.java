package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class AdminInfoCommand extends CompositeCommand {

    public AdminInfoCommand(CompositeCommand parent) {
        super(parent, "info");
    }

    @Override
    public void setup() {
        setPermission("admin.info");
        setOnlyPlayer(false);
        setParameters("commands.admin.info.parameters");
        setDescription("commands.admin.info.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (args.size() > 1 || (args.isEmpty() && !user.isPlayer())) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // If there are no args, then the player wants info on the island at this location
        if (args.isEmpty()) {
            getPlugin().log("DEBUG: getting info - no args");
            if (!getIslands().getIslandAt(user.getLocation()).map(i -> i.showInfo(getPlugin(), user, getWorld())).orElse(false)) {
                user.sendMessage("commands.admin.info.no-island");
                return false;
            }
            return true;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Show info for this player    
        getIslands().getIsland(getWorld(), targetUUID).showInfo(getPlugin(), user, getWorld());       
        return true;
    }
}
